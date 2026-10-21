/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Text a player reads, kept apart from the language it is read in: each sentence declared once in code with its
 * English, the English language file generated from those declarations, and the sentence put into a language only where
 * it is read, so the server never decides a player's language and each player reads their own.
 *
 * <p>A sentence is a {@link TextKey}, a key and its English, declared as a {@code static final} field of a class marked
 * {@link TextHolder}, which is how the generator finds it; its English may hold {@code %s} where values go. Filled with
 * its values ({@link TextKey#with}), it is a {@link Text}: a {@link Text.Translated} sentence, or {@link Text.Literal}
 * words that are data and read the same in every language. {@link GameText} turns a text into the game's
 * {@code Component}, or a {@code String}, in the language this side of the game has loaded. A text travels on the wire
 * by {@link TextCodecs} and in a save by {@link TextTags}, in the form it was made in, so it is read in the language of
 * whoever reads it; {@link TextBounds} cuts what comes from a player, a program or a file to what the wire and a save
 * take, and {@link LongText} keeps text of any length in a save. Beside them, {@link TextFormat} fills the {@code %s}
 * marks the way the game's language files do, {@link TextLists} joins several texts into one, and {@link Glob} matches
 * a name against a pattern of stars and question marks.
 *
 * <p>How to declare and show one, from zero, is in {@code core/docs/TEXT_AND_COLOUR.md}. An example that compiles is
 * J's Example's sentences ({@code example/src/main/java/dev/jstech/example/ExampleTexts.java}), which its logbook and
 * its level sensor show.
 */
@ApiStatus.Experimental
package dev.jstech.core.text;

import org.jetbrains.annotations.ApiStatus;
