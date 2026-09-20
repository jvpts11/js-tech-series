/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.MainframeBlock;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.program.KnotRepository;
import dev.jstech.computers.program.MessengerLog;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The two services whose cost follows how much they are used, in a real world with a real Mainframe.
 *
 * <p>The rules and the arithmetic are covered by plain JUnit over {@code MessengerLog} and
 * {@code KnotRepository}. What only a running machine can answer is here: that installing switches them
 * on, that a powered-off machine serves nothing, that what they keep survives being written to the disk
 * and read back, and that taking one off takes what it held with it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SocialServiceGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    /** Stands a Mainframe up the way placing one does, and hands back its block entity. */
    private static MainframeBlockEntity mainframe(final GameTestHelper helper, final BlockPos at) {
        helper.setBlock(at, ComputingModule.MAINFRAME.get().defaultBlockState()
                .setValue(MainframeBlock.FACING, Direction.NORTH));
        ((MainframeBlock) ComputingModule.MAINFRAME.get()).setPlacedBy(
                helper.getLevel(), helper.absolutePos(at), helper.getBlockState(at), null, ItemStack.EMPTY);
        if (!(helper.getBlockEntity(at) instanceof MainframeBlockEntity machine)) {
            throw new IllegalStateException("no Mainframe at " + at);
        }
        return machine;
    }

    @GameTest(template = ARENA)
    public static void messenger_isOffUntilItIsInstalled(final GameTestHelper helper) {
        final MainframeBlockEntity machine = mainframe(helper, new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(machine.isMessengerInstalled(),
                            "a fresh Mainframe should not be running a messenger");
                    helper.assertFalse(machine.isMessengerActive(), "nothing installed cannot be active");
                    helper.assertTrue(machine.installMessenger(), "installing it should be news");
                    helper.assertFalse(machine.installMessenger(), "installing it twice is not news");
                    helper.assertTrue(machine.isMessengerInstalled(), "it should be installed now");
                })
                .thenSucceed();
    }

    /**
     * A machine with no power serves nobody, however installed the service is.
     *
     * <p>A Mainframe stood up in an arena has no energy behind it, which is exactly the state this rule is
     * about: the service is there, it is not stopped, and it still refuses, because the machine under it
     * is not running.
     */
    @GameTest(template = ARENA)
    public static void messenger_keepsNothingWhileTheMachineHasNoPower(final GameTestHelper helper) {
        final MainframeBlockEntity machine = mainframe(helper, new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    machine.installMessenger();
                    helper.assertFalse(machine.isRunning(), "this arena's Mainframe has no power");
                    helper.assertFalse(machine.isMessengerActive(),
                            "a service on a machine with no power is not serving");
                    helper.assertFalse(machine.messengerSay("lobby", "ada", "the smelter stalled",
                            helper.getLevel().getGameTime(), false),
                            "a machine with no power should keep nothing");
                    helper.assertTrue(machine.messengerLog().size() == 0, "and nothing should be kept");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void messenger_weighsWhatItKeeps(final GameTestHelper helper) {
        final MainframeBlockEntity machine = mainframe(helper, new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    machine.installMessenger();
                    final MessengerLog log = machine.messengerLog();
                    final long before = log.historyBytes();
                    log.say("lobby", "ada", "the smelter stalled",
                            helper.getLevel().getGameTime(), false);
                    helper.assertTrue(log.historyBytes() > before,
                            "keeping a message should cost disk space");
                    helper.assertTrue(log.size() == 1, "one message should be kept");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void messenger_keepsNothingWhileItIsStopped(final GameTestHelper helper) {
        final MainframeBlockEntity machine = mainframe(helper, new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    machine.installMessenger();
                    helper.assertTrue(machine.setMessengerRunning(false), "stopping it should be news");
                    helper.assertFalse(machine.isMessengerActive(), "a stopped service serves nobody");
                    helper.assertFalse(machine.messengerSay("lobby", "ada", "hello",
                            helper.getLevel().getGameTime(), false),
                            "a stopped service should keep nothing");
                    helper.assertTrue(machine.messengerLog().size() == 0, "nothing should have been kept");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void messenger_dropsEverybodyWhenItIsStopped(final GameTestHelper helper) {
        final MainframeBlockEntity machine = mainframe(helper, new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    machine.installMessenger();
                    final MessengerLog log = machine.messengerLog();
                    log.connect("ada", helper.getLevel().getGameTime());
                    log.connect("grace", helper.getLevel().getGameTime());
                    final int busy = log.ramMb();
                    helper.assertTrue(busy > MessengerLog.BASE_RAM_MB,
                            "two people connected should cost more than the floor");
                    machine.setMessengerRunning(false);
                    helper.assertTrue(log.ramMb() == MessengerLog.BASE_RAM_MB,
                            "a stopped service should fall back to its floor, was " + log.ramMb());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void messenger_survivesBeingWrittenAndReadBack(final GameTestHelper helper) {
        final MainframeBlockEntity machine = mainframe(helper, new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    machine.installMessenger();
                    // Said straight into the log, because this is about the writing and not about the gate.
                    machine.messengerLog().say("smelter", "ada", "bay three again",
                            helper.getLevel().getGameTime(), false);
                    final CompoundTag tag = machine.saveWithoutMetadata(helper.getLevel().registryAccess());
                    helper.assertTrue(tag.contains("MessengerLog"),
                            "the tag written should carry the conversation");
                    machine.loadWithComponents(tag, helper.getLevel().registryAccess());
                    helper.assertTrue(machine.isMessengerInstalled(), "it should still be installed");
                    helper.assertTrue(machine.messengerLog().size() == 1,
                            "the conversation should have come back, found "
                                    + machine.messengerLog().size());
                    helper.assertTrue(machine.messengerLog().room("smelter", 10).size() == 1,
                            "it should have come back in the room it was said in");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void messenger_takesItsConversationsWithItWhenRemoved(final GameTestHelper helper) {
        final MainframeBlockEntity machine = mainframe(helper, new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    machine.installMessenger();
                    machine.messengerLog().say("lobby", "ada", "hello",
                            helper.getLevel().getGameTime(), false);
                    helper.assertTrue(machine.uninstallMessenger(), "removing it should be news");
                    helper.assertTrue(machine.messengerLog().size() == 0,
                            "a service nobody can reach keeps nothing");
                    helper.assertTrue(machine.messengerLog().historyBytes() == 0L,
                            "and it weighs nothing either");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void knot_isOffUntilItIsInstalled(final GameTestHelper helper) {
        final MainframeBlockEntity machine = mainframe(helper, new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(machine.isKnotInstalled(), "a fresh Mainframe keeps no source");
                    helper.assertTrue(machine.installKnot(), "installing it should be news");
                    helper.assertFalse(machine.installKnot(), "installing it twice is not news");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void knot_weighsWhatItKeeps(final GameTestHelper helper) {
        final MainframeBlockEntity machine = mainframe(helper, new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    machine.installKnot();
                    final KnotRepository repository = machine.knotRepository();
                    final long before = repository.bytes();
                    // Committed straight into the repository: this is about the weight, not about the gate.
                    helper.assertTrue(repository.commit("plant.sgs", "ada", "first pass",
                            "int floor = 8000;", helper.getLevel().getGameTime()) != null,
                            "it should have kept that");
                    helper.assertTrue(repository.bytes() > before,
                            "keeping a revision should cost disk space");
                })
                .thenSucceed();
    }

    /** Whatever is installed, a machine with no power keeps nothing. */
    @GameTest(template = ARENA)
    public static void knot_keepsNothingWhileTheMachineHasNoPower(final GameTestHelper helper) {
        final MainframeBlockEntity machine = mainframe(helper, new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    machine.installKnot();
                    helper.assertFalse(machine.isKnotActive(),
                            "a service on a machine with no power is not serving");
                    helper.assertTrue(machine.knotCommit("a.sgs", "ada", "m", "x",
                            helper.getLevel().getGameTime()) == null,
                            "a machine with no power should keep nothing");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void knot_keepsNothingWhileItIsNotInstalled(final GameTestHelper helper) {
        final MainframeBlockEntity machine = mainframe(helper, new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> helper.assertTrue(
                        machine.knotCommit("a.sgs", "ada", "m", "x", helper.getLevel().getGameTime()) == null,
                        "a service that is not installed keeps nothing"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void knot_survivesBeingWrittenAndReadBack(final GameTestHelper helper) {
        final MainframeBlockEntity machine = mainframe(helper, new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    machine.installKnot();
                    machine.knotRepository().commit("plant.sgs", "ada", "first pass",
                            "int floor = 8000;", helper.getLevel().getGameTime());
                    machine.knotRepository().commit("plant.sgs", "grace", "raise it",
                            "int floor = 10000;", helper.getLevel().getGameTime());
                    final CompoundTag tag = machine.saveWithoutMetadata(helper.getLevel().registryAccess());
                    machine.loadWithComponents(tag, helper.getLevel().registryAccess());
                    helper.assertTrue(machine.isKnotInstalled(), "it should still be installed");
                    helper.assertTrue(machine.knotRepository().revisionsOf("plant.sgs").size() == 2,
                            "both revisions should have come back, found "
                                    + machine.knotRepository().revisionsOf("plant.sgs").size());
                    helper.assertTrue("int floor = 10000;".equals(machine.knotRepository().head("plant.sgs")),
                            "the newest text should have come back");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void knot_keepsNumberingAfterBeingReadBack(final GameTestHelper helper) {
        final MainframeBlockEntity machine = mainframe(helper, new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    machine.installKnot();
                    machine.knotRepository().commit("a.sgs", "ada", "one", "1",
                            helper.getLevel().getGameTime());
                    machine.knotRepository().commit("a.sgs", "ada", "two", "2",
                            helper.getLevel().getGameTime());
                    final CompoundTag tag = machine.saveWithoutMetadata(helper.getLevel().registryAccess());
                    machine.loadWithComponents(tag, helper.getLevel().registryAccess());
                    final KnotRepository.Revision next = machine.knotRepository().commit("a.sgs", "grace",
                            "three", "3", helper.getLevel().getGameTime());
                    helper.assertTrue(next != null && next.number() == 3,
                            "numbering must carry on from what was read back, got "
                                    + (next == null ? "nothing" : next.number()));
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void knot_takesItsHistoryWithItWhenRemoved(final GameTestHelper helper) {
        final MainframeBlockEntity machine = mainframe(helper, new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    machine.installKnot();
                    machine.knotRepository().commit("a.sgs", "ada", "one", "1",
                            helper.getLevel().getGameTime());
                    helper.assertTrue(machine.uninstallKnot(), "removing it should be news");
                    helper.assertTrue(machine.knotRepository().files().isEmpty(),
                            "a service nobody can reach keeps nothing");
                    helper.assertTrue(machine.knotRepository().bytes() == 0L,
                            "and it weighs nothing either");
                })
                .thenSucceed();
    }
}
