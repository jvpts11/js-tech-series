/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeoBonesTest {

    @Test
    void ofModel_readsTheBonesOfEveryGeometry() {
        final JsonObject model = json("{\"minecraft:geometry\":[{\"bones\":[{\"name\":\"cabinet\"},"
                + "{\"name\":\"fan\"}]},{\"bones\":[{\"name\":\"door\"}]}]}");
        assertEquals(List.of("cabinet", "fan", "door"), GeoBones.ofModel(model));
    }

    @Test
    void ofModel_readsNoBonesFromAFileOfAnotherForm() {
        assertEquals(List.of(), GeoBones.ofModel(json("{\"minecraft:geometry\":{\"bones\":[]}}")));
        assertEquals(List.of(), GeoBones.ofModel(json("{\"minecraft:geometry\":[{\"bones\":[{\"pivot\":[0]}]}]}")));
    }

    @Test
    void ofAnimations_readsEachBoneEachAnimationMovesByItsShortName() {
        final JsonObject file = json("{\"animations\":{\"animation.press.work\":{\"bones\":{\"fan_blade\":{},"
                + "\"piston\":{}}},\"animation.press.jammed\":{\"bones\":{\"lamp\":{}}}}}");
        assertEquals(List.of(new GeoBones.Moved("work", "fan_blade"), new GeoBones.Moved("work", "piston"),
                new GeoBones.Moved("jammed", "lamp")), GeoBones.ofAnimations(file));
    }

    @Test
    void ofAnimations_skipsAnAnimationThatMovesNoBone() {
        final JsonObject file = json("{\"animations\":{\"animation.press.still\":{\"loop\":true}}}");
        assertEquals(List.of(), GeoBones.ofAnimations(file));
    }

    private static JsonObject json(final String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }
}
