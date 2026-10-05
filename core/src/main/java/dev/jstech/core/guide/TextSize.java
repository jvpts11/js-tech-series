/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

/**
 * The sizes and weights text comes in on a manual's page, each with the height of its line.
 *
 * <p>The layout only needs the heights and a way to measure text in each; what each looks like (the font, the scale,
 * bold or not) is the screen's business.
 */
public enum TextSize {
    /** Running text. */
    BODY(10),
    /** Running text in bold: a problem, a step's number, a defined word. */
    BOLD(10),
    /** A heading inside an entry, and an entry's title. */
    HEADING(13),
    /** A page's big title: "Contents", a chapter's name. */
    TITLE(20),
    /** Small text: the header and the foot of a page, captions. */
    SMALL(8),
    /** Small text in bold: the "Figure 3-9." before a caption. */
    SMALL_BOLD(8),
    /** The table font, which lines figures up. */
    TABLE(11),
    /** A big letter heading a letter of the index. */
    LETTER(20),
    /** A chapter's number on its opening page, four times the size of running text. */
    CHAPTER(30);

    private final int lineHeight;

    TextSize(final int lineHeight) {
        this.lineHeight = lineHeight;
    }

    /** How far apart two lines of this size are, in pixels. */
    public int lineHeight() {
        return this.lineHeight;
    }
}
