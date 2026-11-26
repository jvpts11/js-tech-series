/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.screen;

import org.jetbrains.annotations.ApiStatus;

/**
 * A screen with no menu that draws a panel of a fixed size in the middle of the window, as a container screen draws
 * its image. While it is open and its panel does not fit the window at the player's interface scale, the Core lowers
 * the scale just enough for it to fit and puts it back when it closes, as it does for a container screen whose image
 * does not fit; a screen that sizes itself to the window needs none of this. A container screen that draws more than
 * its image round it, a frame, says so here too, and its panel is what has to fit.
 */
@ApiStatus.Experimental
public interface IPanelScreen {

    /** How wide its panel is, in the interface's pixels. */
    int panelWidth();

    /** How tall its panel is, in the interface's pixels. */
    int panelHeight();
}
