/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The marks a machine puts on the glass: the maker's, each Frames edition's, and FreeBSD's.
 *
 * <p>Textures rather than shapes drawn out of fills. A wordmark is a piece of lettering with weights, italics
 * and a face the game's own font does not have, so drawing one out of rectangles and the vanilla font gets the
 * words right and the thing itself wrong. These are the lockups from the approved mocks, rendered once and
 * blitted, which is also what a real machine did: a picture, not text.
 *
 * <p>Each is drawn at four times the size it is shown at, and shrunk to fit. A screen this size is not a grid
 * of chunky pixels: the game draws the whole interface at whatever scale the player set, so a picture made at
 * the size it occupies in the interface is stretched two, three or four times before anybody sees it, and
 * lettering stretched like that turns to mush. Made large and shrunk, it is sharp at every scale, which is the
 * way round that works for anything with letters in it.
 *
 * <p>Nothing here has a ground of its own, so each sits on whatever is behind it: the sky one edition came up
 * on, the black another did, and the near-black the newest machines post against.
 */
public final class SplashLogos {

    /** The size a lockup is shown at in the interface. */
    public static final int W = 192;
    public static final int H = 48;

    /** The size it is drawn at, which is that again four times over. */
    private static final int SUPERSAMPLE = 4;
    private static final int TEX_W = W * SUPERSAMPLE;
    private static final int TEX_H = H * SUPERSAMPLE;

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

    /** FreeBSD's: the red sphere with its two horns and the name under it, for the loader it counts down at. */
    public static final ResourceLocation FREEBSD =
            ResourceLocation.fromNamespaceAndPath("jsc", "textures/gui/splash/freebsd_lockup.png");

    /** The side a mark is shown at, and the side it is drawn at. */
    public static final int MARK = 16;
    private static final int MARK_TEX = MARK * SUPERSAMPLE;

    /** The badge, which is not square, shown and drawn. */
    public static final int BADGE_W = 72;
    public static final int BADGE_H = 36;
    private static final int BADGE_TEX_W = BADGE_W * SUPERSAMPLE;
    private static final int BADGE_TEX_H = BADGE_H * SUPERSAMPLE;

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
        final ResourceLocation mark = markOf(edition);
        smooth(mark);
        g.blit(mark, x, y, side, side, 0.0F, 0.0F, MARK_TEX, MARK_TEX, MARK_TEX, MARK_TEX);
    }

    /** Draws the maker's badge with its top right corner at {@code (right, y)}. */
    public static void badge(final GuiGraphics g, final int right, final int y) {
        smooth(JSC_BADGE);
        g.blit(JSC_BADGE, right - BADGE_W, y, BADGE_W, BADGE_H, 0.0F, 0.0F,
                BADGE_TEX_W, BADGE_TEX_H, BADGE_TEX_W, BADGE_TEX_H);
    }

    /** Draws a lockup centred on {@code cx}, with its top at {@code top}. */
    public static void draw(final GuiGraphics g, final ResourceLocation logo, final int cx, final int top) {
        draw(g, logo, cx, top, W, H);
    }

    /** The same at a size of its own, for the panels that have less room than a full self-test. */
    public static void draw(final GuiGraphics g, final ResourceLocation logo, final int cx, final int top,
                            final int width, final int height) {
        smooth(logo);
        g.blit(logo, cx - width / 2, top, width, height, 0.0F, 0.0F, TEX_W, TEX_H, TEX_W, TEX_H);
    }

    /**
     * Draws a lockup that is not the shape the rest are, with its top left corner at {@code (x, y)}. It is drawn
     * at four times the size given, like every other picture here.
     */
    public static void at(final GuiGraphics g, final ResourceLocation logo, final int x, final int y,
                          final int width, final int height) {
        smooth(logo);
        g.blit(logo, x, y, width, height, 0.0F, 0.0F, width * SUPERSAMPLE, height * SUPERSAMPLE,
                width * SUPERSAMPLE, height * SUPERSAMPLE);
    }

    /**
     * Asks for that texture to be sampled smoothly rather than by nearest pixel.
     *
     * <p>Shrinking a picture by picking one pixel out of every four throws away three quarters of the letters,
     * which is the same ruin as stretching a small one, arrived at from the other side. The game's interface
     * textures are nearest by default because most of them are pixel art drawn at the size they are shown;
     * these are not, so they ask for the other.
     */
    private static void smooth(final ResourceLocation texture) {
        final AbstractTexture loaded = Minecraft.getInstance().getTextureManager().getTexture(texture);
        loaded.setFilter(true, false);
    }
}
