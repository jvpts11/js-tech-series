/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws a program's icon from its texture. Each program ships a real 16x16 pixel-art sprite per Frames edition
 * at {@code textures/gui/program/<program>/<os>.png}, genuinely different artwork per era, not a recolour:
 * Frames 95 is chunky outlined pixel art, Frames XP is glossy Luna pixel art, Frames 11 is flat two-tone pixel
 * art. The blit is keyed by the program id, so an add-on that ships a sprite at this convention path is drawn
 * with no code change here.
 *
 * <p>Every sprite is drawn at its native 16 pixels, 1:1, centered in the requested box, never resampled.
 * The only scaling is the game's own uniform GUI scale, which keeps the pixels crisp exactly like item icons.
 * An unknown or null id falls back to the {@code generic} sprite so a launcher never shows a missing texture;
 * which file stands in for which is {@link SkinSprites}'s to say.
 */
public final class ProgramIcons {

    /** Native sprite size; sprites are authored at this size and drawn 1:1. */
    public static final int SIZE = 16;

    /** Marks an icon set drawn for a desktop as it looked on Legacy-era hardware. */
    public static final String PERIOD_SUFFIX = SkinSprites.PERIOD_SUFFIX;

    private ProgramIcons() {
    }

    /**
     * Draws {@code programId}'s icon in the artwork style of {@code os} (a Frames edition or a Linux desktop
     * environment id), centered in the box and fitted to it when the box is smaller than the sprite: most call
     * sites hand over a box shorter or narrower than 16 (a taskbar button, a menu row), and a sprite drawn at
     * its full size there spilled over the text and borders around it.
     */
    public static void draw(final GuiGraphics g, final int x, final int y, final int w, final int h,
                            final ResourceLocation programId, final String os) {
        final String program = programId == null ? "generic" : programId.getPath();
        SkinSprites.draw(g, SkinSprites.find("program", program, "generic", os), x, y, w, h, SIZE);
    }
}
