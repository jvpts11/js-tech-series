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
import dev.jstech.industrial.api.Consequences;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * The settings of J's Industrial for a world, {@code jsindustrial-server.toml} beside it: whether its research locks
 * what its discoveries open, and the consequences of its pipes, each on unless a server turns it off.
 */
public final class IndustrialServerConfig {

    private static final String RESEARCH = "research";
    private static final String FLUIDS = "fluids";

    /** Whether a recipe that needs a discovery waits until the team of the machine's owner has made it. */
    public static final ConfigKey<Boolean> REQUIRE_RESEARCH = ConfigKey.flag(RESEARCH + ".require_research", true)
            .comment("Whether a recipe that needs a discovery is locked until the team of the machine's owner has "
                            + "made it.",
                    "Off, nothing is locked and no discovery is granted: research can still be done, for its "
                            + "advancements.")
            .named("Require research");
    /** Whether a pipe that something reaches it is not made for breaks, or only refuses it. */
    public static final ConfigKey<Boolean> PIPES_FAIL = Consequences.of(FLUIDS + ".pipes_fail", "Pipes fail",
            "A pipe too hot, too cold, corroded, worn through or past its pressure breaks, as a real one does.",
            "what a pipe is not made for just does not go in, and nothing wears it.");
    /** Whether what a broken pipe lets out does harm. */
    public static final ConfigKey<Boolean> LEAKS_HAZARDOUS = Consequences.of(FLUIDS + ".leaks_are_hazardous",
            "Leaks are hazardous", "A broken pipe spills its liquid and lets its gas out as a cloud that poisons, "
                    + "smothers or explodes as the gas does.", "a leak only loses what the pipe carried.");
    /** Whether hydrogen wears through the steel not made to stand it. */
    public static final ConfigKey<Boolean> HYDROGEN_EMBRITTLES = Consequences.of(FLUIDS + ".hydrogen_embrittles",
            "Hydrogen embrittles steel", "Hydrogen carried in plain steel cracks it after a while, as it does in "
                    + "real pipelines.", "hydrogen runs in any pipe that holds it.");

    public static final ConfigFile FILE = ConfigFile.builder("jsindustrial-server", ConfigSide.SERVER,
                    ConfigFormats.TOML)
            .sectionNamed(RESEARCH, "Research")
            .sectionComment(RESEARCH, "Whether the discoveries of J's Industrial lock what they open.")
            .key(REQUIRE_RESEARCH)
            .sectionNamed(FLUIDS, "Fluids")
            .sectionComment(FLUIDS, "What befalls a pipe past what it is made for, and what leaks out of it.")
            .key(PIPES_FAIL).key(LEAKS_HAZARDOUS).key(HYDROGEN_EMBRITTLES)
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

    /** Whether a pipe past what it is made for breaks. */
    public static boolean pipesFail() {
        return FILE.get(PIPES_FAIL);
    }

    /** Whether what a broken pipe lets out does harm. */
    public static boolean leaksHazardous() {
        return FILE.get(LEAKS_HAZARDOUS);
    }

    /** Whether hydrogen wears through plain steel. */
    public static boolean hydrogenEmbrittles() {
        return FILE.get(HYDROGEN_EMBRITTLES);
    }
}
