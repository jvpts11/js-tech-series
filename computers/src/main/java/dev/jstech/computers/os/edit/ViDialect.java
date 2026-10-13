/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.edit;

import dev.jstech.core.text.Text;

/**
 * Which of the two systems' own {@code vi} is open: one engine underneath, two voices on top.
 *
 * <p>FreeBSD's is nvi, System V's is the original; a player who has met either at a real prompt should meet
 * the same words here. Everything the two disagree on is answered here, in one place, so the editor itself
 * never has to ask which system it is running on.
 */
public enum ViDialect {

    /** FreeBSD's {@code vi}, nvi: the file named bare, a colon, and the line the caret opened on. */
    NVI,

    /** UNIX System V's {@code vi}: the file in quotes, and the count read off it. */
    SYSTEM_V;

    /** What is said on opening a file that was already there. */
    public Text opened(final String name, final int lines, final int characters) {
        return this == NVI ? ViWords.NVI_UNMODIFIED.with(name) : ViWords.SYSV_OPENED.with(name, lines, characters);
    }

    /** What is said on opening a name that named nothing yet. */
    public Text openedNew(final String name) {
        return this == NVI ? ViWords.NVI_NEW_FILE.with(name) : ViWords.SYSV_NEW_FILE.with(name);
    }

    /** What is said once the file has been written. */
    public Text written(final String name, final int lines, final int characters) {
        return this == NVI ? ViWords.NVI_WRITTEN.with(name, lines, characters)
                : ViWords.SYSV_OPENED.with(name, lines, characters);
    }
}
