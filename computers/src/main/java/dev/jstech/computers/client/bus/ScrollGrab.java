/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.bus;

import dev.jstech.computers.gui.layout.BusLayout;

/**
 * A press on a scrollbar of the bus screens and the drag that follows it: the thumb goes where the pointer is, as it
 * does on any scrollbar. The bars are drawn by hand rather than as components, so each remembers where it was drawn
 * this frame, which is where a press is measured; a bar that was only drawn could not be taken hold of at all.
 */
final class ScrollGrab {

    private int x;
    private int top;
    private int height;
    private int content;
    private boolean held;

    /** A bar three pixels wide is hard to hit, so a press this far beside it still takes it. */
    private static final int SLOP = 2;
    /** The shortest a thumb is drawn, as {@link BusDraw#scrollbar} draws it. */
    private static final int MIN_THUMB = 8;

    /** The bar as drawn this frame: at {@code x}, from {@code top}, {@code height} tall, over {@code content}. */
    void drawn(final int x, final int top, final int height, final int content) {
        this.x = x;
        this.top = top;
        this.height = height;
        this.content = content;
    }

    /** Takes hold of the bar when the press is on it; false for a press elsewhere, or on a list that does not scroll. */
    boolean press(final double mx, final double my) {
        held = content > height && mx >= x - SLOP && mx < x + BusLayout.SCROLL_W + SLOP && my >= top
                && my < top + height;
        return held;
    }

    /** Whether the bar is held, so a drag moves it. */
    boolean held() {
        return held;
    }

    /** Lets go of the bar. */
    void release() {
        held = false;
    }

    /** The scroll that puts the thumb's middle under the pointer at {@code my}, kept within the list. */
    int scrollAt(final double my) {
        final int range = content - height;
        if (range <= 0) {
            return 0;
        }
        final int thumb = Math.max(MIN_THUMB, height * height / content);
        final int travel = Math.max(1, height - thumb);
        final long scroll = Math.round((my - top - thumb / 2.0) * range / travel);
        return (int) Math.max(0, Math.min(range, scroll));
    }
}
