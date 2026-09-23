/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.text;

import org.jetbrains.annotations.Nullable;

/**
 * A language: the sentence it has for a key, or nothing when it has none.
 *
 * <p>Pure, so text can be resolved without the game: {@link #ENGLISH} is the English every key is declared with.
 */
@FunctionalInterface
public interface ITextLanguage {

    /** The English every sentence was declared with, and nothing else. */
    ITextLanguage ENGLISH = key -> null;

    /** The sentence this language has for that key, with its {@code %s} still in it; null when it has none. */
    @Nullable
    String pattern(String key);
}
