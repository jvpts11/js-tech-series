/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

/**
 * Where the yard lies in every dimension, and how it is cut into plots: the far east of the world, from 25,600,000
 * blocks out to the edge the game keeps, where no player goes, cut into squares of 128 chunks. Each moving structure
 * keeps its real blocks in a plot of its own there, the whole height of the dimension. Whether a place is in the yard
 * is a single comparison, since the game asks it of nearly every block that changes. Pure: no part of the game.
 */
public final class YardLayout {

    /** The first chunk of the yard, east: everything from here on is the yard's. */
    public static final int FIRST_CHUNK_X = 1_600_000;
    /** The first block of the yard, east. */
    public static final int FIRST_BLOCK_X = FIRST_CHUNK_X * 16;
    /** How many chunks a plot is on a side. */
    public static final int PLOT_CHUNKS = 128;
    /** How many blocks a plot is on a side. */
    public static final int PLOT_BLOCKS = PLOT_CHUNKS * 16;
    /** How many plots the yard holds east to west, short of the edge the game keeps at 29,999,984 blocks. */
    public static final int COLUMNS = 2_140;
    /** How many plots the yard holds north to south. */
    public static final int ROWS = 29_280;
    /** How many plots the yard holds in all. */
    public static final int PLOTS = COLUMNS * ROWS;
    /* The first chunk of the yard, north. */
    private static final int FIRST_CHUNK_Z = -(ROWS / 2) * PLOT_CHUNKS;

    private YardLayout() {
    }

    /** Whether the block at {@code x} east is in the yard, whatever its height and its south. */
    public static boolean isYardBlock(final int x) {
        return x >= FIRST_BLOCK_X;
    }

    /** Whether the place at {@code x} east is in the yard. */
    public static boolean isYard(final double x) {
        return x >= FIRST_BLOCK_X;
    }

    /** Whether the chunk {@code chunkX} east is in the yard. */
    public static boolean isYardChunk(final int chunkX) {
        return chunkX >= FIRST_CHUNK_X;
    }

    /** The plot holding the chunk at {@code (chunkX, chunkZ)}, or -1 when the chunk is no plot's. */
    public static int plotOfChunk(final int chunkX, final int chunkZ) {
        if (chunkX < FIRST_CHUNK_X) {
            return -1;
        }
        final int column = (chunkX - FIRST_CHUNK_X) / PLOT_CHUNKS;
        final int row = Math.floorDiv(chunkZ - FIRST_CHUNK_Z, PLOT_CHUNKS);
        if (column >= COLUMNS || row < 0 || row >= ROWS) {
            return -1;
        }
        return row * COLUMNS + column;
    }

    /** The plot holding the block at {@code (x, z)}, or -1 when it is no plot's. */
    public static int plotOfBlock(final int x, final int z) {
        return plotOfChunk(x >> 4, z >> 4);
    }

    /**
     * The first chunk of plot {@code plot}, east.
     *
     * @throws IllegalArgumentException when there is no such plot
     */
    public static int plotChunkX(final int plot) {
        check(plot);
        return FIRST_CHUNK_X + (plot % COLUMNS) * PLOT_CHUNKS;
    }

    /** The first chunk of plot {@code plot}, north. */
    public static int plotChunkZ(final int plot) {
        check(plot);
        return FIRST_CHUNK_Z + (plot / COLUMNS) * PLOT_CHUNKS;
    }

    /** The block at the middle of plot {@code plot}, east: where a structure's blocks are laid out round. */
    public static int centreX(final int plot) {
        return plotChunkX(plot) * 16 + PLOT_BLOCKS / 2;
    }

    /** The block at the middle of plot {@code plot}, south. */
    public static int centreZ(final int plot) {
        return plotChunkZ(plot) * 16 + PLOT_BLOCKS / 2;
    }

    /** Whether the block at {@code (x, z)} lies inside plot {@code plot}. */
    public static boolean inPlot(final int plot, final int x, final int z) {
        return plotOfBlock(x, z) == plot;
    }

    private static void check(final int plot) {
        if (plot < 0 || plot >= PLOTS) {
            throw new IllegalArgumentException("the yard has plots 0 to " + (PLOTS - 1) + ", not " + plot);
        }
    }
}
