/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The marks a machine puts on the glass: the maker's, and each Frames edition's.
 *
 * <p>Textures rather than shapes drawn out of fills. A wordmark is a piece of lettering with weights, italics
 * and a face the game's own font does not have, so drawing one out of rectangles and the vanilla font gets the
 * words right and the thing itself wrong. These are the lockups from the approved mocks, rendered once and
 * blitted, which is also what a real machine did: a picture, not text.
 *
 * <p>Each is 256 by 64 with nothing but the lockup on it, so it sits on any ground: the sky one edition came up
 * on, the black another did, and the near-black the newest machines post against.
 */
public final class SplashLogos {

    /**
     * The size every lockup is, and the size every one of them is drawn at.
     *
     * <p>Never scaled, either way. A lockup is lettering, and lettering put through anything but a whole
     * multiple comes out as mush: the first of these were made at a size nobody drew them at, and the maker's
     * name above the word turned to a smear. Flat blocks may be halved; words may not.
     */
    public static final int W = 192;
    public static final int H = 48;

    /** Midsoft's mark for the oldest edition, for the sky it came up on. */
    public static final ResourceLocation FRAMES_95 =
            ResourceLocation.fromNamespaceAndPath("jsc", "textures/gui/splash/frames_95_logo.png");

    /** The same house's for the edition after it, for the black one. */
    public static final ResourceLocation FRAMES_XP =
            ResourceLocation.fromNamespaceAndPath("jsc", "textures/gui/splash/frames_xp_logo.png");

    /** The machines' own maker, for the newest edition's start and for every modern self-test. */
    public static final ResourceLocation JSC =
            ResourceLocation.fromNamespaceAndPath("jsc", "textures/gui/splash/jsc_logo.png");

    /** The window mark on its own, per edition, for the places an installer has room for a mark and no words. */
    public static final ResourceLocation FRAMES_95_MARK =
            ResourceLocation.fromNamespaceAndPath("jsc", "textures/gui/splash/frames_95_mark.png");

    public static final ResourceLocation FRAMES_XP_MARK =
            ResourceLocation.fromNamespaceAndPath("jsc", "textures/gui/splash/frames_xp_mark.png");

    public static final ResourceLocation FRAMES_11_MARK =
            ResourceLocation.fromNamespaceAndPath("jsc", "textures/gui/splash/frames_11_mark.png");

    /** The badge a board of the middle age wore in the corner of its self-test. */
    public static final ResourceLocation JSC_BADGE =
            ResourceLocation.fromNamespaceAndPath("jsc", "textures/gui/splash/jsc_badge.png");

    /** The side of a mark, which is square, and which may be halved because it is blocks and not words. */
    public static final int MARK = 16;

    /** The badge, which is not square and is never scaled either. */
    public static final int BADGE_W = 72;
    public static final int BADGE_H = 36;

    private SplashLogos() {
    }

    /** The mark of that edition, by the id its desktop and its registry go by. */
    public static ResourceLocation markOf(final String edition) {
        return switch (edition) {
            case "frames_95" -> FRAMES_95_MARK;
            case "frames_xp" -> FRAMES_XP_MARK;
            default -> FRAMES_11_MARK;
        };
    }

    /** Draws an edition's mark at {@code (x, y)}, {@code size} on a side. */
    public static void mark(final GuiGraphics g, final String edition, final int x, final int y,
                            final int size) {
        final int side = Math.max(1, size);
        g.blit(markOf(edition), x, y, side, side, 0.0F, 0.0F, MARK, MARK, MARK, MARK);
    }

    /** Draws the maker's badge with its top right corner at {@code (right, y)}, at the size it was made. */
    public static void badge(final GuiGraphics g, final int right, final int y) {
        g.blit(JSC_BADGE, right - BADGE_W, y, 0.0F, 0.0F, BADGE_W, BADGE_H, BADGE_W, BADGE_H);
    }

    /** Draws a lockup centred on {@code cx}, with its top at {@code top}, at the size it was made. */
    public static void draw(final GuiGraphics g, final ResourceLocation logo, final int cx, final int top) {
        g.blit(logo, cx - W / 2, top, 0.0F, 0.0F, W, H, W, H);
    }
}
