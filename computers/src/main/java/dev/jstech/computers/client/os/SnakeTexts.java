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

/** The words of the Snake window: its toolbar, the line it waits on, the end of a game and the score. */
@TextHolder
final class SnakeTexts {

    static final TextKey NEW = TextKey.of("jsc.snake.new", "New");
    static final TextKey SLOW = TextKey.of("jsc.snake.slow", "Slow");
    static final TextKey NORMAL = TextKey.of("jsc.snake.normal", "Normal");
    static final TextKey FAST = TextKey.of("jsc.snake.fast", "Fast");
    static final TextKey WALLS = TextKey.of("jsc.snake.walls", "Walls");
    static final TextKey GAME_OVER = TextKey.of("jsc.snake.game_over", "Game over");
    static final TextKey SCORE = TextKey.of("jsc.snake.score", "Score %s");
    static final TextKey AGAIN = TextKey.of("jsc.snake.again", "New for another");
    static final TextKey START_HINT = TextKey.of("jsc.snake.start_hint", "Arrow keys to start");
    static final TextKey SCORE_AND_BEST = TextKey.of("jsc.snake.score_and_best", "Score %s   Best %s");
    static final TextKey LENGTH = TextKey.of("jsc.snake.length", "Length %s");

    private SnakeTexts() {
    }
}
