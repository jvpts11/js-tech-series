/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonReaderTest {

    @Test
    void read_takesEveryKindOfValue() {
        final JsonReader.INode read = JsonReader.read(" {\"name\": \"ore\", \"count\": 12.5, \"on\": true, "
                + "\"off\": false, \"none\": null, \"list\": [1, [], {}]} ");
        final Map<String, JsonReader.INode> members = ((JsonReader.Fields) read).members();
        assertEquals(new JsonReader.Text("ore"), members.get("name"));
        assertEquals(new JsonReader.Num(12.5), members.get("count"));
        assertEquals(new JsonReader.Truth(true), members.get("on"));
        assertEquals(new JsonReader.Truth(false), members.get("off"));
        assertEquals(new JsonReader.Nothing(), members.get("none"));
        assertEquals(new JsonReader.Items(List.of(new JsonReader.Num(1), new JsonReader.Items(List.of()),
                new JsonReader.Fields(Map.of()))), members.get("list"));
        assertEquals(List.of("name", "count", "on", "off", "none", "list"), List.copyOf(members.keySet()));
    }

    @Test
    void read_unescapesTexts() {
        assertEquals(new JsonReader.Text("a\"b\\c/d\ne\tf" + (char) 0x03A3),
                JsonReader.read("\"a\\\"b\\\\c\\/d\\ne\\tf\\u03a3\""));
    }

    @Test
    void read_saysWhereATextStopsBeingJson() {
        assertEquals(5, assertThrows(JsonReader.NotJson.class, () -> JsonReader.read("[1, 2")).where());
        assertEquals(1, assertThrows(JsonReader.NotJson.class, () -> JsonReader.read("{]")).where());
        assertEquals(4, assertThrows(JsonReader.NotJson.class, () -> JsonReader.read("[1] x")).where());
        assertThrows(JsonReader.NotJson.class, () -> JsonReader.read("\"open"));
        assertThrows(JsonReader.NotJson.class, () -> JsonReader.read(""));
        assertThrows(JsonReader.NotJson.class, () -> JsonReader.read("tru"));
        assertThrows(JsonReader.NotJson.class, () -> JsonReader.read("1e"));
    }

    @Test
    void read_refusesATreeNestedDeeperThanItTakes() {
        final String deep = "[".repeat(JsonReader.DEEPEST + 1) + "]".repeat(JsonReader.DEEPEST + 1);
        assertThrows(JsonReader.NotJson.class, () -> JsonReader.read(deep));
        final String deepest = "[".repeat(JsonReader.DEEPEST) + "]".repeat(JsonReader.DEEPEST);
        assertTrue(JsonReader.read(deepest) instanceof JsonReader.Items);
    }
}
