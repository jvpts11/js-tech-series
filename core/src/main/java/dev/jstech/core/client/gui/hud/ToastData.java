/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.hud;

import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import java.util.Objects;

/**
 * Immutable description of a mod toast notification (Operation complete, attack detected, robot stuck, etc.).
 */
@PaletteHolder
public record ToastData(String titleKey, String descriptionKey, Severity severity) {

    /** The accent each severity is drawn with: blue, green, amber and red by default. */
    private static final Palette<Accents> ACCENTS = Palettes.declare(JsCore.MODID, "gui/toast",
            new Accents(0xFF4A90D9, 0xFF5CB85C, 0xFFF0AD4E, 0xFFD9534F));

    /**
     * Severity levels, ordered from least to most urgent.
     */
    public enum Severity {
        INFO,
        SUCCESS,
        WARNING,
        CRITICAL
    }

    public ToastData {
        Objects.requireNonNull(titleKey, "titleKey must not be null");
        Objects.requireNonNull(descriptionKey, "descriptionKey must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        if (titleKey.isBlank()) {
            throw new IllegalArgumentException("titleKey must not be blank");
        }
    }

    public static ToastData of(final String titleKey, final Severity severity) {
        return new ToastData(titleKey, "", severity);
    }

    public boolean hasDescription() {
        return !descriptionKey.isBlank();
    }

    public long defaultDurationMillis() {
        return switch (severity) {
            case INFO -> 3000L;
            case SUCCESS -> 3000L;
            case WARNING -> 5000L;
            case CRITICAL -> 8000L;
        };
    }

    public int accentColor() {
        final Accents accents = ACCENTS.get();
        return switch (severity) {
            case INFO -> accents.info();
            case SUCCESS -> accents.success();
            case WARNING -> accents.warning();
            case CRITICAL -> accents.critical();
        };
    }

    private record Accents(int info, int success, int warning, int critical) {
    }
}