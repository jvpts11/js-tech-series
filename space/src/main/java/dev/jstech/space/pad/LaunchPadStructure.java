/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.pad;

import dev.jstech.core.multiblock.BlockMatch;
import dev.jstech.core.multiblock.InsideRule;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.space.JsSpace;
import dev.jstech.space.crawler.CrawlerLayout;
import net.minecraft.resources.ResourceLocation;

/**
 * The launch pad as one multiblock, as the approved pad stands, for the Structure Projector to show and the Multiblock
 * Builder to build: a square of concrete with its corners cut; the flame trench dug along its middle, lined with Trench
 * Lining on its walls, its ends and its floor and open above; a band of Flame Deflectors across its floor under the
 * pad's middle; a lane of gravel each side of the trench for the crawler's tracks, from the pad's edge to where the
 * crawler stops; and the Pad Service Port by the pad, its terminal. The pedestals are left to the launcher, as each
 * launcher stands on its own.
 *
 * <p>Places are counted from the pad's middle on the ground, the trench along x and the crawler coming from the west.
 */
public final class LaunchPadStructure {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "launch_pad");
    /** How far the trench runs each way from the pad's middle, how far it is open across, and how deep it is dug. */
    public static final int TRENCH_HALF_LENGTH = 18;
    public static final int TRENCH_HALF_WIDTH = 2;
    public static final int TRENCH_DEPTH = 5;
    /** How far the band of deflectors reaches each way along the trench from the pad's middle. */
    public static final int DEFLECTORS_HALF = 4;
    /**
     * Where along the trench the crawler's lanes start, at the pad's edge, and end; and across from the trench's
     * middle, where they lie. The crawler stops with its front tracks on the lanes' last block, its engine seven
     * blocks back. A launcher rolled out of the hangar has its tower behind, so its rocket's axis rides one to six
     * blocks ahead of the engine as its deck is long: ending the lanes three past the middle lays the axis of every
     * rocket the hangar builds over the deflectors.
     */
    public static final int LANES_FROM = -16;
    public static final int LANES_TO = 3;
    public static final int LANE_INSIDE = 4;
    public static final int LANE_OUTSIDE = 6;
    /** Where the crawler's engine stops on the pad, along the trench. */
    public static final int CRAWLER_STOPS = LANES_TO - CrawlerLayout.HALF_LENGTH;
    /** Where the Pad Service Port stands from the pad's middle, by the pad's edge and clear of the launcher's deck. */
    public static final int PORT_X = 6;
    public static final int PORT_Z = 9;
    public static final MultiblockPattern PATTERN = MultiblockPatterns.declare(ID, pattern());
    /* How far the concrete reaches from the middle along each axis, and how far its cut corners are. */
    private static final int CONCRETE_HALF = 16;
    private static final int CONCRETE_CORNERS = 26;
    private static final String CONCRETE = "minecraft:light_gray_concrete";
    private static final String GRAVEL = "minecraft:gravel";
    /* The letters of its places, and of the trench's open inside. */
    private static final char PAVED = 'C';
    private static final char LANE = 'G';
    private static final char LINING = 'L';
    private static final char DEFLECTOR = 'F';
    private static final char OPEN = 'x';

    private LaunchPadStructure() {
    }

    /** Declares the pad's shape, once, from the mod's constructor, before its content registers. */
    public static void declare() {
        // Loading the class declares it.
    }

    /* The pad, layer by layer from the trench's floor up to the ground, where its port stands. */
    private static MultiblockPattern pattern() {
        final int floor = -TRENCH_DEPTH - 1;
        final int end = TRENCH_HALF_LENGTH + 1;
        final MultiblockPattern.Builder builder = MultiblockPattern.builder(ID.getPath());
        for (int y = floor; y <= 0; y++) {
            final String[] rows = new String[CONCRETE_HALF * 2 + 1];
            for (int z = -CONCRETE_HALF; z <= CONCRETE_HALF; z++) {
                final StringBuilder row = new StringBuilder(end * 2 + 1);
                for (int x = -end; x <= end; x++) {
                    row.append(letter(x, y, z));
                }
                rows[z + CONCRETE_HALF] = row.toString();
            }
            builder.layer(rows);
        }
        return builder.where(PAVED, BlockMatch.blocks(CONCRETE))
                .where(LANE, BlockMatch.blocks(GRAVEL))
                .where(LINING, BlockMatch.blocks(PadBlocks.TRENCH_LINING.getId().toString()))
                .where(DEFLECTOR, BlockMatch.blocks(PadBlocks.FLAME_DEFLECTOR.getId().toString()))
                .inside(OPEN, InsideRule.empty())
                .terminal(PadBlocks.PAD_SERVICE_PORT.getId().toString())
                // The layer the port stands in stands on the ground; the trench's under it are dug into it.
                .ground(-floor)
                .build();
    }

    /* What stands at ({@code x}, {@code y}, {@code z}) from the pad's middle on the ground. */
    private static char letter(final int x, final int y, final int z) {
        if (y == 0) {
            return x == PORT_X && z == PORT_Z ? MultiblockPattern.TERMINAL_CHAR : MultiblockPattern.IGNORE_CHAR;
        }
        final boolean open = Math.abs(x) <= TRENCH_HALF_LENGTH && Math.abs(z) <= TRENCH_HALF_WIDTH;
        final boolean lined = Math.abs(x) <= TRENCH_HALF_LENGTH + 1 && Math.abs(z) <= TRENCH_HALF_WIDTH + 1;
        if (y == -TRENCH_DEPTH - 1) {
            return lined ? LINING : MultiblockPattern.IGNORE_CHAR;
        }
        if (open) {
            return y == -TRENCH_DEPTH && Math.abs(x) <= DEFLECTORS_HALF ? DEFLECTOR : OPEN;
        }
        if (lined) {
            return LINING;
        }
        if (y < -1) {
            return MultiblockPattern.IGNORE_CHAR;
        }
        if (x >= LANES_FROM && x <= LANES_TO && Math.abs(z) >= LANE_INSIDE && Math.abs(z) <= LANE_OUTSIDE) {
            return LANE;
        }
        final boolean paved = Math.abs(x) <= CONCRETE_HALF && Math.abs(z) <= CONCRETE_HALF
                && Math.abs(x) + Math.abs(z) <= CONCRETE_CORNERS;
        return paved ? PAVED : MultiblockPattern.IGNORE_CHAR;
    }
}
