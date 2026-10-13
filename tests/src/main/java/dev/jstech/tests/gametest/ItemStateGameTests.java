/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.core.item.ItemMode;
import dev.jstech.core.item.ItemStates;
import dev.jstech.core.item.ItemTexts;
import dev.jstech.core.text.Text;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestItems;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Items with state, declared through the Core: the capability for each thing an item holds, what it holds kept in
 * its components across a save and the wire, the component it starts with, its modes and the mode key's work on the
 * server, an item that holds stacks refusing another that does, and the tooltip's lines.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ItemStateGameTests {

    private static final String ARENA = "empty";

    private ItemStateGameTests() {
    }

    @GameTest(template = ARENA)
    public static void declaredState_givesTheGameCapabilityForEachThingHeld(final GameTestHelper helper) {
        final ItemStack tool = new ItemStack(TestItems.FULL.get());

        helper.assertTrue(tool.getMaxStackSize() == 1, "an item that keeps something inside stacks alone");
        helper.assertTrue(new ItemStack(TestItems.MODES_ONLY.get()).getMaxStackSize() == 64,
                "an item with modes only still stacks");
        final IEnergyStorage energy = tool.getCapability(Capabilities.EnergyStorage.ITEM);
        helper.assertTrue(energy != null && energy.getMaxEnergyStored() == TestItems.ENERGY, "it holds energy");
        helper.assertTrue(energy.receiveEnergy(10_000, false) == TestItems.ENERGY_IN, "taking what it takes a tick");
        final IFluidHandlerItem fluid = tool.getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertTrue(fluid != null && fluid.getTankCapacity(0) == TestItems.FLUID, "it holds a fluid");
        helper.assertTrue(fluid.fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE) == 1_000,
                "it takes water");
        final IItemHandler items = tool.getCapability(Capabilities.ItemHandler.ITEM);
        helper.assertTrue(items != null && items.getSlots() == TestItems.SLOTS, "it holds stacks");
        helper.assertTrue(items.insertItem(0, new ItemStack(Items.STONE, 32), false).isEmpty(), "it takes stone");
        helper.assertTrue(new ItemStack(TestItems.MODES_ONLY.get()).getCapability(Capabilities.ItemHandler.ITEM)
                == null, "an item that holds no stacks has no capability for them");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void heldState_comesBackFromTheSaveAndTheWire(final GameTestHelper helper) {
        final ItemStack tool = filledTool();
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();

        final Tag saved = tool.save(registries);
        final ItemStack loaded = ItemStack.parse(registries, saved).orElseThrow();
        same(helper, tool, loaded, "the save");

        final RegistryFriendlyByteBuf wire = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        ItemStack.STREAM_CODEC.encode(wire, tool);
        same(helper, tool, ItemStack.STREAM_CODEC.decode(wire), "the wire");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void declaredComponent_isOnEveryNewOne(final GameTestHelper helper) {
        final ItemStack tool = new ItemStack(TestItems.FULL.get());

        helper.assertTrue(Integer.valueOf(TestItems.MARK_VALUE).equals(tool.get(TestItems.MARK_COMPONENT.get())),
                "a new tool carries the component it was declared with");
        helper.assertTrue(!new ItemStack(TestItems.MODES_ONLY.get()).has(TestItems.MARK_COMPONENT.get()),
                "an item declared without it does not");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void modes_startAtTheFirstAndGoRoundBothWays(final GameTestHelper helper) {
        final ItemStack tool = new ItemStack(TestItems.FULL.get());

        helper.assertTrue(ItemStates.mode(tool).equals(Optional.of(TestItems.SCAN)), "it starts in its first mode");
        helper.assertTrue(ItemStates.cycleMode(tool, false).equals(Optional.of(TestItems.MARK)), "on to the next");
        helper.assertTrue(ItemStates.cycleMode(tool, false).equals(Optional.of(TestItems.CLEAR)), "and the next");
        helper.assertTrue(ItemStates.cycleMode(tool, false).equals(Optional.of(TestItems.SCAN)), "round to the first");
        helper.assertTrue(ItemStates.cycleMode(tool, true).equals(Optional.of(TestItems.CLEAR)), "back one");
        helper.assertTrue(!ItemStates.setMode(tool, ItemMode.of("other", TestItems.MODE_SCAN)),
                "a mode the item does not have is refused");
        helper.assertTrue(ItemStates.cycleMode(new ItemStack(Items.STICK), false).isEmpty(),
                "an item with no modes has none to go to");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void modeKey_changesTheModeOfTheItemInTheMainHand(final GameTestHelper helper) {
        final FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TestItems.MODES_ONLY.get()));

        helper.assertTrue(ItemStates.cycleHeld(player, false).equals(Optional.of(TestItems.MARK)),
                "the key moves the held item on");
        helper.assertTrue(ItemStates.mode(player.getMainHandItem()).equals(Optional.of(TestItems.MARK)),
                "and the item keeps it");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(ItemStates.cycleHeld(player, false).isEmpty(), "an empty hand has no mode");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void itemInventory_refusesAnItemThatHoldsStacksItself(final GameTestHelper helper) {
        final IItemHandler items = new ItemStack(TestItems.FULL.get()).getCapability(Capabilities.ItemHandler.ITEM);

        helper.assertTrue(!items.isItemValid(0, new ItemStack(TestItems.FULL.get())),
                "one tool is never kept inside another");
        helper.assertTrue(!items.isItemValid(0, new ItemStack(Items.SHULKER_BOX)),
                "nor anything the game keeps out of containers");
        helper.assertTrue(items.isItemValid(0, new ItemStack(TestItems.MODES_ONLY.get())),
                "an item that holds no stacks goes in");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void tooltip_saysWhatTheItemHolds(final GameTestHelper helper) {
        final List<Text> lines = ItemStates.describe(filledTool());

        helper.assertTrue(lines.size() == 4, "a line for the mode, the energy, the fluid and the stacks: " + lines);
        helper.assertTrue(lines.get(0) instanceof Text.Translated mode && mode.key().equals(ItemTexts.MODE),
                "the mode first");
        helper.assertTrue(lines.get(3) instanceof Text.Translated stacks && stacks.key().equals(ItemTexts.ITEMS)
                && stacks.args().get(0).toString().equals("1"), "one stack used of four");
        helper.assertTrue(ItemStates.describe(new ItemStack(Items.STICK)).isEmpty(), "a stick holds nothing");
        helper.succeed();
    }

    /* A full tool in its second mode, with energy, water and a stack of stone in it. */
    private static ItemStack filledTool() {
        final ItemStack tool = new ItemStack(TestItems.FULL.get());
        ItemStates.setMode(tool, TestItems.MARK);
        tool.getCapability(Capabilities.EnergyStorage.ITEM).receiveEnergy(300, false);
        tool.getCapability(Capabilities.FluidHandler.ITEM).fill(new FluidStack(Fluids.WATER, 1_500),
                IFluidHandler.FluidAction.EXECUTE);
        tool.getCapability(Capabilities.ItemHandler.ITEM).insertItem(2, new ItemStack(Items.STONE, 12), false);
        return tool;
    }

    private static void same(final GameTestHelper helper, final ItemStack expected, final ItemStack actual,
                             final String through) {
        helper.assertTrue(ItemStates.mode(actual).equals(Optional.of(TestItems.MARK)), "the mode through " + through);
        helper.assertTrue(actual.getCapability(Capabilities.EnergyStorage.ITEM).getEnergyStored() == 300,
                "the energy through " + through);
        final FluidStack water = actual.getCapability(Capabilities.FluidHandler.ITEM).getFluidInTank(0);
        helper.assertTrue(water.is(Fluids.WATER) && water.getAmount() == 1_500, "the fluid through " + through);
        final ItemStack stone = actual.getCapability(Capabilities.ItemHandler.ITEM).getStackInSlot(2);
        helper.assertTrue(stone.is(Items.STONE) && stone.getCount() == 12, "the stacks through " + through);
        helper.assertTrue(ItemStack.isSameItemSameComponents(expected, actual), "every component through " + through);
    }
}
