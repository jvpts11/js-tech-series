/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScreenSchemaTest {

    private static final List<ScreenSchema.Kind> KINDS = List.of(
            new ScreenSchema.Kind("button", List.of("text", "action"), false),
            new ScreenSchema.Kind("column", List.of(), true),
            new ScreenSchema.Kind("mymod:dial", List.of("bind", "max"), false));

    @Test
    void of_namesEveryKindAComponentMayBe() {
        final JsonObject element = element(ScreenSchema.of(KINDS, List.of("width", "grow"), List.of("gap")));
        final JsonArray kinds = element.getAsJsonObject("properties").getAsJsonObject("type").getAsJsonArray("enum");
        assertEquals(List.of("button", "column", "mymod:dial"), kinds.asList().stream()
                .map(name -> name.getAsString()).toList());
        assertEquals("type", element.getAsJsonArray("required").get(0).getAsString());
    }

    @Test
    void of_givesEveryComponentTheFieldsEveryOneHasAndWhatItHolds() {
        final JsonObject fields = element(ScreenSchema.of(KINDS, List.of("width", "grow"), List.of("gap")))
                .getAsJsonObject("properties");
        assertTrue(fields.has("width") && fields.has("grow") && fields.has("gap"));
        assertEquals("#/definitions/element", fields.getAsJsonObject("children").getAsJsonObject("items")
                .get("$ref").getAsString());
    }

    @Test
    void of_givesAKindItsOwnFieldsOnceItsTypeIsWritten() {
        final JsonArray each = element(ScreenSchema.of(KINDS, List.of(), List.of())).getAsJsonArray("allOf");
        final JsonObject dial = each.get(2).getAsJsonObject();
        assertEquals("mymod:dial", dial.getAsJsonObject("if").getAsJsonObject("properties")
                .getAsJsonObject("type").get("const").getAsString());
        final JsonObject own = dial.getAsJsonObject("then").getAsJsonObject("properties");
        assertTrue(own.has("bind") && own.has("max") && !own.has("text"));
    }

    @Test
    void of_needsAScreensRootAndLetsItNameItsSchema() {
        final JsonObject schema = ScreenSchema.of(KINDS, List.of(), List.of());
        assertEquals(ScreenSchema.DRAFT, schema.get("$schema").getAsString());
        assertEquals("root", schema.getAsJsonArray("required").get(0).getAsString());
        assertTrue(schema.getAsJsonObject("properties").has("$schema"), "a screen's file may name its schema");
    }

    private static JsonObject element(final JsonObject schema) {
        return schema.getAsJsonObject("definitions").getAsJsonObject("element");
    }
}
