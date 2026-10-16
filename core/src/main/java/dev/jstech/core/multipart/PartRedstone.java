/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import dev.jstech.core.connect.Neighbours;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The redstone a block holding parts gives and takes, face by face, for the cable block and the space of parts alone
 * alike: each face gives what the device on it gives, and a lever, a button or a plate in the space gives as the
 * game's own does; wire beside a face joins it when its device's kind deals in redstone, or every face when the space
 * holds one of the game's signal blocks; and a comparator reads the device on the face it stands against. The game
 * names a side by the way from the block asking to this one, so a part's face is the opposite of the side asked about.
 */
public final class PartRedstone {

    private PartRedstone() {
    }

    /**
     * The strength the block at {@code pos} gives the block it is asked by, from {@code direction}: the device on that
     * face, or a lever, button or plate in the space.
     */
    public static int signal(final BlockGetter level, final BlockPos pos, final Direction direction) {
        final SpaceParts parts = partsAt(level, pos);
        return parts == null ? 0 : Math.max(parts.signal(direction.getOpposite()),
                parts.smallSignal(level, pos, direction, false));
    }

    /** The strength it gives into the block it is asked by, which that block then gives its own neighbours. */
    public static int directSignal(final BlockGetter level, final BlockPos pos, final Direction direction) {
        final SpaceParts parts = partsAt(level, pos);
        return parts == null ? 0 : Math.max(parts.directSignal(direction.getOpposite()),
                parts.smallSignal(level, pos, direction, true));
    }

    /** Whether wire coming from {@code direction} joins the block at {@code pos}. */
    public static boolean joins(final BlockGetter level, final BlockPos pos, @Nullable final Direction direction) {
        final SpaceParts parts = direction == null ? null : partsAt(level, pos);
        return parts != null && parts.joinsRedstone(direction.getOpposite());
    }

    /**
     * What a comparator reading the block at {@code pos} gives: the device on the face a comparator stands against,
     * reading this block; with more than one, or none found, the most any face gives.
     */
    public static int comparator(final Level level, final BlockPos pos) {
        final SpaceParts parts = partsAt(level, pos);
        if (parts == null) {
            return 0;
        }
        int read = -1;
        int most = 0;
        for (final Direction face : Direction.values()) {
            final int here = parts.comparator(face);
            most = Math.max(most, here);
            final BlockState there = level.getBlockState(pos.relative(face));
            // A comparator reads the block it faces away from: one on this face, facing away from it, reads this one.
            if (there.getBlock() instanceof ComparatorBlock
                    && there.getValue(HorizontalDirectionalBlock.FACING) == face.getOpposite()) {
                read = Math.max(read, here);
            }
        }
        return read >= 0 ? read : most;
    }

    /** Tells the device on the face toward {@code neighbour} that the block there changed; on the server. */
    public static void neighbourChanged(final Level level, final BlockPos pos, final BlockPos neighbour) {
        final Direction face = Neighbours.faceTowards(pos, neighbour);
        final SpaceParts parts = partsAt(level, pos);
        if (face != null && parts != null && !level.isClientSide()) {
            parts.neighbourChanged(face);
        }
    }

    private static @Nullable SpaceParts partsAt(final BlockGetter level, final BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ISpaceHolder holder ? holder.parts() : null;
    }
}
