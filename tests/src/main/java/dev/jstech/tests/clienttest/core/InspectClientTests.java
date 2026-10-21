/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.diagnostic.DiagnosticsClient;
import dev.jstech.core.inspect.InspectTexts;
import dev.jstech.core.inspect.InspectWatch;
import dev.jstech.core.text.GameText;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

/**
 * A place watched with {@code /jstech inspect watch}: the player's debug screen shows what the server reads of it, as
 * a panel of its own, until they stop.
 */
public final class InspectClientTests {

    private static final BlockPos CHEST = new BlockPos(1, 2, 3);

    private InspectClientTests() {
    }

    @ClientTest(timeoutTicks = 200)
    public static void watch_showsThePlaceOnTheDebugScreenUntilStopped(final ClientTestContext ctx) {
        final String panel = GameText.resolve(InspectTexts.PANEL);
        ctx.thenServer(0, level -> {
            level.setBlockAndUpdate(ctx.abs(CHEST), Blocks.CHEST.defaultBlockState());
            InspectWatch.watch(level.getServer().getPlayerList().getPlayers().getFirst(), ctx.abs(CHEST));
        }).thenWaitUntil(() -> DiagnosticsClient.lines().stream().anyMatch(line -> line.endsWith(panel))
                        && DiagnosticsClient.lines().stream().anyMatch(line -> line.endsWith(": minecraft:chest")), 60,
                "the watched chest on the debug screen")
                .thenServer(0, level -> InspectWatch.stop(level.getServer().getPlayerList().getPlayers().getFirst()))
                .thenWaitUntil(() -> DiagnosticsClient.lines().stream().noneMatch(line -> line.endsWith(panel)), 60,
                        "and gone once the player stops watching")
                .thenServer(0, level -> level.removeBlock(ctx.abs(CHEST), false));
    }
}
