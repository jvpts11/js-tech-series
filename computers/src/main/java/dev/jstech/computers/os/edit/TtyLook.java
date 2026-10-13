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
 * @param titleLeft    what the title row starts with, or empty for an editor with no title row
 * @param titleMiddle  what sits in the middle of it, usually the file
 * @param titleRight   what sits at its end, usually whether the file has been changed
 * @param status       how the row that talks is drawn
 * @param keys         the rows of keys under it, each the same number of columns; empty for none
 * @param page         lines shown in place of the file, such as a help text, each wrapped to the glass where it
 *                     is drawn; empty to show the file
 * @param marksTheEnd  whether the rows past the end of a short file are marked as not being there
 * @param keysOnTop    whether the rows of keys sit above the text instead of below it
 * @param plainInk     whether the file is drawn with no colouring even when a language claims it
 * @param positionLine a row of its own naming where the caret stands, drawn right under the keys; empty for none
 * @param bareKeys     whether a key's chord is written in bright ink on the glass rather than in a badge of its own
 * @param menu         a box drawn over the text, the file still showing round it; {@link Menu#NONE} for none
 */
public record TtyLook(String titleLeft, Text titleMiddle, Text titleRight, Status status,
                      List<List<Key>> keys, List<Text> page, boolean marksTheEnd,
                      boolean keysOnTop, boolean plainInk, Text positionLine, boolean bareKeys, Menu menu) {

    /** A line at the bottom and nothing else, which is what an editor that says nothing about itself gets. */
    public static final TtyLook PLAIN =
            new TtyLook("", Text.EMPTY, Text.EMPTY, Status.LINE, List.of(), List.of(), true);

    /**
     * The seven fields of an editor that keeps its keys under the text: coloured when a language claims the
     * file, and no row naming the caret's place.
     */
    public TtyLook(final String titleLeft, final Text titleMiddle, final Text titleRight, final Status status,
                  final List<List<Key>> keys, final List<Text> page, final boolean marksTheEnd) {
        this(titleLeft, titleMiddle, titleRight, status, keys, page, marksTheEnd, false, false, Text.EMPTY);
    }

    /**
     * An editor with keys on top or not, its own colours or none, and a row naming where the caret stands,
     * with a badge on every chord and no box over the text.
     */
    public TtyLook(final String titleLeft, final Text titleMiddle, final Text titleRight, final Status status,
                  final List<List<Key>> keys, final List<Text> page, final boolean marksTheEnd,
                  final boolean keysOnTop, final boolean plainInk, final Text positionLine) {
        this(titleLeft, titleMiddle, titleRight, status, keys, page, marksTheEnd, keysOnTop, plainInk, positionLine,
                false, Menu.NONE);
    }

    /** How the row that talks is drawn. */
    public enum Status {
        /** A line of its own colour with the caret's position at its end. */
        LINE,
        /** Something said in passing: in brackets, in the middle, and only as wide as what it says. */
        BRACKETED,
        /** A question being answered: a bar the whole width, read from the left. */
        BAR,
        /** Only what is said, on the glass's own ground, with nowhere set aside for the caret's place. */
        MESSAGE
    }

    /**
     * One key an editor lists, and what it does.
     *
     * @param chord how the key is written, {@code ^X} for Control held with X
     * @param does  what pressing it does, in a word or two
     */
    public record Key(String chord, Text does) {
    }

    /**
     * A box drawn over the text rather than in place of it, so the scroll and the caret under it stay
     * exactly as they were: a title across its top, and each row below it chosen by its letter, the arrows
     * or Enter.
     *
     * @param title    the row across its top
     * @param items    each row below it, in the letter order they are chosen by
     * @param selected which row the arrows or Enter would choose
     */
    public record Menu(Text title, List<Text> items, int selected) {

        /** No box drawn over the text. */
        public static final Menu NONE = new Menu(Text.EMPTY, List.of(), 0);

        /** Whether a box is drawn at all. */
        public boolean up() {
            return !this.items.isEmpty();
        }
    }

    /** Whether there is a title row to draw. */
    public boolean titled() {
        return !this.titleLeft.isEmpty();
    }
}
