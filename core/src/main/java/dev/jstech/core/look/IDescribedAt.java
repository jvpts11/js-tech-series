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
import net.minecraft.world.entity.player.Player;

/**
 * A block entity of several pieces, a cable with parts on it or a space of parts, that says what the piece a player
 * looks at is doing, where {@link IDescribed} says what the whole block is doing. It is asked on the server, with the
 * player whose look picks the piece, and its lines are sent to the player's game as {@link IDescribed}'s are.
 */
public interface IDescribedAt {

    /**
     * Adds the lines a player reads about the piece they look at, in the order they read them.
     *
     * @param lines   where the lines go
     * @param details whether the player asked for more, holding the key that shows details
     * @param player  the player looking
     */
    void describe(List<Text> lines, boolean details, Player player);
}
