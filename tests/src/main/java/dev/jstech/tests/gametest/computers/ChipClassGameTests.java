/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import static dev.jstech.tests.testkit.computers.ChainMachineSite.formed;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.machine;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.machineAt;

import com.mojang.authlib.GameProfile;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.chain.ChipClasses;
import dev.jstech.computers.chain.ChipItem;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.chain.machine.WaferWork;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.machine.ProcessingInput;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.tests.JsTests;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Chip classes (computers/docs/design/survival.md, "How a chip is made"): the Packaging Line tests a wafer's dies and
 * sorts them into its bins, the plain chips and as many again of each class the recipes ask of the chip, each in its
 * own bin; a model takes its own class of its chip, so the lot of a bigger model no longer makes a smaller one; and a
 * classed chip says its class in its name.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ChipClassGameTests {

    private static final String ARENA = "empty";
    /* A Vintage chip's layers, and the chips a memory wafer is cut into. */
    private static final int VINTAGE_LAYERS = 2;
    private static final int MEMORY_CHIPS = 8;
    private static final int FULL = 64;

    private ChipClassGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void packaging_sortsAWafersDiesIntoItsBins(final GameTestHelper helper) {
        final ChainMachine line = machine("packaging_line");
        final BlockPos terminal = formed(helper, line);
        final Item chip = ChainParts.MEMORY_CHIPS.getFirst().get();
        final int classes = ChipClasses.count(chip, helper.getLevel().getRecipeManager());
        helper.assertTrue(classes >= 2 && classes <= ChipClasses.MOST, "the recipes ask for classes of the Vintage "
                + "memory chip: " + classes);
        helper.runAfterDelay(20, () -> feed(machineAt(helper, terminal).getInventory(), chip));
        helper.succeedWhen(() -> {
            final ChainMachineBlockEntity machine = machineAt(helper, terminal);
            final int first = machine.layout().firstOutput();
            for (int bin = 0; bin < line.layout().outputs(); bin++) {
                final ItemStack held = machine.getInventory().getStackInSlot(first + bin);
                if (bin <= classes) {
                    helper.assertTrue(held.is(chip) && held.getCount() == MEMORY_CHIPS && ChipClasses.of(held) == bin,
                            "bin " + bin + " holds eight chips of its class: " + held);
                } else {
                    helper.assertTrue(held.isEmpty(), "a bin past the chip's classes stays empty: " + held);
                }
            }
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void packaging_waitsWhileABinIsFull(final GameTestHelper helper) {
        final ChainMachine line = machine("packaging_line");
        final BlockPos terminal = formed(helper, line);
        final Item chip = ChainParts.MEMORY_CHIPS.getFirst().get();
        helper.runAfterDelay(20, () -> {
            final ChainMachineBlockEntity machine = machineAt(helper, terminal);
            // Class A's bin full: the plain chips' bin is empty, but class A goes to its own bin or nowhere.
            machine.getInventory().setStackInSlot(machine.layout().firstOutput() + 1, ChipClasses.stack(chip, 1,
                    FULL));
            feed(machine.getInventory(), chip);
        });
        helper.runAfterDelay(200, () -> {
            final ChainMachineBlockEntity machine = machineAt(helper, terminal);
            helper.assertTrue(machine.getInventory().getStackInSlot(machine.layout().firstOutput()).isEmpty()
                    && machine.getInventory().getStackInSlot(machine.layout().firstOutput() + 2).isEmpty(),
                    "nothing is cut while a bin has no room, and no class spills into another's bin");
            helper.assertTrue(machine.getInventory().getStackInSlot(0).is(ChainParts.FINISHED_WAFERS.getFirst()
                    .get()), "the wafer waits in its port");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void nvme8T_lotMakesItAloneNotTheSmallerOne(final GameTestHelper helper) {
        final ProcessingRecipe big = recipeMaking(helper, "jsc:disk_nvme_8t");
        final ProcessingRecipe small = recipeMaking(helper, "jsc:disk_nvme_4t");
        final ProcessingInput lot = lotOf(big, false);
        helper.assertTrue(big.matches(lot, helper.getLevel()), "the 8T's own lot makes the 8T");
        helper.assertTrue(!small.matches(lot, helper.getLevel()), "and no longer the 4T, which takes another class");
        final ProcessingInput plain = lotOf(big, true);
        helper.assertTrue(!big.matches(plain, helper.getLevel()) && !small.matches(plain, helper.getLevel()),
                "plain memory chips make neither model");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void chip_saysItsClassInItsName(final GameTestHelper helper) {
        final Item chip = ChainParts.MEMORY_CHIPS.get(ChainParts.ERAS.indexOf(HardwareEra.ADVANCED)).get();
        helper.assertTrue(chip instanceof ChipItem, "a memory chip is a chip");
        final Component plain = new ItemStack(chip).getHoverName();
        final Component classed = ChipClasses.stack(chip, 1, 1).getHoverName();
        helper.assertTrue(plain.getContents() instanceof TranslatableContents name
                && name.getKey().equals(chip.getDescriptionId()), "a plain chip keeps its own name");
        helper.assertTrue(classed.getContents() instanceof TranslatableContents name
                && name.getKey().equals(chip.getDescriptionId() + ChipItem.CLASSED)
                && name.getArgs().length == 1 && "A".equals(name.getArgs()[0]),
                "a class A chip says its class in its name: " + classed);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void packagingMenu_showsItsBinsUnderTheWindowOnEveryTab(final GameTestHelper helper) {
        final ChainMachine line = machine("packaging_line");
        final BlockPos terminal = formed(helper, line);
        final FakePlayer owner = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(
                "chip_bins".getBytes(StandardCharsets.UTF_8)), "chip_bins"));
        final ChainMachineBlockEntity machine = machineAt(helper, terminal);
        final MachineMenu menu = new MachineMenu(0, owner.getInventory(), machine);
        final int first = machine.layout().firstOutput();
        final List<Slot> bins = menu.slots.stream().filter(slot -> !(slot.container instanceof Inventory)
                && slot.getContainerSlot() >= first && slot.getContainerSlot() < first + line.layout().outputs())
                .toList();
        helper.assertTrue(bins.size() == line.layout().outputs(), "every output is a bin: " + bins.size());
        for (int i = 0; i < bins.size(); i++) {
            final Slot bin = bins.get(i);
            helper.assertTrue(bin.x == MachineScreenLayout.traySlot(i).x() && bin.y == MachineScreenLayout.traySlot(i)
                    .y(), "bin " + i + " stands in the row under the window");
        }
        menu.clickMenuButton(owner, MachineMenu.FIRST_TAB + MachineMenu.SIDES);
        helper.assertTrue(bins.stream().allMatch(Slot::isActive), "the bins show on another tab too");
        final Optional<Slot> hotbarFirst = menu.slots.stream().filter(slot -> slot.container instanceof Inventory)
                .findFirst();
        helper.assertTrue(hotbarFirst.isPresent() && hotbarFirst.get().y >= MachineScreenLayout.TRAY_Y
                + MachineScreenLayout.TRAY_HEIGHT, "the player's inventory stands under the bins");
        helper.succeed();
    }

    /* A finished Vintage wafer of {@code chip} and what its packaging takes, in the line's ports. */
    private static void feed(final IItemHandler in, final Item chip) {
        final ItemStack finished = new ItemStack(ChainParts.FINISHED_WAFERS.getFirst().get());
        finished.set(ComputingComponents.WAFER_WORK.get(), new WaferWork(BuiltInRegistries.ITEM.getKey(chip),
                VINTAGE_LAYERS, VINTAGE_LAYERS, WaferWork.Step.PACKAGING));
        in.insertItem(0, finished, false);
        in.insertItem(1, new ItemStack(ChainParts.CERAMIC_PACKAGE.get(), MEMORY_CHIPS), false);
        in.insertItem(2, new ItemStack(ChainParts.ALUMINIUM_BONDING_WIRE.get(), 2 * MEMORY_CHIPS), false);
    }

    /* The processing recipe whose first output is {@code made}. */
    private static ProcessingRecipe recipeMaking(final GameTestHelper helper, final String made) {
        final ResourceLocation id = ResourceLocation.parse(made);
        for (final RecipeHolder<?> holder : helper.getLevel().getRecipeManager().getRecipes()) {
            if (holder.value() instanceof ProcessingRecipe recipe && !recipe.outputs().isEmpty()
                    && BuiltInRegistries.ITEM.getKey(recipe.outputs().getFirst().getItem()).equals(id)) {
                return recipe;
            }
        }
        throw new IllegalStateException("no recipe makes " + made);
    }

    /* A machine holding exactly {@code recipe}'s lot, its chips plain when {@code plain}, each input in a slot. */
    private static ProcessingInput lotOf(final ProcessingRecipe recipe, final boolean plain) {
        final List<ItemStack> items = new ArrayList<>();
        for (final SizedIngredient input : recipe.inputs()) {
            final ItemStack shown = input.getItems()[0].copyWithCount(input.count());
            if (plain) {
                shown.remove(ComputingComponents.CHIP_CLASS.get());
            }
            items.add(shown);
        }
        final List<FluidStack> fluids = new ArrayList<>();
        for (final SizedFluidIngredient fluid : recipe.fluidInputs()) {
            fluids.add(fluid.getFluids()[0].copyWithAmount(fluid.amount()));
        }
        final List<Optional<MatterStack>> matter = recipe.matterInputs().stream().map(Optional::of).toList();
        return new ProcessingInput(items, fluids, matter, ProcessingInput.AMBIENT_KELVIN, ProcessingInput.AMBIENT_KPA);
    }
}
