/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

import dev.jstech.core.JsCore;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * What a block too hot does to what is around it: past 300 °C, near where wood catches by itself, it sets alight what
 * burns against it, as the game's fire spreads; past 150 °C it burns whoever stands against it, more the hotter it
 * is, unless they resist fire. Its damage is the Core's own, {@code jscore:heat}, a kind of fire, so a pack can change
 * it. The level's heat asks it as it looks at each block, and a server may turn either off.
 */
@TextHolder
public final class HeatHazards {

    /** Where a hot block sets alight what burns against it, in kelvin: 300 °C, about where wood catches by itself. */
    public static final double IGNITION = 573.15;
    /** Where a hot block burns whoever stands against it, in kelvin: 150 °C. */
    public static final double BURNS_AT = 423.15;
    /** The type of heat damage. */
    public static final ResourceKey<DamageType> HEAT = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "heat"));

    public static final TextKey DIED = TextKey.of("death.attack.jscore.heat", "%1$s was burned by something too hot");
    public static final TextKey DIED_FIGHTING = TextKey.of("death.attack.jscore.heat.player",
            "%1$s was burned by something too hot while fighting %2$s");

    /* How likely something that burns against a hot block is to catch, a second. */
    private static final double CATCHES_A_SECOND = 0.2;
    /* Each so many kelvin past where it burns adds a point of damage to a burn. */
    private static final double KELVIN_A_POINT = 200.0;
    /* How far from the block a burn reaches, in blocks: whoever stands against it or on it. */
    private static final double REACH = 0.5;
    /* Where the fire goes beside what catches: above it first, then around it, below it last. */
    private static final List<Direction> FIRE_GOES = List.of(Direction.UP, Direction.NORTH, Direction.SOUTH,
            Direction.WEST, Direction.EAST, Direction.DOWN);

    private HeatHazards() {
    }

    /** Heat damage in {@code level}. */
    public static DamageSource heat(final Level level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(HEAT));
    }

    /**
     * Sets alight, by chance over {@code seconds}, the block {@code state} at {@code pos} when it burns on its
     * {@code face} that meets a block past {@link #IGNITION}: fire takes the first air beside it.
     */
    public static void ignite(final ServerLevel level, final BlockPos pos, final BlockState state,
                              final Direction face, final double seconds) {
        if (!HeatBalance.ignites() || !level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)
                || !state.isFlammable(level, pos, face) || level.random.nextDouble() >= seconds * CATCHES_A_SECOND) {
            return;
        }
        for (final Direction way : FIRE_GOES) {
            final BlockPos spot = pos.relative(way);
            if (level.getBlockState(spot).isAir()) {
                level.setBlockAndUpdate(spot, BaseFireBlock.getState(level, spot));
                return;
            }
        }
    }

    /** Burns whoever stands against the block at {@code pos} while it is past {@link #BURNS_AT} at {@code kelvin}. */
    public static void burn(final ServerLevel level, final BlockPos pos, final double kelvin) {
        if (!HeatBalance.hotSurfacesBurn() || !(kelvin > BURNS_AT)) {
            return;
        }
        final float points = (float) (1.0 + (kelvin - BURNS_AT) / KELVIN_A_POINT);
        for (final LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(REACH))) {
            entity.hurt(heat(level), points);
        }
    }
}
