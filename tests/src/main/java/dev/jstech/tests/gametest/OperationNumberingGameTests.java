/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.core.operation.OperationStatus;
import dev.jstech.tests.JsTests;
import java.util.Map;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The state of an Operation is one number wherever it is written, and this is what says so.
 *
 * <p>A state travels twice: as the state itself, with the number it carries, and as the byte a saved log and
 * a packet are written with. They have to be the same number. They were not always: the log had a numbering
 * of its own in an order of its own, so the same saved world read one way through the log and another way
 * through the state, and nothing anywhere would have said so.
 *
 * <p>A switch needs a label it can read at compile time, so the log cannot simply ask the state for its
 * number. That is the whole reason this test exists: what the compiler cannot hold, this does.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class OperationNumberingGameTests {

    private static final String ARENA = "empty";

    /** Every state, beside the byte the log writes it as. */
    private static final Map<OperationStatus, Byte> WRITTEN_AS = Map.of(
            OperationStatus.PENDING, OperationRecord.STATUS_PENDING,
            OperationStatus.PROCESSING, OperationRecord.STATUS_PROCESSING,
            OperationStatus.WAITING, OperationRecord.STATUS_WAITING,
            OperationStatus.COMPLETED, OperationRecord.STATUS_COMPLETED,
            OperationStatus.COMPLETED_PARTIAL, OperationRecord.STATUS_PARTIAL,
            OperationStatus.FAILED, OperationRecord.STATUS_FAILED,
            OperationStatus.RESOURCE_LOCKED, OperationRecord.STATUS_RESOURCE_LOCKED,
            OperationStatus.DISCARDED, OperationRecord.STATUS_DISCARDED);

    @GameTest(template = ARENA)
    public static void status_isTheSameNumberInTheLogAsInTheState(final GameTestHelper helper) {
        for (final Map.Entry<OperationStatus, Byte> pair : WRITTEN_AS.entrySet()) {
            helper.assertTrue(pair.getKey().id() == pair.getValue(),
                    pair.getKey() + " is " + pair.getKey().id() + " but the log writes it as " + pair.getValue());
        }
        helper.assertTrue(WRITTEN_AS.size() == OperationStatus.values().length,
                "a state exists that the log has no number for");
        helper.succeed();
    }

    /*
     * Zero is kept back so that a record about an Operation nobody knows can say so, rather than saying the
     * first of the eight and being believed.
     */
    @GameTest(template = ARENA)
    public static void status_keepsZeroForAnOperationNobodyKnows(final GameTestHelper helper) {
        for (final OperationStatus status : OperationStatus.values()) {
            helper.assertTrue(status.id() != OperationStatus.UNKNOWN,
                    status + " took the number kept for an Operation nobody knows");
        }
        helper.assertTrue(OperationStatus.of(OperationStatus.UNKNOWN).isEmpty(),
                "the number kept for an unknown Operation answered with a state");
        helper.assertTrue(OperationStatus.of(OperationStatus.COMPLETED.id()).orElse(null)
                == OperationStatus.COMPLETED, "a state did not come back from its own number");
        helper.succeed();
    }
}
