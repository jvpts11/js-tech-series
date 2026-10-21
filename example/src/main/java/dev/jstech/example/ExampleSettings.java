/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Example.
 */
package dev.jstech.example;

import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;

/**
 * The example mod's settings: one file of the server's, {@code jsexample-server.toml}, which the Core reads, checks
 * against each setting's range, writes back with its comments and shows on its settings screen.
 */
public final class ExampleSettings {

    /** How much energy the generator makes each tick it burns. */
    public static final ConfigKey<Integer> GENERATOR_OUTPUT = ConfigKey.whole("generator.output", 40)
            .range(1, 1_000)
            .comment("How much energy the generator makes each tick it burns fuel.")
            .named("Generator output").unit("FE/t");
    /** The file. */
    public static final ConfigFile FILE = ConfigFile.builder("jsexample-server", ConfigSide.SERVER,
                    ConfigFormats.TOML)
            .comment("How the machines of J's Example behave.")
            .sectionNamed("generator", "Generator")
            .key(GENERATOR_OUTPUT)
            .build();

    private ExampleSettings() {
    }

    /** How much energy the generator makes each tick it burns, as the file says now. */
    public static int generatorOutput() {
        return FILE.get(GENERATOR_OUTPUT);
    }
}
