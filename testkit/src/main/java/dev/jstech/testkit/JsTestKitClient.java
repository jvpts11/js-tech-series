/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit;

import dev.jstech.testkit.client.ClientTestRunner;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** The test kit on a player's game: the client tests' runner, which runs only when the game is started for them. */
@Mod(value = JsTestKit.MODID, dist = Dist.CLIENT)
public final class JsTestKitClient {

    public JsTestKitClient(final IEventBus modEventBus, final ModContainer container) {
        ClientTestRunner.listen();
    }
}
