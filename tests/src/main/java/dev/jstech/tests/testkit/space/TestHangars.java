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
import dev.jstech.space.hangar.HangarStructure;
import dev.jstech.space.shipyard.ShipyardBlocks;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds a Rocket Hangar the way a player does, block by block from its pattern: its floor laid on the ground, every
 * block of its kit where the pattern puts it, its terminal last, facing its chair as a player in the chair's place sets
 * it. Its fittings stand as they were placed, facing north, until the hangar forms and turns them.
 */
public final class TestHangars {

    /* The block each letter of the pattern stands for. */
    private static final Map<Character, Block> BLOCKS = Map.ofEntries(
            Map.entry('W', HangarBlocks.WALL.get()), Map.entry('F', HangarBlocks.FRAME.get()),
            Map.entry('R', HangarBlocks.ROOF.get()), Map.entry('G', HangarBlocks.WINDOW.get()),
            Map.entry('O', HangarBlocks.FLOOR.get()), Map.entry('L', HangarBlocks.LAMP.get()),
            Map.entry('D', HangarBlocks.BAY_DOOR.get()), Map.entry('Z', ShipyardBlocks.RIM.get()),
            Map.entry('V', Blocks.GRAVEL), Map.entry('X', HangarBlocks.CRANE_RAIL.get()),
            Map.entry('Y', HangarBlocks.CRANE_BRIDGE.get()), Map.entry('U', HangarBlocks.CRANE_HOIST.get()),
            Map.entry('K', HangarBlocks.LOADING_DOCK.get()), Map.entry('T', HangarBlocks.CONTROL_FLOOR.get()),
            Map.entry('S', HangarBlocks.STEP.get()), Map.entry('E', HangarBlocks.RAILING.get()),
            Map.entry('C', HangarBlocks.CONSOLE.get()), Map.entry('Q', HangarBlocks.DOOR_CONSOLE.get()),
            Map.entry('H', HangarBlocks.CHAIR.get()), Map.entry('A', HangarBlocks.DESIGN_TABLE.get()),
            Map.entry('B', HangarBlocks.STATUS_BOARD.get()), Map.entry('P', HangarBlocks.PERSONNEL_DOOR.get()),
            Map.entry('I', HangarBlocks.CEILING_LIGHT.get()));

    private TestHangars() {
    }

    /**
     * Builds the hangar standing as {@code turn} says round its terminal at {@code terminal}, its floor a block under
     * the terminal, and returns where the terminal stands.
     */
    public static BlockPos build(final ServerLevel level, final BlockPos terminal, final Orientation turn) {
        final MultiblockPattern pattern = HangarStructure.PATTERN;
        // The ground the floor is laid into, a layer of stone under all of it, so the floor's gravel lies on it.
        for (int z = 0; z < pattern.sizeZ(); z++) {
            for (int x = 0; x < pattern.sizeX(); x++) {
                final int[] off = offset(pattern, turn, x, -1, z);
                level.setBlock(terminal.offset(off[0], off[1], off[2]), Blocks.STONE.defaultBlockState(),
                        Block.UPDATE_CLIENTS);
            }
        }
        for (int y = 0; y < pattern.sizeY(); y++) {
            for (int z = 0; z < pattern.sizeZ(); z++) {
                for (int x = 0; x < pattern.sizeX(); x++) {
                    final Block block = BLOCKS.get(pattern.charAt(x, y, z));
                    if (block != null) {
                        final int[] off = offset(pattern, turn, x, y, z);
                        level.setBlock(terminal.offset(off[0], off[1], off[2]), block.defaultBlockState(),
                                Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        // As drawn the terminal faces east, to its chair.
        final int[] east = turn.apply(1, 0, 0);
        final BlockState console = HangarBlocks.TERMINAL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.fromDelta(east[0], east[1], east[2]));
        level.setBlock(terminal, console, Block.UPDATE_ALL);
        return terminal;
    }

    /**
     * Takes the hangar built round {@code terminal} as {@code turn} says away, and all that stands in its box: the
     * ground under it grass on dirt again, air over it, as a player's test world was.
     */
    public static void clear(final ServerLevel level, final BlockPos terminal, final Orientation turn) {
        final MultiblockPattern pattern = HangarStructure.PATTERN;
        for (int y = -1; y < pattern.sizeY(); y++) {
            final BlockState put = y < 0 ? Blocks.DIRT.defaultBlockState() : y == 0 ? Blocks.GRASS_BLOCK
                    .defaultBlockState() : Blocks.AIR.defaultBlockState();
            for (int z = 0; z < pattern.sizeZ(); z++) {
                for (int x = 0; x < pattern.sizeX(); x++) {
                    final int[] off = offset(pattern, turn, x, y, z);
                    level.setBlock(terminal.offset(off[0], off[1], off[2]), put, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    /** Where the pattern's place ({@code x}, {@code y}, {@code z}), as {@link HangarStructure} draws it, stands. */
    public static BlockPos at(final BlockPos terminal, final Orientation turn, final int x, final int y, final int z) {
        return HangarStructure.world(terminal, turn, x, y, z);
    }

    /* Where the pattern's place ({@code x}, {@code y}, {@code z}) stands from its terminal, turned. */
    private static int[] offset(final MultiblockPattern pattern, final Orientation turn, final int x, final int y,
                                final int z) {
        return turn.apply(x - pattern.terminalX(), y - pattern.terminalY(), z - pattern.terminalZ());
    }
}
