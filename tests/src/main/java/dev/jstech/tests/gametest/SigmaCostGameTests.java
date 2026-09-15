/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.machine.MachineHost;
import dev.jstech.computers.vm.system.CallCost;
import dev.jstech.computers.vm.system.SystemApi;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * What the machine charges a program, against what the editors tell the player it will charge.
 *
 * <p>Every call a program makes of the machine is charged by the runtime straight from its declaration, which the
 * tests of each of the machine's services cover. What is left here is the promise a real computer keeps by never
 * being asked at all: being told about something costs nothing.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SigmaCostGameTests {

    private SigmaCostGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 2;

    /** A computer good enough to answer, with a disk in it and a system on it. */
    private static CraftingComputerBlockEntity computer(final GameTestHelper helper, final BlockPos at) {
        helper.setBlock(at, ComputingModule.CRAFTING_COMPUTER.get());
        if (!(helper.getBlockEntity(at) instanceof CraftingComputerBlockEntity computer)) {
            helper.fail("no computer at " + at);
            return null;
        }
        final ItemStackHandler hw = computer.getHardware();
        hw.setStackInSlot(CraftingComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(ComputingModule.MOTHERBOARD_ATX_P.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.CPU_SLOT,
                new ItemStack(ComputingModule.CPU_ASCENT_965.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.PSU_SLOT,
                new ItemStack(ComputingModule.PSU_650G.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        computer.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp"));
        return computer;
    }

    /** Being told about something costs nothing, which is the whole reason to prefer it to asking. */
    @GameTest(template = ARENA)
    public static void watching_costsNothingToArm(final GameTestHelper helper) {
        final BlockPos at = new BlockPos(2, 2, 2);
        final CraftingComputerBlockEntity computer = computer(helper, at);
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    for (final String member : List.of("Watch", "WatchBelow", "WatchAbove")) {
                        helper.assertTrue(
                                CallCost.FREE.equals(SystemApi.members("Network", member).getFirst().cost()),
                                member + " should cost nothing to arm");
                        /*
                         * The machine is never asked: a watch is answered inside the runtime, which is
                         * what makes it free. If that ever changes, the host starts answering for it and
                         * this stops being true.
                         */
                        helper.assertTrue(!new MachineHost(computer).provides("Watch"),
                                "a watch is the runtime's to answer, not the machine's");
                    }
                })
                .thenSucceed();
    }
}
