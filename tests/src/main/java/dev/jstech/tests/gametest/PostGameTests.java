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
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.operation.payload.FirmwareStatePayload;
import dev.jstech.computers.operation.payload.firmware.FirmwarePayloads;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.boot.BootTiming;
import dev.jstech.computers.os.install.SetupTiming;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The power-on self-test belongs to the machine, not to the screen watching it.
 *
 * <p>It used to be a screen counting to seventy and telling the server when it was done, which meant a machine
 * nobody was looking at never finished coming up, and closing the monitor halfway through started the whole thing
 * over. Now the machine keeps the time: it ends its own self-test whether anybody is watching or not, and how long
 * it takes is read off what is seated in it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PostGameTests {

    private static final String ARENA = "empty";

    private static final BlockPos WHERE = new BlockPos(2, 2, 2);

    /**
     * Longer than any modern machine's self-test, which sits at the floor every self-test has.
     *
     * <p>Read off the rule rather than written down beside it. The floor is a figure to tune in play, and a
     * test that carries its own copy of it fails the day somebody tunes it, saying a machine never finished
     * its self-test when all that happened is that the self-test got longer.
     */
    private static final int PAST_THE_POST =
            (int) (BootTiming.POST_MIN_SECONDS * SetupTiming.TICKS_PER_SECOND) + 20;

    private static final net.minecraft.resources.ResourceLocation DEBIAN =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "debian");

    private PostGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void post_endsWithNobodyWatching(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer =
                TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                        .placeRunningPersonalComputer(WHERE);
        helper.assertTrue(computer.needsPost(), "a machine that was just switched on owes a self-test");
        helper.startSequence()
                .thenExecuteAfter(PAST_THE_POST, () -> helper.assertFalse(computer.needsPost(),
                        "and finishes it on its own, with no screen open anywhere"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void post_remaining_fallsWhileItRuns(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer =
                TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                        .placeRunningPersonalComputer(WHERE);
        final int[] first = new int[1];
        helper.startSequence()
                // One tick in, the machine has worked out how long its own self-test takes.
                .thenExecuteAfter(2, () -> {
                    first[0] = computer.postRemaining();
                    helper.assertTrue(first[0] > 0, "a self-test under way has time left on it");
                })
                .thenExecuteAfter(5, () -> helper.assertTrue(computer.postRemaining() < first[0],
                        "which is less a moment later, so a monitor opened late shows the rest and not the whole"))
                .thenSucceed();
    }

    /**
     * The self-test hands over to the system coming up, which is the machine's work too.
     *
     * <p>The machine used to go from its self-test straight to the desktop. Now there is a step between them, and
     * it belongs to the machine the same way: it has a length of its own and nobody has to watch it.
     */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void post_handsOverToTheSystemComingUp(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer =
                TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                        .placeRunningPersonalComputer(WHERE);
        helper.startSequence()
                .thenExecuteAfter(PAST_THE_POST, () -> {
                    helper.assertFalse(computer.needsPost(), "the self-test is over");
                    helper.assertTrue(computer.booting(), "and the system is coming up");
                    helper.assertTrue(computer.bootTotal() > 0, "for a length the machine worked out");
                })
                // Frames 11 off this machine's solid-state disk: nine seconds, and a little more to be sure.
                .thenExecuteAfter(220, () -> helper.assertFalse(computer.booting(),
                        "which ends on its own, with nobody watching"))
                .thenSucceed();
    }

    /**
     * A machine in a rack comes up the way a machine on a desk does.
     *
     * <p>It used to owe a self-test that nothing ever ran: the flag was there, the rack never carried it along,
     * and the only thing that could end it was a screen open on that bay telling the server it was done. So a
     * rack of servers nobody was looking at sat owing self-tests for ever, and what the monitor showed was the
     * only thing that made one of them come up. Its bay switch is its power button; the rest is its own.
     */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void post_ofARackBay_runsWithoutAnybodyOnThatChannel(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ComputingModule.SERVER_RACK.get());
        if (!(helper.getBlockEntity(pos) instanceof ServerRackBlockEntity rack)) {
            helper.fail("no rack at " + pos);
            return;
        }
        TestWorldBuilder.mountDefaultServer(rack, 0);
        final IOsHost unit = rack.unitHost(0);
        helper.assertTrue(unit.installOs(DEBIAN, 0), "the bay's drive takes a system");
        helper.assertTrue(unit.needsPost(), "a machine that was just mounted owes a self-test");
        final int[] first = new int[1];
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    first[0] = unit.postRemaining();
                    helper.assertTrue(first[0] > 0,
                            "which the bay times for itself, off what is seated in it: " + first[0]);
                })
                .thenExecuteAfter(5, () -> helper.assertTrue(unit.postRemaining() < first[0],
                        "and counts down with no screen open anywhere"))
                .thenExecuteAfter(PAST_THE_POST, () -> {
                    helper.assertFalse(unit.needsPost(), "the self-test ends by itself");
                    helper.assertTrue(unit.atBootMenu() || unit.booting(),
                            "and hands over to the system, which takes its own time too");
                })
                .thenExecuteAfter(400, () -> {
                    helper.assertFalse(unit.booting(), "the system finishes coming up with nobody watching");
                    helper.assertFalse(unit.atBootMenu(), "and nothing is left standing at a menu");
                })
                .thenSucceed();
    }

    /**
     * A bay whose self-test found nothing to boot goes on by itself once a system is on its drive.
     *
     * <p>It stands at its failure asking now and then rather than every tick, because a rack of empty servers
     * asking every tick was half of what an idle base cost; so what is checked here is that the asking still
     * happens, and soon enough that a player who has just installed a system never notices the wait.
     */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void post_ofABayWithNothingToBoot_goesOnOnceASystemArrives(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ComputingModule.SERVER_RACK.get());
        if (!(helper.getBlockEntity(pos) instanceof ServerRackBlockEntity rack)) {
            helper.fail("no rack at " + pos);
            return;
        }
        TestWorldBuilder.mountDefaultServer(rack, 0);
        final IOsHost unit = rack.unitHost(0);
        helper.startSequence()
                .thenExecuteAfter(PAST_THE_POST, () -> {
                    helper.assertTrue(unit.haltedAtPost(),
                            "with nothing on its drives the bay stops at its failure");
                    helper.assertTrue(unit.installOs(DEBIAN, 0),
                            "and its drive takes a system while it stands there");
                })
                // A second to notice, and a couple of ticks to be sure.
                .thenExecuteAfter(22, () -> {
                    helper.assertFalse(unit.haltedAtPost(),
                            "the bay stops standing at a failure that is not true any more");
                    helper.assertTrue(unit.atBootMenu() || unit.booting(),
                            "and goes on to the system it now has");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void post_ofAnEarlierMachine_takesLonger(final GameTestHelper helper) {
        final PersonalComputerBlockEntity modern =
                TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                        .placeRunningPersonalComputer(WHERE);
        final PersonalComputerBlockEntity old = vintage(helper, new BlockPos(4, 2, 2));
        if (old == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(2, () -> helper.assertTrue(old.postRemaining() > modern.postRemaining(),
                        "an older firmware takes longer over the same work: " + old.postRemaining()
                                + " against " + modern.postRemaining()))
                .thenSucceed();
    }

    /**
     * What the self-test reads out is the machine itself, part by part, and not the kind of block it is.
     *
     * <p>Every line of that screen is one of these fields, so a field that comes back empty is a line the
     * player never sees: the board and the video card were both being sent and neither was being drawn, and
     * the memory was counted without ever naming the modules it was counted over.
     */
    @GameTest(template = ARENA)
    public static void post_readsOutTheParts_notTheKindOfMachine(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer =
                TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                        .placeRunningPersonalComputer(WHERE);
        final FirmwareStatePayload state =
                FirmwarePayloads.buildFirmwareState(helper.getLevel(), computer, helper.absolutePos(WHERE));
        final FirmwareStatePayload.Machine machine = state.machine();
        helper.assertFalse(machine.cpuName().isEmpty(), "the processor is named by its own model");
        helper.assertFalse(machine.boardName().isEmpty(), "the board is named");
        helper.assertFalse(machine.ramName().isEmpty(), "and the memory modules are named");
        helper.assertTrue(machine.memoryModules().startsWith(machine.ramModules() + "x "),
                "which the self-test reads out as a count of them: " + machine.memoryModules());
        helper.assertTrue(machine.ramMb() > 0, "with the memory counted over what is seated");
        helper.succeed();
    }

    /**
     * A drive is listed as the three things a firmware printed about one: what it is, how big, what is on it.
     *
     * <p>They travel apart because the three screens that show them put them together three different ways,
     * and one composed sentence cannot be laid out in columns by any of them.
     */
    @GameTest(template = ARENA)
    public static void post_listsEachDrive_byModelAndSizeAndContents(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer =
                TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                        .placeRunningPersonalComputer(WHERE);
        final FirmwareStatePayload state =
                FirmwarePayloads.buildFirmwareState(helper.getLevel(), computer, helper.absolutePos(WHERE));
        FirmwareStatePayload.Entry disk = null;
        for (final FirmwareStatePayload.Entry entry : state.entries()) {
            if (entry.kind() == FirmwareStatePayload.KIND_DISK) {
                disk = entry;
                break;
            }
        }
        if (disk == null) {
            helper.fail("the machine has a disk in it and the firmware found none");
            return;
        }
        helper.assertFalse(disk.device().isEmpty(), "the drive is named by its own model: " + disk.device());
        helper.assertFalse(disk.size().isEmpty(), "and by how much it holds: " + disk.size());
        helper.assertFalse(disk.label().isEmpty(), "and by what is on it: " + disk.label());
        helper.succeed();
    }

    private static PersonalComputerBlockEntity vintage(final GameTestHelper helper, final BlockPos at) {
        helper.setBlock(at, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get());
        if (!(helper.getBlockEntity(at) instanceof PersonalComputerBlockEntity computer)) {
            helper.fail("no vintage personal computer at " + at);
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
        computer.togglePower();
        return computer;
    }
}
