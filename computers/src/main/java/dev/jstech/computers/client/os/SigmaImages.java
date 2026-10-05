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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The pictures programs' image widgets show, read from the machine's disk once and kept: a picture is drawn every
 * frame, and asking the machine for its file that often would be absurd. A path that names no picture is remembered
 * as none, so it is not asked for again.
 */
public final class SigmaImages {

    /** The pictures asked for, by path: empty while one is on its way, or when the path names no picture. */
    private static final Map<String, Optional<PixImage>> PICTURES = new HashMap<>();

    private SigmaImages() {
    }

    /** The picture at that path, asking the machine for it the first time; empty while it is on its way or missing. */
    static Optional<PixImage> get(final BlockPos host, final String path) {
        if (path.isEmpty()) {
            return Optional.empty();
        }
        final Optional<PixImage> held = PICTURES.get(path);
        if (held != null) {
            return held;
        }
        PICTURES.put(path, Optional.empty());
        PacketDistributor.sendToServer(new RequestWallpaperImagePayload(host, path));
        return Optional.empty();
    }

    /** Takes a picture the machine sent back, when one of the widgets asked for it. */
    public static void accept(final String path, final String content) {
        if (PICTURES.containsKey(path)) {
            PICTURES.put(path, content.isEmpty() ? Optional.empty() : Optional.of(PixImage.decode(content)));
        }
    }

    /** Forgets every picture, for a world being left. */
    public static void clear() {
        PICTURES.clear();
    }

    /**
     * Draws a picture inside a rectangle, scaled by whole pixels where it fits and centred, a run of colour at a time;
     * a picture bigger than the rectangle is shown at its own size from its top left, clipped by the rectangle.
     */
    static void paint(final GuiGraphics g, final PixImage image, final int x, final int y, final int w, final int h) {
        final int scale = Math.max(1, Math.min(w / image.width(), h / image.height()));
        final int ox = x + Math.max(0, (w - image.width() * scale) / 2);
        final int oy = y + Math.max(0, (h - image.height() * scale) / 2);
        for (int row = 0; row < image.height(); row++) {
            final int sy = oy + row * scale;
            int runStart = -1;
            int runColour = 0;
            for (int column = 0; column <= image.width(); column++) {
                final int index = column < image.width() ? image.get(column, row) : 0;
                final int colour = column < image.width() && !PixImage.isTransparent(index)
                        ? PixImage.colourOf(index) : 0;
                if (colour == runColour) {
                    continue;
                }
                if (runStart >= 0 && runColour != 0) {
                    g.fill(ox + runStart * scale, sy, ox + column * scale, sy + scale, runColour);
                }
                runStart = column;
                runColour = colour;
            }
        }
    }
}
