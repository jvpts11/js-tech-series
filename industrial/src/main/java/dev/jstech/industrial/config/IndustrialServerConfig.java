/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.config;

import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * The settings of J's Industrial for a world, {@code jsindustrial-server.toml} beside it: whether its research locks
 * what its discoveries open.
 */
public final class IndustrialServerConfig {

    private static final String RESEARCH = "research";

    /** Whether a recipe that needs a discovery waits until the team of the machine's owner has made it. */
    public static final ConfigKey<Boolean> REQUIRE_RESEARCH = ConfigKey.flag(RESEARCH + ".require_research", true)
            .comment("Whether a recipe that needs a discovery is locked until the team of the machine's owner has "
                            + "made it.",
                    "Off, nothing is locked and no discovery is granted: research can still be done, for its "
                            + "advancements.")
            .named("Require research");

    public static final ConfigFile FILE = ConfigFile.builder("jsindustrial-server", ConfigSide.SERVER,
                    ConfigFormats.TOML)
            .sectionNamed(RESEARCH, "Research")
            .sectionComment(RESEARCH, "Whether the discoveries of J's Industrial lock what they open.")
            .key(REQUIRE_RESEARCH)
            .build();

    private IndustrialServerConfig() {
    }

    /** Puts the file beside the world, where the server reads it. */
    public static void register(final IEventBus modEventBus, final ModContainer modContainer) {
        ConfigFiles.register(FILE, modEventBus, modContainer);
    }

    /** Whether J's Industrial's research locks what its discoveries open. */
    public static boolean requireResearch() {
        return FILE.get(REQUIRE_RESEARCH);
    }
}
