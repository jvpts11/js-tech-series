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
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
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

    /** Longer than any modern machine's self-test, which sits at the one-second floor. */
    private static final int PAST_THE_POST = 40;

    private PostGameTests() {
    }

    @GameTest(template = ARENA)
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
