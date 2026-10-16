/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.connect.texture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CtmTexturesTest {

    @Test
    void ofMeta_readsAConnectedGlass() {
        final CtmTexture glass = CtmTextures.ofMeta(json("{\"ctm\": {\"ctm_version\": 1, \"type\": \"ctm:ctm\","
                + " \"layer\": \"cutout\", \"textures\": [\"block/glass-ctm\"]}}")).orElseThrow();
        assertEquals(CtmTexture.Type.CTM, glass.type(), "the type read with CTM's namespace");
        assertEquals(Optional.of("CUTOUT"), glass.layer());
        assertEquals(List.of("block/glass-ctm"), glass.textures());
        assertFalse(glass.joinsInside());
    }

    @Test
    void ofMeta_isNothingForATextureWithoutASection() {
        assertTrue(CtmTextures.ofMeta(json("{\"animation\": {\"frametime\": 2}}")).isEmpty());
    }

    @Test
    void of_readsEveryNameOfEveryType() {
        assertEquals(CtmTexture.Type.HORIZONTAL, type("ctmh"));
        assertEquals(CtmTexture.Type.HORIZONTAL, type("ctm_horizontal"));
        assertEquals(CtmTexture.Type.VERTICAL, type("ctm_vertical"));
        assertEquals(CtmTexture.Type.RANDOM, type("r"));
        assertEquals(CtmTexture.Type.PATTERN, type("v"));
        assertEquals(CtmTexture.Type.SIMPLE, type("sctm"));
        assertEquals(CtmTexture.Type.NORMAL, CtmTextures.of(json("{}")).type(), "no type is a plain texture");
        assertTrue(CtmTextures.of(json("{\"type\": \"sctm\"}")).joinsInside(), "the simple type joins inside");
    }

    @Test
    void of_readsTheExtras() {
        final CtmTexture texture = CtmTextures.of(json("{\"type\": \"pattern\", \"extra\": {\"size\": 4,"
                + " \"light\": {\"block\": 9, \"sky\": 20}, \"ignore_states\": true, \"connect_inside\": true,"
                + " \"x_offset\": 1}}"));
        assertEquals(new CtmTexture.Grid(4, 4, 1, 0), texture.grid());
        assertEquals(Optional.of(new CtmTexture.Light(9, 15)), texture.light(), "sky light at most 15");
        assertTrue(texture.ignoreStates());
        assertTrue(texture.joinsInside());
        assertEquals(new CtmTexture.Grid(3, 2, 0, 0), CtmTextures.of(json("{\"type\": \"random\","
                + " \"extra\": {\"width\": 3, \"height\": 2}}")).grid());
        assertEquals(Optional.of(new CtmTexture.Light(12, 12)), CtmTextures.of(json("{\"extra\": {\"light\": 12}}"))
                .light());
    }

    @Test
    void of_readsWhatATextureJoins() {
        final CtmTexture halo = CtmTextures.of(json("{\"type\": \"edges\", \"textures\": [\"a\", \"b\"],"
                + " \"extra\": {\"connect_to\": [{\"block\": \"lava\"}, {\"tag\": \"c:glass_blocks\"},"
                + " {\"block\": \"water\", \"predicate\": {\"level\": \"8\", \"compare_func\": \"<\"}}]}}"));
        final CtmTexture.ConnectTo to = halo.connectTo().orElseThrow();
        assertEquals("minecraft:lava", to.conditions().getFirst().block());
        assertTrue(to.conditions().get(1).tag());
        final CtmTexture.Predicate level = to.conditions().get(2).predicates().getFirst();
        assertTrue(level.meets("3"));
        assertFalse(level.meets("9"), "the levels compare as numbers");
    }

    @Test
    void of_readsFacesOfTheirOwn() {
        final CtmTexture texture = CtmTextures.of(json("{\"type\": \"ctm\", \"textures\": [\"x\"], \"extra\":"
                + " {\"connect_to\": {\"default\": [{\"block\": \"stone\"}], \"UP\": {\"block\": \"dirt\","
                + " \"defer\": \"or\"}}}}"));
        final CtmTexture.ConnectTo to = texture.connectTo().orElseThrow();
        assertEquals("minecraft:stone", to.conditions().getFirst().block());
        assertEquals(Optional.of("or"), to.sides().get("up").defer());
        assertEquals("minecraft:dirt", to.sides().get("up").conditions().getFirst().block());
    }

    @Test
    void overridesOf_readsAModelsOverridesByTintIndex() {
        final Map<Integer, CtmTexture> overrides = CtmTextures.overridesOf(json("{\"ctm_version\": 1,"
                + " \"ctm_overrides\": {\"0\": {\"type\": \"pattern\", \"extra\": {\"size\": 4}},"
                + " \"2\": {\"type\": \"ctm\", \"textures\": [\"block/slab_ctm\"], \"texture\": \"block/slab\"}}}"));
        assertEquals(CtmTexture.Type.PATTERN, overrides.get(0).type());
        assertEquals(Optional.of("block/slab"), overrides.get(2).texture());
        assertTrue(CtmTextures.overridesOf(json("{\"parent\": \"block/cube_all\"}")).isEmpty());
    }

    @Test
    void of_refusesWhatDoesNotRead() {
        assertThrows(IllegalArgumentException.class, () -> CtmTextures.of(json("{\"type\": \"spiral\"}")));
        assertThrows(IllegalArgumentException.class, () -> CtmTextures.of(json("{\"ctm_version\": 2}")));
        assertThrows(IllegalArgumentException.class, () -> CtmTextures.of(json("{\"type\": \"ctm\"}")),
                "the full type takes a companion");
    }

    private static CtmTexture.Type type(final String word) {
        return CtmTextures.of(json("{\"type\": \"" + word + "\", \"textures\": [\"a\", \"b\"]}")).type();
    }

    private static JsonObject json(final String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }
}
