/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import com.mojang.logging.LogUtils;
import dev.jstech.core.JsCore;
import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

/**
 * What a dimension's rules do: its pull on every living thing in it, its weather, and the air, heat and cold a player
 * not made for them suffers, each a {@link DimensionHazardEvent} another mod may cancel. The hazards are weighed once
 * a second and their harm dealt each tick, so a choking player's air runs down as smoothly as under water.
 */
@EventBusSubscriber(modid = JsCore.MODID)
public final class DimensionEffects {

    private static final Logger LOGGER = LogUtils.getLogger();
    /** What a dimension's pull is kept under on every living thing, so it is taken off when the thing leaves. */
    private static final ResourceLocation GRAVITY = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "dimension_gravity");
    private static final int WEIGH_EVERY = 20;
    /* A player out of water gets back four of air a tick; five taken leaves one a tick lost, as under water. */
    private static final int AIR_TAKEN = 5;
    private static final int DROWNED_AT = -20;
    private static final float DROWNING_DAMAGE = 2.0F;
    /* A player out of powder snow thaws two a tick; three taken leaves one a tick gained, as in it. */
    private static final int COLD_GIVEN = 3;
    private static final float BURN_SECONDS = 3.0F;

    /** The hazards each player stands in, as last weighed. */
    private static final Map<UUID, Set<DimensionHazardEvent.Hazard>> SUFFERING = new ConcurrentHashMap<>();

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
        consider(player, rules, !rules.breathable(), DimensionHazardEvent.Hazard.AIR, hazards);
        consider(player, rules, rules.scorching(), DimensionHazardEvent.Hazard.HEAT, hazards);
        consider(player, rules, rules.freezing(), DimensionHazardEvent.Hazard.COLD, hazards);
        return hazards;
    }

    /** Deals one tick of the hazards {@code player} was last weighed to suffer. */
    public static void suffer(final Player player, final Set<DimensionHazardEvent.Hazard> hazards) {
        if (hazards.contains(DimensionHazardEvent.Hazard.AIR)) {
            final int air = player.getAirSupply() - AIR_TAKEN;
            if (air <= DROWNED_AT) {
                player.setAirSupply(0);
                player.hurt(player.damageSources().drown(), DROWNING_DAMAGE);
            } else {
                player.setAirSupply(air);
            }
        }
        if (hazards.contains(DimensionHazardEvent.Hazard.COLD) && player.canFreeze()) {
            player.setTicksFrozen(Math.min(player.getTicksRequiredToFreeze(), player.getTicksFrozen() + COLD_GIVEN));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(final PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Set<DimensionHazardEvent.Hazard> hazards = SUFFERING.get(player.getUUID());
        if (hazards == null || player.tickCount % WEIGH_EVERY == 0) {
            hazards = weigh(player);
            if (hazards.contains(DimensionHazardEvent.Hazard.HEAT)) {
                player.igniteForSeconds(BURN_SECONDS);
            }
            SUFFERING.put(player.getUUID(), hazards);
        }
        if (!hazards.isEmpty()) {
            suffer(player, hazards);
        }
    }

    @SubscribeEvent
    public static void onJoinLevel(final EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            applyGravity(living);
            SUFFERING.remove(living.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        SUFFERING.remove(event.getEntity().getUUID());
    }

    /**
     * A dimension the datapacks made reads the overworld's weather; the Core hands it weather of its own, which its
     * rules may hold. One the Core made at runtime already has it.
     */
    @SubscribeEvent
    public static void onLevelLoad(final LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() != Level.OVERWORLD
                && level.getLevelData() instanceof DerivedLevelData
                && !(level.getLevelData() instanceof RuledLevelData)) {
            try {
                final Field data = ObfuscationReflectionHelper.findField(Level.class, "levelData");
                data.set(level, new RuledLevelData(level.getServer().getWorldData(), level.dimension()));
            } catch (final RuntimeException | IllegalAccessException refused) {
                LOGGER.warn("The weather of {} stays the overworld's: {}", level.dimension().location(),
                        refused.toString());
            }
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
            SUFFERING.clear();
        });
    }

    private static void consider(final Player player, final DimensionRules rules, final boolean applies,
                                 final DimensionHazardEvent.Hazard hazard,
                                 final Set<DimensionHazardEvent.Hazard> hazards) {
        if (applies && !NeoForge.EVENT_BUS.post(new DimensionHazardEvent(player, hazard, rules)).isCanceled()) {
            hazards.add(hazard);
        }
    }
}
