/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import com.mojang.serialization.Codec;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;
import dev.jstech.core.config.format.IConfigFormat;
import dev.jstech.core.config.format.NbtConfigFormat;
import java.util.List;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * A world's settings file in each format the Core writes, the same settings in each, registered the way a mod
 * registers its own, so the tests can see where each lands, what it says and how it is read back.
 */
public final class TestSettings {

    public static final ConfigKey<Boolean> MENU = ConfigKey.flag("boot.show_menu", true)
            .comment("Whether the boot menu shows.");
    public static final ConfigKey<Integer> SPEED = ConfigKey.whole("speed", 512).range(1, 1024)
            .comment("How fast it goes.");
    public static final ConfigKey<String> DIALECT = ConfigKey.text("dialect", "SIMPLE").allowing("SIMPLE", "STANDARD");
    public static final ConfigKey<List<String>> WORDS = ConfigKey.of("words", Codec.STRING.listOf(), List.of("a", "b"));
    /** Set by the tests that write a file back, and by nothing else, so the others read the defaults. */
    public static final ConfigKey<String> NOTE = ConfigKey.text("note", "untouched");

    public static final ConfigFile TOML = file("jstests-toml", ConfigFormats.TOML);
    public static final ConfigFile YAML = file("jstests-yaml", ConfigFormats.YAML);
    public static final ConfigFile JSON5 = file("jstests-json5", ConfigFormats.JSON5);
    public static final ConfigFile JSON = file("jstests-json", ConfigFormats.JSON);
    public static final ConfigFile NBT = file("jstests-nbt", NbtConfigFormat.INSTANCE);

    private TestSettings() {
    }

    /** Puts every file in its place, from the test mod's constructor. */
    public static void register(final IEventBus modEventBus, final ModContainer modContainer) {
        for (final ConfigFile file : List.of(TOML, YAML, JSON5, JSON, NBT)) {
            ConfigFiles.register(file, modEventBus, modContainer);
        }
    }

    private static ConfigFile file(final String name, final IConfigFormat format) {
        return ConfigFile.builder(name, ConfigSide.SERVER, format)
                .comment("A settings file of the test mod.")
                .sectionComment("boot", "Starting up.")
                .key(MENU)
                .key(SPEED)
                .key(DIALECT)
                .key(WORDS)
                .key(NOTE)
                .build();
    }
}
