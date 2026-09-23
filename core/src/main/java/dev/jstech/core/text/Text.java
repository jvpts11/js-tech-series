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

/**
 * Something a player reads, before it is put in a language: either words that are data, which read the same in
 * every language, or a declared sentence with what goes into it.
 *
 * <p>It stays in this form until it reaches whoever reads it. A screen resolves it in its player's language; the
 * server resolves it in English, which is the language a machine keeps what it writes down in (a file, a pipe, a
 * log), so two players on one machine are shown their own languages and still find the same words in its files.
 *
 * <p>Pure: no game types.
 */
public sealed interface Text permits Text.Literal, Text.Translated {

    /** Nothing at all. */
    Text EMPTY = new Literal("");

    /**
     * Words that are data: a name, a path, a number, what somebody typed. They are never translated, and this is the
     * one way to put fixed words in front of a player, so every such place can be found and read.
     */
    static Text literal(final String value) {
        return new Literal(value == null ? "" : value);
    }

    /** Whatever that is, as text: a text as it is, a key as its sentence, anything else as data. */
    static Text of(final Object value) {
        if (value instanceof Text text) {
            return text;
        }
        if (value instanceof TextKey key) {
            return key.text();
        }
        return literal(String.valueOf(value));
    }

    /** What it says in that language, with everything put into it. */
    String resolve(ITextLanguage language);

    /** Words that are data, the same in every language. */
    record Literal(String value) implements Text {

        public Literal {
            value = value == null ? "" : value;
        }

        @Override
        public String resolve(final ITextLanguage language) {
            return this.value;
        }
    }

    /**
     * A declared sentence and what goes into it.
     *
     * <p>A language that has no word for the key gets the English it was declared with, so a player is never shown
     * a bare key; when the English is not known either (a key that came over the wire from a newer build), the key
     * itself is what is left.
     */
    record Translated(TextKey key, List<Text> args) implements Text {

        public Translated {
            args = List.copyOf(args);
        }

        @Override
        public String resolve(final ITextLanguage language) {
            final String pattern = language.pattern(this.key.key());
            final List<String> resolved = new ArrayList<>(this.args.size());
            for (final Text arg : this.args) {
                resolved.add(arg.resolve(language));
            }
            return TextFormat.apply(pattern != null ? pattern : this.key.english(), resolved);
        }
    }
}
