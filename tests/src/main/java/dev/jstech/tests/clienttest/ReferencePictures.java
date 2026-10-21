/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

/**
 * Pictures a client test compares what is on the screen with: a part of the frame, cut to the GUI's rectangle given,
 * against the reference picture of that name in the test mod's resources, {@code clienttest/references/<name>.png}.
 * A pixel may be a little off in each colour, and a few may be off by more, so the game's own small differences from
 * one card to another pass; a picture that differs more, or has none to compare with, fails the test and leaves what
 * was drawn, and where it differs, in the run's folder, to be looked at and, when it is right, put in its place.
 */
final class ReferencePictures {

    /** Where the references are among the test mod's resources, and where a run leaves what it drew. */
    static final String FOLDER = "/clienttest/references/";
    static final String DRAWN = "reference-pictures";
    /** How far a pixel's colour may be off in each channel, and the share of the pixels that may be off by more. */
    private static final int CHANNEL = 12;
    private static final double SHARE = 0.005;
    /** Where a pixel is off, in the picture of the differences: opaque red. */
    private static final int OFF = 0xFF0000FF;

    private ReferencePictures() {
    }

    /**
     * Fails unless the frame's rectangle {@code x}, {@code y}, {@code width} by {@code height}, in the GUI's pixels,
     * looks as the reference picture {@code name} does.
     */
    static void check(final Minecraft mc, final String name, final int x, final int y, final int width,
                      final int height) {
        try (NativeImage drawn = capture(mc, x, y, width, height)) {
            try (InputStream in = ReferencePictures.class.getResourceAsStream(FOLDER + name + ".png")) {
                if (in == null) {
                    final File left = leave(mc, drawn, name + ".png");
                    throw new ClientTestFailure("there is no reference picture " + name + ": what was drawn is at "
                            + left + "; when it is right, copy it into tests/src/main/resources" + FOLDER);
                }
                try (NativeImage reference = NativeImage.read(in)) {
                    compare(mc, name, drawn, reference);
                }
            }
        } catch (final IOException failed) {
            throw new ClientTestFailure("the reference picture " + name + " could not be read or left", failed);
        }
    }

    /** The frame's rectangle {@code x}, {@code y}, {@code width} by {@code height} in the GUI's pixels, a copy. */
    static NativeImage capture(final Minecraft mc, final int x, final int y, final int width, final int height) {
        final double scale = mc.getWindow().getGuiScale();
        try (NativeImage frame = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            return cut(frame, (int) Math.round(x * scale), (int) Math.round(y * scale),
                    (int) Math.round(width * scale), (int) Math.round(height * scale));
        }
    }

    /** Fails unless {@code drawn} looks as {@code other} does, both pictures of {@code name}. */
    static void same(final Minecraft mc, final String name, final NativeImage drawn, final NativeImage other) {
        try {
            compare(mc, name, drawn, other);
        } catch (final IOException failed) {
            throw new ClientTestFailure("the pictures of " + name + " could not be left", failed);
        }
    }

    private static void compare(final Minecraft mc, final String name, final NativeImage drawn,
                                final NativeImage reference) throws IOException {
        if (drawn.getWidth() != reference.getWidth() || drawn.getHeight() != reference.getHeight()) {
            final File left = leave(mc, drawn, name + ".png");
            throw new ClientTestFailure("the picture " + name + " is " + drawn.getWidth() + " by "
                    + drawn.getHeight() + " and its reference " + reference.getWidth() + " by "
                    + reference.getHeight() + "; what was drawn is at " + left);
        }
        int off = 0;
        try (NativeImage differences = new NativeImage(drawn.getWidth(), drawn.getHeight(), true)) {
            for (int py = 0; py < drawn.getHeight(); py++) {
                for (int px = 0; px < drawn.getWidth(); px++) {
                    final int a = drawn.getPixelRGBA(px, py);
                    final int b = reference.getPixelRGBA(px, py);
                    final boolean differs = Math.abs((a & 0xFF) - (b & 0xFF)) > CHANNEL
                            || Math.abs((a >> 8 & 0xFF) - (b >> 8 & 0xFF)) > CHANNEL
                            || Math.abs((a >> 16 & 0xFF) - (b >> 16 & 0xFF)) > CHANNEL;
                    differences.setPixelRGBA(px, py, differs ? OFF : a);
                    off += differs ? 1 : 0;
                }
            }
            final int most = (int) Math.floor(drawn.getWidth() * drawn.getHeight() * SHARE);
            if (off > most) {
                final File left = leave(mc, drawn, name + ".png");
                leave(mc, differences, name + "-differences.png");
                throw new ClientTestFailure("the picture " + name + " differs from its reference in " + off
                        + " pixels, more than " + most + "; what was drawn is at " + left
                        + ", its differences in red beside it");
            }
        }
    }

    /* A copy of the frame's rectangle, in the frame's pixels. */
    private static NativeImage cut(final NativeImage frame, final int x, final int y, final int width,
                                   final int height) {
        final int w = Math.max(1, Math.min(width, frame.getWidth() - x));
        final int h = Math.max(1, Math.min(height, frame.getHeight() - y));
        final NativeImage out = new NativeImage(w, h, true);
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                out.setPixelRGBA(px, py, frame.getPixelRGBA(x + px, y + py));
            }
        }
        return out;
    }

    private static File leave(final Minecraft mc, final NativeImage picture, final String file) throws IOException {
        final File folder = new File(mc.gameDirectory, DRAWN);
        if (!folder.isDirectory() && !folder.mkdirs()) {
            throw new IOException("the folder " + folder + " could not be made");
        }
        final File out = new File(folder, file);
        picture.writeToFile(out);
        return out;
    }
}
