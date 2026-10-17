/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.JsCore;
import dev.jstech.core.air.AirMix;
import dev.jstech.core.room.GasMath;
import dev.jstech.core.room.RoomHazards;
import dev.jstech.core.room.SealedRooms;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

/**
 * What a dimension's rules do: its pull on every living thing in it, its weather, and the air, heat and cold a player
 * not made for them suffers, each a {@link DimensionHazardEvent} another mod may cancel. The hazards are kinds of the
 * hazard kit ({@link DimensionHazards}), weighed once a second with their harm dealt each tick, so a choking player's
 * air runs down as smoothly as under water.
 */
public final class DimensionEffects {

    private static final Logger LOGGER = CoreLogs.DIMENSIONS.logger();
    /** What a dimension's pull is kept under on every living thing, so it is taken off when the thing leaves. */
    private static final ResourceLocation GRAVITY = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "dimension_gravity");
    /* A player out of water gets back four of air a tick; five taken leaves one a tick lost, as under water. */
    private static final int AIR_TAKEN = 5;
    private static final int DROWNED_AT = -20;
    private static final float DROWNING_DAMAGE = 2.0F;
    /* A player out of powder snow thaws two a tick; three taken leaves one a tick gained, as in it. */
    private static final int COLD_GIVEN = 3;
    private static final float BURN_SECONDS = 3.0F;

    /** The hazards each player stands in, as last weighed, and in which tick. */
    private static final Map<UUID, Weighed> WEIGHED = new ConcurrentHashMap<>();

    private DimensionEffects() {
    }

    /**
     * Puts {@code entity} under the pull of the dimension it is in, taking off the pull of the one it left.
     */
    public static void applyGravity(final LivingEntity entity) {
        final AttributeInstance gravity = entity.getAttribute(Attributes.GRAVITY);
        if (gravity == null) {
            return;
        }
        final double pull = DimensionRulesData.of(entity.level()).gravity();
        gravity.removeModifier(GRAVITY);
        if (pull != 1.0) {
            gravity.addTransientModifier(new AttributeModifier(GRAVITY, pull - 1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /**
     * Weighs the hazards {@code player} stands in, asking the other mods through the event: what a test calls to see
     * the rules at work without waiting a second.
     */
    public static Set<DimensionHazardEvent.Hazard> weigh(final Player player) {
        final Set<DimensionHazardEvent.Hazard> hazards = EnumSet.noneOf(DimensionHazardEvent.Hazard.class);
        if (player.isCreative() || player.isSpectator()) {
            return hazards;
        }
        final DimensionRules rules = DimensionRulesData.of(player.level());
        // A sealed room's air is breathed in it; out of one, the place's, at the height of the eyes.
        final Climate here = SealedRooms.climateAt(player.level(), BlockPos.containing(player.getEyePosition()));
        final AirMix air = SealedRooms.airOf(here);
        final double carbon = air.share(AirMix.CARBON_DIOXIDE);
        consider(player, rules, !here.breathable() || GasMath.stifling(carbon), DimensionHazardEvent.Hazard.AIR,
                hazards);
        consider(player, rules, here.scorching(), DimensionHazardEvent.Hazard.HEAT, hazards);
        consider(player, rules, here.freezing(), DimensionHazardEvent.Hazard.COLD, hazards);
        consider(player, rules, GasMath.headache(carbon), DimensionHazardEvent.Hazard.CARBON_DIOXIDE, hazards);
        consider(player, rules, GasMath.poisons(air.partial(AirMix.OXYGEN, here.pressure())),
                DimensionHazardEvent.Hazard.OXYGEN, hazards);
        return hazards;
    }

    /**
     * The hazards {@code player} stands in this tick, weighed at most once a tick however many ask: what the hazard
     * kit's kinds of a place's air and climate build up from.
     */
    public static Set<DimensionHazardEvent.Hazard> weighed(final Player player) {
        final long now = player.level().getGameTime();
        final Weighed last = WEIGHED.get(player.getUUID());
        if (last != null && last.tick() == now) {
            return last.hazards();
        }
        final Set<DimensionHazardEvent.Hazard> hazards = weigh(player);
        WEIGHED.put(player.getUUID(), new Weighed(now, hazards));
        return hazards;
    }

    /** Deals one tick of {@code hazards} to {@code player}. */
    public static void suffer(final Player player, final Set<DimensionHazardEvent.Hazard> hazards) {
        hazards.forEach(hazard -> suffer(player, hazard));
    }

    /** Deals one tick of {@code hazard} to {@code player}: the air runs out, the cold freezes, the air poisons. */
    public static void suffer(final Player player, final DimensionHazardEvent.Hazard hazard) {
        switch (hazard) {
            case AIR -> {
                final int air = player.getAirSupply() - AIR_TAKEN;
                if (air <= DROWNED_AT) {
                    player.setAirSupply(0);
                    player.hurt(player.damageSources().drown(), DROWNING_DAMAGE);
                } else {
                    player.setAirSupply(air);
                }
            }
            case COLD -> {
                if (player.canFreeze()) {
                    player.setTicksFrozen(Math.min(player.getTicksRequiredToFreeze(),
                            player.getTicksFrozen() + COLD_GIVEN));
                }
            }
            case CARBON_DIOXIDE -> {
                if (!player.hasEffect(MobEffects.CONFUSION)) {
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, RoomHazards.NAUSEA_TICKS));
                }
            }
            case OXYGEN -> {
                if (player.tickCount % RoomHazards.POISONS_EVERY == 0) {
                    player.hurt(RoomHazards.oxygen(player.level()), RoomHazards.POISON_DAMAGE);
                }
            }
            case HEAT -> {
                // The heat sets the player alight once a second, when it is weighed.
            }
        }
    }

    /** Sets {@code player} alight for a few seconds, as the heat of a scorching place does once a second. */
    static void ignite(final Player player) {
        player.igniteForSeconds(BURN_SECONDS);
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor. */
    public static void listen() {
        DimensionHazards.declare();
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, DimensionEffects::onJoinLevel);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, DimensionEffects::onLoggedOut);
        NeoForge.EVENT_BUS.addListener(LevelEvent.Load.class, DimensionEffects::onLevelLoad);
    }

    public static void onJoinLevel(final EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            applyGravity(living);
            WEIGHED.remove(living.getUUID());
        }
    }

    public static void onLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        WEIGHED.remove(event.getEntity().getUUID());
    }

    /**
     * A dimension the datapacks made reads the overworld's weather and time; the Core hands it weather and a clock of
     * its own, which its rules may hold or set going, and gives the clock back the time it had when the world was
     * saved. One the Core made at runtime already has them.
     */
    public static void onLevelLoad(final LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() == Level.OVERWORLD) {
            return;
        }
        if (level.getLevelData() instanceof DerivedLevelData && !(level.getLevelData() instanceof RuledLevelData)) {
            try {
                // The level reads its time and weather through one field, and sets its time and tells the players how
                // fast it moves through the other: both must be the Core's.
                final RuledLevelData ruled = new RuledLevelData(level.getServer().getWorldData(), level.dimension());
                ObfuscationReflectionHelper.findField(Level.class, "levelData").set(level, ruled);
                ObfuscationReflectionHelper.findField(ServerLevel.class, "serverLevelData").set(level, ruled);
            } catch (final RuntimeException | IllegalAccessException refused) {
                LOGGER.warn("The weather and time of {} stay the overworld's: {}", level.dimension().location(),
                        refused.toString());
            }
        }
        if (level.getLevelData() instanceof RuledLevelData ruled) {
            DayClocks.restore(level, ruled);
        }
    }

    /** The rules were read again: every living thing feels the pull of where it stands now. */
    static void rulesChanged() {
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            for (final ServerLevel level : server.getAllLevels()) {
                for (final Entity entity : level.getAllEntities()) {
                    if (entity instanceof LivingEntity living) {
                        applyGravity(living);
                    }
                }
            }
            WEIGHED.clear();
        });
    }

    private static void consider(final Player player, final DimensionRules rules, final boolean applies,
                                 final DimensionHazardEvent.Hazard hazard,
                                 final Set<DimensionHazardEvent.Hazard> hazards) {
        if (applies && !NeoForge.EVENT_BUS.post(new DimensionHazardEvent(player, hazard, rules)).isCanceled()) {
            hazards.add(hazard);
        }
    }

    /* The hazards a player stood in, as weighed in one tick. */
    private record Weighed(long tick, Set<DimensionHazardEvent.Hazard> hazards) {
    }
}
