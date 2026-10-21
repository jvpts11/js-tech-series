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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.ApiStatus;
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
    /** The first of its mods installed, looked up once, since the mods do not change while the game runs. */
    private volatile @Nullable Optional<String> found;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SECTION = "step_aside";
    /** Every system that steps aside, in the order they were made, for the log to tell of. */
    private static final List<StepAside> ALL = new CopyOnWriteArrayList<>();
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
    /** Whether the echoes of places and the muffling through walls are left to a mod of sound physics. */
    public static final ConfigKey<Boolean> SOUND_PHYSICS_KEY = ConfigKey.flag(SECTION + ".sound_physics", true)
            .comment("Whether the Core leaves the echo of the place the listener stands in, and the muffling of "
                    + "sounds through walls, to Sound Physics Remastered or Dynamic Surroundings when one is "
                    + "installed. What the medium does (no sound in a vacuum, dull under water), the speed of sound "
                    + "and the Doppler effect stay the Core's.")
            .named("Leave echoes and walls to a mod of sound physics");
    /** The echoes of places and the muffling through walls, which Sound Physics Remastered gives too. */
    public static final StepAside SOUND_PHYSICS = new StepAside("sound_physics",
            List.of("sound_physics_remastered", "dsurround"), SOUND_PHYSICS_KEY);
    /** Whether the soundscapes of places are left to a mod of ambience. */
    public static final ConfigKey<Boolean> AMBIENCE_KEY = ConfigKey.flag(SECTION + ".ambience", true)
            .comment("Whether the Core leaves the soundscapes of places (a forest, a cave, the wind on a height) to "
                    + "Dynamic Surroundings or AmbientSounds when one is installed. The hum of machines stays.")
            .named("Leave ambience to another mod");
    /** The soundscapes of places, which Dynamic Surroundings and AmbientSounds give too. */
    public static final StepAside AMBIENCE = new StepAside("ambience", List.of("dsurround", "ambientsounds"),
            AMBIENCE_KEY);
    /** Whether footsteps by the material walked on are left to Presence Footsteps. */
    public static final ConfigKey<Boolean> FOOTSTEPS_KEY = ConfigKey.flag(SECTION + ".footsteps", true)
            .comment("Whether the Core leaves footsteps by the material walked on to Presence Footsteps when it is "
                    + "installed. Blows, breaking and placing by material stay the Core's.")
            .named("Leave footsteps to Presence Footsteps");
    /** Footsteps by the material walked on, which Presence Footsteps gives too. */
    public static final StepAside FOOTSTEPS = new StepAside("footsteps", List.of("presencefootsteps"),
            FOOTSTEPS_KEY);
    /** The player's file. */
    public static final ConfigFile FILE = ConfigFile.builder("jstech-step-aside", ConfigSide.CLIENT,
                    ConfigFormats.TOML)
            .comment("Which of the Core's systems step aside on this game for an installed mod that does their job.")
            .sectionComment(SECTION, "Each system, and whether it steps aside while such a mod is installed.")
            .sectionNamed(SECTION, "Stepping aside")
            .key(CONNECTED_TEXTURES_KEY, value -> CONNECTED_TEXTURES.stepsAside = value)
            .key(DYNAMIC_LIGHT_KEY, value -> DYNAMIC_LIGHT.stepsAside = value)
            .key(COLOURED_LIGHT_KEY, value -> COLOURED_LIGHT.stepsAside = value)
            .key(SOUND_PHYSICS_KEY, value -> SOUND_PHYSICS.stepsAside = value)
            .key(AMBIENCE_KEY, value -> AMBIENCE.stepsAside = value)
            .key(FOOTSTEPS_KEY, value -> FOOTSTEPS.stepsAside = value)
            .build();

    private StepAside(final String system, final List<String> mods, final ConfigKey<Boolean> key,
                      final @Nullable ConfigFile file) {
        this.system = system;
        this.mods = List.copyOf(mods);
        this.key = key;
        this.file = file;
        ALL.add(this);
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

    /** Every system that steps aside, the Core's own and those of the series' mods, in the order they were made. */
    public static List<StepAside> all() {
        return List.copyOf(ALL);
    }

    /**
     * Tells the log, once the game has loaded, what each system whose mod is installed does: steps aside for it, or
     * stays on beside it as its setting says; a world's system as each world's settings say.
     */
    @ApiStatus.Internal
    public static void report() {
        for (final StepAside aside : ALL) {
            final Optional<String> mod = aside.installed();
            if (mod.isEmpty()) {
                continue;
            }
            if (aside.file != null) {
                LOGGER.info("The Core's system {} steps aside for {} unless a world's setting {} keeps it on",
                        aside.system, mod.get(), aside.key.dottedPath());
            } else if (aside.stepsAside) {
                LOGGER.info("The Core's system {} steps aside for {}; the setting {} keeps it on as well",
                        aside.system, mod.get(), aside.key.dottedPath());
            } else {
                LOGGER.info("The Core's system {} stays on beside {}, as the setting {} says", aside.system,
                        mod.get(), aside.key.dottedPath());
            }
        }
    }

    /** The system's name in the log, as {@code dynamic_light}. */
    public String system() {
        return this.system;
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
        final Optional<String> known = this.found;
        if (known != null) {
            return known;
        }
        final ModList loaded = ModList.get();
        if (loaded == null) {
            return Optional.empty();
        }
        final Optional<String> first = this.mods.stream().filter(loaded::isLoaded).findFirst();
        this.found = first;
        return first;
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
