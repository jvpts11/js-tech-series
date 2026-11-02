/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import dev.jstech.core.multiblock.BlockMatch;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.PortKind;
import dev.jstech.space.JsSpace;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * The shape of the Rocket Hangar, one building of one size, as the Vehicle Assembly Building's high bay: big enough for
 * the tallest rocket the hangar builds, 96 blocks, standing on its mobile launcher with the launcher's tower beside it,
 * and for the widest, 17 blocks, to roll out on the crawler. Its bay, 29 blocks across, 37 long and 108 high inside
 * its walls of panels, a dark frame column every six blocks and a band every twelve, lamps up their long walls' inner
 * faces and light panels in its roof; its floor of concrete, the crawler's two lanes of gravel down its length; its
 * crane, a rail along the top of each long wall, the bridge across the bay on them over its middle, the hoist under
 * it. In the end wall the stepped door: a lower door 25 wide and 12
 * high for the crawler and the launcher's deck, a slot 17 wide over it up to 106 for the rocket and the tower. Against
 * a long wall the control room, as a launch control room: a row of consoles before the windows into the bay, the
 * hangar's terminal among them, a chair at each; the bay door's console at the row's end; a step at the back with its
 * railing, the Rocket Design table on it; the status board on its end wall; light panels in its ceiling; a door in its
 * outer wall and one onto the bay's floor; and in the outer wall the loading dock the crane's blocks come in through.
 *
 * <p>It is drawn as it stands with its door to the north: across, x, from the west wall at -15 to the control room's
 * outer wall at 27 and its dock's apron past it; up, y, from the floor at -1, laid into the ground, to the roof at 108;
 * along, z, from the door's wall at -19 to the far wall at 19.
 */
public final class HangarStructure {

    /** The pattern's id, and its file's. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "rocket_hangar");
    /** The tallest rocket the hangar builds, in blocks. */
    public static final int TALLEST_ROCKET = 96;
    /** The widest, in blocks: as wide as the door's slot it rolls out through. */
    public static final int WIDEST_ROCKET = 17;
    public static final String TERMINAL = block("hangar_terminal");
    /* The box the pattern is drawn in. */
    private static final int WEST = -15;
    private static final int EAST = 29;
    private static final int BOTTOM = -1;
    private static final int TOP = 108;
    private static final int FRONT = -19;
    private static final int BACK = 19;
    /* The bay, inside its walls; the walls, the roof over them and the floor under it. */
    private static final int BAY_X = 14;
    private static final int BAY_Z = 18;
    private static final int BAY_TOP = 107;
    private static final int WALL_X = 15;
    private static final int WALL_Z = 19;
    private static final int ROOF = 108;
    private static final int FLOOR = -1;
    /*
     * The frame: its columns along the walls, its bands up them, its eaves at their tops. The bay's lamps, on the long
     * walls' inner faces between the columns, a row every twelve blocks up from near the floor to near the crane, so
     * the whole height of the bay is lit as a block's light reaches.
     */
    private static final Set<Integer> COLUMNS = Set.of(-12, -6, 0, 6, 12);
    private static final int BAND_EVERY = 12;
    private static final int EAVES = 107;
    private static final int LAMPS_EVERY = 12;
    private static final int LAMPS_FIRST = 6;
    private static final Set<Integer> LAMPS = Set.of(-15, -9, -3, 3, 9, 15);
    /* Light panels set in the roof over the bay, lighting the crane and the top of the bay its walls' lamps miss. */
    private static final Set<Integer> BAY_LIGHTS_X = Set.of(-12, -6, 0, 6, 12);
    private static final Set<Integer> BAY_LIGHTS_Z = Set.of(-15, -9, -3, 3, 9, 15);
    /* The stepped door: the lower door, the slot over it, their frames, the lintel and the hazard band. */
    private static final int LOWER_HALF = 12;
    private static final int LOWER_TOP = 11;
    private static final int SLOT_HALF = 8;
    private static final int SLOT_TOP = 105;
    private static final int LOWER_FRAME = 13;
    private static final int SLOT_FRAME = 9;
    private static final int LINTEL = 106;
    private static final int BAND = 12;
    /* The crawler's lanes of gravel in the floor, each side of the bay's middle line. */
    private static final int LANE_IN = 3;
    private static final int LANE_OUT = 6;
    /* The crane: its rails along the long walls, its bridge across the bay's middle, its hoist under the bridge. */
    private static final int RAILS = 106;
    private static final int BRIDGE = 107;
    private static final int CRANE_Z = 0;
    /* The control room against the east wall: its box, its walls, its roof. */
    private static final int ROOM_WEST = 16;
    private static final int ROOM_EAST = 27;
    private static final int ROOM_FRONT = -8;
    private static final int ROOM_BACK = 11;
    private static final int ROOM_ROOF = 9;
    /* Its doors: the outer one and the one onto the bay's floor; its windows; the dock and its apron. */
    private static final int OUTER_DOOR_Z = -4;
    private static final int INNER_DOOR_Z = -6;
    private static final int DOOR_TOP = 1;
    private static final int WINDOWS_FROM = -3;
    private static final int WINDOWS_TO = 10;
    private static final int WINDOWS_LOW = 2;
    private static final int WINDOWS_HIGH = 6;
    private static final Set<Integer> OUTER_WINDOWS = Set.of(-7, -5);
    private static final int OUTER_WINDOWS_LOW = 5;
    private static final int OUTER_WINDOWS_HIGH = 6;
    private static final int DOCK_FROM = 4;
    private static final int DOCK_TO = 8;
    private static final int DOCK_TOP = 3;
    private static final int APRON_FROM = 3;
    private static final int APRON_TO = 9;
    private static final int APRON_OUT = 29;
    /* Inside: the consoles before the windows, the terminal among them, the door's console, the chairs. */
    private static final int CONSOLES_X = 17;
    private static final int CONSOLES_FROM = -3;
    private static final int CONSOLES_TO = 8;
    private static final int DOOR_CONSOLE_FROM = 9;
    private static final int DOOR_CONSOLE_TO = 10;
    private static final int TERMINAL_X = 17;
    private static final int TERMINAL_Z = -2;
    private static final int CHAIRS_X = 18;
    private static final Set<Integer> CHAIRS = Set.of(-2, 1, 4, 7);
    /* The step at the back, its railing on its front edge, the table on it; the status board; the lights. */
    private static final int STEP_FRONT = 23;
    private static final int STEP_BACK = 26;
    private static final int RAILING_FROM = -6;
    private static final int RAILING_TO = 9;
    private static final int TABLE_NEAR = 24;
    private static final int TABLE_FAR = 25;
    private static final int TABLE_FROM = 0;
    private static final int TABLE_TO = 4;
    private static final int BOARD_Z = -7;
    private static final int BOARD_NEAR = 17;
    private static final int BOARD_FAR = 22;
    private static final int BOARD_LOW = 3;
    private static final int BOARD_HIGH = 5;
    private static final Set<Integer> LIGHTS_X = Set.of(18, 23);
    private static final Set<Integer> LIGHTS_Z = Set.of(-6, -5, -2, -1, 2, 3, 6, 7);
    private static final char NONE = MultiblockPattern.IGNORE_CHAR;
    private static final double HALF = 0.5;

    public static final MultiblockPattern PATTERN = MultiblockPatterns.declare(ID, pattern());
    /* The places that take their way, their screens or their formed shapes as the hangar forms. */
    private static final List<Fitting> FITTINGS = gather();

    private HangarStructure() {
    }

    /** Declares the pattern, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares it.
    }

    /**
     * The bay of a hangar whose terminal is at {@code terminal}, standing as {@code turn} says: the box inside its
     * walls, the way its crane's bridge spans it, where along its rails the bridge stands and where its hoist hangs.
     */
    public static HangarBay bay(final BlockPos terminal, final Orientation turn) {
        final BlockPos a = world(terminal, turn, -BAY_X, 0, -BAY_Z);
        final BlockPos b = world(terminal, turn, BAY_X, BAY_TOP, BAY_Z);
        final BlockPos low = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()),
                Math.min(a.getZ(), b.getZ()));
        final BlockPos high = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()),
                Math.max(a.getZ(), b.getZ()));
        final int[] across = turn.apply(1, 0, 0);
        final Direction.Axis span = across[0] != 0 ? Direction.Axis.X : Direction.Axis.Z;
        return new HangarBay(low, high, span, world(terminal, turn, -BAY_X, BRIDGE, CRANE_Z),
                world(terminal, turn, 0, RAILS, CRANE_Z));
    }

    /** The places of the hangar's door, the lower door's and the slot's, in a hangar standing as {@code turn} says. */
    public static List<BlockPos> door(final BlockPos terminal, final Orientation turn) {
        final List<BlockPos> out = new ArrayList<>();
        for (int y = 0; y <= SLOT_TOP; y++) {
            for (int x = -LOWER_HALF; x <= LOWER_HALF; x++) {
                if (letter(x, y, FRONT) == 'D') {
                    out.add(world(terminal, turn, x, y, FRONT));
                }
            }
        }
        return out;
    }

    /** The places that take their way, their screens or their formed shapes as the hangar forms. */
    public static List<Fitting> fittings() {
        return FITTINGS;
    }

    /* The places of {@link #fittings()}, gathered once from the layout. */
    private static List<Fitting> gather() {
        final List<Fitting> out = new ArrayList<>();
        for (int y = BOTTOM; y <= TOP; y++) {
            for (int z = FRONT; z <= BACK; z++) {
                for (int x = WEST; x <= EAST; x++) {
                    final char letter = letter(x, y, z);
                    if ("CQHBEPLXYU".indexOf(letter) >= 0) {
                        out.add(new Fitting(x, y, z, letter));
                    }
                }
            }
        }
        return List.copyOf(out);
    }

    /** Where the pattern's place ({@code x}, {@code y}, {@code z}) stands, its terminal at {@code terminal}. */
    public static BlockPos world(final BlockPos terminal, final Orientation turn, final int x, final int y,
                                 final int z) {
        final int[] off = turn.apply(x - TERMINAL_X, y, z - TERMINAL_Z);
        return terminal.offset(off[0], off[1], off[2]);
    }

    /**
     * Where the pattern's point ({@code x}, {@code y}, {@code z}) stands, in blocks from its places' lowest corners:
     * the point (17, 3, -6.875) is on the face of the status board's lowest western block.
     */
    public static Vec3 point(final BlockPos terminal, final Orientation turn, final double x, final double y,
                             final double z) {
        final double[] in = {x - TERMINAL_X - HALF, y - HALF, z - TERMINAL_Z - HALF};
        return Vec3.atCenterOf(terminal).add(turn.xSign() * in[turn.xFrom()], turn.ySign() * in[turn.yFrom()],
                turn.zSign() * in[turn.zFrom()]);
    }

    /** The way the pattern's way {@code drawn} points in a hangar standing as {@code turn} says. */
    public static Direction turned(final Orientation turn, final Direction drawn) {
        final int[] way = turn.apply(drawn.getStepX(), drawn.getStepY(), drawn.getStepZ());
        return Direction.fromDelta(way[0], way[1], way[2]);
    }

    /**
     * The status board's face, as the pattern draws it: its western edge, its bottom, its eastern edge, its top, and
     * the plane its picture lies in, a little out from its blocks' faces, facing south into the control room.
     */
    public static double[] board() {
        return new double[] {BOARD_NEAR, BOARD_LOW, BOARD_FAR + 1, BOARD_HIGH + 1, BOARD_Z + 2.05 / 16};
    }

    /**
     * The Rocket Design table's top, as the pattern draws it: its western edge, its northern edge, its eastern edge,
     * its southern edge, and the height its blueprint lies at, a little over the table's dark glass.
     */
    public static double[] table() {
        return new double[] {TABLE_NEAR, TABLE_FROM, TABLE_FAR + 1, TABLE_TO + 1, 22.05 / 16};
    }

    /* The pattern, layer by layer from the floor, each letter where the layout puts it. */
    private static MultiblockPattern pattern() {
        final MultiblockPattern.Builder builder = MultiblockPattern.builder("rocket_hangar");
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
                .where('W', BlockMatch.blocks(block("hangar_wall")))
                .where('F', BlockMatch.blocks(block("hangar_frame")))
                .where('R', BlockMatch.blocks(block("hangar_roof")))
                .where('G', BlockMatch.blocks(block("hangar_window")))
                .where('O', BlockMatch.blocks(block("hangar_floor")))
                .where('L', BlockMatch.blocks(block("hangar_lamp")))
                .where('D', BlockMatch.blocks(block("bay_door")))
                .where('Z', BlockMatch.blocks(block("pit_rim")))
                .where('V', BlockMatch.blocks("minecraft:gravel"))
                .where('X', BlockMatch.blocks(block("crane_rail")))
                .where('Y', BlockMatch.blocks(block("crane_bridge")))
                .where('U', BlockMatch.blocks(block("crane_hoist")))
                .where('K', BlockMatch.blocks(block("loading_dock")))
                .where('T', BlockMatch.blocks(block("control_floor")))
                .where('S', BlockMatch.blocks(block("control_step")))
                .where('E', BlockMatch.blocks(block("step_railing")))
                .where('C', BlockMatch.blocks(block("hangar_console")))
                .where('Q', BlockMatch.blocks(block("door_console")))
                .where('H', BlockMatch.blocks(block("console_chair")))
                .where('A', BlockMatch.blocks(block("design_table")))
                .where('B', BlockMatch.blocks(block("status_board")))
                .where('P', BlockMatch.blocks(block("personnel_door")))
                .where('I', BlockMatch.blocks(block("ceiling_light")))
                .port('K', PortKind.ITEM_INPUT)
                .terminal(TERMINAL)
                // Its terminal faces east, to the chair before it, as a player in the chair's place sets it.
                .terminalFaces(Orientation.EAST)
                // The floor is laid into the ground; the walls stand on it.
                .ground(-BOTTOM)
                .build();
    }

    /* What stands at ({@code x}, {@code y}, {@code z}) of the hangar. */
    private static char letter(final int x, final int y, final int z) {
        if (x == TERMINAL_X && y == 0 && z == TERMINAL_Z) {
            return MultiblockPattern.TERMINAL_CHAR;
        }
        if (y == FLOOR) {
            return floor(x, z);
        }
        if (Math.abs(x) <= BAY_X && Math.abs(z) <= BAY_Z) {
            if (y == ROOF) {
                return BAY_LIGHTS_X.contains(x) && BAY_LIGHTS_Z.contains(z) ? 'I' : 'R';
            }
            return crane(x, y, z);
        }
        if (Math.abs(x) <= WALL_X && Math.abs(z) <= WALL_Z) {
            return wall(x, y, z);
        }
        if (x >= ROOM_WEST && x <= ROOM_EAST && z >= ROOM_FRONT && z <= ROOM_BACK && y <= ROOM_ROOF) {
            return room(x, y, z);
        }
        if (x > ROOM_EAST && x <= APRON_OUT && z >= APRON_FROM && z <= APRON_TO && y == 0) {
            return 'O';
        }
        return NONE;
    }

    /* The floor: concrete under the bay and its door, the crawler's lanes in it; tiles under the control room. */
    private static char floor(final int x, final int z) {
        final boolean bay = Math.abs(x) <= BAY_X && Math.abs(z) <= BAY_Z;
        final boolean door = z == FRONT && Math.abs(x) <= LOWER_HALF;
        if (bay || door) {
            return Math.abs(x) >= LANE_IN && Math.abs(x) <= LANE_OUT ? 'V' : 'O';
        }
        if (x >= ROOM_WEST && x <= ROOM_EAST && z >= ROOM_FRONT && z <= ROOM_BACK) {
            return 'T';
        }
        return NONE;
    }

    /* In the bay, under its roof: the crane, and the lamps on the long walls' inner faces. */
    private static char crane(final int x, final int y, final int z) {
        if (y == RAILS && Math.abs(x) == BAY_X) {
            return 'X';
        }
        if (Math.abs(x) == BAY_X && LAMPS.contains(z) && y % LAMPS_EVERY == LAMPS_FIRST) {
            return 'L';
        }
        if (y == BRIDGE && z == CRANE_Z) {
            return 'Y';
        }
        return y == RAILS && x == 0 && z == CRANE_Z ? 'U' : NONE;
    }

    /* A place of the hall's walls, or of the roof's rim over them. */
    private static char wall(final int x, final int y, final int z) {
        final boolean corner = Math.abs(x) == WALL_X && Math.abs(z) == WALL_Z;
        if (y == ROOF || corner) {
            return y <= ROOF ? 'F' : NONE;
        }
        if (y > ROOF) {
            return NONE;
        }
        if (z == FRONT) {
            return doorWall(x, y);
        }
        if (x == WALL_X && z >= ROOM_FRONT && z <= ROOM_BACK && y <= ROOM_ROOF) {
            return roomSide(y, z);
        }
        if (y == EAVES || y % BAND_EVERY == 0 && y > 0) {
            return 'F';
        }
        final boolean column = Math.abs(x) == WALL_X ? COLUMNS.contains(z) : COLUMNS.contains(x);
        return column ? 'F' : 'W';
    }

    /* The end wall the door is in: the lower door and the slot over it, their frames, the lintel, the hazard band. */
    private static char doorWall(final int x, final int y) {
        final int off = Math.abs(x);
        if (off <= LOWER_HALF && y <= LOWER_TOP || off <= SLOT_HALF && y > LOWER_TOP && y <= SLOT_TOP) {
            return 'D';
        }
        if (off == LOWER_FRAME && y <= BAND || off == SLOT_FRAME && y >= BAND && y <= LINTEL
                || off < SLOT_FRAME && y == LINTEL || y == EAVES) {
            return 'F';
        }
        return off > SLOT_FRAME && off <= LOWER_HALF && y == BAND ? 'Z' : 'W';
    }

    /* The hall's east wall where the control room stands against it: its windows into the bay, its inner door. */
    private static char roomSide(final int y, final int z) {
        if (z == INNER_DOOR_Z && y <= DOOR_TOP) {
            return 'P';
        }
        return z >= WINDOWS_FROM && z <= WINDOWS_TO && y >= WINDOWS_LOW && y <= WINDOWS_HIGH ? 'G' : 'W';
    }

    /* The control room: its walls and roof, and what stands in it. */
    private static char room(final int x, final int y, final int z) {
        if (y == ROOM_ROOF) {
            if (x == ROOM_EAST) {
                return 'F';
            }
            return LIGHTS_X.contains(x) && LIGHTS_Z.contains(z) ? 'I' : 'R';
        }
        if (x == ROOM_EAST) {
            return outerWall(y, z);
        }
        if (z == ROOM_FRONT || z == ROOM_BACK) {
            return 'W';
        }
        if (z == BOARD_Z && x >= BOARD_NEAR && x <= BOARD_FAR && y >= BOARD_LOW && y <= BOARD_HIGH) {
            return 'B';
        }
        return y == 0 ? furniture(x, z) : NONE;
    }

    /* The control room's outer wall: its door, the loading dock, its windows. */
    private static char outerWall(final int y, final int z) {
        if (z == OUTER_DOOR_Z && y <= DOOR_TOP) {
            return 'P';
        }
        if (z >= DOCK_FROM && z <= DOCK_TO && y <= DOCK_TOP) {
            return 'K';
        }
        return OUTER_WINDOWS.contains(z) && y >= OUTER_WINDOWS_LOW && y <= OUTER_WINDOWS_HIGH ? 'G' : 'W';
    }

    /* On the control room's floor: the consoles and their chairs, the door's console, the step and the table. */
    private static char furniture(final int x, final int z) {
        if (x == CONSOLES_X && z >= CONSOLES_FROM && z <= CONSOLES_TO) {
            return 'C';
        }
        if (x == CONSOLES_X && z >= DOOR_CONSOLE_FROM && z <= DOOR_CONSOLE_TO) {
            return 'Q';
        }
        if (x == CHAIRS_X && CHAIRS.contains(z)) {
            return 'H';
        }
        if (x < STEP_FRONT || x > STEP_BACK) {
            return NONE;
        }
        if (x >= TABLE_NEAR && x <= TABLE_FAR && z >= TABLE_FROM && z <= TABLE_TO) {
            return 'A';
        }
        return x == STEP_FRONT && z >= RAILING_FROM && z <= RAILING_TO ? 'E' : 'S';
    }

    private static String block(final String path) {
        return JsSpace.MODID + ":" + path;
    }

    /**
     * A place of the hangar that takes its way, its screen or its formed shape as the hangar forms.
     *
     * @param x      across, as the pattern draws it
     * @param y      up
     * @param z      along
     * @param letter the pattern's letter for what stands there
     */
    public record Fitting(int x, int y, int z, char letter) {

        /** The way it faces as the pattern draws it, or none for a piece that takes no way of its own. */
        public Direction drawn() {
            return switch (this.letter) {
                case 'C', 'Q' -> Direction.EAST;
                case 'H', 'E' -> Direction.WEST;
                case 'B' -> Direction.SOUTH;
                // A lamp faces into the bay from the wall it is set on.
                case 'L' -> this.x < 0 ? Direction.EAST : Direction.WEST;
                // The outer door faces out of the room, the inner one into it, so each swings into the room.
                case 'P' -> this.x == ROOM_EAST ? Direction.EAST : Direction.WEST;
                default -> Direction.NORTH;
            };
        }

        /** Its place along the console row, from its first console: which screen a console shows. */
        public int along() {
            return this.z - CONSOLES_FROM;
        }

        /** Whether it is the door's console's left block, as seen from its chair: the one further south. */
        public boolean left() {
            return this.z == DOOR_CONSOLE_TO;
        }
    }
}
