/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.screen;

import java.util.List;

/**
 * A screen, or a part of one, that names what it draws itself beyond its widgets: its tabs, its cards, the rows of a
 * list, the places a thing carried can be let go on. An agent reading the screen finds each by its name and its words,
 * and clicks it or drags from it to another by name, as a player reads and points at what they see; a screen drawn
 * all in code is otherwise only pixels to it.
 *
 * <p>The parts are what it draws now, so the places a held thing snaps to are named while it is held and not before.
 * A name is the same for the same thing while the screen is open, such as {@code tab:2} or
 * {@code card:jsspace:small_airlock}; the words are what a player reads on it.
 */
public interface IShownParts {

    /** What it draws now that a player can point at, front to back as it is drawn over: the last over the others. */
    List<ShownPart> shownParts();
}
