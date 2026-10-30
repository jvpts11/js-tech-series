/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.shipyard;

import dev.jstech.core.multiblock.BlockMatch;
import dev.jstech.core.multiblock.InsideRule;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.PortKind;
import dev.jstech.space.JsSpace;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * The shape of the Small Shipyard, a dry dock as its approved drawing has it: the ship's space sunk three blocks into
 * the ground, a pit sixteen blocks wide and twenty-four long that must be dug clear, its rim of hazard concrete; a
 * cradle of hydraulic clamps at four stations along it, each two columns rising from the pit's floor to the hull's
 * belly, a cross beam and a jaw each side; down both sides a line of four lattice towers, eleven blocks to the top of
 * the rails the gantry crane runs on, girders between them under walkways at two heights, a stair from the ground up
 * to both; the crane's bridge across the bay on the rails, a floodlight on each corner tower; on the east side at the
 * back the control booth, on the first walkway, with the terminal inside facing its windows on the bay; at its foot the
 * item port, its conveyor running into the bay; and a feeder on each first walkway's edge. The front, to the north, is
 * open. Its walls and floor are the ground it is dug in, whatever that is.
 *
 * <p>It is drawn as it stands facing north, the open front to the north, as its formed model is made: across, x, from
 * the west tower line at -11 to the booth's outer wall at 11, the bay's middle between 0 and -1; up, y, from the
 * pit's floor at -3 to the crane at 11, the ground's top at 0; along, z, from the front at -13 to the back at 13.
 */
public final class ShipyardStructure {

    /** The pattern's id, and its file's. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "small_shipyard");
    public static final String TERMINAL = block("shipyard_terminal");
    public static final String TOWER = block("dock_tower");
    public static final String RAIL = block("dock_rail");
    public static final String GIRDER = block("dock_girder");
    public static final String WALKWAY = block("dock_walkway");
    public static final String STAIRS = block("dock_stairs");
    public static final String GANTRY = block("dock_gantry");
    public static final String FLOODLIGHT = block("dock_floodlight");
    public static final String BOOTH = block("booth_panel");
    public static final String CONVEYOR = block("dock_conveyor");
    public static final String FEEDER = block("dock_feeder");
    public static final String CRADLE = block("cradle_clamp");
    public static final String RIM = block("pit_rim");
    public static final String ITEM_PORT = block("item_port");
    /* The box the pattern is drawn in, in the dock's own blocks. */
    private static final int WEST = -11;
    private static final int EAST = 11;
    private static final int BOTTOM = -3;
    private static final int TOP = 11;
    private static final int FRONT = -13;
    private static final int BACK = 13;
    /* The tower lines and the walkways along their inner faces, the pit's rim inside them, the pit inside that. */
    private static final int WEST_LINE = -11;
    private static final int EAST_LINE = 10;
    private static final int WEST_WALK = -10;
    private static final int EAST_WALK = 9;
    private static final int WEST_RIM = -9;
    private static final int EAST_RIM = 8;
    private static final int PIT_FRONT = -12;
    private static final int PIT_BACK = 11;
    private static final int PIT_FLOOR = -3;
    private static final int GROUND = -1;
    /* Where the towers stand along each line; the rails on them, the girders and the walkways between. */
    private static final Set<Integer> TOWERS = Set.of(-13, -4, 4, 13);
    private static final int TOWER_TOP = 9;
    private static final int RAILS = 10;
    private static final Set<Integer> WALKS = Set.of(3, 7);
    /* The crane's bridge, over the rails; the floodlights, on the corner towers. */
    private static final int CRANE = 11;
    private static final int CRANE_AT = 1;
    /* The stair, up the west side from the ground at the back to the second walkway, past the first. */
    private static final int STAIR_LINE = -9;
    private static final int STAIR_FOOT = 13;
    private static final int STAIR_STEPS = 8;
    /* The control booth, on the east walkway at the back, its floor reaching out past the tower line. */
    private static final int BOOTH_WEST = 9;
    private static final int BOOTH_EAST = 11;
    private static final int BOOTH_FLOOR = 4;
    private static final int BOOTH_ROOF = 6;
    private static final int BOOTH_FRONT = 9;
    private static final int BOOTH_BACK = 12;
    private static final int TERMINAL_X = 10;
    private static final int TERMINAL_Z = 10;
    /* The item port at the booth's foot, its conveyor into the bay; the feeders, one on each first walkway. */
    private static final int PORT_Z = 9;
    private static final Set<Integer> CONVEYOR_Z = Set.of(6, 7, 8);
    private static final int FEEDER_Y = 4;
    private static final int EAST_FEEDER_Z = -3;
    private static final int WEST_FEEDER_Z = 2;
    /* The cradle's stations along the pit; at each, its columns in the middle and its jaws a block out of the hull. */
    private static final Set<Integer> STATIONS = Set.of(-7, -3, 2, 6);
    private static final Set<Integer> COLUMNS = Set.of(-1, 0);
    private static final int BEAM_WEST = -3;
    private static final int BEAM_EAST = 2;
    private static final Set<Integer> JAWS = Set.of(BEAM_WEST, BEAM_EAST);
    private static final int CRADLE_TOP = 1;
    /*
     * Where a ship stands in the bay: its nose a few blocks in from the open front, its hull's floor two blocks over
     * the ground, its gear stowed over the pit; and the room it may fill, over the pit and under the crane's trolley.
     */
    private static final int SHIP_NOSE = -10;
    private static final int SHIP_FLOOR = 2;
    private static final int ROOM_TOP = 9;
    private static final char NONE = MultiblockPattern.IGNORE_CHAR;
    private static final char PIT = 'x';

    public static final MultiblockPattern PATTERN = MultiblockPatterns.declare(ID, pattern());

    private ShipyardStructure() {
    }

    /** Declares the pattern, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares it.
    }

    /**
     * Where the block ({@code x}, {@code y}, {@code z}) of a ship, in the ship's own frame (across from its centre
     * line, up from its hull's floor, along from its nose), stands in the dock, in the dock's own blocks.
     */
    public static int[] dockOf(final int x, final int y, final int z) {
        return new int[] {x, y + SHIP_FLOOR, z + SHIP_NOSE};
    }

    /**
     * Where the block of a ship's nose on its centre line at its hull's floor, its frame's block (0, 0, 0), stands in
     * the world, in a dock whose terminal is at {@code terminal}, standing as {@code dock} says.
     */
    public static BlockPos shipAt(final BlockPos terminal, final Orientation dock) {
        final int[] at = dockOf(0, 0, 0);
        final int[] off = dock.apply(at[0] - TERMINAL_X, at[1] - BOOTH_FLOOR, at[2] - TERMINAL_Z);
        return terminal.offset(off[0], off[1], off[2]);
    }

    /**
     * Whether the dock has room for a ship's block at ({@code x}, {@code y}, {@code z}) of the ship's frame: over the
     * pit, under the crane's trolley, and clear of the cradle and of every block of the dock.
     */
    public static boolean holds(final int x, final int y, final int z) {
        final int[] at = dockOf(x, y, z);
        return pit(at[0], at[2]) && at[1] >= 0 && at[1] <= ROOM_TOP && letter(at[0], at[1], at[2]) == NONE;
    }

    /* The pattern, layer by layer from the pit's floor, each letter where the layout puts it. */
    private static MultiblockPattern pattern() {
        final MultiblockPattern.Builder builder = MultiblockPattern.builder("small_shipyard");
        for (int y = BOTTOM; y <= TOP; y++) {
            final String[] rows = new String[BACK - FRONT + 1];
            for (int z = FRONT; z <= BACK; z++) {
                final StringBuilder row = new StringBuilder(EAST - WEST + 1);
                for (int x = WEST; x <= EAST; x++) {
                    row.append(letter(x, y, z));
                }
                rows[z - FRONT] = row.toString();
            }
            builder.layer(rows);
        }
        return builder
                .where('T', BlockMatch.blocks(TOWER))
                .where('L', BlockMatch.blocks(RAIL))
                .where('G', BlockMatch.blocks(GIRDER))
                .where('W', BlockMatch.blocks(WALKWAY))
                .where('S', BlockMatch.blocks(STAIRS))
                .where('B', BlockMatch.blocks(GANTRY))
                .where('F', BlockMatch.blocks(FLOODLIGHT))
                .where('P', BlockMatch.blocks(BOOTH))
                .where('V', BlockMatch.blocks(CONVEYOR))
                .where('D', BlockMatch.blocks(FEEDER))
                .where('C', BlockMatch.blocks(CRADLE))
                .where('R', BlockMatch.blocks(RIM))
                .where('I', BlockMatch.blocks(ITEM_PORT))
                .port('I', PortKind.ITEM_INPUT)
                .inside(PIT, InsideRule.empty())
                .terminal(TERMINAL)
                .build();
    }

    /* What stands at ({@code x}, {@code y}, {@code z}) of the dock. */
    private static char letter(final int x, final int y, final int z) {
        if (x == TERMINAL_X && y == BOOTH_FLOOR && z == TERMINAL_Z) {
            return MultiblockPattern.TERMINAL_CHAR;
        }
        if (y < GROUND || y == GROUND && pit(x, z)) {
            return below(x, y, z);
        }
        if (y == GROUND) {
            return rim(x, z) ? 'R' : NONE;
        }
        if (STATIONS.contains(z) && y <= CRADLE_TOP && (COLUMNS.contains(x) || JAWS.contains(x))) {
            return 'C';
        }
        final char frame = frame(x, y, z);
        if (frame != NONE) {
            return frame;
        }
        if (booth(x, y, z)) {
            return 'P';
        }
        return fitting(x, y, z);
    }

    /* In the pit, under the ground's top: the cradle's columns and its cross beam, and the clear pit round them. */
    private static char below(final int x, final int y, final int z) {
        if (!pit(x, z) || y < PIT_FLOOR) {
            return NONE;
        }
        if (STATIONS.contains(z) && (COLUMNS.contains(x) || y == GROUND && x >= BEAM_WEST && x <= BEAM_EAST)) {
            return 'C';
        }
        return PIT;
    }

    /* The towers and the rails on them, the girders and the walkways, the crane, the floodlights and the stair. */
    private static char frame(final int x, final int y, final int z) {
        final boolean line = x == WEST_LINE || x == EAST_LINE;
        if (line && TOWERS.contains(z) && y <= TOWER_TOP) {
            return 'T';
        }
        if (line && y == RAILS) {
            return 'L';
        }
        if (line && WALKS.contains(y)) {
            return 'G';
        }
        if (z == BACK && y == RAILS && x > WEST_LINE && x < EAST_LINE) {
            return 'G';
        }
        if ((x == WEST_WALK || x == EAST_WALK) && WALKS.contains(y)) {
            return 'W';
        }
        if (y == CRANE && z == CRANE_AT && x >= WEST_LINE && x <= EAST_LINE) {
            return 'B';
        }
        if (y == CRANE && line && (z == FRONT || z == BACK)) {
            return 'F';
        }
        final int step = STAIR_FOOT - z;
        if (x == STAIR_LINE && step >= 0 && step < STAIR_STEPS && y == step) {
            return 'S';
        }
        return NONE;
    }

    /*
     * A block of the booth: its walls and its roof, and its floor out past the tower line; inside, round the
     * terminal, it is open.
     */
    private static boolean booth(final int x, final int y, final int z) {
        if (x < BOOTH_WEST || x > BOOTH_EAST || z < BOOTH_FRONT || z > BOOTH_BACK) {
            return false;
        }
        if (y == BOOTH_FLOOR - 1) {
            return x == BOOTH_EAST;
        }
        if (y < BOOTH_FLOOR || y > BOOTH_ROOF) {
            return false;
        }
        final boolean inside = x > BOOTH_WEST && x < BOOTH_EAST && z > BOOTH_FRONT && z < BOOTH_BACK
                && y < BOOTH_ROOF;
        return !inside;
    }

    /* The item port and its conveyor, on the ground under the booth, and the feeders on the walkways' edges. */
    private static char fitting(final int x, final int y, final int z) {
        if (x == EAST_LINE && y == 0 && z == PORT_Z) {
            return 'I';
        }
        if (x == EAST_LINE && y == 0 && CONVEYOR_Z.contains(z)) {
            return 'V';
        }
        if (y == FEEDER_Y && (x == EAST_RIM && z == EAST_FEEDER_Z || x == WEST_RIM && z == WEST_FEEDER_Z)) {
            return 'D';
        }
        return NONE;
    }

    /* Whether ({@code x}, {@code z}) is over the pit. */
    private static boolean pit(final int x, final int z) {
        return x > WEST_RIM && x < EAST_RIM && z >= PIT_FRONT && z <= PIT_BACK;
    }

    /* Whether ({@code x}, {@code z}) is of the rim round the pit: along both sides, and across its back. */
    private static boolean rim(final int x, final int z) {
        final boolean sides = (x == WEST_RIM || x == EAST_RIM) && z >= FRONT && z <= PIT_BACK + 1;
        final boolean back = z == PIT_BACK + 1 && x > WEST_RIM && x < EAST_RIM;
        return sides || back;
    }

    private static String block(final String path) {
        return JsSpace.MODID + ":" + path;
    }
}
