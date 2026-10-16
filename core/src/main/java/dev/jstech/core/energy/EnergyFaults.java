/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import dev.jstech.core.JsCore;
import dev.jstech.core.effect.Effects;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What a line of a class too high does to a block, in the world: a burn smokes and sparks, the block left damaged; an
 * arc flash destroys the block, sets fire around it and burns whoever is close. A block wires its store to it:
 *
 * <pre>{@code
 * store.onFault(what -> EnergyFaults.strike(serverLevel, worldPosition, what));
 * }</pre>
 */
public final class EnergyFaults {

    /** How far an arc flash sets fire and burns, in blocks; and how long whoever it burns stays on fire, in seconds. */
    public static final int ARC_REACH = 3;
    public static final int ARC_FIRE_SECONDS = 5;

    /** The effect an arc flash plays: the arc, its sparks, the flash of the screen, the scorch it leaves. */
    public static final ResourceLocation ARC_FLASH_EFFECT = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "arc_flash");

    private static final int SMOKE = 12;
    private static final int SPARKS = 8;
    private static final int FLASH = 30;
    private static final double SPREAD = 0.35;
    private static final double SPEED = 0.05;

    private EnergyFaults() {
    }

    /** Does what {@code what} does to the block at {@code pos}; nothing for a meeting that harms nothing. */
    public static void strike(final ServerLevel level, final BlockPos pos, final WrongClass what) {
        Objects.requireNonNull(what, "what");
        switch (what) {
            case BURNS -> burn(level, pos);
            case ARC_FLASH -> arcFlash(level, pos);
            case NONE, UNDERVOLTAGE, OVERVOLTAGE -> {
            }
        }
    }

    /** The block at {@code pos} burns: it smokes and sparks, and stays where it is, damaged. */
    public static void burn(final ServerLevel level, final BlockPos pos) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, SMOKE,
                SPREAD, SPREAD, SPREAD, SPEED);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SPARKS, SPREAD, SPREAD, SPREAD, SPEED);
    }

    /**
     * An arc flash at {@code pos}: the block is destroyed, fire is set on the open ground round it, and every living
     * thing within reach is set on fire.
     */
    public static void arcFlash(final ServerLevel level, final BlockPos pos) {
        Effects.at(level, ARC_FLASH_EFFECT, Vec3.atCenterOf(pos)).send();
        level.sendParticles(ParticleTypes.FLASH, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, FLASH,
                SPREAD * 2, SPREAD * 2, SPREAD * 2, SPEED * 4);
        level.destroyBlock(pos, false);
        for (final BlockPos near : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (level.isEmptyBlock(near) && BaseFireBlock.canBePlacedAt(level, near, Direction.UP)) {
                level.setBlockAndUpdate(near, BaseFireBlock.getState(level, near));
            }
        }
        final AABB reach = new AABB(pos).inflate(ARC_REACH);
        for (final LivingEntity burnt : level.getEntitiesOfClass(LivingEntity.class, reach)) {
            burnt.igniteForSeconds(ARC_FIRE_SECONDS);
        }
    }
}
