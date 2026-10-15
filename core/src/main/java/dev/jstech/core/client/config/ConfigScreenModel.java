/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.config;

import dev.jstech.core.config.ConfigDraft;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigScreenTexts;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.ConfigSync;
import dev.jstech.core.config.ConfigTexts;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/**
 * What the settings screen of one mod shows, without how: a tab for each kind of file the mod keeps, each tab's
 * sections (the first part of a setting's path), each section's groups (the second part) that fold away, and each
 * group's settings, with the words the mod's language gives them and a draft of the changes for each file. It says,
 * too, how each file may be changed here: in place, sent to the server it lives on, or not at all.
 */
final class ConfigScreenModel {

    private final String modId;
    private final List<Scope> scopes = new ArrayList<>();
    private final Map<ConfigFile, ConfigDraft> drafts = new LinkedHashMap<>();

    private static final String NEW_LINE = "\n";

    ConfigScreenModel(final String modId) {
        this.modId = modId;
        final Map<ConfigSide, List<Section>> bySide = new LinkedHashMap<>();
        for (final ConfigSide side : List.of(ConfigSide.SERVER, ConfigSide.CLIENT, ConfigSide.COMMON)) {
            bySide.put(side, new ArrayList<>());
        }
        for (final ConfigFile file : ConfigFiles.of(modId)) {
            final List<Section> sections = sectionsOf(file);
            if (!sections.isEmpty()) {
                drafts.put(file, new ConfigDraft(file));
                bySide.get(file.side()).addAll(sections);
            }
        }
        bySide.forEach((side, sections) -> {
            if (!sections.isEmpty()) {
                scopes.add(new Scope(side, List.copyOf(sections)));
            }
        });
    }

    /** How a file may be changed on this screen, here and now. */
    enum Access {
        /** In place: the file lives on this game. */
        LOCAL,
        /** Sent to the server the player is on, which keeps it; the player is an operator. */
        REMOTE,
        /** Shown only: a server's file for a player who is not an operator, or a world's outside any world. */
        READ_ONLY
    }

    List<Scope> scopes() {
        return scopes;
    }

    ConfigDraft draft(final ConfigFile file) {
        return drafts.get(file);
    }

    Map<ConfigFile, ConfigDraft> drafts() {
        return drafts;
    }

    /** How {@code file} may be changed here. */
    static Access access(final ConfigFile file) {
        if (file.side() != ConfigSide.SERVER) {
            return Access.LOCAL;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.hasSingleplayerServer()) {
            return Access.LOCAL;
        }
        if (minecraft.player != null && ConfigSync.fromServer(file)) {
            return minecraft.player.hasPermissions(ConfigSync.EDIT_LEVEL) ? Access.REMOTE : Access.READ_ONLY;
        }
        return Access.READ_ONLY;
    }

    /** Whether {@code file} can be changed here, in place or sent to the server. */
    static boolean editable(final ConfigFile file) {
        return access(file) != Access.READ_ONLY;
    }

    /** What the tab of {@code side} is called. */
    static Component tabTitle(final ConfigSide side) {
        return GameText.component(switch (side) {
            case SERVER -> ConfigScreenTexts.WORLD;
            case CLIENT -> ConfigScreenTexts.PLAYER;
            case COMMON -> ConfigScreenTexts.EVERY_GAME;
        });
    }

    /** What a setting is called, as the mod's language says it. */
    Component name(final ConfigKey<?> key) {
        return Component.translatable(ConfigTexts.key(modId, key.dottedPath()));
    }

    /** What a setting does, all of it, as the mod's language says it; empty when it says nothing. */
    String description(final ConfigKey<?> key) {
        final String translation = ConfigTexts.key(modId, key.dottedPath()) + ConfigTexts.TOOLTIP;
        return I18n.exists(translation) ? I18n.get(translation) : "";
    }

    /** What a number of a setting counts, as the mod's language says it; empty when it counts nothing nameable. */
    String unit(final ConfigKey<?> key) {
        final String translation = ConfigTexts.key(modId, key.dottedPath()) + ConfigTexts.UNIT;
        return I18n.exists(translation) ? I18n.get(translation) : "";
    }

    /** A setting's default, and a number's bounds, each with its unit: the line under its description. */
    String meta(final ConfigKey<?> key) {
        final String unit = unit(key);
        final String after = unit.isEmpty() ? "" : " " + unit;
        final Object fallback = key.defaultValue();
        if (fallback instanceof Boolean on) {
            return GameText.resolve(ConfigScreenTexts.DEFAULT_IS.with(GameText.resolve(on ? ConfigScreenTexts.ON
                    : ConfigScreenTexts.OFF)));
        }
        if (key.range().isPresent()) {
            return GameText.resolve(ConfigScreenTexts.DEFAULT_IN_RANGE.with(ConfigRows.shown(fallback) + after,
                    ConfigRows.shown(key.range().get().min()), ConfigRows.shown(key.range().get().max())));
        }
        return GameText.resolve(ConfigScreenTexts.DEFAULT_IS.with(ConfigRows.shown(plainDefault(key)) + after));
    }

    /** One of a word setting's words as the mod's language says it, or the word itself. */
    String word(final ConfigKey<?> key, final String word) {
        final String translation = ConfigTexts.key(modId, key.dottedPath()) + "." + word.toLowerCase(Locale.ROOT);
        return I18n.exists(translation) ? I18n.get(translation) : word;
    }

    /** Whether a search's words are in a setting's name or description. */
    boolean matches(final ConfigKey<?> key, final String query) {
        if (query.isEmpty()) {
            return true;
        }
        final String said = (name(key).getString() + NEW_LINE + description(key)).toLowerCase(Locale.ROOT);
        for (final String word : query.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (!said.contains(word)) {
                return false;
            }
        }
        return true;
    }

    /* A file's named settings, by section, then by group, in the order the file declares them. */
    private List<Section> sectionsOf(final ConfigFile file) {
        final Map<String, Map<String, List<ConfigKey<?>>>> bySection = new LinkedHashMap<>();
        for (final ConfigKey<?> key : file.keys()) {
            if (key.title().isBlank()) {
                continue;
            }
            final List<String> path = key.path();
            final String section = path.size() >= 2 ? path.get(0) : "";
            final String group = path.size() >= 3 ? path.get(0) + "." + path.get(1) : "";
            bySection.computeIfAbsent(section, ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(group, ignored -> new ArrayList<>()).add(key);
        }
        final List<Section> out = new ArrayList<>();
        bySection.forEach((section, groups) -> {
            final List<Group> made = new ArrayList<>();
            // The settings of a section in no group come first, then the groups in the order the file has them.
            final List<ConfigKey<?>> loose = groups.remove("");
            if (loose != null) {
                made.add(new Group("", Component.empty(), List.copyOf(loose)));
            }
            groups.forEach((group, keys) -> made.add(new Group(group, title(group), List.copyOf(keys))));
            out.add(new Section(file, section, section.isEmpty() ? GameText.component(ConfigScreenTexts.GENERAL)
                    : title(section), List.copyOf(made)));
        });
        return out;
    }

    private static <T> Object plainDefault(final ConfigKey<T> key) {
        return key.plain(key.defaultValue());
    }

    /* A section's or a group's name, as the mod's language says it, or its last word. */
    private Component title(final String path) {
        final String key = ConfigTexts.key(modId, path);
        return I18n.exists(key) ? Component.translatable(key)
                : Component.literal(path.substring(path.lastIndexOf('.') + 1));
    }

    /** The settings of one kind of file, the world's, the player's or every game's, by section. */
    record Scope(ConfigSide side, List<Section> sections) {

        Scope {
            sections = List.copyOf(sections);
        }
    }

    /**
     * One entry of the rail: a part of a file and its groups.
     *
     * @param file   the file
     * @param path   the section's name in the file; empty for the settings in none
     * @param title  what the rail calls it
     * @param groups its groups, the settings in none first
     */
    record Section(ConfigFile file, String path, Component title, List<Group> groups) {

        Section {
            groups = List.copyOf(groups);
        }

        /** How many settings it holds. */
        int count() {
            return groups.stream().mapToInt(group -> group.keys().size()).sum();
        }
    }

    /**
     * Settings that fold away together.
     *
     * @param path  the group's dotted path; empty for the section's settings in no group
     * @param title what its header calls it
     * @param keys  its settings, in the file's order
     */
    record Group(String path, Component title, List<ConfigKey<?>> keys) {

        Group {
            keys = List.copyOf(keys);
        }
    }
}
