/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.edit;

import dev.jstech.core.text.Text;

import java.util.List;

/**
 * What a terminal editor draws round the text, which is most of what makes one recognisable at a glance.
 *
 * <p>Some draw nothing but a line at the bottom. Others keep a title across the top, say what they have to say
 * in brackets in the middle of a row, and spend two more rows listing their keys. An editor describes which
 * of those it is with one of these and the terminal draws it, so how the keys are read and how the glass is
 * laid out stay two separate things. What is words in it is text, read in the player's language where it is drawn.
 *
 * @param titleLeft   what the title row starts with, or empty for an editor with no title row
 * @param titleMiddle what sits in the middle of it, usually the file
 * @param titleRight  what sits at its end, usually whether the file has been changed
 * @param status      how the row that talks is drawn
 * @param keys        the rows of keys under it, each the same number of columns; empty for none
 * @param page        lines shown in place of the file, such as a help text, each wrapped to the glass where it
 *                    is drawn; empty to show the file
 * @param marksTheEnd whether the rows past the end of a short file are marked as not being there
 */
public record TtyLook(String titleLeft, Text titleMiddle, Text titleRight, Status status,
                      List<List<Key>> keys, List<Text> page, boolean marksTheEnd) {

    /** A line at the bottom and nothing else, which is what an editor that says nothing about itself gets. */
    public static final TtyLook PLAIN =
            new TtyLook("", Text.EMPTY, Text.EMPTY, Status.LINE, List.of(), List.of(), true);

    /** How the row that talks is drawn. */
    public enum Status {
        /** A line of its own colour with the caret's position at its end. */
        LINE,
        /** Something said in passing: in brackets, in the middle, and only as wide as what it says. */
        BRACKETED,
        /** A question being answered: a bar the whole width, read from the left. */
        BAR
    }

    /**
     * One key an editor lists, and what it does.
     *
     * @param chord how the key is written, {@code ^X} for Control held with X
     * @param does  what pressing it does, in a word or two
     */
    public record Key(String chord, Text does) {
    }

    /** Whether there is a title row to draw. */
    public boolean titled() {
        return !this.titleLeft.isEmpty();
    }
}
