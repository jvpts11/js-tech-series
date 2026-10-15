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
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

/**
 * Colours as a settings file writes them, {@code #RRGGBB}: read into {@code 0xRRGGBB} and written back, and the codec a
 * colour setting is held to, which takes nothing else.
 */
public final class ConfigColours {

    /** A colour written {@code #RRGGBB}, and only that. */
    public static final Codec<String> CODEC = Codec.STRING.validate(text -> parse(text).isPresent()
            ? DataResult.success(text) : DataResult.error(() -> "a colour written " + text + ": write #RRGGBB"));

    private static final String MARK = "#";
    private static final int DIGITS = 6;

    private ConfigColours() {
    }

    /** {@code #RRGGBB} read as {@code 0xRRGGBB}, or none when it is not written so. */
    public static Optional<Integer> parse(final String text) {
        if (text == null || !text.startsWith(MARK) || text.length() != DIGITS + 1) {
            return Optional.empty();
        }
        final String digits = text.substring(1);
        for (int i = 0; i < digits.length(); i++) {
            if (Character.digit(digits.charAt(i), 16) < 0) {
                return Optional.empty();
            }
        }
        return Optional.of(HexFormat.fromHexDigits(digits));
    }

    /** {@code 0xRRGGBB} written {@code #RRGGBB}, in capitals; what is above the colour is left out. */
    public static String write(final int rgb) {
        return MARK + HexFormat.of().toHexDigits(rgb).substring(2).toUpperCase(Locale.ROOT);
    }
}
