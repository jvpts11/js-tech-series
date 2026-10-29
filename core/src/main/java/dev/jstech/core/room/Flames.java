/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import dev.jstech.core.util.Loaded;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The flames that burn the air's oxygen besides fire ({@link IFlame}): the game's furnaces, which burn their fuel only
 * where a fire could, and whatever flames the mods declare. A flame lit or set where the air holds too little oxygen
 * for a fire goes out at once; one burning in a room goes out the second its air runs that short. Fire itself is the
 * Core's own, kept with each room.
 *
 * <pre>{@code
 * Flames.declare(new IFlame() {
 *     public boolean burning(BlockState state) { return state.is(MY_LAMP.get()) && state.getValue(LIT); }
 *     public void putOut(ServerLevel level, BlockPos pos, BlockState state) {
 *         level.setBlockAndUpdate(pos, state.setValue(LIT, false));
 *     }
 * });
 * }</pre>
 */
public final class Flames {

    private static final List<IFlame> DECLARED = new CopyOnWriteArrayList<>(List.of(new FurnaceFlame()));

    private Flames() {
    }

    /** Declares a flame, once, from a mod's constructor. */
    public static void declare(final IFlame flame) {
        DECLARED.add(flame);
    }

    /** The flame {@code state} is, burning, or none. */
    public static @Nullable IFlame of(final BlockState state) {
        for (final IFlame flame : DECLARED) {
            if (flame.burning(state)) {
                return flame;
            }
        }
        return null;
    }

    /**
     * Whether a flame can burn at {@code pos}, as fire can: in the air there, its room's or the place's; for a block
     * that is itself a wall, a furnace, in the air on any side of it that is no wall, read only in loaded chunks.
     */
    public static boolean canBurn(final ServerLevel level, final BlockPos pos, final BlockState state) {
        if (!Seals.of(level, pos, state).wall()) {
            return GasMath.burns(SealedRooms.oxygenAt(level, pos));
        }
        for (final Direction side : Direction.values()) {
            final BlockPos beside = pos.relative(side);
            if (Loaded.at(level, beside) && !Seals.of(level, beside, level.getBlockState(beside)).wall()
                    && GasMath.burns(SealedRooms.oxygenAt(level, beside))) {
                return true;
            }
        }
        return false;
    }

    /** Whether {@code after}, in place of {@code before}, is a flame lit there, whose air is to be weighed. */
    static boolean lit(final BlockState before, final BlockState after) {
        return of(after) != null && of(before) == null;
    }
}
