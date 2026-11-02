/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.multiblock.Orientation;
import dev.jstech.space.hangar.DoorConsoleBlock;
import dev.jstech.space.hangar.HangarBay;
import dev.jstech.space.hangar.HangarBlocks;
import dev.jstech.space.hangar.HangarConsoleBlock;
import dev.jstech.space.hangar.HangarPieceBlock;
import dev.jstech.space.hangar.HangarStructure;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import dev.jstech.space.hangar.PersonnelDoorBlock;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.space.TestHangars;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Rocket Hangar, one building of one size, built block by block from its pattern: it forms whichever way it is
 * turned, its bay 29 across, 37 long and 108 high wherever it stands; its fittings take their places as it forms, the
 * consoles lighting their screens along the row, the chairs, the status board and the doors turning the way the
 * building has them, the crane's pieces taking their shapes; and as a panel of its wall goes, it comes apart and they
 * go dark. The bay door opens and shuts from its console, the personnel doors as they are used, and a work platform
 * set in the bay takes its shape with its railing where no platform follows it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RocketHangarGameTests {

    private static final String ARENA = "rocket_hangar";
    private static final String BATCH = "jsspace_hangar";
    /* The terminal in the arena's middle, its floor a block under it on the arena's. */
    private static final BlockPos TERMINAL = new BlockPos(40, 2, 42);
    private static final int ACROSS = 29;
    private static final int ALONG = 37;
    private static final int HIGH = 108;
    private static final int FORM_TIME = 400;
    /* Places of the building, as its pattern draws them. */
    private static final int[] FIRST_CONSOLE = {17, 0, -3};
    private static final int[] THIRD_CONSOLE = {17, 0, -1};
    private static final int[] DOOR_RIGHT = {17, 0, 9};
    private static final int[] DOOR_LEFT = {17, 0, 10};
    private static final int[] CHAIR = {18, 0, -2};
    private static final int[] BOARD = {17, 3, -7};
    private static final int[] RAILING = {23, 0, 0};
    private static final int[] WEST_RAIL = {-14, 106, 5};
    private static final int[] HOIST = {0, 106, 0};
    private static final int[] LOWER_DOOR = {0, 0, -19};
    private static final int[] SLOT = {0, 60, -19};
    private static final int[] PANEL = {-15, 50, 3};
    private static final int[] INNER_DOOR = {15, 0, -6};
    private static final int[] OUTER_DOOR = {27, 0, -4};
    private static final int[] TABLE = {24, 0, 2};
    private static final int[] PLATFORM = {0, 20, 0};
    /*
     * The bay lit through: its grid of lights' first place, this far in from the bay's lowest corner each way; the
     * places farthest from its walls' lamps and its roof's, the middle of its floor and high up its middle, and the
     * light a crew needs there, where the dark kept none.
     */
    private static final int LIGHT = 2;
    private static final int[][] DARKEST = {{0, 0, 0}, {0, 90, 0}, {-5, 0, 5}};
    private static final int LIT = 7;
    /* Where on a console's block its screen is pressed: over its middle opens the door, under it shuts it. */
    private static final double UPPER = 1.2;
    private static final double LOWER = 0.9;

    private RocketHangarGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH + "_north", timeoutTicks = FORM_TIME)
    public static void hangar_formsTurnedNorthAndItsFittingsTakeTheirPlaces(final GameTestHelper helper) {
        formsTurned(helper, Orientation.NORTH);
    }

    @GameTest(template = ARENA, batch = BATCH + "_east", timeoutTicks = FORM_TIME)
    public static void hangar_formsTurnedEastAndItsFittingsTakeTheirPlaces(final GameTestHelper helper) {
        formsTurned(helper, Orientation.EAST);
    }

    @GameTest(template = ARENA, batch = BATCH + "_south", timeoutTicks = FORM_TIME)
    public static void hangar_formsTurnedSouthAndItsFittingsTakeTheirPlaces(final GameTestHelper helper) {
        formsTurned(helper, Orientation.SOUTH);
    }

    @GameTest(template = ARENA, batch = BATCH + "_west", timeoutTicks = FORM_TIME)
    public static void hangar_formsTurnedWestAndItsFittingsTakeTheirPlaces(final GameTestHelper helper) {
        formsTurned(helper, Orientation.WEST);
    }

    @GameTest(template = ARENA, batch = BATCH + "_apart", timeoutTicks = FORM_TIME)
    public static void hangar_comesApartWithoutAPanelAndItsFittingsGoDark(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Orientation turn = Orientation.NORTH;
        final BlockPos terminal = TestHangars.build(level, helper.absolutePos(TERMINAL), turn);
        final BlockPos panel = place(terminal, turn, PANEL);
        final BlockPos[] light = new BlockPos[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(hangar(level, terminal).formed(), "the building forms"))
                .thenWaitUntil(() -> {
                    for (final int[] spot : DARKEST) {
                        final int lit = level.getBrightness(LightLayer.BLOCK, place(terminal, turn, spot));
                        helper.assertTrue(lit >= LIT, "its bay is lit through, " + lit + " at " + spot[0] + ", "
                                + spot[1] + ", " + spot[2] + " of it");
                    }
                    light[0] = hangar(level, terminal).bay().orElseThrow().low().offset(LIGHT, LIGHT, LIGHT);
                    helper.assertTrue(level.getBlockState(light[0]).is(Blocks.LIGHT), "a light stands in its grid");
                })
                .thenExecute(() -> level.setBlock(panel, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL))
                .thenWaitUntil(() -> helper.assertTrue(!hangar(level, terminal).formed()
                        && hangar(level, terminal).bay().isEmpty(), "a panel of its wall gone, it comes apart"))
                .thenExecute(() -> {
                    helper.assertTrue(level.getBlockState(light[0]).isAir(), "its bay's lights go with it");
                    helper.assertTrue(level.getBlockState(place(terminal, turn, FIRST_CONSOLE))
                            .getValue(HangarConsoleBlock.SCREEN) == HangarConsoleBlock.Screen.OFF,
                            "its consoles go dark");
                    helper.assertTrue(!level.getBlockState(place(terminal, turn, WEST_RAIL))
                            .getValue(HangarPieceBlock.FORMED), "its crane's pieces are blocks of the kit again");
                    level.setBlock(panel, HangarBlocks.WALL.get().defaultBlockState(), Block.UPDATE_ALL);
                })
                .thenWaitUntil(() -> helper.assertTrue(hangar(level, terminal).formed()
                        && level.getBlockState(place(terminal, turn, FIRST_CONSOLE)).getValue(
                        HangarConsoleBlock.SCREEN) == HangarConsoleBlock.Screen.ROCKET
                        && level.getBlockState(light[0]).is(Blocks.LIGHT),
                        "the panel back, it forms again and lights up, its bay too"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH + "_doors", timeoutTicks = FORM_TIME)
    public static void doors_openFromTheConsoleAndAsTheyAreUsed(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Orientation turn = Orientation.NORTH;
        final BlockPos terminal = TestHangars.build(level, helper.absolutePos(TERMINAL), turn);
        final BlockPos console = place(terminal, turn, DOOR_RIGHT);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(hangar(level, terminal).formed(), "the building forms"))
                .thenExecute(() -> {
                    final FakePlayer player = player(helper);
                    press(helper, player, console, UPPER);
                    helper.assertTrue(open(level, terminal, turn, LOWER_DOOR) && open(level, terminal, turn, SLOT)
                            && hangar(level, terminal).doorOpen(), "pressed high, the console opens the bay door,"
                            + " the lower door and the slot together: " + level.getBlockState(place(terminal, turn,
                            LOWER_DOOR)) + ", the console " + level.getBlockState(console) + " finds its hangar: "
                            + HangarTerminalBlockEntity.holding(level, console).isPresent());
                    press(helper, player, console, LOWER);
                    helper.assertTrue(!open(level, terminal, turn, LOWER_DOOR) && !open(level, terminal, turn, SLOT),
                            "pressed low, it shuts it");
                    for (final int[] door : new int[][] {INNER_DOOR, OUTER_DOOR}) {
                        final BlockPos foot = place(terminal, turn, door);
                        press(helper, player, foot, LOWER);
                        helper.assertTrue(level.getBlockState(foot).getValue(PersonnelDoorBlock.OPEN)
                                && level.getBlockState(foot.above()).getValue(PersonnelDoorBlock.OPEN),
                                "a personnel door used opens, its other block with it");
                        press(helper, player, foot.above(), LOWER);
                        helper.assertTrue(!level.getBlockState(foot).getValue(PersonnelDoorBlock.OPEN),
                                "used again, it shuts");
                    }
                    helper.assertTrue(HangarTerminalBlockEntity.holding(level, place(terminal, turn, TABLE))
                            .filter(found -> found.getBlockPos().equals(terminal)).isPresent(),
                            "the Rocket Design table knows its hangar, whose design screen it opens");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH + "_platforms", timeoutTicks = FORM_TIME)
    public static void platform_setInTheBayTakesItsShape(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Orientation turn = Orientation.NORTH;
        final BlockPos terminal = TestHangars.build(level, helper.absolutePos(TERMINAL), turn);
        final BlockPos back = place(terminal, turn, PLATFORM);
        final BlockPos front = back.north();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(hangar(level, terminal).formed(), "the building forms"))
                .thenExecute(() -> {
                    final BlockState platform = HangarBlocks.WORK_PLATFORM.get().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
                    level.setBlock(back, platform, Block.UPDATE_ALL);
                    level.setBlock(front, platform, Block.UPDATE_ALL);
                    helper.assertTrue(formed(level, back) && formed(level, front)
                            && hangar(level, terminal).platforms().containsAll(List.of(back, front)),
                            "platforms set in the bay take their shapes, and the hangar draws them");
                    helper.assertTrue(level.getBlockState(front).getValue(HangarPieceBlock.EDGE)
                            && !level.getBlockState(back).getValue(HangarPieceBlock.EDGE),
                            "the railing stands at the edge, where no platform follows");
                    level.setBlock(front, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    helper.assertTrue(level.getBlockState(back).getValue(HangarPieceBlock.EDGE)
                            && !hangar(level, terminal).platforms().contains(front),
                            "the front one taken, the other has its railing");
                })
                .thenSucceed();
    }

    /* Builds the hangar turned {@code turn}, and checks it forms and its fittings take their places as it stands. */
    private static void formsTurned(final GameTestHelper helper, final Orientation turn) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestHangars.build(level, helper.absolutePos(TERMINAL), turn);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(hangar(level, terminal).formed(), "the building forms turned "
                        + turn))
                .thenExecute(() -> {
                    final HangarTerminalBlockEntity hangar = hangar(level, terminal);
                    final HangarBay bay = hangar.bay().orElseThrow();
                    final boolean along = turn == Orientation.NORTH || turn == Orientation.SOUTH;
                    helper.assertTrue(bay.height() == HIGH && bay.width() == (along ? ACROSS : ALONG)
                            && bay.depth() == (along ? ALONG : ACROSS), "its bay is 29 across, 37 long and 108"
                            + " high: " + bay);
                    helper.assertTrue(bay.pad().equals(place(terminal, turn, new int[] {0, 0, 0})),
                            "the rocket stands in the bay's middle");
                    helper.assertTrue(hangar.number() > 0, "it takes its number among the world's hangars");
                    final Direction toChair = HangarStructure.turned(turn, Direction.EAST);
                    final BlockState first = level.getBlockState(place(terminal, turn, FIRST_CONSOLE));
                    helper.assertTrue(first.getValue(HangarConsoleBlock.SCREEN) == HangarConsoleBlock.Screen.ROCKET
                            && level.getBlockState(place(terminal, turn, THIRD_CONSOLE)).getValue(
                            HangarConsoleBlock.SCREEN) == HangarConsoleBlock.Screen.LIST
                            && first.getValue(HorizontalDirectionalBlock.FACING) == toChair,
                            "the consoles light their screens along the row, toward their chairs: " + first);
                    helper.assertTrue(level.getBlockState(place(terminal, turn, DOOR_LEFT)).getValue(
                            DoorConsoleBlock.SCREEN) == HangarConsoleBlock.Screen.LEFT && level.getBlockState(
                            place(terminal, turn, DOOR_RIGHT)).getValue(DoorConsoleBlock.SCREEN)
                            == HangarConsoleBlock.Screen.RIGHT, "the door's console shows its picture, half on each");
                    helper.assertTrue(facing(level, terminal, turn, CHAIR) == toChair.getOpposite()
                            && facing(level, terminal, turn, RAILING) == toChair.getOpposite()
                            && facing(level, terminal, turn, BOARD) == HangarStructure.turned(turn, Direction.SOUTH),
                            "the chairs and the step's railing face the consoles, the status board the room");
                    final BlockState rail = level.getBlockState(place(terminal, turn, WEST_RAIL));
                    helper.assertTrue(rail.getValue(HangarPieceBlock.FORMED) && rail.getValue(
                            HorizontalDirectionalBlock.FACING) == HangarStructure.turned(turn, Direction.EAST)
                            && level.getBlockState(place(terminal, turn, HOIST)).getValue(HangarPieceBlock.FORMED),
                            "the crane's pieces take their shapes, its rails looking into the bay: " + rail);
                })
                .thenSucceed();
    }

    private static HangarTerminalBlockEntity hangar(final ServerLevel level, final BlockPos terminal) {
        return (HangarTerminalBlockEntity) level.getBlockEntity(terminal);
    }

    private static BlockPos place(final BlockPos terminal, final Orientation turn, final int[] at) {
        return TestHangars.at(terminal, turn, at[0], at[1], at[2]);
    }

    private static Direction facing(final ServerLevel level, final BlockPos terminal, final Orientation turn,
                                    final int[] at) {
        return level.getBlockState(place(terminal, turn, at)).getValue(HorizontalDirectionalBlock.FACING);
    }

    private static boolean open(final ServerLevel level, final BlockPos terminal, final Orientation turn,
                                final int[] at) {
        final BlockState door = level.getBlockState(place(terminal, turn, at));
        return door.is(HangarBlocks.BAY_DOOR.get()) && door.getValue(HangarPieceBlock.OPEN);
    }

    private static boolean formed(final ServerLevel level, final BlockPos at) {
        return level.getBlockState(at).getValue(HangarPieceBlock.FORMED);
    }

    /* The shared fake player, empty-handed and standing. */
    private static FakePlayer player(final GameTestHelper helper) {
        final FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.getInventory().clearContent();
        player.setShiftKeyDown(false);
        return player;
    }

    /* Uses the block at {@code at} empty-handed, pressing it {@code high} blocks up from its foot, on its top. */
    private static void press(final GameTestHelper helper, final FakePlayer player, final BlockPos at,
                              final double high) {
        final Vec3 hit = new Vec3(at.getX() + 0.5, at.getY() + high, at.getZ() + 0.5);
        helper.getLevel().getBlockState(at).useWithoutItem(helper.getLevel(), player, new BlockHitResult(hit,
                Direction.UP, at, false));
    }
}
