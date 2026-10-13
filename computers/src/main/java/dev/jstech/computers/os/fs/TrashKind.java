/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * How a desktop keeps what was deleted on it: each family of systems has its own habit, and its own name for
 * the place.
 *
 * <p>The Frames desktops put a folder called {@code RECYCLER} at the root of the disk, give every file in it a
 * serial name, and write down in one index where each came from. The desktops of Linux and FreeBSD follow the
 * freedesktop.org habit: a folder in the home with the files themselves in {@code files} and one small note per
 * file in {@code info}. CDE keeps a folder of its own in the home, with the files under their names and one index
 * beside them.
 */
@TextHolder
public enum TrashKind {

    /** Frames: the Recycle Bin, at the root of the system disk. */
    RECYCLER(TextKey.of("jsc.trash.recycle_bin", "Recycle Bin")),

    /** The Linux and FreeBSD desktops: the Trash, in the home. */
    FREEDESKTOP(TextKey.of("jsc.trash.trash", "Trash")),

    /** CDE, on whichever system runs it: the Trash Can, in the home. */
    CDE(TextKey.of("jsc.trash.trash_can", "Trash Can"));

    private final TextKey title;

    TrashKind(final TextKey title) {
        this.title = title;
    }

    /** The habit of a desktop, by whether its system is a Unix and whether the desktop is CDE. */
    public static TrashKind of(final boolean unix, final boolean cde) {
        if (cde) {
            return CDE;
        }
        return unix ? FREEDESKTOP : RECYCLER;
    }

    /** What the desktops of this habit call the place, in English: the name its window is known by. */
    public String title() {
        return this.title.english();
    }

    /** What the desktops of this habit call the place, as the player reads it. */
    public Text titleText() {
        return this.title.text();
    }
}
