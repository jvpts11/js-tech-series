/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.connect.RelativeFace;
import dev.jstech.core.inventory.FaceMode;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.machine.MachineResource;
import dev.jstech.core.machine.MachineSides;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestKitMachine;
import dev.jstech.tests.TestKitMachine.FactoryBlockEntity;
import dev.jstech.tests.TestKitMachine.KitMachineBlockEntity;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * What each face of a machine does with each resource: a face closed to items offers none, one that takes in only
 * takes and one that gives out only gives; a face closed to energy offers no energy; a machine pushes what it makes
 * into the chest beside it and pulls what it works from another, and pushes gas into the machine beside; and its sides
 * are saved and read back.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MachineSidesGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos MACHINE = new BlockPos(4, 2, 4);

    private MachineSidesGameTests() {
    }

    @GameTest(template = ARENA)
    public static void face_letsThroughOnlyWhatItIsSetTo(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = kit(helper, MACHINE);
        final BlockPos at = helper.absolutePos(MACHINE);
        machine.sides().set(MachineResource.ITEMS, RelativeFace.TOP, FaceMode.NONE);
        machine.sides().set(MachineResource.ITEMS, RelativeFace.FRONT, FaceMode.IN);
        machine.sides().set(MachineResource.ITEMS, RelativeFace.BACK, FaceMode.OUT);
        machine.getInventory().setStackInSlot(TestKitMachine.OUT, new ItemStack(Items.STONE, 4));
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, at, Direction.UP) == null,
                "a face closed to items offers none");
        final IItemHandler front = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, at,
                Direction.NORTH);
        helper.assertTrue(front.insertItem(0, new ItemStack(Items.COBBLESTONE), true).isEmpty(),
                "a face that takes in takes");
        helper.assertTrue(front.extractItem(TestKitMachine.OUT, 1, true).isEmpty(), "and gives nothing");
        final IItemHandler back = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, at,
                Direction.SOUTH);
        helper.assertTrue(!back.insertItem(0, new ItemStack(Items.COBBLESTONE), true).isEmpty(),
                "a face that gives out takes nothing");
        helper.assertTrue(back.extractItem(TestKitMachine.OUT, 1, true).is(Items.STONE), "and gives");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void face_closedToEnergyOffersNone(final GameTestHelper helper) {
        helper.setBlock(MACHINE, TestKitMachine.FACTORY.get());
        final FactoryBlockEntity factory = (FactoryBlockEntity) helper.getLevel().getBlockEntity(
                helper.absolutePos(MACHINE));
        factory.sides().set(MachineResource.ENERGY, RelativeFace.BOTTOM, FaceMode.NONE);
        final BlockPos at = helper.absolutePos(MACHINE);
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, at, Direction.DOWN)
                == null, "a face closed to energy offers none");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, at, Direction.UP)
                != null, "the others still take it");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void push_putsWhatItMakesIntoTheChestBeside(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = loaded(helper);
        helper.setBlock(MACHINE.east(), Blocks.CHEST);
        machine.sides().setAutomation(MachineResource.ITEMS, MachineSides.Automation.NONE.pushing(true, 64));
        final ChestBlockEntity chest = (ChestBlockEntity) helper.getLevel().getBlockEntity(
                helper.absolutePos(MACHINE.east()));
        helper.succeedWhen(() -> helper.assertTrue(chest.getItem(0).is(Items.STONE),
                "the stone it made is in the chest: " + chest.getItem(0)));
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void pull_takesWhatItWorksFromTheChestBeside(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = kit(helper, MACHINE);
        machine.power().portStore().setStored(TestKitMachine.CAPACITY);
        helper.setBlock(MACHINE.west(), Blocks.CHEST);
        final ChestBlockEntity chest = (ChestBlockEntity) helper.getLevel().getBlockEntity(
                helper.absolutePos(MACHINE.west()));
        chest.setItem(0, new ItemStack(Items.COBBLESTONE, 8));
        machine.sides().setAutomation(MachineResource.ITEMS, MachineSides.Automation.NONE.pulling(true, 64));
        helper.succeedWhen(() -> {
            helper.assertTrue(chest.getItem(0).isEmpty(), "the chest is emptied");
            helper.assertTrue(machine.getInventory().getStackInSlot(TestKitMachine.OUT).is(Items.STONE),
                    "and the machine works it");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void push_putsGasIntoTheMachineBeside(final GameTestHelper helper) {
        final KitMachineBlockEntity from = kit(helper, MACHINE);
        final KitMachineBlockEntity to = kit(helper, MACHINE.east());
        from.matter().outputs(MatterState.GAS).getFirst().insert(
                new MatterStack(Elements.of(Element.OXYGEN), 400L), false);
        from.sides().setAutomation(MachineResource.GAS, MachineSides.Automation.NONE.pushing(true, 1_000));
        helper.succeedWhen(() -> helper.assertTrue(to.matter().inputs(MatterState.GAS)
                        .getFirst().held().map(MatterStack::amount).orElse(0L) == 400L,
                "the oxygen it made is in the next machine's input"));
    }

    @GameTest(template = ARENA)
    public static void sides_areSavedAndReadBack(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = kit(helper, MACHINE);
        machine.sides().set(MachineResource.GAS, RelativeFace.LEFT, FaceMode.OUT);
        machine.sides().setAutomation(MachineResource.ITEMS, MachineSides.Automation.NONE.pushing(true, 16));
        final CompoundTag saved = new CompoundTag();
        machine.sides().save(saved, helper.getLevel().registryAccess());
        final MachineSides back = new MachineSides(() -> Set.of(MachineResource.ITEMS, MachineResource.GAS),
                () -> { });
        back.load(saved, helper.getLevel().registryAccess());
        helper.assertTrue(back.mode(MachineResource.GAS, RelativeFace.LEFT) == FaceMode.OUT
                && back.mode(MachineResource.GAS, RelativeFace.RIGHT) == FaceMode.BOTH, "the faces come back");
        helper.assertTrue(back.automation(MachineResource.ITEMS).push()
                && back.automation(MachineResource.ITEMS).pushRate() == 16, "and what it does by itself");
        helper.succeed();
    }

    private static KitMachineBlockEntity kit(final GameTestHelper helper, final BlockPos pos) {
        helper.setBlock(pos, TestKitMachine.BLOCK.get());
        return (KitMachineBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    private static KitMachineBlockEntity loaded(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = kit(helper, MACHINE);
        machine.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 2));
        machine.power().portStore().setStored(TestKitMachine.CAPACITY);
        return machine;
    }
}
