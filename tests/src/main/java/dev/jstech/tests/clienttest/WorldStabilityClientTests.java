/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import com.mojang.serialization.Lifecycle;
import dev.jstech.tests.TestWorldGen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;

/**
 * A world made with the series' mods is one the game opens without its warning about experimental settings: the
 * game counts a world of more than its own three dimensions as experimental, so a dimension a mod only ever copies
 * while the game runs adds no stem the world loads. A world that grew a fourth would stop every test that saves and
 * reopens it on that warning.
 */
public final class WorldStabilityClientTests {

    private WorldStabilityClientTests() {
    }

    @ClientTest(timeoutTicks = 100)
    public static void world_staysOfTheGamesOwnDimensions(final ClientTestContext ctx) {
        ctx.then(0, () -> {
            final MinecraftServer server = ctx.mc().getSingleplayerServer();
            ctx.assertTrue(server != null, "the test world runs on this game");
            ctx.assertTrue(!server.registryAccess().registryOrThrow(Registries.LEVEL_STEM)
                    .containsKey(TestWorldGen.MOON.id()), "the moon, only ever copied, is no stem of the world");
            ctx.assertTrue(server.getWorldData().worldGenSettingsLifecycle().equals(Lifecycle.stable()),
                    "and the world is stable, opened again without a warning; it is "
                            + server.getWorldData().worldGenSettingsLifecycle());
        });
    }
}
