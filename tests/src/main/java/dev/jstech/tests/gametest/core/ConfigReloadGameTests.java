/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.tests.JsTests;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A settings file read again from disk, as NeoForge reads it on a thread of its own whenever it changes: a reading of
 * what the Core itself wrote a moment before a later writing leaves the later values standing, where a setting changed
 * twice in quick succession went back to the first; an edit made to the file is still taken. Alone in its batch, as
 * it changes settings of J's Computers' world file.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ConfigReloadGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jscore_config_reload";
    private static final String FILE_NAME = "jscomputers-server.toml";
    /* The ticks NeoForge's watcher, on the clock, has to see each change: the test's ticks run faster than that. */
    private static final int PATIENT = 6000;

    private ConfigReloadGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = PATIENT)
    public static void reload_ofAnEarlierWritingLeavesTheLaterValuesStanding(final GameTestHelper helper) {
        final ConfigFile file = ComputersServerConfig.FILE;
        final Path path = ModConfigs.getFileMap().get(FILE_NAME).getFullPath();
        final boolean kept = file.get(ComputersServerConfig.LIST_COMMANDS);
        final boolean catalog = file.get(ComputersServerConfig.SOUNDFOUNDRY_CATALOG);
        file.set(ComputersServerConfig.LIST_COMMANDS, !kept);
        final byte[] earlier = read(path);
        file.set(ComputersServerConfig.LIST_COMMANDS, kept);
        // What NeoForge's late reading finds: the file as the earlier writing left it.
        write(path, earlier);
        // The file is read again on NeoForge's watcher's time, not the game's: the later value is written back once it
        // is, and never if the earlier writing read back were taken.
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(file.get(ComputersServerConfig.LIST_COMMANDS) == kept
                        && text(path).contains("list_commands = " + kept), "the earlier writing read back leaves "
                        + "the later value standing, and the file says it again"))
                // An edit to the file, a value the Core never wrote: it is taken.
                .thenExecute(() -> write(path, text(path).replace("catalog = " + catalog, "catalog = " + !catalog)
                        .getBytes(StandardCharsets.UTF_8)))
                .thenWaitUntil(() -> helper.assertTrue(file.get(ComputersServerConfig.SOUNDFOUNDRY_CATALOG)
                        == !catalog, "an edit made to the file is taken"))
                .thenExecute(() -> file.set(ComputersServerConfig.SOUNDFOUNDRY_CATALOG, catalog))
                .thenSucceed();
    }

    private static byte[] read(final Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String text(final Path path) {
        return new String(read(path), StandardCharsets.UTF_8);
    }

    private static void write(final Path path, final byte[] bytes) {
        try {
            Files.write(path, bytes);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
