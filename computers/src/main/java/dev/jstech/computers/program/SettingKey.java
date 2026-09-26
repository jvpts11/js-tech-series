/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.audio.SoundOutput;
import java.util.Locale;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * Every key a machine's settings store takes, declared once with how a value for it is read and applied.
 *
 * <p>The {@code config} command, the Settings window and every other window that changes a setting send the
 * same {@code key=value}, and this is the one place a key is known. A value is read forgivingly where people
 * write it more than one way ({@code 12h} or {@code 12}, {@code on} or {@code true}) and refused where it means
 * nothing, so the caller can say so; a number is clamped by the store, never refused for being out of range.
 */
public enum SettingKey {

    CLOCK("clock", (s, v) -> choose(v, Set.of("12h", "12"), Set.of("24h", "24"), s::setClock12h)),

    /** A theme's name and accent; the wallpaper that comes with it is the desktop's, set by whoever owns that. */
    THEME("theme", (s, v) -> {
        ThemePreset.byId(v).applyTo(s);
        return true;
    }),

    TASKBAR("taskbar", (s, v) -> choose(v, Set.of("center", "centre", "centered"), Set.of("left"),
            s::setTaskbarCentered)),

    DARKMODE("darkmode", (s, v) -> choose(v, Set.of("on", "true", "dark"), Set.of("off", "false", "light"),
            s::setDarkMode)),

    GUISCALE("guiscale", (s, v) -> {
        final Integer n = parseInt(v);
        if (n != null) {
            s.setGuiScale(n);
        }
        return n != null;
    }),

    BRIGHTNESS("brightness", (s, v) -> {
        final Integer n = parseInt(v);
        if (n != null) {
            s.setBrightness(n);
        }
        return n != null;
    }),

    /** A percentage, with or without the sign after it. */
    VOLUME("volume", (s, v) -> {
        final Integer n = parseInt(v.endsWith("%") ? v.substring(0, v.length() - 1) : v);
        if (n != null) {
            s.setVolume(n);
        }
        return n != null;
    }),

    MUTE("mute", (s, v) -> choose(v, Set.of("on", "true", "yes"), Set.of("off", "false", "no"), s::setMuted)),

    OUTPUT("output", (s, v) -> {
        final SoundOutput output = SoundOutput.byId(v);
        if (output != null) {
            s.setSoundOutput(output);
        }
        return output != null;
    }),

    SAVEDRIVE("savedrive", (s, v) -> {
        if (v.isEmpty() || !Character.isLetter(v.charAt(0))) {
            return false;
        }
        s.setDefaultSaveDrive(v.charAt(0));
        return true;
    }),

    AUTOOPEN("autoopen", (s, v) -> choose(v, Set.of("on", "true"), Set.of("off", "false"), s::setRemovableAutoOpen)),

    REMOTE("remote", (s, v) -> choose(v, Set.of("on", "true"), Set.of("off", "false"), s::setRemoteAllowed)),

    /** Six hex digits, with or without a {@code #} in front. */
    ACCENT("accent", (s, v) -> {
        final Integer argb = parseAccent(v);
        if (argb != null) {
            s.setAccent(argb);
        }
        return argb != null;
    }),

    /**
     * Pinning what is pinned already is not a mistake worth refusing: the panel asks for the state it wants, and
     * either way the program ends up pinned.
     */
    PIN("pin", (s, v) -> !ComputerSettings.normalizeId(v).isEmpty() && (s.isPinned(v) || s.pin(v))),

    UNPIN("unpin", (s, v) -> {
        s.unpin(v);
        return !ComputerSettings.normalizeId(v).isEmpty();
    }),

    /** Starring what is starred already asks for the state it wants, and either way it is starred. */
    FAVOURITE("favourite", (s, v) -> !v.isEmpty() && (s.isFavourite(v) || s.favourite(v))),

    UNFAVOURITE("unfavourite", (s, v) -> {
        s.unfavourite(v);
        return !v.isEmpty();
    }),

    /** {@code <data id>=<index>}: which recipe to open the craft of that item on; a negative index forgets it. */
    RECIPE("recipe", (s, v) -> {
        final int eq = v.lastIndexOf('=');
        final Integer index = eq <= 0 ? null : parseInt(v.substring(eq + 1));
        if (index != null) {
            s.setRecipeChoice(v.substring(0, eq), index);
        }
        return index != null;
    });

    /** What a key naming the program that opens one extension starts with: {@code defaultapp:txt}. */
    public static final String DEFAULT_APP_PREFIX = "defaultapp:";

    private final String key;
    private final IApplier applier;

    SettingKey(final String key, final IApplier applier) {
        this.key = key;
        this.applier = applier;
    }

    /** The key under that name, case aside, or null when the store takes no such key. */
    @Nullable
    public static SettingKey byKey(@Nullable final String key) {
        final String wanted = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
        for (final SettingKey each : values()) {
            if (each.key.equals(wanted)) {
                return each;
            }
        }
        return null;
    }

    /**
     * Applies {@code key=value} to that store: true when the key is one the store takes and the value meant
     * something, false for a key it does not know or a value it could not read, so the caller can send the key
     * elsewhere or say what was wrong.
     */
    public static boolean apply(final ComputerSettings settings, @Nullable final String key,
                                @Nullable final String value) {
        final String k = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
        final String v = value == null ? "" : value.trim();
        if (k.startsWith(DEFAULT_APP_PREFIX)) {
            settings.setDefaultApp(k.substring(DEFAULT_APP_PREFIX.length()), v);
            return true;
        }
        final SettingKey known = byKey(k);
        return known != null && known.applier.apply(settings, v);
    }

    /** The key as it is typed and sent. */
    public String key() {
        return this.key;
    }

    /** Sets a yes-or-no setting from the words that mean each, or refuses a word that means neither. */
    private static boolean choose(final String value, final Set<String> yes, final Set<String> no,
                                  final IFlag flag) {
        final String v = value.toLowerCase(Locale.ROOT);
        if (yes.contains(v)) {
            flag.set(true);
            return true;
        }
        if (no.contains(v)) {
            flag.set(false);
            return true;
        }
        return false;
    }

    @Nullable
    private static Integer parseInt(final String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    /** Six hex digits (optionally {@code #}-prefixed) as an opaque ARGB int, or null. */
    @Nullable
    private static Integer parseAccent(final String value) {
        final String hex = value.startsWith("#") ? value.substring(1) : value;
        if (hex.length() != 6) {
            return null;
        }
        try {
            return 0xFF << 24 | Integer.parseInt(hex, 16);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    /** How one key takes a value, already trimmed; false when the value means nothing to it. */
    @FunctionalInterface
    private interface IApplier {
        boolean apply(ComputerSettings settings, String value);
    }

    /** A yes-or-no setting's setter. */
    @FunctionalInterface
    private interface IFlag {
        void set(boolean value);
    }
}
