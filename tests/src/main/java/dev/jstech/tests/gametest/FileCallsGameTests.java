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
import dev.jstech.computers.machine.MachineServices;
import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.program.IWorldCall;
import dev.jstech.computers.vm.program.IWorldFunction;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.MemberId;
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
 * The calls a program makes on a machine's drives, as a real computer with a real disk answers them: each does to the
 * disk what it says and tells how many bytes it moved, and a computer in no world cannot be reached.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class FileCallsGameTests {

    private FileCallsGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 2;
    private static final BlockPos AT = new BlockPos(2, 2, 2);
    private static final String TEXT = "string";

    /** Where a call says what it moved, for the calls whose bytes the test does not look at. */
    private static final IWorldCall UNCOUNTED = bytes -> {
    };

    /** A computer with enough hardware to run, a system on it, and a drive to write to. */
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

    private static MemberId file(final String name, final String... parameters) {
        return new MemberId("File", name, List.of(parameters));
    }

    /** Makes a call on the drives the way a program's line would, straight to what the machine bound for it. */
    private static Object call(final MachineServices host, final IWorldCall told, final MemberId id,
                               final Object... arguments) {
        final IWorldFunction bound = host.bind(id);
        if (bound == null) {
            throw new IllegalStateException(id.describe() + " is not answered by the machine");
        }
        return bound.call(told, null, arguments, 1);
    }

    @GameTest(template = ARENA)
    public static void file_writesAddsToListsAndDeletesOnARealDisk(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper);
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachineServices host = computer.services();
                    final long[] moved = {0};
                    final IWorldCall counted = bytes -> moved[0] += bytes;

                    helper.assertTrue(Boolean.TRUE.equals(call(host, UNCOUNTED, file("Write", TEXT, TEXT),
                            "log.txt", "one")), "the file is written");
                    helper.assertTrue(Boolean.TRUE.equals(call(host, counted, file("Append", TEXT, TEXT),
                            "log.txt", ";two")), "and added to");
                    helper.assertTrue(moved[0] == 4, "adding to it moves only what is added; moved " + moved[0]);
                    moved[0] = 0;
                    final Object read = call(host, counted, file("Read", TEXT), "log.txt");
                    helper.assertTrue("one;two".equals(read), "it holds both; got " + read);
                    helper.assertTrue(moved[0] == 7, "reading it moves the whole file; moved " + moved[0]);
                    helper.assertTrue(Boolean.TRUE.equals(call(host, UNCOUNTED, file("MkDir", TEXT), "logs")),
                            "a folder is made");
                    final Object listed = call(host, UNCOUNTED, file("List", TEXT), "");
                    helper.assertTrue(listed instanceof Values.ListValue names && names.items().contains("log.txt")
                                    && names.items().contains("logs/"),
                            "the folder is listed with a slash beside the file");
                    helper.assertTrue(Boolean.TRUE.equals(call(host, UNCOUNTED, file("Delete", TEXT), "log.txt")),
                            "the file is deleted");
                    helper.assertTrue(Boolean.FALSE.equals(call(host, UNCOUNTED, file("Exists", TEXT), "log.txt")),
                            "and is not there any more");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void append_makesTheFileWhenThereIsNone(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper);
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachineServices host = computer.services();

                    helper.assertTrue(Boolean.TRUE.equals(call(host, UNCOUNTED, file("Append", TEXT, TEXT),
                            "fresh.txt", "first")), "adding to a file that is not there makes it");
                    final Object read = call(host, UNCOUNTED, file("Read", TEXT), "fresh.txt");
                    helper.assertTrue("first".equals(read), "holding only what was added; got " + read);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void tryRead_answersFalseAndFillsInNothingWhenThereIsNoFile(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper);
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final IWorldFunction tryRead = computer.services().bind(file("TryRead", TEXT, "out " + TEXT));
                    final Object[] asked = {"missing.txt", null};

                    final Object found = tryRead.call(UNCOUNTED, null, asked, 1);

                    helper.assertTrue(Boolean.FALSE.equals(found) && "".equals(asked[1]),
                            "nothing found and nothing filled in; got " + found + " and " + asked[1]);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void read_stopsTheProgramWithWhatTheDiskSaysWhenThereIsNoFile(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper);
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    try {
                        call(computer.services(), UNCOUNTED, file("Read", TEXT), "nothing.txt");
                        helper.fail("reading a file that is not there stops the program");
                    } catch (final Halt halt) {
                        helper.assertTrue(halt.reason() == Halt.Reason.NO_SUCH_MEMBER && !halt.getMessage().isBlank(),
                                "the program is stopped with the disk's own words; got " + halt.getMessage());
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void file_cannotBeReachedFromAComputerInNoWorld(final GameTestHelper helper) {
        final CraftingComputerBlockEntity placed = computer(helper);
        if (placed == null) {
            return;
        }
        final CraftingComputerBlockEntity loose =
                new CraftingComputerBlockEntity(placed.getBlockPos(), placed.getBlockState());

        try {
            call(loose.services(), UNCOUNTED, file("Exists", TEXT), "a.txt");
            helper.fail("a computer in no world has no drives to reach");
        } catch (final Halt halt) {
            helper.assertTrue("this machine cannot reach File".equals(halt.getMessage()),
                    "it says the drives cannot be reached; got " + halt.getMessage());
        }
        helper.succeed();
    }
}
