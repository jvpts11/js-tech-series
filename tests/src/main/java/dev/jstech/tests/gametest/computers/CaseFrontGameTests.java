/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.blockentity.CaseFront;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The front of a small computer's case: what each of its bays and ports takes, what is let fall when the part in a
 * bay is taken out or swapped, and that what they hold and how the trays stand is kept in the save.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CaseFrontGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos WHERE = new BlockPos(2, 2, 2);

    private CaseFrontGameTests() {
    }

    @GameTest(template = ARENA)
    public static void bayDrive_takesTheDiscsItReadsAndNoOthers(final GameTestHelper helper) {
        final CaseFront front = pc(helper, ComputingModule.LEGACY_PERSONAL_COMPUTER.get(),
                HardwareItems.INTERNAL_CD_DRIVE.get(), HardwareItems.INTERNAL_DVD_DRIVE.get()).front();
        helper.assertTrue(front.accepts(0, stack(ComputingModule.CD_ROM.get())), "a CD drive takes a CD");
        helper.assertFalse(front.accepts(0, stack(ComputingModule.DVD_RW.get())), "and no DVD");
        helper.assertTrue(front.accepts(1, stack(ComputingModule.CD_ROM.get()))
                && front.accepts(1, stack(ComputingModule.DVD_RW.get())), "a DVD drive takes a CD and a DVD");
        helper.assertFalse(front.accepts(1, stack(ComputingModule.BD_ROM.get())), "and no Blu-ray");
        helper.assertFalse(front.accepts(0, stack(ComputingModule.USB_FLASH_DRIVE.get())),
                "a drive takes no stick");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void bluRayDrive_readsEveryDiscAndAnEmptyBayTakesNothing(final GameTestHelper helper) {
        final CaseFront front = pc(helper, ComputingModule.LEGACY_PERSONAL_COMPUTER.get(),
                HardwareItems.INTERNAL_BLU_RAY_DRIVE.get()).front();
        helper.assertTrue(front.accepts(0, stack(ComputingModule.BD_ROM.get()))
                && front.accepts(0, stack(ComputingModule.DVD_RW.get()))
                && front.accepts(0, stack(ComputingModule.CD_ROM.get())), "a Blu-ray drive reads every disc");
        helper.assertFalse(front.accepts(1, stack(ComputingModule.CD_ROM.get())), "an empty bay takes nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void hotSwapBay_takesTheDisksTheComputerTakes(final GameTestHelper helper) {
        final CaseFront front = pc(helper, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get(),
                HardwareItems.HOT_SWAP_DRIVE_BAY.get()).front();
        helper.assertTrue(front.accepts(0, stack(HardwareItems.DISK_TRENCH_20M.get())),
                "a Vintage hot-swap bay takes a Vintage disk");
        helper.assertFalse(front.accepts(0, stack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_1))),
                "and no NVMe drive, which the Vintage computer does not take");
        helper.assertFalse(front.accepts(0, stack(ComputingModule.CD_ROM.get())), "nor a disc");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void ports_takeAStickOnTheCasesThatHaveThem(final GameTestHelper helper) {
        final CaseFront legacy = pc(helper, ComputingModule.LEGACY_PERSONAL_COMPUTER.get()).front();
        helper.assertFalse(legacy.accepts(CaseFront.portSlot(0), stack(ComputingModule.USB_FLASH_DRIVE.get())),
                "a Legacy case has no front port");
        final CaseFront transition = pc(helper, ComputingModule.TRANSITION_PERSONAL_COMPUTER.get()).front();
        helper.assertTrue(transition.ports() == 2, "a Transition case has two front ports");
        helper.assertTrue(transition.accepts(CaseFront.portSlot(1), stack(ComputingModule.USB_FLASH_DRIVE.get())),
                "which take a stick");
        helper.assertFalse(transition.accepts(CaseFront.portSlot(0), stack(ComputingModule.CD_ROM.get())),
                "and no disc");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void partTakenOut_letsFallTheDiscItHeld(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = pc(helper, ComputingModule.LEGACY_PERSONAL_COMPUTER.get(),
                HardwareItems.INTERNAL_CD_DRIVE.get());
        final CaseFront front = pc.front();
        front.tray(0).press(helper.getLevel(), pc.getBlockPos());
        helper.assertTrue(front.insert(0, stack(ComputingModule.CD_ROM.get())).isEmpty(), "the disc goes in");
        pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.BAY_SLOTS_START, ItemStack.EMPTY);
        helper.assertTrue(front.held(0).isEmpty(), "taking the drive out empties its bay");
        helper.assertFalse(front.tray(0).isOpen(), "and its tray is in for the next drive");
        helper.assertItemEntityPresent(ComputingModule.CD_ROM.get(), WHERE, 2.0);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void front_isKeptInTheSave(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = pc(helper, ComputingModule.TRANSITION_PERSONAL_COMPUTER.get(),
                HardwareItems.INTERNAL_DVD_DRIVE.get());
        final CaseFront front = pc.front();
        front.tray(0).press(helper.getLevel(), pc.getBlockPos());
        front.insert(0, stack(ComputingModule.DVD_RW.get()));
        front.insert(CaseFront.portSlot(1), stack(ComputingModule.USB_FLASH_DRIVE.get()));
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        final CompoundTag saved = pc.saveWithFullMetadata(registries);
        final PersonalComputerBlockEntity loaded = new PersonalComputerBlockEntity(pc.getBlockPos(), pc.getBlockState());
        loaded.loadWithComponents(saved, registries);
        helper.assertTrue(loaded.front().held(0).is(ComputingModule.DVD_RW.get()), "the disc on the tray is kept");
        helper.assertTrue(loaded.front().tray(0).isOpen(), "and the tray out");
        helper.assertTrue(loaded.front().held(CaseFront.portSlot(1)).is(ComputingModule.USB_FLASH_DRIVE.get()),
                "and the stick in its port");
        helper.succeed();
    }

    /* A Personal Computer in that case, with these parts in its bays from the first. */
    private static PersonalComputerBlockEntity pc(final GameTestHelper helper, final Block block,
                                                  final Item... bays) {
        helper.setBlock(WHERE, block);
        final PersonalComputerBlockEntity pc = helper.getBlockEntity(WHERE);
        for (int bay = 0; bay < bays.length; bay++) {
            pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.BAY_SLOTS_START + bay, new ItemStack(bays[bay]));
        }
        return pc;
    }

    private static ItemStack stack(final Item item) {
        return new ItemStack(item);
    }
}
