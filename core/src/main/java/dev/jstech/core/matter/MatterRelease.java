/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.matter;

import dev.jstech.core.material.MatterState;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

/**
 * Matter let out into the world, which is gone once it is out: a gas rises and is lost, a supercritical fluid flashes
 * into gas and is lost the same way, a slurry spills, plasma flares and dissipates. What a block that breaks does with
 * what it held is its own choice (a tank may keep its contents in the item it drops); this is what letting it out
 * looks like. Exotic matter is never let out.
 */
public final class MatterRelease {

    /* How many millibuckets each puff of what is let out stands for, and the fewest and most puffs drawn. */
    private static final long PER_PUFF = 250L;
    private static final int FEWEST_PUFFS = 4;
    private static final int MOST_PUFFS = 40;
    private static final double SPREAD = 0.3;
    private static final double SPEED = 0.05;

    private MatterRelease() {
    }

    /**
     * Lets {@code stack} of {@code state} out at {@code pos}, where it is lost, drawn as its state looks escaping.
     *
     * @throws IllegalArgumentException for exotic matter, which is never let out, and for solids and liquids, which
     *                                  the game drops and spills itself
     */
    public static void release(final ServerLevel level, final BlockPos pos, final MatterState state,
                               final MatterStack stack) {
        Objects.requireNonNull(stack, "stack");
        final ParticleOptions look = switch (state) {
            case GAS, SUPERCRITICAL -> ParticleTypes.CLOUD;
            case SLURRY -> ParticleTypes.SPLASH;
            case PLASMA -> ParticleTypes.ELECTRIC_SPARK;
            case EXOTIC -> throw new IllegalArgumentException("exotic matter is never let out, and "
                    + stack.material().id() + " was");
            case SOLID, LIQUID -> throw new IllegalArgumentException("the game drops solids and spills liquids "
                    + "itself, and " + stack.material().id() + " was let out as one");
        };
        final int puffs = (int) Math.max(FEWEST_PUFFS, Math.min(MOST_PUFFS, stack.amount() / PER_PUFF));
        level.sendParticles(look, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, puffs, SPREAD, SPREAD,
                SPREAD, SPEED);
    }
}
