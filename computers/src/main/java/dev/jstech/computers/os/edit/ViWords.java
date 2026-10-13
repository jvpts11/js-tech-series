/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.edit;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the two systems' own {@code vi} say at the bottom of the glass, each in its own voice.
 *
 * <p>Neither keeps a ruler in the corner the way a modern clone does: this line is the whole of what either
 * says, so it carries the file's name, whether it was there before, and how big it turned out to be. Read in
 * the player's language where it is drawn, as every word a terminal editor says here is.
 */
@TextHolder
public final class ViWords {

    /* nvi, FreeBSD's own: the name bare, a colon, and the line the caret stands on. */
    public static final TextKey NVI_UNMODIFIED = TextKey.of("jsc.vi.nvi.unmodified", "%s: unmodified: line 1");
    public static final TextKey NVI_NEW_FILE = TextKey.of("jsc.vi.nvi.new_file", "%s: new file: line 1");
    public static final TextKey NVI_WRITTEN = TextKey.of("jsc.vi.nvi.written", "%s: %s lines, %s characters");

    /* System V's own: the name in quotes, and the count read off the file. */
    public static final TextKey SYSV_OPENED = TextKey.of("jsc.vi.sysv.opened", "\"%s\" %s lines, %s characters");
    public static final TextKey SYSV_NEW_FILE = TextKey.of("jsc.vi.sysv.new_file", "\"%s\" [New file]");

    private ViWords() {
    }
}
