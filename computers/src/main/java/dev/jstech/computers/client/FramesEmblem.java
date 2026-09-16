/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The maker's mark of the desktop family: a window of four panes, in the colours of the edition wearing it.
 *
 * <p>One emblem, drawn in every place that edition shows its own face: the firmware setup, the installer, the
 * screen it comes up behind and the button at the corner of its desktop. It is the same mark in all of them on
 * purpose, because that is what a mark is for.
 *
 * <p>The panes are laid out as a window and not as a flag: the older editions leant theirs and the newest
 * squared it up, and the colours are each edition's own rather than one set of four borrowed by all three.
 */
public final class FramesEmblem {

    private FramesEmblem() {
    }

    /**
     * Draws the emblem of that edition.
     *
     * @param size    how wide and tall the whole mark is, gap included
     * @param edition the edition's id path, as the desktop and the registry name it
     */
    public static void draw(final GuiGraphics g, final int x, final int y, final int size, final String edition) {
        final int gap = Math.max(1, size / 10);
        final int half = (size - gap) / 2;
        final int[] panes = panesOf(edition);
        g.fill(x, y, x + half, y + half, panes[0]);
        g.fill(x + half + gap, y, x + size, y + half, panes[1]);
        g.fill(x, y + half + gap, x + half, y + size, panes[2]);
        g.fill(x + half + gap, y + half + gap, x + size, y + size, panes[3]);
    }

    /**
     * The four panes of that edition, clockwise from the top left.
     *
     * <p>An edition nobody here knows wears the newest one's, which is the plainest of the three.
     */
    public static int[] panesOf(final String edition) {
        return switch (edition) {
            case "frames_95" -> new int[]{0xFF000080, 0xFF1F8A8A, 0xFF5FC3C3, 0xFF3A4FA8};
            case "frames_xp" -> new int[]{0xFF2C66BD, 0xFF6F9FE0, 0xFF9BD164, 0xFF4E8B26};
            default -> new int[]{0xFF5B84F0, 0xFF5B84F0, 0xFF5B84F0, 0xFF5B84F0};
        };
    }
}
