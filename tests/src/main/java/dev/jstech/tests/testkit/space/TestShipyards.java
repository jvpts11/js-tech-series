/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit.space;

import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.space.hangar.HangarBlocks;
import dev.jstech.space.shipyard.ShipyardBlocks;
import dev.jstech.space.shipyard.ShipyardStructure;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds a Small Shipyard the way a player does, block by block from its pattern: dug into a ground of stone, its pit
 * clear, every block of the kit where the pattern puts it, its terminal last, facing the booth's back as a player in
 * the booth places it.
 */
public final class TestShipyards {

    /* The block each letter of the pattern stands for. */
    private static final Map<Character, Block> BLOCKS = Map.ofEntries(
            Map.entry('T', ShipyardBlocks.TOWER.get()), Map.entry('L', ShipyardBlocks.RAIL.get()),
            Map.entry('G', ShipyardBlocks.GIRDER.get()), Map.entry('W', ShipyardBlocks.WALKWAY.get()),
            Map.entry('S', ShipyardBlocks.STAIRS.get()), Map.entry('B', ShipyardBlocks.GANTRY.get()),
            Map.entry('F', ShipyardBlocks.FLOODLIGHT.get()), Map.entry('P', ShipyardBlocks.BOOTH.get()),
            Map.entry('V', ShipyardBlocks.CONVEYOR.get()), Map.entry('D', ShipyardBlocks.FEEDER.get()),
            Map.entry('C', ShipyardBlocks.CRADLE.get()), Map.entry('R', ShipyardBlocks.RIM.get()),
            Map.entry('I', HangarBlocks.ITEM_PORT.get()));
    /*
     * The ground round the pit, as far as the dock reaches and a block beyond: from under the pit's floor, a block
     * below the pattern's lowest layer, up to the layer the rim lies in, four blocks in all.
     */
    private static final int MARGIN = 1;
    private static final int GROUND_DEPTH = 4;
    private static final char PIT = 'x';

    private TestShipyards() {
    }

    /**
     * Builds the dock with the lowest corner of its pattern, the pit's floor's west front corner, at {@code corner},
     * and returns where its terminal stands.
     */
    public static BlockPos build(final ServerLevel level, final BlockPos corner) {
        return build(level, corner, Orientation.NORTH);
    }

    /**
     * Builds the dock standing as {@code turn} says, the lowest corner of the box it stands in at {@code corner}, and
     * returns where its terminal stands.
     */
    public static BlockPos build(final ServerLevel level, final BlockPos corner, final Orientation turn) {
        final MultiblockPattern pattern = ShipyardStructure.PATTERN;
        // The box the dock stands in, from its terminal: each corner of the pattern turned.
        final int[] low = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
        final int[] high = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        for (final int x : new int[] {0, pattern.sizeX() - 1}) {
            for (final int z : new int[] {0, pattern.sizeZ() - 1}) {
                final int[] off = offset(pattern, turn, x, 0, z);
                for (int axis = 0; axis < low.length; axis++) {
                    low[axis] = Math.min(low[axis], off[axis]);
                    high[axis] = Math.max(high[axis], off[axis]);
                }
            }
        }
        final BlockPos terminal = corner.offset(-low[0], pattern.terminalY(), -low[2]);
        for (int x = low[0] - MARGIN; x <= high[0] + MARGIN; x++) {
            for (int z = low[2] - MARGIN; z <= high[2] + MARGIN; z++) {
                for (int y = corner.getY() - 1; y < corner.getY() + GROUND_DEPTH - 1; y++) {
                    level.setBlock(new BlockPos(terminal.getX() + x, y, terminal.getZ() + z), Blocks.STONE
                            .defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        for (int y = 0; y < pattern.sizeY(); y++) {
            for (int z = 0; z < pattern.sizeZ(); z++) {
                for (int x = 0; x < pattern.sizeX(); x++) {
                    final char letter = pattern.charAt(x, y, z);
                    final int[] off = offset(pattern, turn, x, y, z);
                    final BlockPos at = terminal.offset(off[0], off[1], off[2]);
                    if (letter == PIT) {
                        level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    } else if (BLOCKS.containsKey(letter)) {
                        level.setBlock(at, BLOCKS.get(letter).defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        // As drawn the terminal faces east, to the booth's back.
        final int[] east = turn.apply(1, 0, 0);
        final BlockState console = ShipyardBlocks.TERMINAL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.fromDelta(east[0], east[1], east[2]));
        level.setBlock(terminal, console, Block.UPDATE_ALL);
        return terminal;
    }

    /* Where the pattern's place ({@code x}, {@code y}, {@code z}) stands from its terminal, turned. */
    private static int[] offset(final MultiblockPattern pattern, final Orientation turn, final int x, final int y,
                                final int z) {
        return turn.apply(x - pattern.terminalX(), y - pattern.terminalY(), z - pattern.terminalZ());
    }
}
