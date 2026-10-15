/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JavaOps;
import dev.jstech.core.config.format.PlainValues;
import dev.jstech.core.util.DeclarationError;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One setting: where it sits in its file, how its value is written, what it is when nobody set it, what it is held to,
 * and the comment a person reads above it.
 *
 * <p>The value is written through a {@link Codec}, so a setting can hold anything that has one: a number or a word,
 * and as well a list, a map, an id or a record of its own. A setting is held to at most one thing: a number to a
 * range, into which a value outside it is pulled to the nearer end; or a word to a list, outside which a value falls
 * back to the default. Either way a slip in a file costs one setting its value, not the world its settings.
 *
 * <p>Declared once and kept as a constant: {@code ConfigKey.whole("media.download_kilobytes_per_second", 256)
 * .range(16, 65_536).comment("...")}.
 *
 * @param path         where it sits: its sections, then its own name
 * @param codec        how its value is written in a file and read back
 * @param defaultValue what it is when nobody set it, or when what was set cannot be read
 * @param range        the range a number is pulled into, when it has one
 * @param allowed      the only words a text may be, when it is held to a list
 * @param comment      what a person reads above it in the file, a line each
 * @param title        what it is called in English where it is shown (a settings screen), or empty when it is shown
 *                     nowhere but its file
 * @param unit         what a number of it counts, in English as a settings screen writes it after the number
 *                     ({@code "KB/s"}, {@code "ticks"}), or empty when it counts nothing nameable
 * @param chooser      the chooser a settings screen opens to pick its value ({@code "jscore:style"}), registered on the
 *                     client, or none when the screen's own control for its kind of value is enough
 */
public record ConfigKey<T>(List<String> path, Codec<T> codec, T defaultValue, Optional<ConfigKeyRange<?>> range,
                           Optional<List<String>> allowed, List<String> comment, String title, String unit,
                           Optional<String> chooser) {

    public ConfigKey {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(codec, "codec");
        Objects.requireNonNull(defaultValue, "defaultValue");
        Objects.requireNonNull(range, "range");
        Objects.requireNonNull(allowed, "allowed");
        Objects.requireNonNull(comment, "comment");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(chooser, "chooser");
        if (path.isEmpty()) {
            throw DeclarationError.of(null, "a setting", "has no name",
                    "give it its sections and its name as a dotted path, as in \"media.download_speed\"");
        }
        for (final String segment : path) {
            if (segment == null || segment.isBlank() || segment.contains(".")) {
                throw DeclarationError.of(null, "the setting " + path, "has a part that is empty or holds a dot",
                        "write each section and the name as words of their own, without dots inside them");
            }
        }
        path = List.copyOf(path);
        comment = List.copyOf(comment);
        if (range.isPresent() && allowed.isPresent()) {
            throw DeclarationError.of(null, "the setting " + String.join(".", path),
                    "is held to a range and to a list of words at once",
                    "keep range(...) for a number or allowing(...) for a word, not both");
        }
        if (range.isPresent()) {
            checkRange(path, defaultValue, range.get());
        }
        if (allowed.isPresent()) {
            checkAllowed(path, defaultValue, allowed.get());
            allowed = Optional.of(List.copyOf(allowed.get()));
        }
    }

    /** A setting that is on or off. */
    public static ConfigKey<Boolean> flag(final String path, final boolean defaultValue) {
        return of(path, Codec.BOOL, defaultValue);
    }

    /** A whole number. */
    public static ConfigKey<Integer> whole(final String path, final int defaultValue) {
        return of(path, Codec.INT, defaultValue);
    }

    /** A whole number too large for an int, such as a count of bytes. */
    public static ConfigKey<Long> wholeLong(final String path, final long defaultValue) {
        return of(path, Codec.LONG, defaultValue);
    }

    /** A number with a fraction. */
    public static ConfigKey<Double> number(final String path, final double defaultValue) {
        return of(path, Codec.DOUBLE, defaultValue);
    }

    /** A piece of text. */
    public static ConfigKey<String> text(final String path, final String defaultValue) {
        return of(path, Codec.STRING, defaultValue);
    }

    /** A setting of any kind that has a codec, at a dotted path ({@code "section.name"}). */
    public static <T> ConfigKey<T> of(final String path, final Codec<T> codec, final T defaultValue) {
        return new ConfigKey<>(ConfigTree.path(path), codec, defaultValue, Optional.empty(), Optional.empty(),
                List.of(), "", "", Optional.empty());
    }

    /** The same setting, a number held to {@code min} and {@code max}, both included. */
    public <N extends Number & Comparable<N>> ConfigKey<T> range(final N min, final N max) {
        return new ConfigKey<>(this.path, this.codec, this.defaultValue, Optional.of(new ConfigKeyRange<>(min, max)),
                Optional.empty(), this.comment, this.title, this.unit, this.chooser);
    }

    /** The same setting, a text held to these words; any other falls back to the default. */
    public ConfigKey<T> allowing(final String... words) {
        return new ConfigKey<>(this.path, this.codec, this.defaultValue, Optional.empty(),
                Optional.of(List.of(words)), this.comment, this.title, this.unit, this.chooser);
    }

    /** The same setting, with these lines above it in the file; on a settings screen, they are its tooltip. */
    public ConfigKey<T> comment(final String... lines) {
        return new ConfigKey<>(this.path, this.codec, this.defaultValue, this.range, this.allowed,
                Arrays.asList(lines), this.title, this.unit, this.chooser);
    }

    /**
     * The same setting, picked on a settings screen with the chooser {@code id} registered on the client: a screen of
     * its own that shows what each value means (a style with its preview, an item with its picture) and hands the
     * choice back.
     */
    public ConfigKey<T> choosing(final String id) {
        return new ConfigKey<>(this.path, this.codec, this.defaultValue, this.range, this.allowed, this.comment,
                this.title, this.unit, Optional.of(id));
    }

    /**
     * The same setting, called this in English where it is shown: a settings screen lists it by this name, translated
     * as every other name of the mod is.
     */
    public ConfigKey<T> named(final String english) {
        return new ConfigKey<>(this.path, this.codec, this.defaultValue, this.range, this.allowed, this.comment,
                english, this.unit, this.chooser);
    }

    /**
     * The same setting, a number of these: a settings screen writes the unit after the number and after its default
     * and bounds, translated as the setting's name is.
     */
    public ConfigKey<T> unit(final String english) {
        return new ConfigKey<>(this.path, this.codec, this.defaultValue, this.range, this.allowed, this.comment,
                this.title, english, this.chooser);
    }

    /** Where it sits, written with dots: {@code "boot.show_boot_menu"}. */
    public String dottedPath() {
        return String.join(".", this.path);
    }

    /** Its own name, the last part of its path. */
    public String name() {
        return this.path.get(this.path.size() - 1);
    }

    /** A value of it as a file holds it: plain maps, lists, text, numbers and booleans. */
    public Object plain(final T value) {
        return PlainValues.value(this.codec.encodeStart(JavaOps.INSTANCE, value)
                .getOrThrow(problem -> new IllegalArgumentException(dottedPath() + " cannot hold " + value + ": "
                        + problem)));
    }

    /** What a file holds for it, read as a value of it, or why it cannot be. */
    public DataResult<T> read(final Object plain) {
        return this.codec.parse(JavaOps.INSTANCE, PlainValues.shapedLike(plain, plain(this.defaultValue)));
    }

    /**
     * The lines a file writes above it: its own comment, then what it is held to and its default, so a person editing
     * the file by hand knows the bounds without looking them up.
     */
    public List<String> describedComment() {
        final List<String> out = new ArrayList<>(this.comment);
        this.range.ifPresent(bounds -> out.add("Range: " + bounds.min() + " to " + bounds.max()));
        this.allowed.ifPresent(words -> out.add("One of: " + String.join(", ", words)));
        out.add("Default: " + plain(this.defaultValue));
        return out;
    }

    private static void checkRange(final List<String> path, final Object defaultValue, final ConfigKeyRange<?> range) {
        /*
         * The bounds have to be the setting's own kind of number, or the value would be compared with a bound of
         * another class and throw where it should have been pulled into the range.
         */
        if (defaultValue.getClass() != range.min().getClass() || defaultValue.getClass() != range.max().getClass()) {
            throw DeclarationError.of(null, "the setting " + String.join(".", path), "is a "
                    + defaultValue.getClass().getSimpleName() + " held to a range of "
                    + range.min().getClass().getSimpleName(),
                    "write the range's ends in the setting's own kind of number, as in 16L for a whole one");
        }
        if (!inside(range, defaultValue)) {
            throw DeclarationError.of(null, "the setting " + String.join(".", path), "starts at " + defaultValue
                    + ", outside its range [" + range.min() + ", " + range.max() + "]",
                    "move the default into the range, or widen the range");
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean inside(final ConfigKeyRange range, final Object value) {
        return range.contains((Number & Comparable) value);
    }

    private static void checkAllowed(final List<String> path, final Object defaultValue, final List<String> words) {
        // A word that falls back to the default has to fall back to a word that is allowed itself.
        if (!(defaultValue instanceof String word)) {
            throw DeclarationError.of(null, "the setting " + String.join(".", path),
                    "is held to a list of words but is not a text setting",
                    "hold only a text setting to words; a number is held to a range instead");
        }
        if (words.isEmpty()) {
            throw DeclarationError.of(null, "the setting " + String.join(".", path), "is held to a list of no words",
                    "name the words it may be, the default among them");
        }
        if (!words.contains(word)) {
            throw DeclarationError.of(null, "the setting " + String.join(".", path), "starts at " + word
                    + ", which is not one of " + words, "make the default one of the words, or add it to them");
        }
    }
}
