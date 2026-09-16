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
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.os.boot.BootLines;
import dev.jstech.computers.os.boot.BootSequence;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * What a system says while it comes up is read off the machine it is coming up on.
 *
 * <p>A line that was written in advance would say the same thing on every computer, which is the one thing a
 * starting machine must never do: the drive letters are the drives that are in, and a step about the network
 * reports what answered, or says that nothing did.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class BootSequenceGameTests {

    private static final String ARENA = "empty";

    private static final BlockPos WHERE = new BlockPos(2, 2, 2);

    private static final ResourceLocation MC_DOS =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mc_dos");

    private BootSequenceGameTests() {
    }

    @GameTest(template = ARENA)
    public static void dos_readsTheDrivesThatAreInTheMachine(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintageWithDos(helper);
        if (computer == null) {
            return;
        }
        final BootSequence sequence = BootLines.forMachine(computer);
        helper.assertTrue(sequence.title().startsWith("Starting MC-DOS"),
                "the system names itself as it starts: " + sequence.title());
        helper.assertTrue(has(sequence, "C:"), "the first drive gets the first letter: " + labels(sequence));
        helper.assertFalse(has(sequence, "D:"), "and a machine with one drive has no second letter");
        helper.assertTrue(has(sequence, "HIMEM"), "the memory above the line is counted: " + labels(sequence));
        helper.succeed();
    }

    /** No cable, no network step: a machine does not report a link it does not have. */
    @GameTest(template = ARENA)
    public static void dos_withNoCable_saysNothingOfANetwork(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintageWithDos(helper);
        if (computer == null) {
            return;
        }
        helper.assertFalse(has(BootLines.forMachine(computer), "NET"),
                "a machine with no cable reaching it has no network line to show");
        helper.succeed();
    }

    /** A machine with no system has nothing to come up, and so nothing to say. */
    @GameTest(template = ARENA)
    public static void aMachineWithNoSystem_hasNoSequence(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintage(helper);
        if (computer == null) {
            return;
        }
        helper.assertTrue(BootLines.forMachine(computer).isEmpty(), "nothing installed, nothing to show");
        helper.succeed();
    }

    private static boolean has(final BootSequence sequence, final String label) {
        for (final BootSequence.Line line : sequence.lines()) {
            if (line.label().equals(label)) {
                return true;
            }
        }
        return false;
    }

    private static String labels(final BootSequence sequence) {
        final StringBuilder out = new StringBuilder();
        for (final BootSequence.Line line : sequence.lines()) {
            out.append(line.label()).append(' ');
        }
        return out.toString();
    }

    private static PersonalComputerBlockEntity vintageWithDos(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintage(helper);
        if (computer == null) {
            return null;
        }
        if (!computer.installOs(MC_DOS)) {
            helper.fail("MC-DOS would not install on the vintage machine");
            return null;
        }
        return computer;
    }

    private static PersonalComputerBlockEntity vintage(final GameTestHelper helper) {
        helper.setBlock(WHERE, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get());
        if (!(helper.getBlockEntity(WHERE) instanceof PersonalComputerBlockEntity computer)) {
            helper.fail("no vintage personal computer at " + WHERE);
            return null;
        }
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_BABYAT_VINTAGE.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_486SX.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_SIMM_4.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT,
                new ItemStack(HardwareItems.PSU_300B.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        return computer;
    }
}
