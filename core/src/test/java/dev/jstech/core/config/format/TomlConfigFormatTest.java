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

class TomlConfigFormatTest {

    @Test
    void write_thenRead_givesTheSameValues() throws ConfigFormatException {
        final Map<String, Object> values = sample();

        final byte[] written = ConfigFormats.TOML.write(values, IConfigComments.NONE);

        assertEquals(values, ConfigFormats.TOML.read(written));
    }

    @Test
    void read_keepsTheOrderTheFileHas() throws ConfigFormatException {
        final Map<String, Object> read = ConfigFormats.TOML.read("zebra = 1\napple = 2\nmango = 3\n"
                .getBytes(StandardCharsets.UTF_8));

        assertEquals(List.of("zebra", "apple", "mango"), List.copyOf(read.keySet()));
    }

    @Test
    void write_putsEachCommentAboveWhatItDescribes() {
        final String written = new String(ConfigFormats.TOML.write(sample(), TomlConfigFormatTest::comments),
                StandardCharsets.UTF_8);
        final List<String> lines = written.lines().toList();

        assertEquals("# The top of the file", lines.get(0));
        final int speed = indexOfStarting(lines, "speed");
        assertTrue(lines.get(speed - 1).contains("How fast it goes"), written);
        final int section = indexOfStarting(lines, "[boot]");
        assertTrue(lines.get(section - 1).contains("Starting up"), written);
    }

    @Test
    void read_aBrokenFile_saysItIsNotToml() {
        assertThrows(ConfigFormatException.class,
                () -> ConfigFormats.TOML.read("speed = = 3\n[boot".getBytes(StandardCharsets.UTF_8)));
    }

    private static Map<String, Object> sample() {
        final Map<String, Object> boot = new LinkedHashMap<>();
        boot.put("show_menu", true);
        boot.put("wait", 3);
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put("speed", 512);
        values.put("factor", 0.6);
        values.put("bytes", 5_000_000_000L);
        values.put("name", "Frames \"95\"");
        values.put("words", List.of("a", "b"));
        values.put("boot", boot);
        return values;
    }

    private static List<String> comments(final List<String> path) {
        return switch (String.join(".", path)) {
            case "" -> List.of("The top of the file");
            case "speed" -> List.of("How fast it goes");
            case "boot" -> List.of("Starting up");
            default -> List.of();
        };
    }

    private static int indexOfStarting(final List<String> lines, final String start) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).strip().startsWith(start)) {
                return i;
            }
        }
        throw new AssertionError("no line starts with " + start + " in " + lines);
    }
}
