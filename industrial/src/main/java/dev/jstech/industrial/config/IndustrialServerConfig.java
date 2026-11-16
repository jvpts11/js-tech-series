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
 * what its discoveries open, the consequences of its pipes, each on unless a server turns it off, each kind of its
 * tools, which a server may leave out, and whether its own recipes replace J's Core's for the basic lines.
 */
public final class IndustrialServerConfig {

    private static final String RESEARCH = "research";
    private static final String FLUIDS = "fluids";
    private static final String TOOLS = "tools";
    private static final String RECIPES = "recipes";

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
    /** Whether each kind of the mod's tools is made: its swords, pickaxes, axes, shovels and hoes. */
    public static final ConfigKey<Boolean> SWORDS = toolKind("swords").named("Swords");
    public static final ConfigKey<Boolean> PICKAXES = toolKind("pickaxes").named("Pickaxes");
    public static final ConfigKey<Boolean> AXES = toolKind("axes").named("Axes");
    public static final ConfigKey<Boolean> SHOVELS = toolKind("shovels").named("Shovels");
    public static final ConfigKey<Boolean> HOES = toolKind("hoes").named("Hoes");
    /**
     * Whether J's Industrial's own recipes make J's Core's basic lines and battery, its pipes, its cable and its
     * battery bank, in place of the Core's recipes of the game's own materials; off as it comes, J's Industrial
     * having none of its own for them yet.
     */
    public static final ConfigKey<Boolean> INDUSTRIAL_BASIC_LINES = ConfigKey.flag(RECIPES + ".industrial_basic_lines",
                    false)
            .comment("Whether J's Industrial's own recipes make J's Core's basic pipes, cable and battery bank in place "
                            + "of the Core's recipes of the game's own materials.",
                    "Off, the Core's recipes make them.")
            .named("J's Industrial makes the basic lines");

    public static final ConfigFile FILE = ConfigFile.builder("jsindustrial-server", ConfigSide.SERVER,
                    ConfigFormats.TOML)
            .sectionNamed(RESEARCH, "Research")
            .sectionComment(RESEARCH, "Whether the discoveries of J's Industrial lock what they open.")
            .key(REQUIRE_RESEARCH)
            .sectionNamed(FLUIDS, "Fluids")
            .sectionComment(FLUIDS, "What befalls a pipe past what it is made for, and what leaks out of it.")
            .key(PIPES_FAIL).key(LEAKS_HAZARDOUS).key(HYDROGEN_EMBRITTLES)
            .sectionNamed(TOOLS, "Tools")
            .sectionComment(TOOLS, "Which kinds of J's Industrial's tools are made.")
            .key(SWORDS).key(PICKAXES).key(AXES).key(SHOVELS).key(HOES)
            .sectionNamed(RECIPES, "Recipes")
            .sectionComment(RECIPES, "Which recipes J's Industrial gives in place of J's Core's.")
            .key(INDUSTRIAL_BASIC_LINES)
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

    /* The switch of a kind of the mod's tools, named {@code plural} in its setting and its comment, on as it comes. */
    private static ConfigKey<Boolean> toolKind(final String plural) {
        return ConfigKey.flag(TOOLS + "." + plural, true).comment("Whether J's Industrial's " + plural + " are made.",
                "Off, their recipes are gone and they leave the creative tab; those already made keep working.");
    }
}
