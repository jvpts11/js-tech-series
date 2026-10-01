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
 */
public record ConfigKey<T>(List<String> path, Codec<T> codec, T defaultValue, Optional<ConfigKeyRange<?>> range,
                           Optional<List<String>> allowed, List<String> comment, String title) {

    public ConfigKey {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(codec, "codec");
        Objects.requireNonNull(defaultValue, "defaultValue");
        Objects.requireNonNull(range, "range");
        Objects.requireNonNull(allowed, "allowed");
        Objects.requireNonNull(comment, "comment");
        Objects.requireNonNull(title, "title");
        if (path.isEmpty()) {
            throw new IllegalArgumentException("a setting needs a name");
        }
        for (final String segment : path) {
            if (segment == null || segment.isBlank() || segment.contains(".")) {
                throw new IllegalArgumentException("each part of a setting's path is a name without dots: " + path);
            }
        }
        path = List.copyOf(path);
        comment = List.copyOf(comment);
        if (range.isPresent() && allowed.isPresent()) {
            throw new IllegalArgumentException("a setting is held to a range or to a list, not both: " + path);
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
                List.of(), "");
    }

    /** The same setting, a number held to {@code min} and {@code max}, both included. */
    public <N extends Number & Comparable<N>> ConfigKey<T> range(final N min, final N max) {
        return new ConfigKey<>(this.path, this.codec, this.defaultValue, Optional.of(new ConfigKeyRange<>(min, max)),
                Optional.empty(), this.comment, this.title);
    }

    /** The same setting, a text held to these words; any other falls back to the default. */
    public ConfigKey<T> allowing(final String... words) {
        return new ConfigKey<>(this.path, this.codec, this.defaultValue, Optional.empty(),
                Optional.of(List.of(words)), this.comment, this.title);
    }

    /** The same setting, with these lines above it in the file; on a settings screen, they are its tooltip. */
    public ConfigKey<T> comment(final String... lines) {
        return new ConfigKey<>(this.path, this.codec, this.defaultValue, this.range, this.allowed,
                Arrays.asList(lines), this.title);
    }

    /**
     * The same setting, called this in English where it is shown: a settings screen lists it by this name, translated
     * as every other name of the mod is.
     */
    public ConfigKey<T> named(final String english) {
        return new ConfigKey<>(this.path, this.codec, this.defaultValue, this.range, this.allowed, this.comment,
                english);
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
            throw new IllegalArgumentException("the range of " + path + " has to be in its own kind of number, "
                    + defaultValue.getClass().getSimpleName() + ", not " + range.min().getClass().getSimpleName());
        }
        if (!inside(range, defaultValue)) {
            throw new IllegalArgumentException("the default of " + path + ", " + defaultValue
                    + ", is outside its range [" + range.min() + ", " + range.max() + "]");
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean inside(final ConfigKeyRange range, final Object value) {
        return range.contains((Number & Comparable) value);
    }

    private static void checkAllowed(final List<String> path, final Object defaultValue, final List<String> words) {
        // A word that falls back to the default has to fall back to a word that is allowed itself.
        if (!(defaultValue instanceof String word)) {
            throw new IllegalArgumentException("only a text setting can be held to a list of words: " + path);
        }
        if (words.isEmpty()) {
            throw new IllegalArgumentException("the words " + path + " is held to cannot be none");
        }
        if (!words.contains(word)) {
            throw new IllegalArgumentException("the default of " + path + ", " + word + ", is not one of " + words);
        }
    }
}
