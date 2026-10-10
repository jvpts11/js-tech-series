/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.JsTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import static dev.jstech.tests.testkit.NetworkFixtures.fullHandler;
import static dev.jstech.tests.testkit.NetworkFixtures.port;
import static dev.jstech.tests.testkit.NetworkFixtures.seededRack;
import static dev.jstech.tests.testkit.NetworkFixtures.storageNetwork;

/**
 * GameTests for the Operation queues and lock contention: excess Operations staying pending, and a contended
 * lock being waited on or timing out as resource locked.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class OperationQueueAndLockGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private OperationQueueAndLockGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 140)
    public static void operationQueue_excessOpsStayPendingThenRun(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        final ItemStackHandler fullDest = fullHandler();
        final ItemStackHandler goodDest = new ItemStackHandler(9);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    seededRack(helper).getServerStorage(0).insert(Items.COBBLESTONE, 100);
                })
                .thenExecuteAfter(2, () -> {
                    // op1 occupies the single queue and stalls against the full destination.
                    helper.assertTrue(mainframe.submitNetworkSelect(Items.COBBLESTONE, 40,
                            port(fullDest), "full") != null, "op1 dispatched");
                    // op2 is ready (60 unlocked cobblestone cover its 30) but has no free queue.
                    helper.assertTrue(mainframe.submitNetworkSelect(Items.COBBLESTONE, 30,
                            port(goodDest), "good") != null, "op2 dispatched");
                })
                .thenExecuteAfter(6, () -> {
                    final var records = mainframe.activeOperationRecords();
                    helper.assertTrue(records.size() == 2, "both ops in flight; got " + records.size());
                    helper.assertTrue(records.get(0).status() == OperationRecord.STATUS_PROCESSING,
                            "op1 holds the queue (PROCESSING); got " + records.get(0).status());
                    helper.assertTrue(!records.get(0).subs().isEmpty(),
                            "the streaming op exposes its SubOperation rows");
                    helper.assertTrue(records.get(1).status() == OperationRecord.STATUS_PENDING,
                            "op2 queues behind the single queue (PENDING); got " + records.get(1).status());
                })
                .thenExecuteAfter(90, () -> {
                    final long left = NetworkStorage.of(helper.getLevel(), mainframe.networkUuid())
                            .count(Items.COBBLESTONE);
                    helper.assertTrue(left == 70,
                            "op1 moved nothing (full dest) and op2 moved its 30 after promotion; left " + left);
                    final var log = mainframe.recentOperations();
                    helper.assertTrue(log.size() >= 2, "both ops logged; got " + log.size());
                    helper.assertTrue(log.get(0).status() == OperationRecord.STATUS_COMPLETED
                                    && log.get(0).moved() == 30,
                            "op2 completed its 30 after the queue freed");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 140)
    public static void lockContention_waitsThenAcquiresWhenLockFrees(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        final ItemStackHandler fullDest = fullHandler();
        final ItemStackHandler goodDest = new ItemStackHandler(9);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () ->
                        seededRack(helper).getServerStorage(0).insert(Items.COBBLESTONE, 100))
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(mainframe.submitNetworkSelect(Items.COBBLESTONE, 100,
                            port(fullDest), "full") != null, "the lock holder dispatched");
                    helper.assertTrue(mainframe.submitNetworkSelect(Items.COBBLESTONE, 20,
                            port(goodDest), "good") != null, "the contender dispatched");
                })
                .thenExecuteAfter(6, () -> {
                    final var records = mainframe.activeOperationRecords();
                    helper.assertTrue(records.size() == 2, "both ops in flight; got " + records.size());
                    helper.assertTrue(records.get(1).status() == OperationRecord.STATUS_WAITING,
                            "the contender WAITs on the holder's lock; got " + records.get(1).status());
                })
                .thenExecuteAfter(90, () -> {
                    final long left = NetworkStorage.of(helper.getLevel(), mainframe.networkUuid())
                            .count(Items.COBBLESTONE);
                    helper.assertTrue(left == 80,
                            "the contender acquired the freed lock and moved its 20; left " + left);
                    helper.assertTrue(mainframe.recentOperations().get(0).status()
                                    == OperationRecord.STATUS_COMPLETED,
                            "the contender completed after acquiring");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void lockContention_timesOutAsResourceLocked(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        final ItemStackHandler fullDest = fullHandler();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () ->
                        seededRack(helper).getServerStorage(0).insert(Items.COBBLESTONE, 100))
                .thenExecuteAfter(2, () -> helper.assertTrue(
                        mainframe.submitNetworkSelect(Items.COBBLESTONE, 100, port(fullDest), "full") != null,
                        "the lock holder dispatched"))
                .thenExecuteAfter(4, () -> {
                    // All 100 are locked by the stalled holder: this contender starts WAITING.
                    final var contender = new dev.jstech.computers.operation
                            .NetworkSelectOperation(helper.getLevel(), mainframe.networkUuid(),
                            StorageKey.of(Items.COBBLESTONE), 50, port(new ItemStackHandler(9)), "test",
                            OperationRecord.TYPE_SELECT, java.util.UUID.randomUUID(),
                            mainframe.networkIndex(), null, null, 3);
                    helper.assertTrue(contender.isWaiting(), "the contender starts WAITING");
                    for (int i = 0; i < 5; i++) {
                        contender.tick(1_000L); // retries past its 3-tick timeout
                    }
                    helper.assertTrue(contender.isDone(), "the wait timed out");
                    helper.assertTrue(contender.status() == OperationRecord.STATUS_RESOURCE_LOCKED,
                            "timeout settles as RESOURCE_LOCKED; got " + contender.status());
                })
                .thenSucceed();
    }
}
