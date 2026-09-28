/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.core.client.gui.logic.TextDocument;
import org.junit.jupiter.api.Test;

class TtyLineCountTest {

    @Test
    void of_withATrailingNewline_leavesTheEmptyLineOutOfTheCount() {
        final TextDocument doc = new TextDocument();
        doc.setText("one\ntwo\nthree\n");
        assertEquals(3, TtyLineCount.of(doc));
    }

    @Test
    void of_withNoTrailingNewline_countsTheLastLine() {
        final TextDocument doc = new TextDocument();
        doc.setText("one\ntwo\nthree");
        assertEquals(3, TtyLineCount.of(doc));
    }

    @Test
    void of_anEmptyFile_isNoLinesAtAll() {
        final TextDocument doc = new TextDocument();
        doc.setText("");
        assertEquals(0, TtyLineCount.of(doc));
    }
}
