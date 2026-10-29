/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.config.ConfigKey;
import dev.jstech.space.config.SpaceServerConfig;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * J's Space's settings, {@code jsspace-server.toml}: every consequence on unless a server turns it off, each saying
 * what happens when it is off, the two rules of balance that start off, and the whole file read by the server.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SpaceSettingsGameTests {

    private static final String ARENA = "empty";
    private static final String OFF = "Off, ";
    /* The rules of balance that start off: fast travel, and J's Industrial's chains as the only path. */
    private static final List<ConfigKey<Boolean>> STARTING_OFF = List.of(SpaceServerConfig.FAST_TRAVEL,
            SpaceServerConfig.INDUSTRIAL_CHAIN_ONLY);

    private SpaceSettingsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void consequences_areOnAndSayWhatHappensWhenOff(final GameTestHelper helper) {
        for (final ConfigKey<Boolean> key : SpaceServerConfig.CONSEQUENCES) {
            helper.assertTrue(key.defaultValue(), key.dottedPath() + " is on unless a server turns it off");
            helper.assertTrue(key.comment().size() == 2 && key.comment().get(1).startsWith(OFF),
                    key.dottedPath() + " says what it is and what happens when off: " + key.comment());
            helper.assertTrue(!key.title().isEmpty(), key.dottedPath() + " is named for the settings screen");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void rulesOfBalance_fastTravelAndTheIndustrialChainStartOff(final GameTestHelper helper) {
        for (final ConfigKey<Boolean> key : STARTING_OFF) {
            helper.assertTrue(!key.defaultValue(), key.dottedPath() + " starts off");
            helper.assertTrue(!key.title().isEmpty() && key.comment().get(key.comment().size() - 1).startsWith(OFF),
                    key.dottedPath() + " is named and says what off means: " + key.comment());
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void file_holdsEverySettingAndIsReadByTheServer(final GameTestHelper helper) {
        helper.assertTrue(SpaceServerConfig.FILE.keys().size()
                        == SpaceServerConfig.CONSEQUENCES.size() + STARTING_OFF.size()
                        + SpaceServerConfig.SIZES.size(),
                "the file holds the consequences, the rules of balance and the sizes, and nothing else: "
                        + SpaceServerConfig.FILE.keys().size());
        for (final ConfigKey<Integer> size : SpaceServerConfig.SIZES) {
            helper.assertTrue(size.range().isPresent() && !size.title().isEmpty(),
                    size.dottedPath() + " is held to its range and named for the settings screen");
        }
        for (final ConfigKey<?> key : SpaceServerConfig.FILE.keys()) {
            helper.assertTrue(SpaceServerConfig.FILE.get(key).equals(key.defaultValue()),
                    "the server reads " + key.dottedPath() + " from the world's file, at its default");
        }
        helper.succeed();
    }
}
