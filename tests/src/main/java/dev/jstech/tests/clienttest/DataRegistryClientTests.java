/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestData;
import net.minecraft.resources.ResourceLocation;

/**
 * Content from datapacks on a player's game: the notes the server read were sent as the player joined and are kept
 * apart from the server's, and the shades a datapack filled arrived with the world's registries.
 */
public final class DataRegistryClientTests {

    private DataRegistryClientTests() {
    }

    @ClientTest(timeoutTicks = 100)
    public static void synced_reachThePlayersGame(final ClientTestContext ctx) {
        ctx.thenWaitUntil(() -> TestData.NOTES.clientEntries().size() == 2, 40,
                        "the notes to reach the player's game")
                .thenAssert(0, () -> TestData.NOTES.clientEntries()
                        .get(ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "second"))
                        .equals(new TestData.Shade("second", 2)), "the second note as the server read it")
                .thenAssert(0, () -> ctx.mc().level != null && TestData.NOTES.entries(ctx.mc().level)
                        == TestData.NOTES.clientEntries(), "a level on the player's side reads the player's copy")
                .thenAssert(0, () -> ctx.mc().level != null && ctx.mc().level.registryAccess()
                        .registryOrThrow(TestData.SHADES)
                        .containsKey(ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "teal")),
                        "the datapack's teal reached the player's registries");
    }
}
