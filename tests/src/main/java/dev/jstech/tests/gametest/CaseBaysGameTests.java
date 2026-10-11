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
import dev.jstech.computers.block.IComputerCase;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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

    /** The saved slots of a machine's parts: the compound that says how many there are. */
    private static CompoundTag handlerOf(final CompoundTag saved) {
        final CompoundTag part = saved.getCompound("Hardware");
        return part.contains("Size") ? part : part.getCompound("Hardware");
    }
}
