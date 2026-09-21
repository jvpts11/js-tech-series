/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.edit;

import dev.jstech.core.client.gui.logic.TextDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** One pass of search-and-replace: once round the file from the cursor, and never over what it put in. */
class NanoReplaceTest {

    private static TextDocument holding(final String text, final int line, final int col) {
        final TextDocument doc = new TextDocument();
        doc.setText(text);
        doc.setCursor(line, col);
        return doc;
    }

    @Test
    void all_replacesEveryPlaceInTheFile() {
        final TextDocument doc = holding("-j1 here\nand -j1 there\n-j1", 0, 0);
        final NanoReplace pass = new NanoReplace(doc, "-j1", "-j4");
        assertTrue(pass.next());
        pass.all();
        assertEquals("-j4 here\nand -j4 there\n-j4", doc.text());
        assertEquals(3, pass.done());
    }

    @Test
    void next_startsAtTheCursorAndComesRoundToWhatIsBeforeIt() {
        final TextDocument doc = holding("one\none\none", 1, 0);
        final NanoReplace pass = new NanoReplace(doc, "one", "two");
        assertTrue(pass.next());
        assertEquals(1, doc.cursorLine(), "the first place is the one the cursor is at");
        pass.replace();
        assertTrue(pass.next());
        assertEquals(2, doc.cursorLine());
        pass.skip();
        assertTrue(pass.next());
        assertEquals(0, doc.cursorLine(), "and the last is the one before where it began");
        pass.replace();
        assertFalse(pass.next(), "once round and no further");
        assertEquals("two\ntwo\none", doc.text());
        assertEquals(2, pass.done());
    }

    @Test
    void all_aReplacementThatHoldsWhatWasSearchedFor_stillEnds() {
        final TextDocument doc = holding("a a\na", 0, 2);
        final NanoReplace pass = new NanoReplace(doc, "a", "aa");
        assertTrue(pass.next());
        pass.all();
        assertEquals("aa aa\naa", doc.text());
        assertEquals(3, pass.done());
    }

    @Test
    void next_whatStartsBeforeTheCursorOnItsOwnLine_isTheLastStretch() {
        final TextDocument doc = holding("x y x", 0, 2);
        final NanoReplace pass = new NanoReplace(doc, "x", "long");
        assertTrue(pass.next());
        assertEquals(4, doc.cursorCol(), "after the cursor first");
        pass.replace();
        assertTrue(pass.next());
        assertEquals(0, doc.cursorCol(), "then round to what was before it");
        pass.replace();
        assertFalse(pass.next());
        assertEquals("long y long", doc.text());
    }

    @Test
    void next_nothingToFind_isFalseAtOnceAndChangesNothing() {
        final TextDocument doc = holding("nothing of the kind", 0, 0);
        final NanoReplace pass = new NanoReplace(doc, "zebra", "horse");
        assertFalse(pass.next());
        assertFalse(new NanoReplace(doc, "", "anything").next(), "and searching for nothing finds nothing");
        assertEquals("nothing of the kind", doc.text());
        assertEquals(0, pass.done());
    }
}
