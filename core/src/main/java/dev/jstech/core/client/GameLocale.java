/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client;

import java.text.NumberFormat;
import java.util.Locale;
import net.minecraft.client.Minecraft;

/**
 * The locale of the language the player picked in the game, which is not the computer's: numbers a screen shows are
 * grouped the way that language groups them ("1,204" in English, "1.204" in Portuguese).
 */
public final class GameLocale {

    private GameLocale() {
    }

    /** The locale of the game's language now. */
    public static Locale locale() {
        return Locale.forLanguageTag(Minecraft.getInstance().getLanguageManager().getSelected().replace('_', '-'));
    }

    /** A count, grouped the way the game's language groups it. */
    public static String count(final long value) {
        return NumberFormat.getIntegerInstance(locale()).format(value);
    }
}
