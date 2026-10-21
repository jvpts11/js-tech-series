/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ScreenFileLinesTest {

    private static final String FILE = String.join("\n",
            "{",
            "  \"width\": 176,",
            "  \"root\": {",
            "    \"type\": \"column\",",
            "    \"margin\": {\"top\": 2},",
            "    \"children\": [",
            "      {\"type\": \"title\"},",
            "      {",
            "        \"type\": \"row\", \"options\": [\"a, b\", \"c\"],",
            "        \"children\": [",
            "          {\"type\": \"label\", \"text\": \"a \\\"{quoted}\\\" word\"},",
            "          {\"type\": \"button\"}",
            "        ]",
            "      },",
            "      {\"type\": \"inventory\"}",
            "    ]",
            "  }",
            "}");

    @Test
    void of_findsTheRootAndEachChildByItsPlace() {
        final Map<List<Integer>, Integer> lines = ScreenFileLines.of(FILE);
        assertEquals(3, lines.get(List.of()));
        assertEquals(7, lines.get(List.of(0)));
        assertEquals(8, lines.get(List.of(1)));
        assertEquals(11, lines.get(List.of(1, 0)));
        assertEquals(12, lines.get(List.of(1, 1)));
        assertEquals(15, lines.get(List.of(2)));
    }

    @Test
    void of_takesNoObjectThatIsNoComponent() {
        final Map<List<Integer>, Integer> lines = ScreenFileLines.of(FILE);
        assertEquals(6, lines.size(), "the margin's object and the words in strings are no components: " + lines);
        assertFalse(lines.containsValue(5));
    }
}
