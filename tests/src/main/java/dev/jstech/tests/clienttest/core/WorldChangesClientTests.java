/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.field.ChunkFieldsClient;
import dev.jstech.core.client.notification.NotificationsClient;
import dev.jstech.core.world.event.WorldEvents;
import dev.jstech.tests.TestWorldChanges;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;

/**
 * The world changing as a player meets it: a field over the chunks reaching their game for the chunks round them, and
 * a coming storm warned of to the player carrying its instrument, to set beside what the warning should look like.
 */
public final class WorldChangesClientTests {

    private static final int WAIT = 200;
    private static final float AMOUNT = 50F;
    private static final float CLOSE = 0.01F;
    private static final int STORM_IN = 300;
    private static final int STORM_WAIT = 400;

    private WorldChangesClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void field_reachesThePlayersGameForTheChunksRoundThem(final ClientTestContext ctx) {
        final ChunkPos[] at = new ChunkPos[1];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            if (at[0] != null) {
                TestWorldChanges.STILL.add(ctx.serverPlayer().serverLevel(), at[0], -AMOUNT);
            }
        }));
        ctx.thenServer(0, level -> {
            at[0] = ctx.serverPlayer().chunkPosition();
            TestWorldChanges.STILL.add(ctx.serverPlayer().serverLevel(), at[0], AMOUNT);
        })
                .thenWaitUntil(() -> Math.abs(ChunkFieldsClient.value(TestWorldChanges.STILL, at[0]) - AMOUNT) < CLOSE,
                        WAIT, "the field's value to reach the player's game")
                .then(1, () -> ctx.assertTrue(ChunkFieldsClient.value(TestWorldChanges.STILL,
                        new ChunkPos(at[0].x + 1, at[0].z)) == 0, "a chunk beside it holds none"))
                .thenServer(1, level -> TestWorldChanges.STILL.add(ctx.serverPlayer().serverLevel(), at[0], -AMOUNT))
                .thenWaitUntil(() -> ChunkFieldsClient.value(TestWorldChanges.STILL, at[0]) == 0, WAIT,
                        "the field taken away to reach the player's game");
    }

    @ClientTest(timeoutTicks = 800)
    public static void storm_isWarnedOfToThePlayerCarryingItsInstrument(final ClientTestContext ctx) {
        final String warning = WorldEvents.WARNING.id().toString();
        ctx.afterTest(() -> ctx.server().execute(() -> {
            WorldEvents.stop(ctx.serverPlayer().serverLevel(), TestWorldChanges.STORM);
            takeCompasses(ctx.serverPlayer());
        }));
        ctx.then(0, () -> NotificationsClient.board().clear())
                .thenServer(0, level -> {
                    final ServerPlayer player = ctx.serverPlayer();
                    WorldEvents.stop(player.serverLevel(), TestWorldChanges.STORM);
                    player.getInventory().add(new ItemStack(Items.COMPASS));
                    WorldEvents.plan(player.serverLevel(), TestWorldChanges.STORM, STORM_IN, Optional.empty());
                })
                .thenWaitUntil(() -> NotificationsClient.board().showing(Util.getMillis()).stream()
                        .anyMatch(entry -> entry.kind().equals(warning)), STORM_WAIT,
                        "the storm's warning to show on the player's game")
                .thenScreenshot(4, "storm_warning");
    }

    private static void takeCompasses(final ServerPlayer player) {
        final Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(Items.COMPASS)) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
    }
}
