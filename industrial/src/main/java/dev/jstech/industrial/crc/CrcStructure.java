/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.crc;

import dev.jstech.core.multiblock.BlockMatch;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.PortKind;
import dev.jstech.industrial.JsIndustrial;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * The shape of a Computational Research Centre: 3 blocks wide, 3 high and 9 deep, of its own blocks. The terminal is
 * the middle of its front, at a player's eyes; inside, along its body, three gates with a bus between each and at its
 * ends; casing all round. Its entries go on the six places of the two lower rows of the rear face and nowhere else,
 * the top row of the rear being its vent: exactly one Power Entry, at most one Link Socket, at most one Heat Port,
 * and the Coolant Inlet and Outlet together, at most one pair (industrial/docs/design/science.md).
 *
 * <p>It is drawn as it stands facing north, the terminal at the north end, as its formed model is made.
 */
public final class CrcStructure {

    /** The pattern's id, and its file's. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID,
            "computational_research_centre");
    public static final String TERMINAL = block("crc_terminal");
    public static final String CASING = block("crc_casing");
    public static final String GATE = block("crc_gate");
    public static final String BUS = block("crc_bus");
    public static final String POWER_ENTRY = block("crc_power_entry");
    public static final String LINK_SOCKET = block("crc_link_socket");
    public static final String COOLANT_INLET = block("crc_coolant_inlet");
    public static final String COOLANT_OUTLET = block("crc_coolant_outlet");
    public static final String HEAT_PORT = block("crc_heat_port");
    /** How many columns and rows of the rear face take entries. */
    public static final int REAR_COLUMNS = 3;
    public static final int REAR_ROWS = 2;
    /* Where the rear face is, from the terminal, along the body. */
    private static final int REAR = 8;
    /* The terminal's column, the middle one, and its layer, the middle one too. */
    private static final int MIDDLE = 1;
    private static final int TERMINAL_LAYER = 1;

    public static final MultiblockPattern PATTERN = MultiblockPatterns.declare(ID,
            MultiblockPattern.builder("computational_research_centre")
                    .layer("CCC", "CCC", "CCC", "CCC", "CCC", "CCC", "CCC", "CCC", "RRR")
                    .layer("C#C", "CBC", "CGC", "CBC", "CGC", "CBC", "CGC", "CBC", "RRR")
                    .layer("CCC", "CCC", "CCC", "CCC", "CCC", "CCC", "CCC", "CCC", "CCC")
                    .where('C', BlockMatch.blocks(CASING))
                    .where('G', BlockMatch.blocks(GATE))
                    .where('B', BlockMatch.blocks(BUS))
                    .where('R', BlockMatch.blocks(CASING))
                    .entry('R', POWER_ENTRY, BlockMatch.blocks(POWER_ENTRY), PortKind.ENERGY_INPUT)
                    .entry('R', LINK_SOCKET, BlockMatch.blocks(LINK_SOCKET), null)
                    .entry('R', COOLANT_INLET, BlockMatch.blocks(COOLANT_INLET), PortKind.FLUID_INPUT)
                    .entry('R', COOLANT_OUTLET, BlockMatch.blocks(COOLANT_OUTLET), PortKind.FLUID_OUTPUT)
                    .entry('R', HEAT_PORT, BlockMatch.blocks(HEAT_PORT), PortKind.HEAT)
                    .limit(POWER_ENTRY, 1, 1)
                    .limit(LINK_SOCKET, 0, 1)
                    .limit(COOLANT_INLET, 0, 1)
                    .limit(HEAT_PORT, 0, 1)
                    .together(COOLANT_INLET, COOLANT_OUTLET)
                    .terminal(TERMINAL)
                    .build());

    private CrcStructure() {
    }

    /** Declares the pattern, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares it.
    }

    /**
     * Where the rear place of {@code column} (0 the west one, as it stands facing north) and {@code row} (0 the
     * bottom one) is, for a structure whose terminal is at {@code terminal} standing as {@code orientation} says.
     */
    public static BlockPos rearPlace(final BlockPos terminal, final Orientation orientation, final int column,
                                     final int row) {
        final int[] offset = orientation.apply(column - MIDDLE, row - TERMINAL_LAYER, REAR);
        return terminal.offset(offset[0], offset[1], offset[2]);
    }

    private static String block(final String path) {
        return JsIndustrial.MODID + ":" + path;
    }
}
