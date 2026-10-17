/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.weather;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import dev.jstech.core.data.DataRegistry;
import dev.jstech.core.dimension.Winds;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.DimensionState;
import dev.jstech.core.world.event.WorldEvent;
import dev.jstech.core.world.event.WorldEventBalance;
import dev.jstech.core.world.event.WorldEventType;
import dev.jstech.core.world.event.WorldEvents;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The kinds of weather of the worlds ({@link WeatherKind}), registered by any mod in code or by a data pack under
 * {@code data/<namespace>/jstech/weathers/}: each comes with its world event, so it falls where, when and for as long
 * as the event says. Once a second the weathers under way in each dimension are sent to the players there, whose game
 * draws what falls and closes the fog in; what lives out in one near a player is harmed and given its effect, its
 * lightning strikes near the players out in it, and the wind blows as hard as it says. A server that makes an event
 * harmless keeps its weather harmless.
 *
 * <pre>{@code
 * public static final ResourceLocation DUST_STORM = Weathers.weather(rl("dust_storm")).falls(Fall.DUST, 0xC08A60, 0.9)
 *         .fog(0xB07A50, 0.7).wind(4).declare();     // with the world event rl("dust_storm")
 * }</pre>
 */
public final class Weathers {

    /** The kinds the data packs write, by id. */
    public static final DataRegistry<WeatherKind> FILES = DataRegistry.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "weathers"), "jstech/weathers", WeatherKind.CODEC)
            .synced().register();

    private static final DimensionState<Spots> ACTIVE = CoreState.builder(
                    ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "weathers"), Spots.CODEC, Spots.CLEAR)
            .synced(ByteBufCodecs.fromCodec(Spots.CODEC)).dimension();
    private static final Map<ResourceLocation, WeatherKind> DECLARED = new LinkedHashMap<>();
    private static final int EVERY = 20;
    private static final int SECOND = 20;
    private static final double MINUTE_SECONDS = 60.0;
    private static final int STRIKES_WITHIN = 24;
    /* How far round each player a weather acts on what lives, in blocks. */
    private static final double NEAR = 48;

    private Weathers() {
    }

    /** Registers the weathers under way and hands the game's bus the second's work, once, from the Core. */
    public static void listen() {
        CoreStates.register(ACTIVE);
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % EVERY == 0) {
                tick(level);
            }
        });
        Winds.factor((level, at) -> windAt(level, BlockPos.containing(at)));
    }

    /** Starts declaring the kind of weather {@code id}, which its world event of the same id brings. */
    public static WeatherBuilder weather(final ResourceLocation id) {
        return new WeatherBuilder(id);
    }

    /** Every kind on {@code level}'s side, by id: the declared ones, then the files, which replace or add. */
    public static Map<ResourceLocation, WeatherKind> all(final Level level) {
        final Map<ResourceLocation, WeatherKind> byId;
        synchronized (DECLARED) {
            byId = new TreeMap<>(DECLARED);
        }
        byId.putAll(FILES.entries(level));
        return byId;
    }

    /**
     * The weathers under way at {@code pos} of {@code level}: as the server worked them out last second, on either
     * side, the player's game reading what it was sent for the dimension it stands in.
     */
    public static List<WeatherKind> at(final Level level, final BlockPos pos) {
        final List<Spot> spots = spots(level);
        if (spots.isEmpty()) {
            return List.of();
        }
        final List<WeatherKind> here = new ArrayList<>();
        for (final Spot spot : spots) {
            final WeatherKind kind = kind(level, spot.weather());
            if (kind != null && spot.covers(pos)) {
                here.add(kind);
            }
        }
        return here;
    }

    /**
     * How hard the weathers at {@code pos} make the wind blow, as a share of the wind without them; asked for every
     * particle the wind moves, so it costs nothing while no weather is under way.
     */
    public static double windAt(final Level level, final BlockPos pos) {
        double wind = 1;
        for (final Spot spot : spots(level)) {
            final WeatherKind kind = kind(level, spot.weather());
            if (kind != null && spot.covers(pos)) {
                wind *= kind.wind();
            }
        }
        return wind;
    }

    /** The kind {@code id} on {@code level}'s side: a file's, else the declared one; none for neither. */
    public static @Nullable WeatherKind kind(final Level level, final ResourceLocation id) {
        final WeatherKind written = FILES.entries(level).get(id);
        if (written != null) {
            return written;
        }
        synchronized (DECLARED) {
            return DECLARED.get(id);
        }
    }

    /* The weathers under way in {@code level}, as the server last worked them out. */
    private static List<Spot> spots(final Level level) {
        return (level instanceof ServerLevel server ? ACTIVE.get(server) : ACTIVE.client()).spots();
    }

    /** Whether {@code pos} is out in the weather: under the open sky, or anywhere for one no roof keeps off. */
    public static boolean outIn(final Level level, final BlockPos pos, final WeatherKind kind) {
        return !kind.sheltered() || level.canSeeSky(pos);
    }

    /* Finds what is under way, sends it on when it changed, and has it act on whoever is out in it. */
    private static void tick(final ServerLevel level) {
        final Map<ResourceLocation, WeatherKind> kinds = all(level);
        final List<Spot> spots = new ArrayList<>();
        if (!kinds.isEmpty()) {
            final Map<ResourceLocation, WorldEventType> events = WorldEvents.all(level);
            for (final WorldEvent event : WorldEvents.active(level)) {
                final WorldEventType type = events.get(event.type());
                for (final Map.Entry<ResourceLocation, WeatherKind> kind : kinds.entrySet()) {
                    if (type != null && kind.getValue().event().equals(event.type())) {
                        spots.add(new Spot(kind.getKey(), event.centre(), type.radius()));
                    }
                }
            }
        }
        final Spots now = new Spots(spots);
        if (!now.equals(ACTIVE.get(level))) {
            ACTIVE.set(level, now);
        }
        final Set<LivingEntity> bodies = new LinkedHashSet<>();
        for (final Spot spot : spots) {
            final WeatherKind kind = kinds.get(spot.weather());
            if (kind != null && !WorldEventBalance.harmless(kind.event())) {
                act(level, spot, kind, bodies);
            }
        }
        for (final LivingEntity body : bodies) {
            weather(level, body);
        }
    }

    /**
     * Gives {@code body} a second of every weather out in which it stands: its harm, when its second comes, and its
     * effect, renewed; nothing for a player in creative or looking on, and nothing of a weather the server makes
     * harmless.
     */
    public static void weather(final ServerLevel level, final LivingEntity body) {
        if (!body.isAlive() || body instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        final BlockPos head = BlockPos.containing(body.getEyePosition());
        for (final WeatherKind kind : at(level, head)) {
            if (WorldEventBalance.harmless(kind.event()) || !outIn(level, head, kind)) {
                continue;
            }
            kind.harm().ifPresent(harm -> {
                if (body.tickCount / SECOND % harm.every() == 0) {
                    body.hurt(damage(level, harm.damageType()), harm.amount());
                }
            });
            kind.effect().flatMap(effect -> BuiltInRegistries.MOB_EFFECT.getHolder(effect.effect())
                            .map(holder -> new MobEffectInstance(holder, effect.seconds() * SECOND,
                                    effect.amplifier())))
                    .ifPresent(body::addEffect);
        }
    }

    /*
     * Gathers what lives near the players a weather covers (only the loaded world near them is looked at), for it to
     * act on, and has its lightning strike near one of the players out in it.
     */
    private static void act(final ServerLevel level, final Spot spot, final WeatherKind kind,
                            final Set<LivingEntity> bodies) {
        final List<ServerPlayer> out = new ArrayList<>();
        for (final ServerPlayer player : level.players()) {
            final BlockPos head = BlockPos.containing(player.getEyePosition());
            if (player.isSpectator() || !spot.covers(head)) {
                continue;
            }
            if (outIn(level, head, kind)) {
                out.add(player);
            }
            if (kind.harm().isPresent() || kind.effect().isPresent()) {
                bodies.addAll(level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(NEAR),
                        living -> spot.covers(living.blockPosition())));
            }
        }
        if (!out.isEmpty() && kind.lightning() > 0 && level.random.nextDouble() < kind.lightning() / MINUTE_SECONDS) {
            strike(level, out.get(level.random.nextInt(out.size())).blockPosition());
        }
    }

    /* Lightning on the ground somewhere near {@code near}. */
    private static void strike(final ServerLevel level, final BlockPos near) {
        final BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, near.offset(
                level.random.nextInt(STRIKES_WITHIN * 2 + 1) - STRIKES_WITHIN, 0,
                level.random.nextInt(STRIKES_WITHIN * 2 + 1) - STRIKES_WITHIN));
        final LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(Vec3.atBottomCenterOf(at));
            level.addFreshEntity(bolt);
        }
    }

    /* The harm of the damage type {@code id}, or the game's generic harm when no data pack holds it. */
    private static DamageSource damage(final ServerLevel level, final ResourceLocation id) {
        final Optional<Holder.Reference<DamageType>> type = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, id));
        return type.<DamageSource>map(DamageSource::new).orElseGet(() -> level.damageSources().generic());
    }

    /**
     * Where a weather is under way in a dimension.
     *
     * @param weather the kind of weather
     * @param centre  the middle of what it covers, for one round a place; none for the whole dimension
     * @param radius  how far round its middle it reaches, in blocks
     */
    public record Spot(ResourceLocation weather, Optional<BlockPos> centre, int radius) {

        public static final Codec<Spot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("weather").forGetter(Spot::weather),
                BlockPos.CODEC.optionalFieldOf("centre").forGetter(Spot::centre),
                Codec.INT.optionalFieldOf("radius", 0).forGetter(Spot::radius))
                .apply(instance, Spot::new));

        /** Whether it covers {@code pos}. */
        public boolean covers(final BlockPos pos) {
            if (this.centre.isEmpty() || this.radius <= 0) {
                return true;
            }
            final long dx = pos.getX() - this.centre.get().getX();
            final long dz = pos.getZ() - this.centre.get().getZ();
            return dx * dx + dz * dz <= (long) this.radius * this.radius;
        }
    }

    /**
     * The weathers under way in a dimension.
     *
     * @param spots each one, where it is
     */
    public record Spots(List<Spot> spots) {

        public static final Spots CLEAR = new Spots(List.of());
        public static final Codec<Spots> CODEC = Spot.CODEC.listOf().xmap(Spots::new, Spots::spots);

        public Spots {
            spots = List.copyOf(spots);
        }
    }

    /** A kind of weather being declared. */
    public static final class WeatherBuilder {

        private final ResourceLocation id;
        private ResourceLocation event;
        private WeatherKind.Fall falls = WeatherKind.Fall.NONE;
        private int colour = 0xFFFFFF;
        private double density = 0.5;
        private Optional<Integer> fogColour = Optional.empty();
        private double sight;
        private double wind = 1;
        private Optional<WeatherKind.Harm> harm = Optional.empty();
        private Optional<WeatherKind.Effect> effect = Optional.empty();
        private double lightning;
        private boolean sheltered = true;

        private WeatherBuilder(final ResourceLocation id) {
            this.id = id;
            this.event = id;
        }

        /** Comes with the world event {@code brought}, not the one of its own id. */
        public WeatherBuilder brought(final ResourceLocation brought) {
            this.event = brought;
            return this;
        }

        /** What falls in it, in what colour ({@code 0xRRGGBB}), and how thick, 0 to 1. */
        public WeatherBuilder falls(final WeatherKind.Fall what, final int inColour, final double thick) {
            this.falls = what;
            this.colour = inColour;
            this.density = Math.clamp(thick, 0.0, 1.0);
            return this;
        }

        /** Its fog's colour, and how much it closes the view in, 0 to 1. */
        public WeatherBuilder fog(final int inColour, final double closes) {
            this.fogColour = Optional.of(inColour);
            this.sight = Math.clamp(closes, 0.0, 1.0);
            return this;
        }

        /** How hard it makes the wind blow, as a share of the wind without it. */
        public WeatherBuilder wind(final double share) {
            this.wind = Math.clamp(share, 0.0, WeatherKind.MOST_WIND);
            return this;
        }

        /** Harms a body out in it: {@code amount} half hearts of the damage type {@code type} every {@code every} s. */
        public WeatherBuilder harms(final ResourceLocation type, final float amount, final int every) {
            this.harm = Optional.of(new WeatherKind.Harm(type, amount, Math.max(1, every)));
            return this;
        }

        /** Gives a body out in it the effect {@code effect}, renewed for {@code seconds} while it stays out. */
        public WeatherBuilder gives(final ResourceLocation given, final int amplifier, final int seconds) {
            this.effect = Optional.of(new WeatherKind.Effect(given, amplifier, Math.max(1, seconds)));
            return this;
        }

        /** Its lightning strikes {@code perMinute} times a minute near someone out in it. */
        public WeatherBuilder lightning(final double perMinute) {
            this.lightning = Math.clamp(perMinute, 0.0, WeatherKind.MOST_LIGHTNING);
            return this;
        }

        /** No roof keeps it off: it acts indoors too. */
        public WeatherBuilder reachesIndoors() {
            this.sheltered = false;
            return this;
        }

        /** Declares it; its id. */
        public ResourceLocation declare() {
            synchronized (DECLARED) {
                DECLARED.put(this.id, new WeatherKind(this.event, this.falls, this.colour, this.density,
                        this.fogColour, this.sight, this.wind, this.harm, this.effect, this.lightning,
                        this.sheltered));
            }
            return this.id;
        }
    }
}
