/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.stepaside;

import com.mojang.logging.LogUtils;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * One of the Core's systems stepping aside for an installed mod that does the same job, so the two do not do it twice:
 * the system names the mods that do what it does, and while one is installed the Core's own is off, unless its setting
 * keeps it, the player's for a system of looks and comfort, the world's for one that changes the world's data, such as
 * unifying its recipes ({@link #of}). The log says, once, what stepped aside for what.
 *
 * <pre>{@code
 * if (StepAside.CONNECTED_TEXTURES.steps()) {
 *     return;   // Continuity or CTM draws the packs' connected textures
 * }
 * }</pre>
 * A game rule of a mod that rests on the Core's system never steps aside.
 */
public final class StepAside {

    private final String system;
    private final List<String> mods;
    private final ConfigKey<Boolean> key;
    /** The file the setting is in, read as the world's data reads it; none for the player's own file below. */
    private final @Nullable ConfigFile file;
    private final AtomicBoolean told = new AtomicBoolean();
    private volatile boolean stepsAside = true;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SECTION = "step_aside";
    /** Whether the connected textures of resource packs are left to Continuity or CTM. */
    public static final ConfigKey<Boolean> CONNECTED_TEXTURES_KEY = ConfigKey.flag(SECTION + ".connected_textures",
                    true)
            .comment("Whether the Core leaves the connected textures of resource packs to Continuity or CTM when one "
                    + "is installed. Off keeps the Core's own as well, and the two may then draw over each other.")
            .named("Leave connected textures to Continuity or CTM");
    /** The connected textures of resource packs, which Continuity and CTM read too. */
    public static final StepAside CONNECTED_TEXTURES = new StepAside("connected_textures",
            List.of("continuity", "ctm"), CONNECTED_TEXTURES_KEY);
    /** Whether the light of what is held and what burns is left to a mod of dynamic light. */
    public static final ConfigKey<Boolean> DYNAMIC_LIGHT_KEY = ConfigKey.flag(SECTION + ".dynamic_light", true)
            .comment("Whether the Core leaves the light of what is held, what burns and what glows to "
                    + "LambDynamicLights or Sodium Dynamic Lights when one is installed.")
            .named("Leave dynamic light to another mod");
    /** The light of what is held, burns and glows, which LambDynamicLights and Sodium Dynamic Lights give too. */
    public static final StepAside DYNAMIC_LIGHT = new StepAside("dynamic_light",
            List.of("lambdynlights", "sodiumdynamiclights"), DYNAMIC_LIGHT_KEY);
    /** Whether coloured light and its glow are left to Shimmer. */
    public static final ConfigKey<Boolean> COLOURED_LIGHT_KEY = ConfigKey.flag(SECTION + ".coloured_light", true)
            .comment("Whether the Core leaves coloured light and the glow of bright things to Shimmer when it is "
                    + "installed.")
            .named("Leave coloured light to Shimmer");
    /** Coloured light and the glow of bright things, which Shimmer gives too. */
    public static final StepAside COLOURED_LIGHT = new StepAside("coloured_light", List.of("shimmer"),
            COLOURED_LIGHT_KEY);
    /** The player's file. */
    public static final ConfigFile FILE = ConfigFile.builder("jstech-step-aside", ConfigSide.CLIENT,
                    ConfigFormats.TOML)
            .comment("Which of the Core's systems step aside on this game for an installed mod that does their job.")
            .sectionComment(SECTION, "Each system, and whether it steps aside while such a mod is installed.")
            .sectionNamed(SECTION, "Stepping aside")
            .key(CONNECTED_TEXTURES_KEY, value -> CONNECTED_TEXTURES.stepsAside = value)
            .key(DYNAMIC_LIGHT_KEY, value -> DYNAMIC_LIGHT.stepsAside = value)
            .key(COLOURED_LIGHT_KEY, value -> COLOURED_LIGHT.stepsAside = value)
            .build();

    private StepAside(final String system, final List<String> mods, final ConfigKey<Boolean> key,
                      final @Nullable ConfigFile file) {
        this.system = system;
        this.mods = List.copyOf(mods);
        this.key = key;
        this.file = file;
    }

    private StepAside(final String system, final List<String> mods, final ConfigKey<Boolean> key) {
        this(system, mods, key, null);
    }

    /**
     * A system of any part of the series stepping aside for {@code mods}, as its switch {@code key} in {@code file}
     * says: a world's system, such as one that changes the world's recipes, has its switch in a world's file, read
     * as the world's data loads.
     */
    public static StepAside of(final String system, final List<String> mods, final ConfigFile file,
                               final ConfigKey<Boolean> key) {
        return new StepAside(Objects.requireNonNull(system, "system"), mods, Objects.requireNonNull(key, "key"),
                Objects.requireNonNull(file, "file"));
    }

    /** The mods that do this system's job, by id. */
    public List<String> mods() {
        return this.mods;
    }

    /** The setting that lets it step aside. */
    public ConfigKey<Boolean> key() {
        return this.key;
    }

    /** The first of those mods that is installed, by id, whether or not the setting lets the system step aside. */
    public Optional<String> installed() {
        final ModList loaded = ModList.get();
        return loaded == null ? Optional.empty() : this.mods.stream().filter(loaded::isLoaded).findFirst();
    }

    /** Whether the system steps aside: one of its mods is installed and the setting lets it; logged the first time. */
    public boolean steps() {
        final Optional<String> mod = installed();
        final boolean allowed = this.file == null ? this.stepsAside : ConfigFiles.forData(this.file).get(this.key);
        if (mod.isEmpty() || !allowed) {
            return false;
        }
        if (this.told.compareAndSet(false, true)) {
            LOGGER.info("The Core's system {} steps aside for {}; the setting {} keeps it on as well", this.system,
                    mod.get(), this.key.dottedPath());
        }
        return true;
    }
}
