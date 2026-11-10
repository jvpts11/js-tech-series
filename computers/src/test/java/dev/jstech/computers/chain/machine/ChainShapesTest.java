/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ChainShapesTest {

    @Test
    void layers_readsLayersFromTheBottomAndRowsFromTheFront() {
        assertEquals(List.of(List.of("ab", "cd"), List.of("#b", "ef")), ChainShapes.layers("ab,cd", "#b,ef"));
    }

    @Test
    void legend_readsEachLetterAndItsBlock() {
        assertEquals(Map.of('a', "item_outlet", 'b', "grinder_drum"),
                ChainShapes.legend("a=item_outlet, b=grinder_drum"));
    }

    @Test
    void legend_refusesWhatIsNoLetterAndBlock() {
        assertThrows(IllegalArgumentException.class, () -> ChainShapes.legend("ab=item_outlet"));
        assertThrows(IllegalArgumentException.class, () -> ChainShapes.legend("a"));
    }

    @Test
    void sound_takesAWholeBoxWithOneTerminal() {
        final Map<Character, String> legend = ChainShapes.legend("a=x,b=y");
        assertTrue(ChainShapes.sound(ChainShapes.layers("ab,ba", "a#,bb"), legend));
    }

    @Test
    void sound_refusesARaggedBoxAStrangeLetterOrNoTerminal() {
        final Map<Character, String> legend = ChainShapes.legend("a=x,b=y");
        assertFalse(ChainShapes.sound(ChainShapes.layers("ab,b", "a#,bb"), legend));
        assertFalse(ChainShapes.sound(ChainShapes.layers("ab,ba", "a#,bz"), legend));
        assertFalse(ChainShapes.sound(ChainShapes.layers("ab,ba", "ab,bb"), legend));
        assertFalse(ChainShapes.sound(ChainShapes.layers("#b,ba", "a#,bb"), legend));
    }
}
