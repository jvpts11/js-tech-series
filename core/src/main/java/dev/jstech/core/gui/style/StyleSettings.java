/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.style;

import com.mojang.serialization.Codec;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;
import java.util.Map;
import java.util.Optional;

/**
 * The styles a player chose, kept in their own settings, {@code jstech-interface.toml}: one for everything, and one
 * for a given mod's screens where they want another. Nothing chosen leaves every screen in its mod's default, which is
 * the game's own style everywhere but on J's Computers' computing blocks, which wear their era's. The server imposes
 * nothing: these are the player's alone.
 */
public final class StyleSettings {

    /** What the chooser of styles is registered as. */
    public static final String CHOOSER = "jscore:style";

    /** The section the choices sit in. */
    private static final String STYLES = "styles";

    /** The style for every screen; empty leaves each screen in its mod's default. */
    public static final ConfigKey<String> EVERYTHING = ConfigKey.of(STYLES + ".everything", Codec.STRING, "")
            .comment("The style every screen of every mod is drawn in, as \"namespace:name\".",
                    "Empty leaves each screen in its own mod's default.")
            .named("Style for every screen").choosing(CHOOSER);

    /** A style for a given mod's screens, over the one for everything, by mod id. */
    public static final ConfigKey<Map<String, String>> BY_MOD = ConfigKey.of(STYLES + ".by_mod",
                    Codec.unboundedMap(Codec.STRING, Codec.STRING), Map.<String, String>of())
            .comment("A style for one mod's screens, over the one for every screen: mod id = \"namespace:name\".")
            .named("Style by mod").choosing(CHOOSER);

    /** The player's file. */
    public static final ConfigFile FILE = ConfigFile.builder("jstech-interface", ConfigSide.CLIENT, ConfigFormats.TOML)
            .comment("How the screens of the mods built on J's Core look on this game.")
            .sectionComment(STYLES, "The styles screens are drawn in.")
            .sectionNamed(STYLES, "Styles")
            .key(EVERYTHING, value -> everything = value)
            .key(BY_MOD, value -> byMod = Map.copyOf(value))
            .build();

    private static volatile String everything = "";
    private static volatile Map<String, String> byMod = Map.of();

    private StyleSettings() {
    }

    /**
     * The style the player chose for {@code modid}'s screens: theirs for that mod, else theirs for everything, else
     * none, which leaves the mod's default.
     */
    public static Optional<String> chosenFor(final String modid) {
        final String forMod = byMod.get(modid);
        if (forMod != null && !forMod.isBlank()) {
            return Optional.of(forMod);
        }
        return everything.isBlank() ? Optional.empty() : Optional.of(everything);
    }

    /** The style chosen for everything, empty when none is. */
    public static String everything() {
        return everything;
    }

    /** The styles chosen for given mods. */
    public static Map<String, String> byMod() {
        return byMod;
    }
}
