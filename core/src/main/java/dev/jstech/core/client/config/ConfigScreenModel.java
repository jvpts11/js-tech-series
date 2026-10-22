/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.config;

import dev.jstech.core.JsCore;
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
import java.util.IdentityHashMap;
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
    /* The settings of other mods, kept NeoForge's way, the Core's screen shows beside its own. */
    private final Map<ConfigKey<?>, ForeignConfigs.Setting> foreign = new IdentityHashMap<>();

    private static final String NEW_LINE = "\n";
    /* The fold of a file's settings in no section of theirs, which the file's own name heads. */
    private static final String WHOLE_FILE = "#file";

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
        // Only the Core's own screen, the series' settings, shows the other mods'.
        final List<ForeignConfigs.Mod> mods = modId.equals(JsCore.MODID) ? ForeignConfigs.all(modId) : List.of();
        for (final ForeignConfigs.Mod mod : mods) {
            for (final ForeignConfigs.File file : mod.files()) {
                drafts.put(file.file(), new ConfigDraft(file.file()));
                foreign.putAll(file.settings());
            }
        }
        bySide.forEach((side, sections) -> {
            final List<Other> others = new ArrayList<>();
            for (final ForeignConfigs.Mod mod : mods) {
                final List<Section> here = new ArrayList<>();
                final List<Elsewhere> elsewhere = new ArrayList<>();
                for (final ForeignConfigs.File file : mod.files()) {
                    if (file.file().side() == side) {
                        here.add(foreignSectionOf(file));
                    } else {
                        elsewhere.add(new Elsewhere(file.name(), file.file().side()));
                    }
                }
                if (!here.isEmpty()) {
                    others.add(new Other(mod.modId(), mod.name(), List.copyOf(here), List.copyOf(elsewhere)));
                }
            }
            if (!sections.isEmpty() || !others.isEmpty()) {
                scopes.add(new Scope(side, List.copyOf(sections), List.copyOf(others)));
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
        if (ForeignConfigs.isForeign(file)) {
            // Another mod's world settings live on the server, which the Core does not write for it.
            return Access.READ_ONLY;
        }
        if (minecraft.player != null && ConfigSync.fromServer(file)) {
            return ConfigSync.mayEditOnServer(file) ? Access.REMOTE : Access.READ_ONLY;
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

    /** What a setting is called, as the mod's language says it, or as another mod names its own. */
    Component name(final ConfigKey<?> key) {
        final ForeignConfigs.Setting other = foreign.get(key);
        if (other != null) {
            return Component.literal(other.title());
        }
        return Component.translatable(ConfigTexts.key(modId, key.dottedPath()));
    }

    /** What a setting does, all of it, as the mod's language says it; empty when it says nothing. */
    String description(final ConfigKey<?> key) {
        final ForeignConfigs.Setting other = foreign.get(key);
        if (other != null) {
            return other.comment();
        }
        final String translation = ConfigTexts.key(modId, key.dottedPath()) + ConfigTexts.TOOLTIP;
        return I18n.exists(translation) ? I18n.get(translation) : "";
    }

    /** What a number of a setting counts, as the mod's language says it; empty when it counts nothing nameable. */
    String unit(final ConfigKey<?> key) {
        if (foreign.containsKey(key)) {
            return "";
        }
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
        if (foreign.containsKey(key)) {
            return ForeignConfigs.words(word.toLowerCase(Locale.ROOT));
        }
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

    /*
     * Another mod's file as one section: a fold for each of its sections, which says the file it is in, and the
     * settings in none of them under the file's own name.
     */
    private static Section foreignSectionOf(final ForeignConfigs.File file) {
        final Map<String, List<ConfigKey<?>>> byGroup = new LinkedHashMap<>();
        for (final ConfigKey<?> key : file.file().keys()) {
            byGroup.computeIfAbsent(key.path().size() >= 2 ? key.path().get(0) : WHOLE_FILE, ignored ->
                    new ArrayList<>()).add(key);
        }
        final String base = file.name().substring(file.name().lastIndexOf('/') + 1);
        final List<Group> groups = new ArrayList<>();
        byGroup.forEach((group, keys) -> groups.add(new Group(group, Component.literal(ForeignConfigs.words(
                group.equals(WHOLE_FILE) ? ForeignConfigs.stem(base) : group)), List.copyOf(keys), base)));
        return new Section(file.file(), "", Component.literal(base), List.copyOf(groups));
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

    /**
     * The settings of one kind of file, the world's, the player's or every game's, by section; and the other mods'
     * of that kind, by mod.
     */
    record Scope(ConfigSide side, List<Section> sections, List<Other> others) {

        Scope {
            sections = List.copyOf(sections);
            others = List.copyOf(others);
        }
    }

    /**
     * Another mod's settings of one kind of file, kept NeoForge's way: a section for each of its files of that kind,
     * and where its other files are.
     *
     * @param modId     its id
     * @param name      what it calls itself
     * @param sections  a section for each of its files of the kind
     * @param elsewhere its files of the other kinds, under the other tabs
     */
    record Other(String modId, String name, List<Section> sections, List<Elsewhere> elsewhere) {

        /** How many settings it holds under this tab. */
        int count() {
            return sections.stream().mapToInt(Section::count).sum();
        }
    }

    /**
     * Another mod's file under another tab.
     *
     * @param file its name, with its folder
     * @param side the kind of file it is, which names its tab
     */
    record Elsewhere(String file, ConfigSide side) {
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
     * @param file  the file its header names at its right, or empty for its count of settings there
     */
    record Group(String path, Component title, List<ConfigKey<?>> keys, String file) {

        Group {
            keys = List.copyOf(keys);
        }

        Group(final String path, final Component title, final List<ConfigKey<?>> keys) {
            this(path, title, keys, "");
        }
    }
}
