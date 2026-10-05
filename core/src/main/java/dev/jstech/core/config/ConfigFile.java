/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import dev.jstech.core.config.format.ConfigFormatException;
import dev.jstech.core.config.format.IConfigFormat;
import dev.jstech.core.config.format.PlainValues;
import dev.jstech.core.persistence.UpgradeChain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.slf4j.LoggerFactory;

/**
 * A settings file: its name, whose it is, the format it is written in, the version of its layout, and the settings
 * it holds, each with what takes its value when the file is read.
 *
 * <p>Declared once, with a builder, and kept as a constant:
 *
 * <pre>{@code
 * ConfigFile FILE = ConfigFile.builder("jscomputers-server", ConfigSide.SERVER, ConfigFormats.TOML)
 *         .version(1)
 *         .comment("How the computers of J's Computers behave.")
 *         .key(SHOW_BOOT_MENU)
 *         .key(ETHERNET_SPEED, Songs::setEthernetSpeed)
 *         .build();
 * }</pre>
 *
 * <p>Every file keeps the version of its layout at the top, under {@value #VERSION_KEY}. A file from an older version
 * takes the upgrade steps from its version on before it is read; one from a newer version of the mod is read as far as
 * it can be and never written over, so going back to an older mod does not destroy what a newer one wrote. What a
 * file lacks takes its default, what nothing declares is dropped, and a file that needed any of that is written back
 * whole, with the comments of today.
 *
 * <p>This is the file as the mod sees it, with nothing of the game in it; where it lives and when it is read is
 * {@link ConfigFiles}'s business.
 */
public final class ConfigFile {

    private final String name;
    private final ConfigSide side;
    private final IConfigFormat format;
    private final int version;
    private final List<String> comment;
    private final Map<String, List<String>> sections;
    private final Map<String, String> sectionTitles;
    private final Map<String, ConfigKey<?>> keys;
    private final Map<String, Consumer<Object>> uses;
    private final UpgradeChain<Map<String, Object>> upgrades;
    private final ConfigValidator validator;
    private final IConfigLogger logger;
    /** Each setting's value as read, by its dotted path; a setting not read yet is at its default. */
    private final Map<String, Object> values = new ConcurrentHashMap<>();
    private volatile Runnable saver = () -> { };
    private volatile boolean newer;

    /** The name every file keeps the version of its layout under, first in the file. */
    public static final String VERSION_KEY = "config_version";
    private static final List<String> VERSION_COMMENT = List.of(ConfigTexts.VERSION_TOOLTIP.english());

    private ConfigFile(final Builder builder) {
        this.name = builder.name;
        this.side = builder.side;
        this.format = builder.format;
        this.upgrades = builder.upgrades.build();
        this.version = this.upgrades.version();
        this.comment = List.copyOf(builder.comment);
        this.sections = Collections.unmodifiableMap(new LinkedHashMap<>(builder.sections));
        this.sectionTitles = Collections.unmodifiableMap(new LinkedHashMap<>(builder.sectionTitles));
        this.keys = Collections.unmodifiableMap(new LinkedHashMap<>(builder.keys));
        this.uses = Map.copyOf(builder.uses);
        this.logger = builder.logger;
        this.validator = new ConfigValidator(builder.logger);
    }

    /** Starts declaring a file called {@code name} (without its extension), with whose it is and its format. */
    public static Builder builder(final String name, final ConfigSide side, final IConfigFormat format) {
        return new Builder(name, side, format);
    }

    public String name() {
        return this.name;
    }

    public ConfigSide side() {
        return this.side;
    }

    public IConfigFormat format() {
        return this.format;
    }

    public int version() {
        return this.version;
    }

    /** The file's name with its format's extension: {@code jscomputers-server.toml}. */
    public String fileName() {
        return this.name + "." + this.format.extension();
    }

    /** The lines at the top of the file. */
    public List<String> comment() {
        return this.comment;
    }

    /** The sections that have a comment of their own, by dotted path, in the order they were declared. */
    public Map<String, List<String>> sections() {
        return this.sections;
    }

    /** What each named section is called in English, by dotted path. */
    public Map<String, String> sectionTitles() {
        return this.sectionTitles;
    }

    /** The settings, in the order they were declared and are written. */
    public Collection<ConfigKey<?>> keys() {
        return this.keys.values();
    }

    /** Whether the last file read was written by a newer version of the mod, and so is never written over. */
    public boolean newer() {
        return this.newer;
    }

    /** The value a setting of this file has now: as last read, or its default before anything was. */
    @SuppressWarnings("unchecked")
    public <T> T get(final ConfigKey<T> key) {
        own(key);
        return (T) this.values.getOrDefault(key.dottedPath(), key.defaultValue());
    }

    /**
     * Sets a setting from inside the game (a player moving a slider, say): the value is held to what the setting is
     * held to, passed on to what takes it, and the file written, unless it came from a newer version of the mod.
     */
    public <T> void set(final ConfigKey<T> key, final T value) {
        stage(key, value);
        save();
    }

    /**
     * Sets a setting as {@link #set} does but leaves the file as it is, so several settings changed together are
     * written once, by {@link #save}.
     */
    public <T> void stage(final ConfigKey<T> key, final T value) {
        own(key);
        store(key, this.validator.validate(key, key.plain(value)).value());
    }

    /** What {@code value} becomes held to what {@code key} is held to, as setting it would make it. */
    public <T> T validated(final ConfigKey<T> key, final T value) {
        own(key);
        return this.validator.validate(key, key.plain(value)).value();
    }

    /** Writes the file as its settings are now, unless it came from a newer version of the mod. */
    public void save() {
        if (!this.newer) {
            this.saver.run();
        }
    }

    /** Every setting back to its default, passed on to what takes it: a world closing, for a world's settings. */
    public void reset() {
        this.values.clear();
        this.newer = false;
        for (final ConfigKey<?> key : this.keys.values()) {
            use(key, key.defaultValue());
        }
    }

    /** Reads a whole file of this format; see {@link #read(Map)}. */
    public ReadOutcome read(final byte[] file) throws ConfigFormatException {
        return read(this.format.read(file));
    }

    /**
     * Reads a file's plain values: upgrades them when the file is older, then takes each setting's value from them,
     * held to what it is held to, and passes it on. Says what the reading found, so the caller knows whether the file
     * should be written back.
     */
    public ReadOutcome read(final Map<String, Object> file) {
        final Map<String, Object> plain = PlainValues.map(file);
        final int found = versionOf(plain);
        final boolean fromNewer = this.upgrades.isNewer(found);
        final boolean upgraded = found < this.version;
        if (fromNewer) {
            this.logger.warn(fileName() + " was written by a newer version of its mod (layout " + found + ", this one "
                    + "knows " + this.version + "); it is read as far as it can be and not written over");
        }
        this.upgrades.upgrade(plain, found);
        boolean corrected = false;
        for (final ConfigKey<?> key : this.keys.values()) {
            corrected |= take(key, ConfigTree.get(plain, key.path()));
        }
        corrected |= hasUnknown(plain, List.of());
        this.newer = fromNewer;
        return new ReadOutcome(found, upgraded, corrected, fromNewer);
    }

    /** The values as the file holds them: the version first, then each setting at its place, in declared order. */
    public Map<String, Object> toPlain() {
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put(VERSION_KEY, this.version);
        for (final ConfigKey<?> key : this.keys.values()) {
            ConfigTree.put(out, key.path(), plainOf(key));
        }
        return out;
    }

    /** The whole file as its format writes it, with every comment. */
    public byte[] write() {
        return this.format.write(toPlain(), this::commentAt);
    }

    /** The comment above a place in the file: the top, the version, a section or a setting. */
    public List<String> commentAt(final List<String> path) {
        if (path.isEmpty()) {
            return this.comment;
        }
        if (path.size() == 1 && path.get(0).equals(VERSION_KEY)) {
            return VERSION_COMMENT;
        }
        final String dotted = String.join(".", path);
        final ConfigKey<?> key = this.keys.get(dotted);
        if (key != null) {
            return key.describedComment();
        }
        return this.sections.getOrDefault(dotted, List.of());
    }

    /** What writing the file takes, set by whatever keeps it on disk; it runs after a setting changes in the game. */
    public void onSave(final Runnable save) {
        this.saver = Objects.requireNonNull(save, "save");
    }

    private void own(final ConfigKey<?> key) {
        if (this.keys.get(key.dottedPath()) != key) {
            throw new IllegalArgumentException(key.dottedPath() + " is not a setting of " + fileName());
        }
    }

    /** Takes a setting's value from what the file holds; true when the file needs writing back for it. */
    private <T> boolean take(final ConfigKey<T> key, final Object raw) {
        if (raw == null) {
            // A setting the file does not have yet, such as one a newer version added: its default, quietly.
            store(key, key.defaultValue());
            return true;
        }
        final IConfigValidationResult<T> result = this.validator.validate(key, raw);
        store(key, result.value());
        return !(result instanceof IConfigValidationResult.Valid<T>);
    }

    private <T> void store(final ConfigKey<T> key, final T value) {
        this.values.put(key.dottedPath(), value);
        use(key, value);
    }

    private void use(final ConfigKey<?> key, final Object value) {
        final Consumer<Object> use = this.uses.get(key.dottedPath());
        if (use != null) {
            use.accept(value);
        }
    }

    private <T> Object plainOf(final ConfigKey<T> key) {
        return key.plain(get(key));
    }

    /** Whether the file has a value nothing declares, under the section at {@code at}. */
    private boolean hasUnknown(final Map<?, ?> section, final List<String> at) {
        for (final Map.Entry<?, ?> entry : section.entrySet()) {
            final List<String> path = new ArrayList<>(at);
            path.add(String.valueOf(entry.getKey()));
            final String dotted = String.join(".", path);
            if (at.isEmpty() && dotted.equals(VERSION_KEY) || this.keys.containsKey(dotted)) {
                continue;
            }
            final boolean isSection = this.keys.keySet().stream().anyMatch(key -> key.startsWith(dotted + "."));
            if (!isSection || !(entry.getValue() instanceof Map<?, ?> inner) || hasUnknown(inner, path)) {
                return true;
            }
        }
        return false;
    }

    private static int versionOf(final Map<String, Object> plain) {
        // A file written before files had versions has none: it is the version before the first.
        return plain.get(VERSION_KEY) instanceof Number number ? number.intValue() : 0;
    }

    /**
     * What reading a file found.
     *
     * @param foundVersion the version of the layout the file was written in; 0 for a file from before versions
     * @param upgraded     whether it was older and took upgrade steps
     * @param corrected    whether a value was missing, could not be read, broke its bounds, or was unknown
     * @param newer        whether a newer version of the mod wrote it
     */
    public record ReadOutcome(int foundVersion, boolean upgraded, boolean corrected, boolean newer) {

        /** Whether the file should be written back as it now reads: never one from a newer mod. */
        public boolean rewrite() {
            return !this.newer && (this.upgraded || this.corrected);
        }
    }

    /** Declares a settings file, a part at a time; {@link #build()} checks the parts hold together. */
    public static final class Builder {

        private final String name;
        private final ConfigSide side;
        private final IConfigFormat format;
        private final Map<String, List<String>> sections = new LinkedHashMap<>();
        private final Map<String, String> sectionTitles = new LinkedHashMap<>();
        private final Map<String, ConfigKey<?>> keys = new LinkedHashMap<>();
        private final Map<String, Consumer<Object>> uses = new LinkedHashMap<>();
        private final UpgradeChain.Builder<Map<String, Object>> upgrades;
        private List<String> comment = List.of();
        private IConfigLogger logger = LoggerFactory.getLogger(ConfigFile.class)::warn;

        private Builder(final String name, final ConfigSide side, final IConfigFormat format) {
            if (name == null || name.isBlank() || name.contains("/") || name.contains("\\") || name.contains(".")) {
                throw new IllegalArgumentException("a settings file's name is one word, without its extension: "
                        + name);
            }
            this.name = name;
            this.side = Objects.requireNonNull(side, "side");
            this.format = Objects.requireNonNull(format, "format");
            this.upgrades = UpgradeChain.builder(name);
        }

        /** The version of the file's layout, counted from 1; raise it when a step is needed to read older files. */
        public Builder version(final int layout) {
            this.upgrades.version(layout);
            return this;
        }

        /** The lines at the top of the file. */
        public Builder comment(final String... lines) {
            this.comment = Arrays.asList(lines);
            return this;
        }

        /** A comment above a section, at its dotted path; on a settings screen, the section's tooltip. */
        public Builder sectionComment(final String path, final String... lines) {
            this.sections.put(path, Arrays.asList(lines));
            return this;
        }

        /** What a section is called in English where it is shown, a settings screen listing it by this name. */
        public Builder sectionNamed(final String path, final String english) {
            this.sectionTitles.put(path, english);
            return this;
        }

        /** A setting, read for whoever asks the file for it. */
        public <T> Builder key(final ConfigKey<T> key) {
            if (this.keys.putIfAbsent(key.dottedPath(), key) != null) {
                throw new IllegalArgumentException("two settings at " + key.dottedPath() + " in " + this.name);
            }
            return this;
        }

        /** A setting, and what takes its value each time the file is read and each time it is set. */
        @SuppressWarnings("unchecked")
        public <T> Builder key(final ConfigKey<T> key, final Consumer<? super T> use) {
            key(key);
            this.uses.put(key.dottedPath(), value -> use.accept((T) value));
            return this;
        }

        /** The step that takes a file of layout {@code from} to layout {@code from + 1}. */
        public Builder upgrade(final int from, final IConfigUpgrade step) {
            Objects.requireNonNull(step, "step");
            this.upgrades.step(from, values -> {
                step.upgrade(values);
                return values;
            });
            return this;
        }

        /** Where the file says what it did not take as written; the log, unless told otherwise. */
        public Builder logger(final IConfigLogger log) {
            this.logger = Objects.requireNonNull(log, "log");
            return this;
        }

        public ConfigFile build() {
            for (final String path : this.keys.keySet()) {
                if (path.equals(VERSION_KEY)) {
                    throw new IllegalArgumentException(VERSION_KEY + " is the file's own, not a setting's");
                }
                for (final String other : this.keys.keySet()) {
                    if (other.startsWith(path + ".")) {
                        throw new IllegalArgumentException(path + " is a setting and a section at once in "
                                + this.name);
                    }
                }
            }
            return new ConfigFile(this);
        }
    }
}
