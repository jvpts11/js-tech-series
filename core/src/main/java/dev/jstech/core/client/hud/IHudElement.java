/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.hud;

import dev.jstech.core.gui.HudStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * One thing a mod shows on the player's screen while they play: a reading of the machine in their hand, a timer, a
 * meter. It says whether it shows now and how big it is, and draws itself where the Core puts it, in its corner with
 * the others.
 */
public interface IHudElement {

    /** Whether it shows this frame; asked every frame, so a quick answer. */
    boolean shown(Minecraft minecraft);

    /** How big it is this frame, in the GUI's pixels. */
    HudStack.Size size(Minecraft minecraft);

    /** Draws it with its top-left corner at {@code x}, {@code y}. */
    void render(GuiGraphics graphics, DeltaTracker delta, int x, int y);
}
