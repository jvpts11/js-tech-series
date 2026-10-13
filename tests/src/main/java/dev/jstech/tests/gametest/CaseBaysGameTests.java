/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.IComputerCase;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.BayDriveSpec;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.item.BayDriveItem;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The 5.25-inch bays on the front of a small computer's case, where an internal drive or the hot-swap bay goes: as many
 * as its era's case has, on every one of the three machines that share the case, and still there in a machine saved
 * before the cases had them, with its parts where they were.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CaseBaysGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos WHERE = new BlockPos(2, 2, 2);
    /** The bays of each era's case: one on the AT tower, two on the ATX towers, one on a Standard case, none after. */
    private static final Map<HardwareEra, Integer> BAYS = Map.of(HardwareEra.VINTAGE, 1, HardwareEra.LEGACY, 2,
            HardwareEra.TRANSITION, 2, HardwareEra.STANDARD, 1, HardwareEra.ADVANCED, 0);
    /** The cases there are: nine for each of the Personal, Crafting and Cluster Management Computers. */
    private static final int CASES = 27;
    /** How many parts a small computer held before its case had bays. */
    private static final int SLOTS_BEFORE_BAYS = 13;

    private CaseBaysGameTests() {
    }

    @GameTest(template = ARENA)
    public static void everyCase_hasTheBaysOfItsEra(final GameTestHelper helper) {
        int cases = 0;
        for (final Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof IComputerCase computerCase)) {
                continue;
            }
            helper.setBlock(WHERE, block);
            final AbstractComputerBlockEntity machine = helper.getBlockEntity(WHERE);
            helper.assertValueEqual(machine.caseBays(), BAYS.get(computerCase.chassisEra()),
                    "the 5.25-inch bays of " + BuiltInRegistries.BLOCK.getKey(block));
            cases++;
        }
        helper.assertValueEqual(cases, CASES, "the cases looked at");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void everyBayPart_goesInEveryBayACaseHas(final GameTestHelper helper) {
        final List<Item> parts = List.of(HardwareItems.HOT_SWAP_DRIVE_BAY.get(), HardwareItems.INTERNAL_CD_DRIVE.get(),
                HardwareItems.INTERNAL_DVD_DRIVE.get(), HardwareItems.INTERNAL_BLU_RAY_DRIVE.get());
        for (final Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof IComputerCase)) {
                continue;
            }
            helper.setBlock(WHERE, block);
            final AbstractComputerBlockEntity machine = helper.getBlockEntity(WHERE);
            final ItemStackHandler slots = machine.getHardware();
            final String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
            for (int bay = 0; bay < PersonalComputerBlockEntity.BAY_SLOTS; bay++) {
                final int slot = PersonalComputerBlockEntity.BAY_SLOTS_START + bay;
                final boolean has = bay < machine.caseBays();
                for (final Item part : parts) {
                    helper.assertValueEqual(slots.isItemValid(slot, new ItemStack(part)), has,
                            "whether bay " + bay + " of " + name + " takes " + part);
                }
                helper.assertFalse(slots.isItemValid(slot, new ItemStack(HardwareItems.DISK_LINK_IDE_4G.get())),
                        "a bay of " + name + " takes no disk");
            }
            helper.assertFalse(slots.isItemValid(PersonalComputerBlockEntity.DISK_SLOTS_START,
                    new ItemStack(HardwareItems.INTERNAL_CD_DRIVE.get())), "a disk slot of " + name + " takes no drive");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void aBay_holdsOnePart(final GameTestHelper helper) {
        helper.setBlock(WHERE, ComputingModule.LEGACY_PERSONAL_COMPUTER.get());
        final PersonalComputerBlockEntity pc = helper.getBlockEntity(WHERE);
        final ItemStack left = pc.getHardware().insertItem(PersonalComputerBlockEntity.BAY_SLOTS_START,
                new ItemStack(HardwareItems.INTERNAL_CD_DRIVE.get(), 2), false);
        helper.assertValueEqual(left.getCount(), 1, "the drives a bay leaves out of two");
        final ItemStack second = pc.getHardware().insertItem(PersonalComputerBlockEntity.BAY_SLOTS_START,
                new ItemStack(HardwareItems.HOT_SWAP_DRIVE_BAY.get()), false);
        helper.assertValueEqual(second.getCount(), 1, "a full bay takes nothing more");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void disks_goInAComputerOfTheirEraOrANewerOne(final GameTestHelper helper) {
        final ItemStack nvme = new ItemStack(firstNvme());
        final ItemStack vintage = new ItemStack(HardwareItems.DISK_TRENCH_20M.get());
        helper.setBlock(WHERE, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get());
        final PersonalComputerBlockEntity old = helper.getBlockEntity(WHERE);
        helper.assertFalse(old.acceptsDisk(nvme), "an NVMe goes in no Vintage computer");
        helper.assertFalse(old.getHardware().isItemValid(PersonalComputerBlockEntity.DISK_SLOTS_START, nvme),
                "a Vintage computer's disk slot refuses an NVMe");
        helper.assertTrue(old.acceptsDisk(vintage), "a Vintage disk goes in a Vintage computer");
        helper.setBlock(WHERE, ComputingModule.ADVANCED_PERSONAL_COMPUTER.get());
        final PersonalComputerBlockEntity newest = helper.getBlockEntity(WHERE);
        helper.assertTrue(newest.acceptsDisk(vintage), "an old disk goes in a newer computer");
        helper.assertTrue(newest.acceptsDisk(nvme), "an NVMe goes in an Advanced computer");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void bayParts_areRegisteredUnderTheirNames(final GameTestHelper helper) {
        assertPart(helper, HardwareItems.HOT_SWAP_DRIVE_BAY.get(), "hot_swap_drive_bay", "Hot-Swap Drive Bay",
                BayDriveSpec.Kind.HOT_SWAP, HardwareEra.VINTAGE);
        assertPart(helper, HardwareItems.INTERNAL_CD_DRIVE.get(), "internal_cd_drive", "Internal CD Drive",
                BayDriveSpec.Kind.CD, HardwareEra.LEGACY);
        assertPart(helper, HardwareItems.INTERNAL_DVD_DRIVE.get(), "internal_dvd_drive", "Internal DVD Drive",
                BayDriveSpec.Kind.DVD, HardwareEra.TRANSITION);
        assertPart(helper, HardwareItems.INTERNAL_BLU_RAY_DRIVE.get(), "internal_blu_ray_drive",
                "Internal Blu-ray Drive", BayDriveSpec.Kind.BLU_RAY, HardwareEra.ADVANCED);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void machineSavedBeforeTheBays_keepsItsPartsAndHasItsBays(final GameTestHelper helper) {
        helper.setBlock(WHERE, ComputingModule.LEGACY_PERSONAL_COMPUTER.get());
        final PersonalComputerBlockEntity pc = helper.getBlockEntity(WHERE);
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        final int lastDisk = PersonalComputerBlockEntity.DISK_SLOTS_START + PersonalComputerBlockEntity.DISK_SLOTS - 1;
        pc.getHardware().setStackInSlot(lastDisk, new ItemStack(HardwareItems.DISK_LINK_IDE_4G.get()));

        // Saved as a machine was before the bays: its parts in thirteen slots, the last disk in the last of them.
        final CompoundTag saved = pc.saveWithFullMetadata(registries);
        handlerOf(saved).putInt("Size", SLOTS_BEFORE_BAYS);
        final PersonalComputerBlockEntity loaded = new PersonalComputerBlockEntity(pc.getBlockPos(),
                pc.getBlockState());
        loaded.loadWithComponents(saved, registries);

        helper.assertValueEqual(loaded.getHardware().getSlots(), PersonalComputerBlockEntity.HARDWARE_SLOTS,
                "the slots of a machine loaded from an older save");
        helper.assertTrue(loaded.getHardware().getStackInSlot(lastDisk).is(HardwareItems.DISK_LINK_IDE_4G.get()),
                "the disk is in the slot it was saved in");
        helper.assertValueEqual(loaded.caseBays(), 2, "a Legacy case's bays");
        helper.succeed();
    }

    /** Fails unless the part is registered under that id and name, and is that kind of bay part of that era. */
    private static void assertPart(final GameTestHelper helper, final BayDriveItem item, final String id,
                                   final String name, final BayDriveSpec.Kind kind, final HardwareEra era) {
        helper.assertValueEqual(BuiltInRegistries.ITEM.getKey(item),
                ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, id), "the id of " + name);
        helper.assertValueEqual(item.getDescription().getString(), name, "the name of " + id);
        helper.assertValueEqual(item.spec().kind(), kind, "what " + name + " is");
        helper.assertValueEqual(item.spec().era(), era, "the era of " + name);
    }

    /** The smallest NVMe disk there is. */
    private static DiskItem firstNvme() {
        for (final DiskSize size : DiskSize.values()) {
            if (size.comesAs(StorageTier.NVME)) {
                return ComputingModule.disk(StorageTier.NVME, size);
            }
        }
        throw new IllegalStateException("no NVMe disk is registered");
    }

    /** The saved slots of a machine's parts: the compound that says how many there are. */
    private static CompoundTag handlerOf(final CompoundTag saved) {
        final CompoundTag part = saved.getCompound("Hardware");
        return part.contains("Size") ? part : part.getCompound("Hardware");
    }
}
