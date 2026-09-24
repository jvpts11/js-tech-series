/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What Solitaire's window, toolbar and status line say. */
@TextHolder
final class SolitaireTexts {

    static final TextKey TITLE = TextKey.of("jsc.solitaire.title", "Solitaire");
    static final TextKey NEW_GAME = TextKey.of("jsc.solitaire.new_game", "New");
    static final TextKey FINISH = TextKey.of("jsc.solitaire.finish", "Finish");
    static final TextKey SCORE_MOVES = TextKey.of("jsc.solitaire.score_moves", "Score %s   Moves %s");
    static final TextKey YOU_WIN = TextKey.of("jsc.solitaire.you_win", "You win");

    private SolitaireTexts() {
    }
}
