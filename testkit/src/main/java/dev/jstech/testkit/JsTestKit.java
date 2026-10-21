/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit;

import com.mojang.logging.LogUtils;
import dev.jstech.testkit.gametest.GameTestTimes;
import dev.jstech.testkit.gametest.StallMonitor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * J's Core Test Kit: what a mod built on J's Core tests itself with. It is a mod so its helpers stand in the game
 * beside the mods they test; it adds nothing to the game and is a test dependency only, never installed.
 */
@Mod(JsTestKit.MODID)
public final class JsTestKit {

    public static final String MODID = "jstestkit";

    private static final Logger LOGGER = LogUtils.getLogger();

    public JsTestKit(final IEventBus modEventBus, final ModContainer modContainer) {
        LOGGER.info("J's Core Test Kit {} loaded, for tests only", modContainer.getModInfo().getVersion());
        // A run of GameTests reports each test's time as it ends, and says where it hangs while it hangs.
        GameTestTimes.listen();
        StallMonitor.listen();
    }
}
