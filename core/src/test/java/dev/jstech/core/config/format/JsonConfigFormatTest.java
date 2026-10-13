/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonConfigFormatTest {

    @Test
    void write_thenRead_givesTheSameValues() throws ConfigFormatException {
        final Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("master", 0.75);
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put("volumes", inner);
        values.put("muted", List.of("jsc:fan", "jsc:beep"));
        values.put("visual_cues", false);
        values.put("count", 7);

        assertEquals(values, ConfigFormats.JSON.read(ConfigFormats.JSON.write(values, IConfigComments.NONE)));
    }

    @Test
    void read_aNumber_isWholeOrAFractionAsTheFileWroteIt() throws ConfigFormatException {
        final Map<String, Object> read = ConfigFormats.JSON.read(
                "{\"small\": 5, \"big\": 5000000000, \"fraction\": 5.0}".getBytes(StandardCharsets.UTF_8));

        assertEquals(5, read.get("small"));
        assertEquals(5_000_000_000L, read.get("big"));
        assertEquals(5.0, read.get("fraction"));
    }

    @Test
    void write_carriesNoComments() {
        final String written = new String(ConfigFormats.JSON.write(Map.of("a", 1), path -> List.of("Never here")),
                StandardCharsets.UTF_8);

        assertFalse(written.contains("Never here"));
    }

    @Test
    void read_whatIsNotAnObject_isRefused() {
        assertThrows(ConfigFormatException.class,
                () -> ConfigFormats.JSON.read("[1, 2]".getBytes(StandardCharsets.UTF_8)));
    }
}
