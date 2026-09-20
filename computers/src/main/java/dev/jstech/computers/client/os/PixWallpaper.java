/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.RequestWallpaperImagePayload;
import dev.jstech.computers.os.fs.PixImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * A picture a player drew, hanging on the desktop of the machine it was drawn on.
 *
 * <p>Held for as long as it stays chosen and asked for once when it changes, because a wallpaper is drawn
 * every frame and reading a file that often would be absurd. A picture that cannot be found leaves the
 * desktop with the plain wallpaper rather than with nothing.
 */
public final class PixWallpaper {

    /** What a wallpaper choice looks like when it names a picture rather than one of the built-in styles. */
    public static final String PREFIX = "pix:";

    private static String wanted = "";
    @Nullable
    private static PixImage picture;

    private PixWallpaper() {
    }

    /** The path inside a wallpaper choice, or empty when the choice is not a picture. */
    public static String pathOf(final String choice) {
        return choice != null && choice.startsWith(PREFIX) ? choice.substring(PREFIX.length()) : "";
    }

    /** A choice that names {@code path} as the wallpaper. */
    public static String choiceFor(final String path) {
        return PREFIX + path;
    }

    /**
     * Says which picture the desktop wants, asking the machine for it when it is a new one.
     *
     * <p>Called from the draw, so it has to do nothing at all in the ordinary case, which is the same
     * picture as last frame.
     */
    public static void want(final BlockPos host, final String choice) {
        final String path = pathOf(choice);
        if (path.equals(wanted)) {
            return;
        }
        wanted = path;
        picture = null;
        if (!path.isEmpty() && host != null) {
            PacketDistributor.sendToServer(new RequestWallpaperImagePayload(host, path));
        }
    }

    /** Takes the picture the machine sent back, when it is still the one being waited for. */
    public static void accept(final String path, final String content) {
        if (!path.equals(wanted)) {
            return;
        }
        picture = content.isEmpty() ? null : PixImage.decode(content);
    }

    /** Forgets what is held, for a desktop being left. */
    public static void clear() {
        wanted = "";
        picture = null;
    }

    /**
     * Draws the picture across the glass, or answers false when there is none to draw.
     *
     * <p>Scaled up by whole pixels and centred, never stretched by a fraction: a drawing made pixel by
     * pixel looks wrong the moment its pixels stop being square.
     */
    public static boolean paint(final GuiGraphics g, final int width, final int height) {
        final PixImage image = picture;
        if (image == null) {
            return false;
        }
        final int scale = Math.max(1, Math.min(width / image.width(), height / image.height()));
        final int drawnW = image.width() * scale;
        final int drawnH = image.height() * scale;
        final int ox = (width - drawnW) / 2;
        final int oy = (height - drawnH) / 2;
        // Whatever the picture does not cover stays the colour of its own first pixel, so nothing shows through.
        g.fill(0, 0, width, height, backdrop(image));
        /*
         * One run of colour at a time rather than one pixel at a time. This is drawn behind the whole
         * desktop in every frame, so a rectangle per pixel would be sixteen thousand of them per frame for
         * a picture nobody is even looking at closely.
         */
        for (int y = 0; y < image.height(); y++) {
            final int sy = oy + y * scale;
            int runStart = -1;
            int runColour = 0;
            for (int x = 0; x <= image.width(); x++) {
                final int index = x < image.width() ? image.get(x, y) : 0;
                final int colour = x < image.width() && !PixImage.isTransparent(index)
                        ? PixImage.colourOf(index) : 0;
                if (colour == runColour) {
                    continue;
                }
                if (runStart >= 0 && runColour != 0) {
                    g.fill(ox + runStart * scale, sy, ox + x * scale, sy + scale, runColour);
                }
                runStart = x;
                runColour = colour;
            }
        }
        return true;
    }

    /** The colour the wall around the picture is painted, taken from the picture's own corner. */
    private static int backdrop(final PixImage image) {
        final int corner = image.get(0, 0);
        return PixImage.isTransparent(corner) ? 0xFF1E1E1E : PixImage.colourOf(corner);
    }
}
