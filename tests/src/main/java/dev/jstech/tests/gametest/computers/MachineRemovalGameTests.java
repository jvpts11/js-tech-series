/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.MainframePartBlock;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A machine that holds parts gives them back however it goes: broken by a player in survival, blown up, or taken by
 * whatever removes blocks without a player (another mod's quarry, a command). A Mainframe and a rack give back their
 * own cabinet too, which their loot does not. A player in creative takes the machine and nothing falls, as before.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MachineRemovalGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos AT = new BlockPos(2, 2, 2);

    private MachineRemovalGameTests() {
    }

    @GameTest(template = ARENA)
    public static void computer_removedWithoutAPlayer_givesItsPartsBack(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(AT);
        final List<ItemStack> held = contents(pc.getHardware());
        helper.getLevel().destroyBlock(helper.absolutePos(AT), true);
        expectEach(helper, held, dropped(helper), "the computer's parts");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void computer_brokenInSurvival_givesEachPartBackOnce(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(AT);
        final List<ItemStack> held = contents(pc.getHardware());
        breakAs(helper, AT, playerIn(helper, GameType.SURVIVAL));
        expectEach(helper, held, dropped(helper), "the computer's parts");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void computer_brokenInCreative_dropsNothing(final GameTestHelper helper) {
        TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(AT);
        breakAs(helper, AT, playerIn(helper, GameType.CREATIVE));
        helper.assertTrue(dropped(helper).isEmpty(), "nothing falls; got " + dropped(helper));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void mainframe_removedWithoutAPlayer_givesItselfAndItsPartsBack(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = TestWorldBuilder.forGameTest(helper).placeRunningMainframe(AT);
        standUp(helper, AT);
        final List<ItemStack> held = contents(mainframe.getInventory());
        held.add(new ItemStack(ComputingModule.MAINFRAME.get()));
        helper.getLevel().destroyBlock(helper.absolutePos(AT), true);
        expectEach(helper, held, dropped(helper), "the Mainframe and its parts");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void mainframe_partRemovedWithoutAPlayer_givesItAllBack(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = TestWorldBuilder.forGameTest(helper).placeRunningMainframe(AT);
        standUp(helper, AT);
        final List<ItemStack> held = contents(mainframe.getInventory());
        held.add(new ItemStack(ComputingModule.MAINFRAME.get()));
        helper.getLevel().destroyBlock(helper.absolutePos(partOfTheMainframe(helper)), true);
        expectEach(helper, held, dropped(helper), "the Mainframe and its parts, one of its blocks taken");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void mainframe_brokenInSurvival_givesItselfBackOnce(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = TestWorldBuilder.forGameTest(helper).placeRunningMainframe(AT);
        standUp(helper, AT);
        final List<ItemStack> held = contents(mainframe.getInventory());
        held.add(new ItemStack(ComputingModule.MAINFRAME.get()));
        breakAs(helper, AT, playerIn(helper, GameType.SURVIVAL));
        expectEach(helper, held, dropped(helper), "the Mainframe and its parts");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void mainframe_brokenInCreative_dropsNothing(final GameTestHelper helper) {
        TestWorldBuilder.forGameTest(helper).placeRunningMainframe(AT);
        standUp(helper, AT);
        breakAs(helper, AT, playerIn(helper, GameType.CREATIVE));
        helper.assertTrue(dropped(helper).isEmpty(), "nothing falls; got " + dropped(helper));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void rack_removedWithoutAPlayer_givesItselfAndItsServersBack(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = TestWorldBuilder.forGameTest(helper).placeSeededRack(AT);
        standUp(helper, AT);
        final List<ItemStack> held = contents(rack.getServers());
        held.addAll(contents(rack.getFrontSlots()));
        held.add(new ItemStack(ComputingModule.SERVER_RACK.get()));
        helper.getLevel().destroyBlock(helper.absolutePos(AT), true);
        expectEach(helper, held, dropped(helper), "the rack, its servers and its drives");
        helper.succeed();
    }

    /* A player in that game mode, with the abilities the game gives it, which a mock player lacks. */
    private static Player playerIn(final GameTestHelper helper, final GameType mode) {
        final Player player = helper.makeMockPlayer(mode);
        mode.updatePlayerAbilities(player.getAbilities());
        return player;
    }

    /* What a player's break does to the block, as the game's server does it: the block's own hook, then it goes. */
    private static void breakAs(final GameTestHelper helper, final BlockPos relative, final Player player) {
        final ServerLevel level = helper.getLevel();
        final BlockPos pos = helper.absolutePos(relative);
        final BlockState state = level.getBlockState(pos);
        final BlockState broken = state.getBlock().playerWillDestroy(level, pos, state, player);
        broken.onDestroyedByPlayer(level, pos, player, true, level.getFluidState(pos));
    }

    /* A whole cabinet stands up as one placed by a player does, its parts around the controller. */
    private static void standUp(final GameTestHelper helper, final BlockPos relative) {
        final BlockPos pos = helper.absolutePos(relative);
        final BlockState state = helper.getLevel().getBlockState(pos);
        state.getBlock().setPlacedBy(helper.getLevel(), pos, state, null, ItemStack.EMPTY);
    }

    /* A block of the Mainframe's cabinet other than its controller. */
    private static BlockPos partOfTheMainframe(final GameTestHelper helper) {
        for (final BlockPos near : BlockPos.betweenClosed(AT.offset(-2, -1, -2), AT.offset(2, 3, 2))) {
            if (helper.getBlockState(near).getBlock() instanceof MainframePartBlock) {
                return near.immutable();
            }
        }
        throw new IllegalStateException("the Mainframe stands with no part around its controller");
    }

    private static List<ItemStack> contents(final ItemStackHandler handler) {
        final List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (!handler.getStackInSlot(slot).isEmpty()) {
                stacks.add(handler.getStackInSlot(slot).copy());
            }
        }
        return stacks;
    }

    private static List<ItemStack> dropped(final GameTestHelper helper) {
        final AABB around = new AABB(helper.absolutePos(AT)).inflate(4);
        final List<ItemStack> stacks = new ArrayList<>();
        for (final ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, around)) {
            stacks.add(entity.getItem());
        }
        return stacks;
    }

    /* Each item held comes back exactly as many times as it was held: none lost, none made twice. */
    private static void expectEach(final GameTestHelper helper, final List<ItemStack> held,
                                   final List<ItemStack> dropped, final String what) {
        helper.assertFalse(held.isEmpty(), "the machine held something to give back");
        for (final ItemStack stack : held) {
            final Item item = stack.getItem();
            final int want = held.stream().filter(one -> one.is(item)).mapToInt(ItemStack::getCount).sum();
            final int got = dropped.stream().filter(one -> one.is(item)).mapToInt(ItemStack::getCount).sum();
            helper.assertTrue(got == want, what + ": " + want + " of " + item + " held, " + got + " fell; all that"
                    + " fell " + dropped);
        }
    }
}
