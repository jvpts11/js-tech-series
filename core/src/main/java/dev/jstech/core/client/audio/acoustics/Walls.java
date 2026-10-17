/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.acoustics;

import dev.jstech.core.audio.Occlusion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The walls between the listener and a sound, one block at a time along the straight line, and how much of the sound
 * gets through them by what each is made of: wool little, stone some, metal much. The block the sound comes from and
 * the one the listener stands in do not count: a machine does not muffle itself.
 */
public final class Walls {

    /** The longest line walked, in blocks; a sound farther than this is too faint for walls to matter. */
    private static final int MOST_STEPS = 48;

    private Walls() {
    }

    /** The share of a sound at {@code source} that reaches a listener at {@code listener} through the walls between. */
    public static float through(final Level level, final Vec3 listener, final Vec3 source) {
        final BlockPos from = BlockPos.containing(listener);
        final BlockPos to = BlockPos.containing(source);
        final Vec3 step = source.subtract(listener);
        final double length = step.length();
        if (length < 1.0) {
            return 1.0F;
        }
        final int steps = Math.min(MOST_STEPS, (int) Math.ceil(length * 2));
        double letThrough = 1.0;
        BlockPos last = from;
        for (int i = 1; i < steps; i++) {
            final BlockPos at = BlockPos.containing(listener.add(step.scale((double) i / steps)));
            if (at.equals(last) || at.equals(from) || at.equals(to)) {
                continue;
            }
            last = at;
            final BlockState state = level.getBlockState(at);
            if (!state.isAir()) {
                letThrough *= BlockAcoustics.through(state, state.isSolidRender(level, at));
            }
        }
        return Occlusion.share(letThrough);
    }
}
