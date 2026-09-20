/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.program.SolitaireGame;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Draws a playing card, and the four suit marks, at the size a card is on a computer's screen here.
 *
 * <p>Its own class because a card is a picture rather than a rule, and because the suits are drawn out of
 * plain rectangles: the game's font has no pip in it, and a letter where a suit belongs reads as a mistake
 * rather than as a card. Every mark is built from the same few fills, so they all sit on the pixel grid and
 * none of them is resampled.
 */
public final class PlayingCards {

    /** How big a card is drawn, which the whole of the solitaire layout is measured in. */
    public static final int WIDTH = 20;
    public static final int HEIGHT = 28;

    private static final int FACE = 0xFFFAF8F0;
    private static final int EDGE = 0xFF2B2B2B;
    private static final int RED = 0xFFB4231F;
    private static final int BLACK = 0xFF1A1A1A;
    private static final int BACK_DARK = 0xFF24427A;
    private static final int BACK_LIGHT = 0xFF2B4E8C;
    private static final int BACK_EDGE = 0xFF14264A;
    private static final int EMPTY_EDGE = 0x66FFFFFF;
    private static final int EMPTY_FILL = 0x24000000;

    private PlayingCards() {
    }

    /** The colour a suit is printed in. */
    public static int inkOf(final SolitaireGame.Suit suit) {
        return suit.red() ? RED : BLACK;
    }

    /** The one or two letters a rank is written with, which is how a card names itself in a corner. */
    public static String rankLabel(final int rank) {
        return switch (rank) {
            case 1 -> "A";
            case 11 -> "J";
            case 12 -> "Q";
            case 13 -> "K";
            default -> String.valueOf(rank);
        };
    }

    /** A card lying face up: its rank in the corner and its suit beside and below it. */
    public static void face(final GuiGraphics g, final Font font, final int x, final int y,
                            final SolitaireGame.Card card) {
        g.fill(x, y, x + WIDTH, y + HEIGHT, EDGE);
        g.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, FACE);
        final int ink = inkOf(card.suit());
        final String label = rankLabel(card.rank());
        g.drawString(font, label, x + 2, y + 2, ink, false);
        suit(g, card.suit(), x + WIDTH - 9, y + 2);
        // The big mark in the middle is what a card is recognised by across a table of them.
        suit(g, card.suit(), x + (WIDTH - 7) / 2, y + HEIGHT - 12);
    }

    /**
     * A card lying face up but mostly covered by the one below it, as a tableau run is.
     *
     * <p>Only the top strip shows, so it carries the rank and the small suit and nothing else.
     */
    /** The shortest strip that can still carry a rank, which is the game's own reason to stack this close. */
    public static final int MIN_READABLE_STRIP = 9;

    public static void faceStrip(final GuiGraphics g, final Font font, final int x, final int y,
                                 final int visible, final SolitaireGame.Card card) {
        final int h = Math.max(3, visible);
        g.fill(x, y, x + WIDTH, y + h, EDGE);
        g.fill(x + 1, y + 1, x + WIDTH - 1, y + h, FACE);
        /*
         * A strip too short for a rank is drawn blank rather than with a letter sliced in half. The
         * stacking step is chosen to stay above this, so in an ordinary game every card in a run is
         * readable; this is only what happens when a pile grows longer than the felt is tall.
         */
        if (h < MIN_READABLE_STRIP) {
            return;
        }
        final int ink = inkOf(card.suit());
        g.drawString(font, rankLabel(card.rank()), x + 2, y + 2, ink, false);
        suit(g, card.suit(), x + WIDTH - 9, y + 2);
    }

    /** A card lying face down, in the pattern every back in the deck shares. */
    public static void back(final GuiGraphics g, final int x, final int y, final int visible) {
        final int h = Math.max(3, visible);
        g.fill(x, y, x + WIDTH, y + h, BACK_EDGE);
        g.fill(x + 1, y + 1, x + WIDTH - 1, y + h, BACK_DARK);
        // A diagonal weave, drawn as short steps, which is what a patterned back looks like this small.
        for (int row = 1; row < h - 1; row++) {
            for (int col = 1 + ((row + 1) % 3); col < WIDTH - 1; col += 3) {
                g.fill(x + col, y + row, x + col + 1, y + row + 1, BACK_LIGHT);
            }
        }
    }

    /** An empty place a card may be put, drawn as an outline so it reads as a slot and not as a card. */
    public static void empty(final GuiGraphics g, final int x, final int y) {
        g.fill(x, y, x + WIDTH, y + HEIGHT, EMPTY_FILL);
        g.fill(x, y, x + WIDTH, y + 1, EMPTY_EDGE);
        g.fill(x, y + HEIGHT - 1, x + WIDTH, y + HEIGHT, EMPTY_EDGE);
        g.fill(x, y, x + 1, y + HEIGHT, EMPTY_EDGE);
        g.fill(x + WIDTH - 1, y, x + WIDTH, y + HEIGHT, EMPTY_EDGE);
    }

    /** An empty foundation, which says which suit belongs on it. */
    public static void emptyFoundation(final GuiGraphics g, final int x, final int y,
                                       final SolitaireGame.Suit suit) {
        empty(g, x, y);
        suitTinted(g, suit, x + (WIDTH - 7) / 2, y + (HEIGHT - 7) / 2);
    }

    /** The stock with nothing left in it, which is the place a player clicks to go round again. */
    public static void emptyStock(final GuiGraphics g, final int x, final int y) {
        empty(g, x, y);
        final int cx = x + WIDTH / 2;
        final int cy = y + HEIGHT / 2;
        // A ring, for the turn back to the beginning.
        g.fill(cx - 4, cy - 4, cx + 4, cy - 3, EMPTY_EDGE);
        g.fill(cx - 4, cy + 3, cx + 4, cy + 4, EMPTY_EDGE);
        g.fill(cx - 4, cy - 3, cx - 3, cy + 3, EMPTY_EDGE);
        g.fill(cx + 3, cy - 3, cx + 4, cy + 3, EMPTY_EDGE);
    }

    /** One suit mark, seven pixels across, in that suit's own colour. */
    public static void suit(final GuiGraphics g, final SolitaireGame.Suit suit, final int x, final int y) {
        mark(g, suit, x, y, inkOf(suit));
    }

    /** The same mark, faint, for an empty place that is only saying what belongs there. */
    private static void suitTinted(final GuiGraphics g, final SolitaireGame.Suit suit,
                                   final int x, final int y) {
        mark(g, suit, x, y, suit.red() ? 0x80B4231F : 0x80FFFFFF);
    }

    private static void mark(final GuiGraphics g, final SolitaireGame.Suit suit,
                             final int x, final int y, final int colour) {
        switch (suit) {
            case DIAMONDS -> diamond(g, x, y, colour);
            case HEARTS -> heart(g, x, y, colour);
            case SPADES -> spade(g, x, y, colour);
            case CLUBS -> club(g, x, y, colour);
        }
    }

    /* Each shape is a short table of spans: the row, where it starts and how wide, so the pixels line up. */

    private static void diamond(final GuiGraphics g, final int x, final int y, final int c) {
        span(g, x, y, 0, 3, 1, c);
        span(g, x, y, 1, 2, 3, c);
        span(g, x, y, 2, 1, 5, c);
        span(g, x, y, 3, 0, 7, c);
        span(g, x, y, 4, 1, 5, c);
        span(g, x, y, 5, 2, 3, c);
        span(g, x, y, 6, 3, 1, c);
    }

    private static void heart(final GuiGraphics g, final int x, final int y, final int c) {
        span(g, x, y, 0, 1, 2, c);
        span(g, x, y, 0, 4, 2, c);
        span(g, x, y, 1, 0, 7, c);
        span(g, x, y, 2, 0, 7, c);
        span(g, x, y, 3, 1, 5, c);
        span(g, x, y, 4, 2, 3, c);
        span(g, x, y, 5, 3, 1, c);
    }

    private static void spade(final GuiGraphics g, final int x, final int y, final int c) {
        span(g, x, y, 0, 3, 1, c);
        span(g, x, y, 1, 2, 3, c);
        span(g, x, y, 2, 1, 5, c);
        span(g, x, y, 3, 0, 7, c);
        span(g, x, y, 4, 0, 7, c);
        span(g, x, y, 5, 3, 1, c);
        span(g, x, y, 6, 2, 3, c);
    }

    private static void club(final GuiGraphics g, final int x, final int y, final int c) {
        span(g, x, y, 0, 2, 3, c);
        span(g, x, y, 1, 2, 3, c);
        span(g, x, y, 2, 0, 7, c);
        span(g, x, y, 3, 0, 7, c);
        span(g, x, y, 4, 2, 3, c);
        span(g, x, y, 5, 3, 1, c);
        span(g, x, y, 6, 2, 3, c);
    }

    private static void span(final GuiGraphics g, final int x, final int y,
                             final int row, final int from, final int width, final int colour) {
        g.fill(x + from, y + row, x + from + width, y + row + 1, colour);
    }
}
