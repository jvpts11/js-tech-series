/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The look of the Media Player on each desktop, after the player that desktop really shipped: the Media Player of 95
 * with its track bar, the Frames Media Player of XP with its rail and its bars, of 7 with its library, and of 10 with
 * its picture and its round button; 11's Media Player, KDE's Dragon Player, GNOME's Videos, Cinnamon's Celluloid,
 * CDE's Audio with its wave, and Quince's QuinceTime Player.
 */
enum MediaPlayerLook {

    FRAMES_95(240, 178, List.of(MediaPlayerTexts.FILE, MediaPlayerTexts.DEVICE, MediaPlayerTexts.SCALE,
            MediaPlayerTexts.HELP)),
    FRAMES_XP(318, 206, List.of(MediaPlayerTexts.FILE, MediaPlayerTexts.VIEW, MediaPlayerTexts.PLAY,
            MediaPlayerTexts.TOOLS, MediaPlayerTexts.HELP)),
    FRAMES_7(316, 206, List.of()),
    FRAMES_10(286, 202, List.of()),
    FRAMES_11(316, 216, List.of()),
    KDE(276, 210, List.of()),
    GNOME(276, 206, List.of()),
    CINNAMON(288, 206, List.of()),
    CDE(248, 152, List.of(MediaPlayerTexts.FILE, MediaPlayerTexts.OPTIONS, MediaPlayerTexts.HELP)),
    QUINCE(206, 150, List.of());

    private final int width;
    private final int height;
    private final List<TextKey> menus;

    MediaPlayerLook(final int width, final int height, final List<TextKey> menus) {
        this.width = width;
        this.height = height;
        this.menus = menus;
    }

    /** The window's width at first. */
    int width() {
        return width;
    }

    /** The window's height at first. */
    int height() {
        return height;
    }

    /** The menus along its bar, for a player that has one in its window. */
    List<TextKey> menus() {
        return menus;
    }

    /** Whether it lists the songs and sequences beside what it plays, rather than only playing. */
    boolean lists() {
        return this == FRAMES_XP || this == FRAMES_7 || this == FRAMES_11 || this == CINNAMON || this == GNOME;
    }

    /** The look of the desktop {@code desktopId} names; the newest Frames' for one it does not know. */
    static MediaPlayerLook of(@Nullable final ResourceLocation desktopId) {
        final String path = desktopId == null ? "" : desktopId.getPath();
        if (path.startsWith("quince")) {
            return QUINCE;
        }
        return switch (path) {
            case "frames_95" -> FRAMES_95;
            case "frames_xp" -> FRAMES_XP;
            case "frames_7" -> FRAMES_7;
            case "frames_10" -> FRAMES_10;
            case "kde_plasma" -> KDE;
            case "gnome" -> GNOME;
            case "cinnamon" -> CINNAMON;
            case "cde" -> CDE;
            default -> FRAMES_11;
        };
    }
}
