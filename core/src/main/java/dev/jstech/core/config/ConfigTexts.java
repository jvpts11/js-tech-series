/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import dev.jstech.core.config.format.ConfigFormats;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The words NeoForge's settings screen shows for a mod's settings files: each setting's and each section's name, and
 * its comment as the tooltip, under keys the mod's language file holds and translates like any other name. Only TOML
 * files are on that screen, so only they need them; every setting and section of one has to have a name, and a file
 * that leaves one out stops the language file from being written.
 */
@TextHolder
public final class ConfigTexts {

    /**
     * The version every file keeps of its layout, at {@link ConfigFile#VERSION_KEY}, shown on the screen under the
     * Core's one name. The keys are written out whole so the checks that read the sources find them.
     */
    public static final TextKey VERSION = TextKey.of("jscore.configuration.config_version", "Layout version");
    public static final TextKey VERSION_TOOLTIP = TextKey.of("jscore.configuration.config_version.tooltip",
            "The version of this file's layout. The game upgrades an older file to it; leave it as it is.");

    /** What the key of a setting's or a section's comment ends in, after its own key. */
    public static final String TOOLTIP = ".tooltip";
    /** What the key of a setting's unit ends in, after its own key. */
    public static final String UNIT = ".unit";

    private ConfigTexts() {
    }

    /** The key a setting or a section of a mod's file is shown under, at its dotted path. */
    public static String key(final String modId, final String path) {
        return modId + ".configuration." + path;
    }

    /** Every key a settings screen shows for {@code modId}'s files, with its English. */
    public static Map<String, String> english(final String modId) {
        final Map<String, String> out = new LinkedHashMap<>();
        for (final ConfigFile file : ConfigFiles.of(modId)) {
            if (file.format() != ConfigFormats.TOML) {
                continue;
            }
            for (final String section : sectionsOf(file)) {
                final String title = file.sectionTitles().get(section);
                if (title == null || title.isBlank()) {
                    throw new IllegalStateException("the section " + section + " of " + file.fileName()
                            + " is shown on the settings screen and has no name; give it one with sectionNamed");
                }
                out.put(key(modId, section), title);
                final List<String> comment = file.sections().getOrDefault(section, List.of());
                if (!comment.isEmpty()) {
                    out.put(key(modId, section) + TOOLTIP, String.join("\n", comment));
                }
            }
            for (final ConfigKey<?> setting : file.keys()) {
                if (setting.title().isBlank()) {
                    throw new IllegalStateException("the setting " + setting.dottedPath() + " of " + file.fileName()
                            + " is shown on the settings screen and has no name; give it one with named");
                }
                out.put(key(modId, setting.dottedPath()), setting.title());
                if (!setting.comment().isEmpty()) {
                    out.put(key(modId, setting.dottedPath()) + TOOLTIP, String.join("\n", setting.comment()));
                }
                if (!setting.unit().isBlank()) {
                    out.put(key(modId, setting.dottedPath()) + UNIT, setting.unit());
                }
            }
        }
        return out;
    }

    /** Every section a file's settings sit in, outermost first, each once. */
    public static List<String> sectionsOf(final ConfigFile file) {
        final List<String> out = new ArrayList<>();
        for (final ConfigKey<?> setting : file.keys()) {
            for (int depth = 1; depth < setting.path().size(); depth++) {
                final String section = String.join(".", setting.path().subList(0, depth));
                if (!out.contains(section)) {
                    out.add(section);
                }
            }
        }
        return out;
    }
}
