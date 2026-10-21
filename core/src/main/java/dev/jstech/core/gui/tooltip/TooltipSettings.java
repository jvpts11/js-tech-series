/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.tooltip;

import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;

/**
 * What the Core adds to every item's tooltip on this player's game, kept in {@code jstech-tooltips.toml}: the name of
 * the mod an item comes from, last, and a bar of how worn a worn item is.
 */
public final class TooltipSettings {

    private static final String SECTION = "tooltips";

    /** Whether a tooltip ends with the name of the item's mod. */
    public static final ConfigKey<Boolean> MOD_NAME = ConfigKey.flag(SECTION + ".mod_name", true)
            .comment("Whether every item's tooltip ends with the name of the mod it comes from, in blue, unless "
                    + "another mod already names it.")
            .named("Name the item's mod");
    /** Whether a worn item shows a bar of its wear. */
    public static final ConfigKey<Boolean> DURABILITY = ConfigKey.flag(SECTION + ".durability", true)
            .comment("Whether a worn item's tooltip shows a bar of how much it has left, in the colour of its bar on "
                    + "the icon.")
            .named("Show how worn an item is");
    /** The player's file. */
    public static final ConfigFile FILE = ConfigFile.builder("jstech-tooltips", ConfigSide.CLIENT, ConfigFormats.TOML)
            .comment("What the series adds to items' tooltips on this game.")
            .sectionComment(SECTION, "The name of the item's mod, and the bar of how worn it is.")
            .sectionNamed(SECTION, "Tooltips")
            .key(MOD_NAME, value -> modName = value)
            .key(DURABILITY, value -> durability = value)
            .build();

    private static volatile boolean modName = true;
    private static volatile boolean durability = true;

    private TooltipSettings() {
    }

    /** Whether a tooltip ends with the name of the item's mod. */
    public static boolean modName() {
        return modName;
    }

    /** Whether a worn item shows a bar of its wear. */
    public static boolean durability() {
        return durability;
    }
}
