/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.help;

/**
 * The form a desktop's help program takes, each the one its system had: the same manuals and commands in every one.
 */
public enum HelpForm {
    /** Frames 95's Help Topics: tabs for Contents, Index and Find, a tree of books, and Display. */
    FRAMES_95,
    /** Frames 95's topic window, which Display opens: Help Topics and Back over the topic. */
    FRAMES_95_TOPIC,
    /** Frames XP's Help and Support Center: a blue band with the search, a row of buttons, the tree and the page. */
    FRAMES_XP,
    /** Frames 7's Help and Support: arrows and a search field over the tree and the page. */
    FRAMES_7,
    /** Frames 10's and 11's Get Help: one search field across the top, the tree, and the page on a card. */
    GET_HELP,
    /** KDE's Help Center: the tree in a grey pane and the page beside it. */
    KDE,
    /** GNOME's and Cinnamon's Help: no tree, a trail of where the page is, and topics as lists of links. */
    YELP,
    /** CDE's Help Viewer: a menu bar, the topic hierarchy above the page, and its four buttons under it. */
    CDE
}
