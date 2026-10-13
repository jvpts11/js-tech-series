/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.operation.payload.OperationRecord.MoveRow;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.JsTests;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * An Operation record travels in packets whose lists have a hard cap, so a record built from an Operation that
 * touched more servers than the cap must be cut where it is built, not fail when it is encoded.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class OperationRecordGameTests {

    private static final String ARENA = "empty";

    private OperationRecordGameTests() {
    }

    @GameTest(template = ARENA)
    public static void constructor_capsMoveRowsAtTheWireLimit(final GameTestHelper helper) {
        final List<MoveRow> moves = new ArrayList<>();
        for (int i = 0; i < OperationRecord.MAX_MOVES + 10; i++) {
            moves.add(new MoveRow("server " + i, 1L, "terminal"));
        }
        final OperationRecord record = new OperationRecord(OperationRecord.TYPE_SELECT,
                StorageKey.of(Items.STONE), 100L, 50L, OperationRecord.STATUS_COMPLETED, moves);
        helper.assertTrue(record.moves().size() == OperationRecord.MAX_MOVES, "the moves are cut to the cap");
        final RegistryFriendlyByteBuf buf =
                new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        OperationRecord.STREAM_CODEC.encode(buf, record);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void statusText_readsTheSameWordAsTheStableToken(final GameTestHelper helper) {
        helper.assertTrue(OperationRecord.statusText(OperationRecord.STATUS_COMPLETED).english()
                .equals(OperationRecord.statusName(OperationRecord.STATUS_COMPLETED)), "done reads as done");
        helper.succeed();
    }
}
