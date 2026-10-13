/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.look;

import dev.jstech.core.text.Text;
import java.util.List;

/**
 * A block entity that says what it is doing to a player who looks at it: a machine at work and how far along, a tank
 * and what it holds. It is asked on the server, so it can say what only the server knows, and its lines are sent to
 * the player's game for the look-at tooltips to show: Jade's, when Jade is installed.
 */
public interface IDescribed {

    /**
     * Adds the lines a player looking at it reads, in the order they read them.
     *
     * @param lines   where the lines go
     * @param details whether the player asked for more, holding the key that shows details
     */
    void describe(List<Text> lines, boolean details);
}
