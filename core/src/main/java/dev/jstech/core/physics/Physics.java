/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.physics;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.region.Box;
import dev.jstech.core.registry.CoreAttachments;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.DimensionState;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Places, dimensions and bodies with laws of their own ({@link Laws}). A dimension's rules may hold laws for the whole
 * of it; a mod puts an area of its own in a dimension ({@link #add}), kept with the world and sent to the players
 * there, whose games move them by it; an entity may carry laws with it wherever it goes ({@link #own}). Where several
 * hold, an entity's own come first, then the highest area's, then the dimension's.
 *
 * <ul>
 *   <li>Gravity pulls whatever falls (players on their own game, everything else on the server, items, falling blocks
 *   and arrows too, which the game's own gravity leaves out of a dimension's pull).</li>
 *   <li>Time runs slower or faster for what lives and moves there, every entity but players, and for the series' own
 *   machines; never for another mod's blocks.</li>
 *   <li>Light, space and matter: {@link SpaceLaws} and {@link MatterLaws}, and what the player sees on their game.</li>
 * </ul>
 *
 * <pre>{@code
 * Physics.add(level, new PhysicsArea("lab", Box.between(0, 60, 0, 15, 75, 15), new Laws(
 *         Optional.of(new Pull.Scaled(0)), Optional.of(0.5), ...), 0));    // weightless, time at half speed
 * Physics.mass(level, "singularity", centre, 2.0, 4.0);                     // space curved round a micro black hole
 * }</pre>
 */
public final class Physics {

    private static final DimensionState<Areas> AREAS = CoreState.builder(
                    ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "physics_areas"), Areas.CODEC, Areas.NONE)
            .synced(ByteBufCodecs.fromCodec(Areas.CODEC)).dimension();
    private static final String VERSION = "1";
    /* What a mass does round it: time slowed to this share at its heart, and the view bent this hard. */
    private static final double MASS_TIME = 0.5;
    private static final double MASS_LENS = 0.8;
    /* How far round its radius a mass's pull and its other laws reach, in radii. */
    private static final int MASS_REACH = 4;
    private static final Map<Entity, Double> CARRIED = new WeakHashMap<>();
    private static final Map<Entity, Integer> EXTRA = new WeakHashMap<>();
    private static volatile Laws ownOnClient = Laws.NONE;
    private static Field aboveGround;

    private Physics() {
    }

    /** Registers the areas, the payload and what moves things, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        CoreStates.register(AREAS);
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(VERSION)
                .playToClient(OwnLawsPayload.TYPE, OwnLawsPayload.STREAM_CODEC, Physics::onOwnLaws));
        NeoForge.EVENT_BUS.addListener(EntityTickEvent.Pre.class, Physics::beforeTick);
        NeoForge.EVENT_BUS.addListener(EntityTickEvent.Post.class, Physics::afterTick);
        SpaceLaws.listen();
        MatterLaws.listen();
    }

    /** Puts {@code area} in {@code level}, in place of any of the same id. */
    public static void add(final ServerLevel level, final PhysicsArea area) {
        AREAS.update(level, areas -> areas.with(area));
    }

    /** Takes the area {@code id} out of {@code level}; whether there was one. */
    public static boolean remove(final ServerLevel level, final String id) {
        final boolean had = AREAS.get(level).byId().containsKey(id);
        if (had) {
            AREAS.update(level, areas -> areas.without(id));
        }
        return had;
    }

    /**
     * Space curved round a mass at {@code centre} of {@code level}, as a micro black hole curves it: pulling at
     * {@code g} at {@code radius} and harder within, time slowed near it, and the view bent round it, all within four
     * of its radius.
     */
    public static PhysicsArea mass(final ServerLevel level, final String id, final Vec3 centre, final double g,
                                   final double radius) {
        final int reach = (int) Math.ceil(radius * MASS_REACH);
        final Box box = Box.around((int) Math.floor(centre.x), (int) Math.floor(centre.y), (int) Math.floor(centre.z),
                reach);
        final PhysicsArea area = new PhysicsArea(id, box, new Laws(Optional.of(new Pull.Centre(centre.x, centre.y,
                centre.z, g, radius)), Optional.of(MASS_TIME), Optional.of(new Laws.Light(0xFFFFFF, 0, MASS_LENS, 0,
                0, 0)), Optional.empty(), Optional.empty()), Integer.MAX_VALUE);
        add(level, area);
        return area;
    }

    /** The areas of {@code level}: the server's on the server, those it sent on a player's game. */
    public static List<PhysicsArea> areas(final Level level) {
        return level instanceof ServerLevel server ? AREAS.get(server).sorted() : AREAS.client().sorted();
    }

    /** The laws holding at {@code at} of {@code level}: the highest area's over the dimension's. */
    public static Laws lawsAt(final Level level, final Vec3 at) {
        Laws laws = DimensionRulesData.of(level).laws().orElse(Laws.NONE);
        for (final PhysicsArea area : areas(level)) {
            if (area.contains(at)) {
                laws = laws.under(area.laws());
            }
        }
        return laws;
    }

    /** The laws {@code entity} is under: those it carries over those where it stands. */
    public static Laws lawsOf(final Entity entity) {
        final Laws where = lawsAt(entity.level(), entity.position());
        final Laws own;
        if (entity.level().isClientSide()) {
            own = entity instanceof Player player && player.isLocalPlayer() ? ownOnClient : Laws.NONE;
        } else {
            own = entity.hasData(CoreAttachments.OWN_LAWS.get()) ? entity.getData(CoreAttachments.OWN_LAWS.get())
                    : Laws.NONE;
        }
        return own.none() ? where : where.under(own);
    }

    /** Has {@code entity} carry {@code laws} wherever it goes; {@link Laws#NONE} to carry none. */
    public static void own(final Entity entity, final Laws laws) {
        entity.setData(CoreAttachments.OWN_LAWS.get(), laws);
        if (entity instanceof ServerPlayer player && player.connection != null
                && player.connection.hasChannel(OwnLawsPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, new OwnLawsPayload(laws));
        }
    }

    /**
     * Pulls {@code entity} one tick as {@code laws} and its dimension say: what the game's own pull does not, its fall
     * scaled, turned along a direction or towards a point; nothing for what does not fall.
     */
    public static void pull(final Entity entity, final Laws laws) {
        if (entity.isNoGravity() || entity instanceof Player player && player.getAbilities().flying) {
            return;
        }
        final double dimension = DimensionRulesData.of(entity.level()).gravity();
        final boolean living = entity instanceof LivingEntity;
        final double falling = entity.getGravity();
        final double fall = living ? (dimension > 0 ? falling / dimension
                : ((LivingEntity) entity).getAttributeBaseValue(Attributes.GRAVITY)) : falling;
        Pull pull = laws.gravity().orElse(null);
        if (pull == null) {
            // The game pulls the living by the dimension already; the rest only as hard as the overworld does.
            if (living || dimension == 1.0) {
                return;
            }
            pull = new Pull.Scaled(dimension);
        }
        if (fall <= 0) {
            return;
        }
        final double[] change = switch (pull) {
            case Pull.Scaled scaled -> new double[] {0, PhysicsMath.scaled(scaled.g(), fall, falling), 0};
            case Pull.Towards towards -> PhysicsMath.towards(towards.x(), towards.y(), towards.z(), towards.g(), fall,
                    falling);
            case Pull.Centre centre -> PhysicsMath.centre(entity.getX(), entity.getY() + entity.getBbHeight() / 2,
                    entity.getZ(), centre.x(), centre.y(), centre.z(), centre.g(), centre.radius(), fall, falling);
        };
        entity.addDeltaMovement(new Vec3(change[0], change[1], change[2]));
    }

    /*
     * Before a tick: whether the entity ticks at all at its rate of time (on the server, every entity but players), and
     * its pull, on the side that moves it.
     */
    private static void beforeTick(final EntityTickEvent.Pre event) {
        final Entity entity = event.getEntity();
        final Level level = entity.level();
        // Each side moves what it owns: the player's game its own player, the server every other entity.
        if (level.isClientSide() ? entity instanceof Player player && !player.isLocalPlayer()
                : entity instanceof Player && !(entity instanceof ServerPlayer)) {
            return;
        }
        final Laws laws = lawsOf(entity);
        if (!level.isClientSide()) {
            if (entity instanceof ServerPlayer player) {
                keepAloft(player, laws);
                return;
            }
            if (!timed(entity, laws.timeRate())) {
                event.setCanceled(true);
                return;
            }
        }
        pull(entity, laws);
    }

    /* After a tick: the further ticks of an entity whose time runs faster, each pulled as the first. */
    private static void afterTick(final EntityTickEvent.Post event) {
        final Entity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        final Integer more = EXTRA.remove(entity);
        for (int i = 0; more != null && i < more && !entity.isRemoved(); i++) {
            pull(entity, lawsOf(entity));
            entity.tick();
        }
    }

    /* Whether an entity ticks this tick at {@code rate}, keeping what is left of a tick for the next. */
    private static boolean timed(final Entity entity, final double rate) {
        if (rate == 1.0) {
            CARRIED.remove(entity);
            return true;
        }
        final double[] ticks = PhysicsMath.ticks(rate, CARRIED.getOrDefault(entity, 0.0));
        CARRIED.put(entity, ticks[1]);
        final int times = (int) ticks[0];
        if (times > 1) {
            EXTRA.put(entity, times - 1);
        }
        return times > 0;
    }

    /*
     * A player floating under a law that lets them float is not a cheat: the server, which sends away a player off the
     * ground too long, is told they stood on something.
     */
    private static void keepAloft(final ServerPlayer player, final Laws laws) {
        final boolean floats = laws.gravity().map(pull -> !(pull instanceof Pull.Scaled scaled) || scaled.g() < 1)
                .orElse(false);
        if (!floats || player.connection == null) {
            return;
        }
        try {
            if (aboveGround == null) {
                aboveGround = ObfuscationReflectionHelper.findField(ServerGamePacketListenerImpl.class,
                        "aboveGroundTickCount");
            }
            aboveGround.setInt(player.connection, 0);
        } catch (final RuntimeException | IllegalAccessException refused) {
            // Without it a floating player may be sent away for flying; the law still holds.
            aboveGround = null;
        }
    }

    private static void onOwnLaws(final OwnLawsPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> ownOnClient = payload.laws());
    }

    /**
     * The areas of a dimension.
     *
     * @param byId   each area, by its id
     * @param sorted every area, the lowest ranked first, so the highest's laws are laid over the rest; worked out
     *               once, as it is read for every entity every tick
     */
    public record Areas(Map<String, PhysicsArea> byId, List<PhysicsArea> sorted) {

        public static final Areas NONE = new Areas(Map.of());
        public static final Codec<Areas> CODEC = Codec.unboundedMap(Codec.STRING, PhysicsArea.CODEC)
                .xmap(Areas::new, Areas::byId);

        public Areas {
            byId = Map.copyOf(byId);
            sorted = List.copyOf(sorted);
        }

        public Areas(final Map<String, PhysicsArea> byId) {
            this(byId, rank(byId));
        }

        /** These areas with {@code area} in place of any of its id. */
        Areas with(final PhysicsArea area) {
            final Map<String, PhysicsArea> changed = new LinkedHashMap<>(this.byId);
            changed.put(area.id(), area);
            return new Areas(changed);
        }

        /** These areas without the one of {@code id}. */
        Areas without(final String id) {
            final Map<String, PhysicsArea> changed = new LinkedHashMap<>(this.byId);
            changed.remove(id);
            return new Areas(changed);
        }

        private static List<PhysicsArea> rank(final Map<String, PhysicsArea> byId) {
            final List<PhysicsArea> all = new ArrayList<>(byId.values());
            all.sort(Comparator.comparingInt(PhysicsArea::priority));
            return all;
        }
    }
}
