/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.os.PanelStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The maker's mark of the desktop family: a window of four panes, in the colours of the edition wearing it.
 *
 * <p>One small picture per edition, at {@code textures/gui/emblem/<edition>.png}, drawn wherever that edition shows
 * its own face at a glance: the Start button at the corner of its desktop and the head of its installer. It is the
 * same mark in all of them on purpose, because that is what a mark is for.
 */
public final class FramesEmblem {

    /** How big the mark is drawn, which is the size it is made at, so it is never resampled. */
    public static final int SIZE = 9;

    private FramesEmblem() {
    }

    /** Draws that edition's mark with its top-left at {@code (x, y)}; an edition nobody knows wears the newest's. */
    public static void draw(final GuiGraphics g, final int x, final int y, final PanelStyle edition) {
        g.blit(texture(edition), x, y, 0.0F, 0.0F, SIZE, SIZE, SIZE, SIZE);
    }

    private static ResourceLocation texture(final PanelStyle edition) {
        final String id = switch (edition) {
            case FRAMES_95 -> "frames_95";
            case FRAMES_XP -> "frames_xp";
            default -> "frames_11";
        };
        return ResourceLocation.fromNamespaceAndPath("jsc", "textures/gui/emblem/" + id + ".png");
    }
}
