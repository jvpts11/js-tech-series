/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import com.mojang.logging.LogUtils;
import dev.jstech.core.JsCore;
import dev.jstech.tests.testkit.TestSettings;
import dev.jstech.tests.testkit.TestStates;
import dev.jstech.tests.testkit.ToyLanguage;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.gametest.GameTestHooks;
import org.slf4j.Logger;

/**
 * The test mod of the J's Tech Series: a development tool, never published and never to be installed. It
 * carries the GameTests, the client tests and the test kit for every mod, so a test may span several mods
 * and no shipped jar holds test code. It adds nothing to the game.
 */
@Mod(JsTests.MODID)
public final class JsTests {

    public static final String MODID = "jstests";

    public static final Logger LOGGER = LogUtils.getLogger();

    public JsTests(final IEventBus modEventBus, final ModContainer modContainer) {
        LOGGER.warn("J's Tech Series Tests {} loaded. This is a development-only test mod: it is not part of the"
                + " series, adds nothing to the game and must not be installed.", modContainer.getModInfo().getVersion());
        // The sounds the tests play through the series' sound system, on files the game already has.
        TestBlocks.declare();
        // Items that hold everything an item can, to prove the Core's items with state.
        TestItems.declare();
        // A corrosive liquid and a hot gas, to prove the Core's fluids and pipes.
        TestFluids.declare();
        // A registry datapacks fill and notes read from datapack files, both sent to the players.
        TestData.declare();
        TestSounds.CONTENT.register(modEventBus);
        // A cable that never shares a block, to show the shared block refusing it company.
        TestCableTypes.register(modEventBus);
        // A state of every scope, on both sides, since the client tests watch them arrive.
        TestStates.register();
        if (GameTestHooks.isGametestServer()) {
            /*
             * The language API's tests need a language that is not the series' own, and languages are only taken
             * while the game loads; a client run stays as a player sees it.
             */
            JsCore.languages().register(new ToyLanguage());
            // A world's settings file in every format, where the settings tests can read and write them.
            TestSettings.register(modEventBus, modContainer);
        }
    }
}
