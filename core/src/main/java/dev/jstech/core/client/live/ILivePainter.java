/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.live;

import net.minecraft.client.gui.GuiGraphics;

/**
 * What draws one picture shown in the world: given the graphics of the picture's own texture, it draws what the
 * screen shows into an area of that size at the origin, in the same units and with the same calls as a screen on the
 * player's monitor.
 */
@FunctionalInterface
public interface ILivePainter {

    void paint(GuiGraphics graphics, int width, int height, float partialTick);
}
