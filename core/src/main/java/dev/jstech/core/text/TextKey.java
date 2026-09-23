/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A sentence a player reads, declared once where it is used: the key it is translated under and its English.
 *
 * <p>The English written here is the whole of it: the English language file is generated from these declarations,
 * so a sentence is added, reworded or removed in one place, beside the code that says it, and a translation follows
 * the key. Declare them as {@code static final} fields of a class marked {@link TextHolder}, which is how the
 * generator finds them.
 *
 * <p>The English may hold {@code %s} (or {@code %1$s}, {@code %2$s} for arguments in another order) where the
 * arguments go, as the game's own language files do.
 *
 * <p>Pure: no game types, so a sentence can be declared and resolved anywhere.
 *
 * @param key     the key, in the vanilla form: words joined by dots, one of them naming the mod
 * @param english what the sentence says in English
 */
public record TextKey(String key, String english) {

    /* Words joined by dots, as the game's own keys are; a few of those (itemGroup) carry a capital. */
    private static final Pattern KEY = Pattern.compile("[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)+");

    public TextKey {
        if (key == null || !KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("a text key is words joined by dots: " + key);
        }
        if (english == null || english.isBlank()) {
            throw new IllegalArgumentException("a text key needs its English: " + key);
        }
    }

    /** A sentence under that key, saying that in English. */
    public static TextKey of(final String key, final String english) {
        return new TextKey(key, english);
    }

    /** The sentence with nothing put into it. */
    public Text text() {
        return new Text.Translated(this, List.of());
    }

    /**
     * The sentence with its arguments put in, in order. An argument that is a {@link Text} or a {@code TextKey} is
     * translated with the sentence; anything else is data and goes in as it is written.
     */
    public Text with(final Object... args) {
        final List<Text> texts = new ArrayList<>(args.length);
        for (final Object arg : args) {
            texts.add(Text.of(arg));
        }
        return new Text.Translated(this, texts);
    }
}
