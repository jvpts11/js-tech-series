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

/** The short names Minesweeper's buttons give its three boards, short enough for the strip they share. */
@TextHolder
final class MinesweeperTexts {

    static final TextKey BEGINNER = TextKey.of("jsc.minesweeper.beginner_short", "Beg");
    static final TextKey INTERMEDIATE = TextKey.of("jsc.minesweeper.intermediate_short", "Int");
    static final TextKey EXPERT = TextKey.of("jsc.minesweeper.expert_short", "Exp");

    private MinesweeperTexts() {
    }
}
