/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.conveyor.BeltLane;
import dev.jstech.core.conveyor.BeltSide;
import dev.jstech.core.conveyor.BeltSlope;
import dev.jstech.core.conveyor.ConveyorBlock;
import dev.jstech.core.conveyor.ConveyorBlockEntity;
import dev.jstech.core.conveyor.DiverterBlock;
import dev.jstech.core.conveyor.ElevatorBlock;
import dev.jstech.core.inventory.ItemFilter;
import dev.jstech.core.jstechseries.tools.SharedTools;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestConveyors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's pieces of conveyor beyond the plain belt: belts that rise and fall a block, laid to meet one another as
 * rails do and turned by a wrench; chutes that drop items into what is below or out of their bottom; bucket elevators
 * that lift items and tip them out to their front; a splitter that deals items out in turn, a join that takes from each
 * side in turn, a diverter that turns aside what its filter names and learns it from a click; a machine that works
 * items on the belt under it; and a belt broken, which lets fall what was on it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ConveyorPieceGameTests {

    private static final String ARENA = "empty";
    private static final int Y = 2;
    private static final int TIMEOUT = 200;

    private ConveyorPieceGameTests() {
    }

    @GameTest(template = ARENA)
    public static void risingBelts_carryItemsUpABlock(final GameTestHelper helper) {
        belt(helper, new BlockPos(1, Y, 2), BeltSlope.FLAT);
        belt(helper, new BlockPos(2, Y, 2), BeltSlope.RISING);
        belt(helper, new BlockPos(3, Y + 1, 2), BeltSlope.FLAT);
        helper.setBlock(new BlockPos(4, Y + 1, 2), Blocks.CHEST);
        helper.assertTrue(!helper.getBlockState(new BlockPos(2, Y, 2)).getValue(ConveyorBlock.END),
                "the rising belt carries its run on into the belt above and ahead");
        piece(helper, new BlockPos(1, Y, 2)).put(new ItemStack(Items.DIAMOND), 0.0);
        helper.succeedWhen(() -> holds(helper, new BlockPos(4, Y + 1, 2), Items.DIAMOND, 1));
    }

    @GameTest(template = ARENA)
    public static void fallingBelts_carryItemsDownABlock(final GameTestHelper helper) {
        belt(helper, new BlockPos(1, Y + 1, 2), BeltSlope.FLAT);
        belt(helper, new BlockPos(2, Y, 2), BeltSlope.FALLING);
        belt(helper, new BlockPos(3, Y, 2), BeltSlope.FLAT);
        helper.setBlock(new BlockPos(4, Y, 2), Blocks.CHEST);
        helper.assertTrue(!helper.getBlockState(new BlockPos(1, Y + 1, 2)).getValue(ConveyorBlock.END),
                "the high belt carries its run on into the falling one below and ahead");
        piece(helper, new BlockPos(1, Y + 1, 2)).put(new ItemStack(Items.EMERALD), 0.0);
        helper.succeedWhen(() -> holds(helper, new BlockPos(4, Y, 2), Items.EMERALD, 1));
    }

    @GameTest(template = ARENA)
    public static void beltsLaid_slopeToMeetABeltABlockOff(final GameTestHelper helper) {
        final BlockPos low = new BlockPos(1, Y, 2);
        final BlockPos high = new BlockPos(2, Y + 1, 2);
        belt(helper, low, BeltSlope.FLAT);
        helper.setBlock(high.below(), Blocks.STONE);
        final FakePlayer player = facingEast(helper, new ItemStack(TestConveyors.BELT.get()));
        click(helper, player, high.below(), Direction.UP);
        helper.assertTrue(helper.getBlockState(high).is(TestConveyors.BELT.get()), "a belt laid on the stone");
        helper.assertTrue(helper.getBlockState(low).getValue(ConveyorBlock.SLOPE) == BeltSlope.RISING,
                "the belt below and behind rose to meet it");
        helper.assertTrue(!helper.getBlockState(low).getValue(ConveyorBlock.END), "and the two are one run");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SharedTools.WRENCH.get()));
        click(helper, player, high, Direction.UP);
        helper.assertTrue(helper.getBlockState(high).getValue(ConveyorBlock.SLOPE) == BeltSlope.RISING,
                "a wrench turned the laid belt to rise");
        player.getInventory().clearContent();
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void chutes_dropWhatFallsInIntoTheChestBelow(final GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, Y, 2), Blocks.CHEST);
        helper.setBlock(new BlockPos(2, Y + 1, 2), TestConveyors.CHUTE.get());
        helper.setBlock(new BlockPos(2, Y + 2, 2), TestConveyors.CHUTE.get());
        helper.assertTrue(!helper.getBlockState(new BlockPos(2, Y + 2, 2)).getValue(ConveyorBlock.END),
                "the top chute carries its run on into the one below");
        helper.spawnItem(Items.GOLD_INGOT, new Vec3(2.5, Y + 3.4, 2.5));
        helper.succeedWhen(() -> holds(helper, new BlockPos(2, Y, 2), Items.GOLD_INGOT, 1));
    }

    @GameTest(template = ARENA)
    public static void chutes_letItemsFallOutOfTheirBottom(final GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, Y + 1, 2), TestConveyors.CHUTE.get());
        piece(helper, new BlockPos(2, Y + 1, 2)).put(new ItemStack(Items.APPLE), 0.0);
        helper.succeedWhen(() -> helper.assertItemEntityPresent(Items.APPLE, new BlockPos(2, Y, 2), 1.0));
    }

    @GameTest(template = ARENA)
    public static void elevators_liftItemsAndTipThemOutToTheirFront(final GameTestHelper helper) {
        final BlockState elevator = TestConveyors.ELEVATOR.get().defaultBlockState()
                .setValue(ElevatorBlock.FACING, Direction.EAST);
        for (int y = Y; y <= Y + 2; y++) {
            helper.setBlock(new BlockPos(2, y, 2), elevator);
        }
        helper.setBlock(new BlockPos(3, Y + 2, 2), Blocks.CHEST);
        // A belt hands its item in at the elevator's foot, from the side.
        belt(helper, new BlockPos(1, Y, 2), BeltSlope.FLAT);
        piece(helper, new BlockPos(1, Y, 2)).put(new ItemStack(Items.IRON_INGOT), 0.0);
        helper.succeedWhen(() -> holds(helper, new BlockPos(3, Y + 2, 2), Items.IRON_INGOT, 1));
    }

    @GameTest(template = ARENA)
    public static void splitters_dealOneItemToEachWayInTurn(final GameTestHelper helper) {
        final BlockPos at = new BlockPos(2, Y, 2);
        helper.setBlock(at, facing(TestConveyors.SPLITTER.get(), Direction.EAST));
        final BlockPos front = at.east();
        final BlockPos left = at.north();
        final BlockPos right = at.south();
        helper.setBlock(front, Blocks.CHEST);
        helper.setBlock(left, Blocks.CHEST);
        helper.setBlock(right, Blocks.CHEST);
        final ConveyorBlockEntity splitter = piece(helper, at);
        for (int k = 0; k < 3; k++) {
            splitter.put(new ItemStack(Items.STONE), k * 0.25);
        }
        helper.succeedWhen(() -> {
            holds(helper, front, Items.STONE, 1);
            holds(helper, left, Items.STONE, 1);
            holds(helper, right, Items.STONE, 1);
        });
    }

    @GameTest(template = ARENA, timeoutTicks = TIMEOUT)
    public static void joins_takeFromEachSideInTurn(final GameTestHelper helper) {
        final BlockPos at = new BlockPos(2, Y, 2);
        helper.setBlock(at, facing(TestConveyors.JOIN.get(), Direction.EAST));
        final BlockPos behind = at.west();
        final BlockPos beside = at.north();
        belt(helper, behind, BeltSlope.FLAT);
        helper.setBlock(beside, TestConveyors.BELT.get().defaultBlockState().setValue(ConveyorBlock.FACING,
                Direction.SOUTH));
        for (int k = 0; k < 4; k++) {
            piece(helper, behind).put(new ItemStack(Items.STONE), k * 0.25);
            piece(helper, beside).put(new ItemStack(Items.DIRT), k * 0.25);
        }
        // Nothing takes from the join, so it fills and holds: half from each line, give or take one.
        helper.succeedWhen(() -> {
            final ConveyorBlockEntity join = piece(helper, at);
            helper.assertTrue(join.lane().size() == 5, "the join full, five to a block");
            int stone = 0;
            for (final BeltLane.Entry<ItemStack> entry : join.lane().entries()) {
                stone += entry.item().is(Items.STONE) ? 1 : 0;
            }
            helper.assertTrue(stone == 2 || stone == 3, "both lines took turns, " + stone + " stones of 5");
        });
    }

    @GameTest(template = ARENA)
    public static void diverters_turnAsideWhatTheirFilterNames(final GameTestHelper helper) {
        final BlockPos at = new BlockPos(2, Y, 2);
        helper.setBlock(at, facing(TestConveyors.DIVERTER.get(), Direction.EAST)
                .setValue(DiverterBlock.SIDE, BeltSide.LEFT));
        helper.setBlock(at.east(), Blocks.CHEST);
        helper.setBlock(at.north(), Blocks.CHEST);
        final ConveyorBlockEntity diverter = piece(helper, at);
        diverter.setFilter(ItemFilter.only(new ItemFilter.Fuzzy("minecraft:diamond", 0L)));
        diverter.put(new ItemStack(Items.DIAMOND), 0.0);
        diverter.put(new ItemStack(Items.STONE), 0.5);
        helper.succeedWhen(() -> {
            holds(helper, at.north(), Items.DIAMOND, 1);
            holds(helper, at.east(), Items.STONE, 1);
        });
    }

    @GameTest(template = ARENA)
    public static void diverters_learnTheirFilterFromAClickAndForgetItSneaking(final GameTestHelper helper) {
        final BlockPos at = new BlockPos(2, Y, 2);
        helper.setBlock(at, facing(TestConveyors.DIVERTER.get(), Direction.EAST));
        final FakePlayer player = facingEast(helper, new ItemStack(Items.REDSTONE));
        click(helper, player, at, Direction.UP);
        helper.assertTrue(DiverterBlock.diverts(piece(helper, at).filter(), new ItemStack(Items.REDSTONE)),
                "a click with redstone made the diverter turn redstone aside");
        helper.assertTrue(!DiverterBlock.diverts(piece(helper, at).filter(), new ItemStack(Items.STONE)),
                "and nothing else");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SharedTools.WRENCH.get()));
        click(helper, player, at, Direction.UP);
        helper.assertTrue(helper.getBlockState(at).getValue(DiverterBlock.SIDE) == BeltSide.RIGHT,
                "a wrench turned it to its other side");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        click(helper, player, at, Direction.UP);
        player.setShiftKeyDown(false);
        helper.assertTrue(piece(helper, at).filter().rules().isEmpty(), "sneaking empty-handed, it forgot its filter");
        player.getInventory().clearContent();
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = TIMEOUT)
    public static void machinesAbove_workItemsOnTheBeltAsTheyPass(final GameTestHelper helper) {
        final BlockPos first = new BlockPos(1, Y, 2);
        final BlockPos middle = new BlockPos(2, Y, 2);
        final BlockPos chest = new BlockPos(4, Y, 2);
        belt(helper, first, BeltSlope.FLAT);
        belt(helper, middle, BeltSlope.FLAT);
        belt(helper, new BlockPos(3, Y, 2), BeltSlope.FLAT);
        helper.setBlock(middle.above(), TestConveyors.BELT_PRESS.get());
        helper.setBlock(chest, Blocks.CHEST);
        piece(helper, first).put(new ItemStack(Items.COBBLESTONE), 0.0);
        piece(helper, first).put(new ItemStack(Items.STONE), 0.5);
        helper.succeedWhen(() -> {
            holds(helper, chest, Items.GRAVEL, 1);
            holds(helper, chest, Items.STONE, 1);
            helper.assertTrue(count(helper, chest, Items.COBBLESTONE) == 0, "the cobblestone pressed on the way");
        });
    }

    @GameTest(template = ARENA)
    public static void brokenBelts_letFallWhatWasOnThem(final GameTestHelper helper) {
        final BlockPos at = new BlockPos(2, Y, 2);
        belt(helper, at, BeltSlope.FLAT);
        piece(helper, at).put(new ItemStack(Items.LAPIS_LAZULI, 7), 0.5);
        helper.destroyBlock(at);
        helper.succeedWhen(() -> helper.assertItemEntityCountIs(Items.LAPIS_LAZULI, at, 1.5, 7));
    }

    /* A test belt at {@code pos}, facing east, lying as {@code slope} says. */
    private static void belt(final GameTestHelper helper, final BlockPos pos, final BeltSlope slope) {
        helper.setBlock(pos, facing(TestConveyors.BELT.get(), Direction.EAST).setValue(ConveyorBlock.SLOPE, slope));
    }

    private static BlockState facing(final Block block, final Direction way) {
        return block.defaultBlockState().setValue(ConveyorBlock.FACING, way);
    }

    private static ConveyorBlockEntity piece(final GameTestHelper helper, final BlockPos relative) {
        if (!(helper.getBlockEntity(relative) instanceof ConveyorBlockEntity piece)) {
            throw new GameTestAssertException("no piece of conveyor at " + relative.toShortString());
        }
        return piece;
    }

    private static void holds(final GameTestHelper helper, final BlockPos chest, final Item item, final int least) {
        final int found = count(helper, chest, item);
        helper.assertTrue(found >= least, "the chest at " + chest.toShortString() + " holds " + found + " of " + item);
    }

    private static int count(final GameTestHelper helper, final BlockPos chest, final Item item) {
        if (!(helper.getBlockEntity(chest) instanceof ChestBlockEntity box)) {
            throw new GameTestAssertException("no chest at " + chest.toShortString());
        }
        int found = 0;
        for (int slot = 0; slot < box.getContainerSize(); slot++) {
            found += box.getItem(slot).is(item) ? box.getItem(slot).getCount() : 0;
        }
        return found;
    }

    /* A player looking east, the way the test belts run, holding {@code held}. */
    private static FakePlayer facingEast(final GameTestHelper helper, final ItemStack held) {
        final FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        final BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        player.moveTo(origin.getX() + 0.5, origin.getY() + Y, origin.getZ() + 0.5, -90F, 0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return player;
    }

    /* {@code player} uses what it holds on the {@code face} of the block at {@code relative}. */
    private static void click(final GameTestHelper helper, final FakePlayer player, final BlockPos relative,
                              final Direction face) {
        final BlockPos pos = helper.absolutePos(relative);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos).relative(face, 0.5), face, pos, false));
    }
}
