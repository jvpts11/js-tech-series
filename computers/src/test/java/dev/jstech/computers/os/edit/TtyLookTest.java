/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.text.Text;
import java.util.List;
import org.junit.jupiter.api.Test;

class TtyLookTest {

    @Test
    void sevenArgConstructor_keepsKeysBelowColourAndNoPositionRow() {
        final TtyLook look = new TtyLook("nano", Text.EMPTY, Text.EMPTY, TtyLook.Status.LINE, List.of(),
                List.of(), true);
        assertFalse(look.keysOnTop(), "keys sit under the text");
        assertFalse(look.plainInk(), "the file keeps its colours");
        assertTrue(look.positionLine().isEmpty(), "nothing names where the caret stands");
        assertFalse(look.bareKeys(), "a chord keeps its badge");
        assertFalse(look.menu().up(), "no box is drawn over the text");
    }

    @Test
    void plain_isTheLineStatusWithKeysBelowAndColour() {
        assertEquals(TtyLook.Status.LINE, TtyLook.PLAIN.status());
        assertFalse(TtyLook.PLAIN.keysOnTop());
        assertFalse(TtyLook.PLAIN.plainInk());
        assertFalse(TtyLook.PLAIN.menu().up());
    }

    @Test
    void menuNone_isNoBoxAtAll() {
        assertFalse(TtyLook.Menu.NONE.up());
        assertTrue(TtyLook.Menu.NONE.items().isEmpty());
    }

    @Test
    void titled_isFalseWithNoTitleLeft() {
        assertFalse(TtyLook.PLAIN.titled());
    }
}
