/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.core.cable.BundleShape;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.Lane;
import dev.jstech.core.cable.Wire;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The hookups a device that takes its cables from behind shows, as Quince's all-in-ones and the desk access points do:
 * a socket and a cord for each lane of the cable block behind it where a line ends on its back. The model carries a
 * bone for each lane, {@code <prefix><column>_<row>}, the column counted from the model's -x side and the row from the
 * bottom, kept at {@code column * 3 + row}.
 */
final class BackHookups {

    /** The cable block's lanes on a face, across and up. */
    static final int LANES = 3;

    private BackHookups() {
    }

    /** The nine bones of the hookups named from {@code prefix}, in the order {@link #plugged} answers. */
    static String[] bones(final String prefix) {
        final String[] out = new String[LANES * LANES];
        for (int column = 0; column < LANES; column++) {
            for (int row = 0; row < LANES; row++) {
                out[column * LANES + row] = prefix + column + "_" + row;
            }
        }
        return out;
    }

    /**
     * Which lanes of the cable block behind a device at {@code pos} facing {@code facing} end a wire on its back, of
     * the wires {@code which} takes; none when nothing is behind it.
     */
    static boolean[] plugged(@Nullable final Level level, final BlockPos pos, final Direction facing,
                             final Predicate<Wire> which) {
        final boolean[] plugged = new boolean[LANES * LANES];
        if (level == null || !(level.getBlockEntity(pos.relative(facing.getOpposite()))
                instanceof CableBlockEntity cable)) {
            return plugged;
        }
        final List<Wire> wires = cable.wires();
        // The model's +x runs the world's way clockwise of its facing; a plug's across runs east or south.
        final Direction modelX = facing.getClockWise();
        final int sign = modelX.getStepX() + modelX.getStepZ();
        for (final BundleShape.Plug plug : cable.shape().plugs()) {
            if (plug.face() == facing.get3DDataValue() && plug.strand() >= 0 && plug.strand() < wires.size()
                    && which.test(wires.get(plug.strand()))) {
                final int column = Mth.clamp((int) Math.round(plug.across() * sign / Lane.PITCH) + 1, 0, LANES - 1);
                final int row = Mth.clamp((int) Math.round(plug.up() / Lane.PITCH) + 1, 0, LANES - 1);
                plugged[column * LANES + row] = true;
            }
        }
        return plugged;
    }
}
