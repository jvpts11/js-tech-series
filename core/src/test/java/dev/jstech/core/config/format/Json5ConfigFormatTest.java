/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Json5ConfigFormatTest {

    @Test
    void write_thenRead_givesTheSameValues() throws ConfigFormatException {
        final Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("show menu", true);
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put("speed", 512);
        values.put("bytes", 5_000_000_000L);
        values.put("factor", 0.6);
        values.put("name", "line one\nline \"two\"\t\\");
        values.put("words", List.of("a", "b"));
        values.put("empty", List.of());
        values.put("boot", inner);

        final byte[] written = ConfigFormats.JSON5.write(values, path -> List.of("About " + path));

        assertEquals(values, ConfigFormats.JSON5.read(written));
    }

    @Test
    void read_takesEverythingJson5Allows() throws ConfigFormatException {
        final String text = """
                // a comment
                {
                  /* a block
                     comment */
                  bare: 'single quoted',
                  "quoted": "with \\x41 and \\u0042",
                  hex: 0x1F,
                  plus: +3,
                  leading: .5,
                  trailing: 5.,
                  big: Infinity,
                  small: -Infinity,
                  odd: NaN,
                  split: "one \\
                two",
                  list: [1, 2, 3,],
                }
                """;

        final Map<String, Object> read = ConfigFormats.JSON5.read(text.getBytes(StandardCharsets.UTF_8));

        assertEquals("single quoted", read.get("bare"));
        assertEquals("with A and B", read.get("quoted"));
        assertEquals(31, read.get("hex"));
        assertEquals(3, read.get("plus"));
        assertEquals(0.5, read.get("leading"));
        assertEquals(5.0, read.get("trailing"));
        assertEquals(Double.POSITIVE_INFINITY, read.get("big"));
        assertEquals(Double.NEGATIVE_INFINITY, read.get("small"));
        assertTrue(((Double) read.get("odd")).isNaN());
        assertEquals("one two", read.get("split"));
        assertEquals(List.of(1, 2, 3), read.get("list"));
    }

    @Test
    void write_putsEachCommentAboveItsKeyAndQuotesOnlyWhatNeedsIt() {
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put("plain_name", 1);
        values.put("a name with spaces", 2);

        final String written = new String(ConfigFormats.JSON5.write(values,
                path -> path.isEmpty() ? List.of("Top") : List.of("About " + path.get(0))), StandardCharsets.UTF_8);
        final List<String> lines = written.lines().toList();

        assertEquals("// Top", lines.get(0));
        assertTrue(written.contains("  // About plain_name\n  plain_name: 1"), written);
        assertTrue(written.contains("\"a name with spaces\": 2"), written);
    }

    @Test
    void read_aMistake_saysTheLineItIsOn() {
        final ConfigFormatException mistake = assertThrows(ConfigFormatException.class,
                () -> ConfigFormats.JSON5.read("{\n  a: 1,\n  b 2\n}".getBytes(StandardCharsets.UTF_8)));

        assertTrue(mistake.getMessage().contains("line 3"), mistake.getMessage());
    }

    @Test
    void read_textBrokenOverALine_isRefused() {
        assertThrows(ConfigFormatException.class,
                () -> ConfigFormats.JSON5.read("{a: \"one\ntwo\"}".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void read_listsNestedThousandsDeep_areRefusedRatherThanOverflowingTheStack() {
        final String deep = "{a: " + "[".repeat(20_000) + "]".repeat(20_000) + "}";
        assertThrows(ConfigFormatException.class,
                () -> ConfigFormats.JSON5.read(deep.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void read_nestingUpToTheLimit_isRead() throws ConfigFormatException {
        final int levels = IConfigFormat.DEEPEST_NESTING - 1;
        final String nested = "{a: " + "[".repeat(levels) + "]".repeat(levels) + "}";
        assertTrue(ConfigFormats.JSON5.read(nested.getBytes(StandardCharsets.UTF_8)).containsKey("a"));
    }
}
