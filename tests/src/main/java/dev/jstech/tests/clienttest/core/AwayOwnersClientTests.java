/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.region.AwayOwners;
import dev.jstech.core.region.ChunkLoadingBalance;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.UUID;
import net.minecraft.world.level.storage.ServerLevelData;

/**
 * When each owner left is kept with the world: an owner noted away before the game is saved and opened again is away
 * since then still, so a server that lets chunks go after an hour lets theirs go once an hour of its running has gone
 * by, while an owner never seen leaving counts from the moment they are first looked at.
 */
public final class AwayOwnersClientTests {

    private static final int SETTLE = 20;
    private static final int HOURS_GONE = 2;

    private AwayOwnersClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void awayOwners_countTheirTimeAwayAcrossARestart(final ClientTestContext ctx) {
        final UUID away = UUID.randomUUID();
        final UUID unseen = UUID.randomUUID();
        final boolean kept = ChunkLoadingBalance.keepWhileAway();
        final int hours = ChunkLoadingBalance.awayHours();
        ctx.afterTest(() -> ctx.server().execute(() -> {
            ChunkLoadingBalance.setKeepWhileAway(kept);
            ChunkLoadingBalance.setAwayHours(hours);
            AwayOwners.back(ctx.server(), away);
            AwayOwners.back(ctx.server(), unseen);
        }));
        ctx.thenServer(0, level -> AwayOwners.left(level.getServer(), away))
                .thenSaveAndReload(SETTLE)
                .thenServer(0, level -> {
                    ChunkLoadingBalance.setKeepWhileAway(true);
                    ChunkLoadingBalance.setAwayHours(1);
                    // Two hours of the server's running go by at once, as they would with the game left on.
                    final ServerLevelData time = (ServerLevelData) level.getServer().overworld().getLevelData();
                    time.setGameTime(time.getGameTime() + HOURS_GONE * ChunkLoadingBalance.TICKS_AN_HOUR);
                    ctx.assertTrue(AwayOwners.letGo(level.getServer(), away) && AwayOwners.letGoOf(away),
                            "the owner noted away before the restart has been away the two hours since, and their "
                            + "chunks are let go");
                    ctx.assertTrue(!AwayOwners.letGo(level.getServer(), unseen) && !AwayOwners.letGoOf(unseen),
                            "while an owner never seen leaving counts from now");
                });
    }
}
