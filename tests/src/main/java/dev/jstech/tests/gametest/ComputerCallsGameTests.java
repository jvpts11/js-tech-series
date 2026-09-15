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
import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.program.IWorldCall;
import dev.jstech.computers.vm.program.IWorldFunction;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.IMemberSpec;
import dev.jstech.computers.vm.system.MemberId;
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
 * What a program reads of the computer it runs on, as a real computer answers it: every call and value the system
 * declares is answered, each tells something true of the computer, and a computer in no world cannot be reached.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ComputerCallsGameTests {

    private ComputerCallsGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 2;
    private static final BlockPos AT = new BlockPos(2, 2, 2);

    /** Where a call says what it moved; nothing read of the computer is priced by its size. */
    private static final IWorldCall UNCOUNTED = bytes -> {
    };

    /** A computer with enough hardware to run, a system on it, and a drive. */
    private static CraftingComputerBlockEntity computer(final GameTestHelper helper) {
        helper.setBlock(AT, ComputingModule.CRAFTING_COMPUTER.get());
        if (!(helper.getBlockEntity(AT) instanceof CraftingComputerBlockEntity computer)) {
            helper.fail("no computer at " + AT);
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

    /**
     * Reads one of the computer's values, or makes one of its calls that takes nothing, the way a program's line does.
     */
    private static Object ask(final MachineHost host, final String name) {
        final MemberId id = new MemberId("Computer", name, List.of());
        final IWorldFunction bound = host.bind(id);
        if (bound == null) {
            throw new IllegalStateException(id.describe() + " is not answered by the machine");
        }
        return bound.call(UNCOUNTED, null, new Object[0], 1);
    }

    @GameTest(template = ARENA)
    public static void bind_answersEveryCallAndValueTheSystemDeclaresOnTheComputer(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper);
        if (computer == null) {
            return;
        }
        final MachineHost host = new MachineHost(computer);

        for (final IMemberSpec member : SystemApi.type("Computer").members()) {
            helper.assertTrue(host.bind(member.id()) != null, member.id().describe() + " is answered by the machine");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void computer_tellsWhatARealComputerIsAndHolds(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper);
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachineHost host = new MachineHost(computer);

                    final Object name = ask(host, "Name");
                    helper.assertTrue(name instanceof String said && !said.isEmpty(), "it has a name; got " + name);
                    final Object os = ask(host, "Os");
                    helper.assertTrue(os instanceof Values.Obj system && "jsc:frames_xp".equals(system.get("Id")),
                            "it names the system it has installed");
                    final Object cpu = ask(host, "Cpu");
                    helper.assertTrue(cpu instanceof Values.Obj record && "CpuInfo".equals(record.type()),
                            "its processor comes back as a record");
                    final Object disks = ask(host, "Disks");
                    helper.assertTrue(disks instanceof Values.ListValue list && list.items().stream()
                                    .anyMatch(one -> one instanceof Values.Obj disk && "C".equals(disk.get("Mount"))),
                            "its system drive is among its disks");
                    helper.assertTrue(ask(host, "RamMb") instanceof Integer, "its memory is a whole number");
                    helper.assertTrue(ask(host, "FreeRamMb") instanceof Integer, "and so is what is free of it");
                    helper.assertTrue(ask(host, "Online") instanceof Boolean, "it says whether it is on");
                    helper.assertTrue(ask(host, "Programs") instanceof Values.ListValue, "it lists its programs");
                    helper.assertTrue(ask(host, "Processes") instanceof Values.ListValue, "and what it is running");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void computer_cannotBeReachedFromAComputerInNoWorld(final GameTestHelper helper) {
        final CraftingComputerBlockEntity placed = computer(helper);
        if (placed == null) {
            return;
        }
        final CraftingComputerBlockEntity loose =
                new CraftingComputerBlockEntity(placed.getBlockPos(), placed.getBlockState());
        final MachineHost host = new MachineHost(loose);

        try {
            ask(host, "Name");
            helper.fail("a computer in no world cannot be read");
        } catch (final Halt halt) {
            helper.assertTrue("this machine cannot reach Computer".equals(halt.getMessage()),
                    "it says the computer cannot be reached; got " + halt.getMessage());
        }
        helper.assertTrue(!host.hasDesktop(), "and it has no desktop to open a window on");
        helper.succeed();
    }
}
