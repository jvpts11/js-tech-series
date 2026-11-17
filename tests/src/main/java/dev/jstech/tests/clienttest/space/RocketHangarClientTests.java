/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.multiblock.Orientation;
import dev.jstech.space.hangar.HangarBlocks;
import dev.jstech.space.hangar.HangarDoorMotion;
import dev.jstech.space.hangar.HangarPieceBlock;
import dev.jstech.space.hangar.HangarStructure;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import dev.jstech.space.hangar.PersonnelDoorSwing;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestHangars;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A Rocket Hangar on a player's game, the building built whole and formed: its crane's bridge is drawn as its yellow
 * girder over the bay, seen from a perch near the top of the bay; and a deck of work platforms along a wall shows its
 * hazard band and yellow railing along the edge it faces, seen from the bay's floor.
 */
public final class RocketHangarClientTests {

    /* The hangar's terminal, its door to the north; the bay's middle at (5, 2, 11) of the test's place. */
    private static final BlockPos TERMINAL = new BlockPos(22, 2, 9);
    private static final Orientation TURN = Orientation.NORTH;
    /* Where the player stands on the bay's floor, and the perch near its top the bridge is seen from. */
    private static final BlockPos EYE = new BlockPos(7, 2, 11);
    private static final BlockPos PERCH = new BlockPos(-4, 103, 8);
    /* The bridge's underside, away from the hoist under its middle, near the lamps of the west wall. */
    private static final Vec3 UNDER_BRIDGE = new Vec3(-7.5, 109.15, 11.5);
    /* A deck of work platforms along the east wall, facing west into the bay; the middle of its edge's band. */
    private static final int DECK_X0 = 17;
    private static final int DECK_X1 = 18;
    private static final int DECK_Z0 = 9;
    private static final int DECK_Z1 = 14;
    private static final int DECK_Y = 8;
    private static final Vec3 BAND = new Vec3(DECK_X0, DECK_Y + 0.75, 12.0);
    private static final int ARRIVE = 400;
    private static final int SETTLE = 20;
    /* How far round the terminal the floor's light is read, in blocks, every so many; how much dimmer is darker. */
    private static final int LIGHT_REACH = 30;
    private static final int LIGHT_STEP = 3;
    private static final int DIMMER = 2;
    private static final int PATCH = 4;
    private static final int CHANNEL = 0xFF;
    /* How far red and green stand over blue on the girder's underside, shaded as the game shades a face below. */
    private static final int YELLOW = 30;
    private static final int WINDOW = 10;
    /* How far red stands over blue in the band's yellow under the deck, in the bay's light; green half as far. */
    private static final int YELLOWER = 35;
    private static final double ENOUGH_YELLOW = 0.1;
    /*
     * The approved views: from the step at the back of the control room, looking over the consoles through the windows
     * into the bay; the status board's middle on its end wall; and from outside, off the door's corner, the building.
     */
    private static final BlockPos ON_STEP = new BlockPos(30, 2, 7);
    private static final Vec3 INTO_BAY = new Vec3(13.0, 6.0, 15.0);
    private static final Vec3 BOARD_MIDDLE = new Vec3(25.0, 6.5, 4.15);
    private static final BlockPos OUTSIDE = new BlockPos(-45, 2, -75);
    private static final Vec3 BUILDING = new Vec3(8.0, 48.0, 11.0);
    /*
     * A lit screen's pixel: bright and strong in colour. The row of consoles in the view from the step, below and to
     * the left of the crosshair, the player's hand left out; how much of it the screens fill at least. The board round
     * the crosshair, and how much of it its picture's colours fill.
     */
    private static final int SCREEN_BRIGHT = 150;
    private static final int SCREEN_STRONG = 80;
    private static final int ROW_LEFT = -200;
    private static final int ROW_RIGHT = 80;
    private static final int ROW_TOP = 30;
    private static final int ROW_BOTTOM = 60;
    private static final double ENOUGH_SCREENS = 0.01;
    private static final int BOARD_WINDOW = 40;
    private static final double ENOUGH_BOARD = 0.05;
    /*
     * The bay door seen from outside: across, its middle; the eye up from the floor and out from the door's front, in
     * the hangar's pattern; and up the door, where it looks. How much two pixels may differ and be the same, in each
     * channel, and how many must differ for the door to have moved.
     */
    private static final double DOOR_MIDDLE = 0.5;
    private static final double EYE_UP = 8;
    private static final double OUT_FRONT = 45;
    private static final double LOOK_UP = 14;
    private static final int SAME = 6;
    private static final int MOVED = 400;
    /*
     * The control room's outer personnel door, its foot's place in the pattern; a player in the room a few blocks from
     * it, along it, at a person's eye's height, and the door's middle across.
     */
    private static final int[] OUTER_DOOR = {27, 0, -4};
    private static final double IN_THE_ROOM = 23.5;
    private static final double AT_THE_DOOR = 27.5;
    private static final double BY_THE_DOOR = -3.5;
    private static final double PERSON_EYE = 1.6;

    private RocketHangarClientTests() {
    }

    /*
     * The control room's outer personnel door seen from inside the room: shut, swinging, and open, each drawn otherwise
     * than the one before; half way through its swing, its leaf turned part way about its hinge.
     */
    @ClientTest(timeoutTicks = 1600)
    public static void personnelDoor_swingsItsLeafAboutItsHingeAsItOpens(final ClientTestContext ctx) {
        final BlockPos terminal = ctx.abs(TERMINAL);
        final int[][] frames = new int[3][];
        final Vec3[] look = new Vec3[1];
        ctx.afterTest(() -> {
            ctx.mc().options.hideGui = false;
            ctx.server().execute(() -> TestHangars.clear(ctx.serverLevel(), terminal, TURN));
        });
        ctx.thenServer(0, level -> TestHangars.build(level, terminal, TURN))
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(terminal)
                        instanceof HangarTerminalBlockEntity hangar && hangar.formed(), ARRIVE,
                        "the formed hangar to reach the player")
                .thenServer(0, level -> {
                    final Vec3 eye = HangarStructure.point(terminal, TURN, IN_THE_ROOM, PERSON_EYE, BY_THE_DOOR);
                    look[0] = HangarStructure.point(terminal, TURN, AT_THE_DOOR, PERSON_EYE - 0.5, BY_THE_DOOR);
                    ctx.serverPlayer().teleportTo(level, eye.x, eye.y - ctx.serverPlayer().getEyeHeight(), eye.z, 0,
                            0);
                })
                .then(SETTLE, () -> {
                    ctx.lookAt(look[0]);
                    ctx.mc().options.hideGui = true;
                })
                .then(SETTLE * 2, () -> frames[0] = frame(ctx))
                .thenScreenshot(0, "hangar-personnel-door-shut")
                .thenServer(0, level -> {
                    final BlockPos door = TestHangars.at(terminal, TURN, OUTER_DOOR[0], OUTER_DOOR[1], OUTER_DOOR[2]);
                    level.getBlockState(door).useWithoutItem(level, ctx.serverPlayer(), new BlockHitResult(
                            Vec3.atCenterOf(door), Direction.WEST, door, false));
                })
                .then(PersonnelDoorSwing.TICKS / 2, () -> frames[1] = frame(ctx))
                .thenScreenshot(0, "hangar-personnel-door-swinging")
                .then(PersonnelDoorSwing.TICKS + SETTLE, () -> {
                    frames[2] = frame(ctx);
                    ctx.assertTrue(changed(frames[0], frames[1]) > MOVED && changed(frames[1], frames[2]) > MOVED,
                            "half way through its swing, its leaf is drawn between shut and open: "
                                    + changed(frames[0], frames[1]) + " and " + changed(frames[1], frames[2])
                                    + " pixels");
                })
                .thenScreenshot(0, "hangar-personnel-door-open");
    }

    /*
     * The bay door seen from outside, square on: shut, moving, and open, each drawn otherwise than the one before; half
     * way, its lower door's halves partly slid apart and its slot's leaves partly folded.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void bayDoor_drawsItsHalvesSlidingApartAndItsLeavesFoldingAsItOpens(final ClientTestContext ctx) {
        final BlockPos terminal = ctx.abs(TERMINAL);
        final int[][] frames = new int[3][];
        final Vec3[] look = new Vec3[1];
        ctx.afterTest(() -> {
            ctx.mc().options.hideGui = false;
            ctx.server().execute(() -> TestHangars.clear(ctx.serverLevel(), terminal, TURN));
        });
        ctx.thenServer(0, level -> TestHangars.build(level, terminal, TURN))
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(terminal)
                        instanceof HangarTerminalBlockEntity hangar && hangar.formed(), ARRIVE,
                        "the formed hangar to reach the player")
                .thenServer(0, level -> {
                    final int front = HangarStructure.doorCells(terminal, TURN).getFirst().z();
                    final Vec3 eye = HangarStructure.point(terminal, TURN, DOOR_MIDDLE, EYE_UP, front - OUT_FRONT);
                    look[0] = HangarStructure.point(terminal, TURN, DOOR_MIDDLE, LOOK_UP, front);
                    ctx.serverPlayer().teleportTo(level, eye.x, eye.y - ctx.serverPlayer().getEyeHeight(), eye.z, 0,
                            0);
                })
                .then(SETTLE, () -> {
                    ctx.lookAt(look[0]);
                    ctx.mc().options.hideGui = true;
                })
                .then(SETTLE * 2, () -> frames[0] = frame(ctx))
                .thenScreenshot(0, "hangar-bay-door-shut")
                .thenServer(0, level -> ((HangarTerminalBlockEntity) level.getBlockEntity(terminal)).door(true))
                .then(HangarDoorMotion.TICKS / 2, () -> frames[1] = frame(ctx))
                .thenScreenshot(0, "hangar-bay-door-moving")
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(terminal) instanceof HangarTerminalBlockEntity
                        hangar && hangar.doorOpen() && !hangar.doorMoving(), ARRIVE, "the door all the way open")
                .then(SETTLE, () -> {
                    frames[2] = frame(ctx);
                    ctx.assertTrue(changed(frames[0], frames[1]) > MOVED && changed(frames[1], frames[2]) > MOVED,
                            "half way, it is drawn between shut and open: " + changed(frames[0], frames[1]) + " and "
                                    + changed(frames[1], frames[2]) + " pixels");
                })
                .thenScreenshot(0, "hangar-bay-door-open");
    }

    @ClientTest(timeoutTicks = 1600)
    public static void hangar_drawsItsFormedBridgeAsAGirder(final ClientTestContext ctx) {
        final int[][] seen = new int[1][];
        ctx.afterTest(() -> ctx.server().execute(() -> TestHangars.clear(ctx.serverLevel(), ctx.abs(TERMINAL),
                TURN)));
        ctx.thenServer(0, level -> build(ctx, level))
                .thenTeleport(SETTLE, EYE, Direction.EAST)
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(TERMINAL))
                        instanceof HangarTerminalBlockEntity terminal && terminal.formed() && ctx.mc().level
                        .getBlockState(bridge(ctx)).getValue(HangarPieceBlock.FORMED), ARRIVE,
                        "the formed hangar to reach the player")
                .thenTeleport(SETTLE, PERCH, Direction.SOUTH)
                .then(SETTLE, () -> ctx.lookAt(UNDER_BRIDGE.add(Vec3.atLowerCornerOf(ctx.origin()))))
                .then(SETTLE, () -> seen[0] = mean(ctx))
                .thenScreenshot(0, "hangar-formed")
                .then(0, () -> ctx.assertTrue(seen[0][0] - seen[0][2] > YELLOW && seen[0][1] - seen[0][2] > YELLOW,
                        "the bridge's girder is drawn yellow over the bay: " + seen[0][0] + ", " + seen[0][1] + ", "
                                + seen[0][2]))
                // Down on the floor, up at the work platforms' deck: its hazard band and its railing along its edge.
                .thenTeleport(SETTLE, EYE, Direction.EAST)
                .then(SETTLE, () -> ctx.lookAt(BAND.add(Vec3.atLowerCornerOf(ctx.origin()))))
                .then(SETTLE, () -> { })
                .thenScreenshot(0, "hangar-platform")
                .then(0, () -> {
                    final double yellow = yellow(ctx);
                    ctx.assertTrue(yellow >= ENOUGH_YELLOW, "the platform's edge is drawn with its hazard band and"
                            + " railing: " + Math.round(yellow * 100) + "% yellow round the crosshair");
                });
    }

    @ClientTest(timeoutTicks = 1600)
    public static void hangar_standsAsItsApprovedBuilding(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.server().execute(() -> TestHangars.clear(ctx.serverLevel(), ctx.abs(TERMINAL),
                TURN)));
        ctx.thenServer(0, level -> TestHangars.build(level, ctx.abs(TERMINAL), TURN))
                .thenTeleport(SETTLE, ON_STEP, Direction.WEST)
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(TERMINAL))
                        instanceof HangarTerminalBlockEntity terminal && terminal.formed(), ARRIVE,
                        "the formed hangar to reach the player")
                // From the step at the back of the control room, over the consoles to the windows into the bay.
                .then(SETTLE, () -> ctx.lookAt(INTO_BAY.add(Vec3.atLowerCornerOf(ctx.origin()))))
                .then(SETTLE, () -> { })
                .thenScreenshot(0, "hangar-control-room")
                .then(0, () -> {
                    final double lit = share(ctx, ROW_LEFT, ROW_RIGHT, ROW_TOP, ROW_BOTTOM,
                            RocketHangarClientTests::screenLit);
                    ctx.assertTrue(lit >= ENOUGH_SCREENS, "the consoles' screens are lit along the row: "
                            + Math.round(lit * 10_000) / 100.0 + "% of the row");
                })
                // The status board on the end wall, its picture lit.
                .then(SETTLE, () -> ctx.lookAt(BOARD_MIDDLE.add(Vec3.atLowerCornerOf(ctx.origin()))))
                .then(SETTLE, () -> { })
                .thenScreenshot(0, "hangar-status-board")
                .then(0, () -> {
                    final double lit = share(ctx, -BOARD_WINDOW, BOARD_WINDOW, -BOARD_WINDOW, BOARD_WINDOW,
                            RocketHangarClientTests::screenLit);
                    ctx.assertTrue(lit >= ENOUGH_BOARD, "the status board's picture is lit round the crosshair: "
                            + Math.round(lit * 100) + "%");
                })
                // From outside, far off its door's corner: the whole building, as the approved exterior shows it.
                .thenTeleport(SETTLE, OUTSIDE, Direction.SOUTH)
                .then(SETTLE, () -> ctx.lookAt(BUILDING.add(Vec3.atLowerCornerOf(ctx.origin()))))
                .then(SETTLE * 2, () -> { })
                .thenScreenshot(0, "hangar-exterior");
    }

    /*
     * The formed hangar saved and opened again: the light over its floor, its lamps' and the sky's, is as it was before,
     * place by place, not dark until a block beside changes.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void hangar_keepsItsLightThroughASaveAndAReload(final ClientTestContext ctx) {
        final int[][] before = new int[1][];
        ctx.afterTest(() -> ctx.server().execute(() -> TestHangars.clear(ctx.serverLevel(), ctx.abs(TERMINAL),
                TURN)));
        ctx.thenServer(0, level -> TestHangars.build(level, ctx.abs(TERMINAL), TURN))
                .thenTeleport(SETTLE, ON_STEP, Direction.WEST)
                .thenWaitUntilServer(level -> level.getBlockEntity(ctx.abs(TERMINAL))
                        instanceof HangarTerminalBlockEntity terminal && terminal.formed(), ARRIVE,
                        "the hangar to form", level -> "not formed")
                .thenServer(SETTLE, level -> before[0] = light(level, ctx.abs(TERMINAL)))
                .thenSaveAndReload(0)
                .thenWaitUntilServer(level -> level.getBlockEntity(ctx.abs(TERMINAL))
                        instanceof HangarTerminalBlockEntity terminal && terminal.formed(), ARRIVE,
                        "the hangar to be formed again, opened from its files", level -> "not formed")
                .thenServer(SETTLE, level -> {
                    final int[] after = light(level, ctx.abs(TERMINAL));
                    int darker = 0;
                    int lit = 0;
                    for (int i = 0; i < after.length; i++) {
                        lit += before[0][i] > 0 ? 1 : 0;
                        darker += after[i] < before[0][i] - DIMMER ? 1 : 0;
                    }
                    ctx.assertTrue(lit > 0 && darker == 0, "opened again, the hangar's floor is as lit as it was: "
                            + darker + " of " + lit + " lit places darker");
                });
    }

    /* The light over the floor round the terminal, the brighter of the sky's and the lamps', place by place. */
    private static int[] light(final ServerLevel level, final BlockPos terminal) {
        final List<Integer> out = new ArrayList<>();
        for (int dx = -LIGHT_REACH; dx <= LIGHT_REACH; dx += LIGHT_STEP) {
            for (int dz = -LIGHT_REACH; dz <= LIGHT_REACH; dz += LIGHT_STEP) {
                out.add(level.getMaxLocalRawBrightness(terminal.offset(dx, 0, dz)));
            }
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    /*
     * Whether a pixel is a lit screen's: one of its pictures' cyan, green, amber or red, bright and strong, which
     * neither the room's grey nor the windows' blue-grey nor the lamps' white is.
     */
    private static boolean screenLit(final int argb) {
        final int r = argb >> 16 & CHANNEL;
        final int g = argb >> 8 & CHANNEL;
        final int b = argb & CHANNEL;
        final int most = Math.max(r, Math.max(g, b));
        return most > SCREEN_BRIGHT && most - Math.min(r, Math.min(g, b)) > SCREEN_STRONG;
    }

    /*
     * How much of a box round the crosshair, from {@code left} to {@code right} and from {@code top} to
     * {@code bottom} pixels off it, is as {@code is} says.
     */
    private static double share(final ClientTestContext ctx, final int left, final int right, final int top,
                                final int bottom, final IntPredicate is) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2;
        int found = 0;
        int count = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = left; dx <= right; dx++) {
                for (int dy = top; dy <= bottom; dy++) {
                    if (is.test(frame.pixel(x + dx, y + dy))) {
                        found++;
                    }
                    count++;
                }
            }
        }
        return (double) found / count;
    }

    /* A block of the crane's bridge, over the bay's middle line. */
    private static BlockPos bridge(final ClientTestContext ctx) {
        return TestHangars.at(ctx.abs(TERMINAL), TURN, -4, 107, 0);
    }

    /* How much of a window round the crosshair is yellow, red and green well over blue. */
    private static double yellow(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2;
        int yellow = 0;
        int count = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = -WINDOW; dx <= WINDOW; dx++) {
                for (int dy = -WINDOW; dy <= WINDOW; dy++) {
                    final int argb = frame.pixel(x + dx, y + dy);
                    final int b = argb & CHANNEL;
                    if ((argb >> 16 & CHANNEL) > b + YELLOWER && (argb >> 8 & CHANNEL) > b + YELLOWER / 2) {
                        yellow++;
                    }
                    count++;
                }
            }
        }
        return (double) yellow / count;
    }

    /*
     * The hangar built whole, a deck of work platforms along its east wall facing into the bay, and a perch of barrier
     * near the top of the bay to stand on.
     */
    private static void build(final ClientTestContext ctx, final ServerLevel level) {
        TestHangars.build(level, ctx.abs(TERMINAL), TURN);
        for (int x = DECK_X0; x <= DECK_X1; x++) {
            for (int z = DECK_Z0; z <= DECK_Z1; z++) {
                level.setBlockAndUpdate(ctx.abs(new BlockPos(x, DECK_Y, z)), HangarBlocks.WORK_PLATFORM.get()
                        .defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
            }
        }
        level.setBlockAndUpdate(ctx.abs(PERCH.below()), Blocks.BARRIER.defaultBlockState());
    }

    /* The mean colour of a patch beside the crosshair, which whitens its own middle. */
    private static int[] mean(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2 + PATCH;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2 + PATCH;
        final long[] sum = new long[3];
        int count = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = -PATCH / 2; dx <= PATCH / 2; dx++) {
                for (int dy = -PATCH / 2; dy <= PATCH / 2; dy++) {
                    final int argb = frame.pixel(x + dx, y + dy);
                    sum[0] += argb >> 16 & CHANNEL;
                    sum[1] += argb >> 8 & CHANNEL;
                    sum[2] += argb & CHANNEL;
                    count++;
                }
            }
        }
        return new int[] {(int) (sum[0] / count), (int) (sum[1] / count), (int) (sum[2] / count)};
    }

    /* How many pixels of {@code a} and {@code b} differ. */
    private static int changed(final int[] a, final int[] b) {
        int out = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            if (differ(a[i], b[i])) {
                out++;
            }
        }
        return out;
    }

    /* The whole frame, its pixels row by row. */
    private static int[] frame(final ClientTestContext ctx) {
        final int width = ctx.mc().getWindow().getGuiScaledWidth();
        final int height = ctx.mc().getWindow().getGuiScaledHeight();
        final int[] out = new int[width * height];
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    out[y * width + x] = frame.pixel(x, y);
                }
            }
        }
        return out;
    }

    /* Whether two pixels differ by more than {@link #SAME} in a channel. */
    private static boolean differ(final int a, final int b) {
        for (int shift = 0; shift <= 16; shift += 8) {
            if (Math.abs((a >> shift & CHANNEL) - (b >> shift & CHANNEL)) > SAME) {
                return true;
            }
        }
        return false;
    }
}
