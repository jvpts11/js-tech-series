/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class BedrockReaderTest {

    private static final double EPSILON = 1.0e-5;

    @Test
    void geometry_readsEveryModelOfTheSeriesAsItIs() {
        final List<String> wrong = new ArrayList<>();
        final List<Path> files = filesEnding(".geo.json");
        assertTrue(files.size() > 400, "the series' models are found: " + files.size());
        for (final Path file : files) {
            try {
                final List<ModelData> models = BedrockReader.geometry(file.getFileName().toString(), json(file));
                assertFalse(models.getFirst().bones().isEmpty(), file + " has bones");
            } catch (final RuntimeException broken) {
                wrong.add(file + ": " + broken.getMessage());
            }
        }
        if (!wrong.isEmpty()) {
            fail("models that do not read: " + wrong);
        }
    }

    @Test
    void animations_readEveryAnimationOfTheSeriesAsItIs() {
        final List<String> wrong = new ArrayList<>();
        final List<Path> files = filesEnding(".animation.json");
        assertFalse(files.isEmpty(), "the series' animations are found");
        int animations = 0;
        for (final Path file : files) {
            try {
                // A file of no animations yet, for a model that does not move, reads as none.
                animations += BedrockReader.animations(file.toString(), json(file)).size();
            } catch (final RuntimeException broken) {
                wrong.add(file + ": " + broken.getMessage());
            }
        }
        if (!wrong.isEmpty()) {
            fail("animations that do not read: " + wrong);
        }
        assertTrue(animations > 0, "and they hold animations");
    }

    @Test
    void geometry_turnsBedrocksXRoundIntoTheCoresSpace() {
        final JsonObject file = JsonParser.parseString("""
                {"format_version": "1.12.0", "minecraft:geometry": [{
                  "description": {"identifier": "geometry.test", "texture_width": 32, "texture_height": 16},
                  "bones": [
                    {"name": "root", "pivot": [2, 0, 0], "rotation": [10, 20, 30]},
                    {"name": "arm", "parent": "root", "pivot": [4, 8, 0],
                     "cubes": [{"origin": [1, 2, 3], "size": [4, 5, 6], "uv": [0, 0]}],
                     "locators": {"tip": [1, 2, 3]}}
                  ]}]}
                """).getAsJsonObject();
        final ModelData model = BedrockReader.geometry("test", file).getFirst();
        assertEquals("geometry.test", model.name());
        assertEquals(32, model.textureWidth());
        final ModelBone root = model.bone("root").orElseThrow();
        assertEquals(-2, root.pivot().x(), EPSILON, "a pivot's X the other way");
        assertEquals(-10, root.euler().x(), EPSILON, "a turn about X the other way");
        assertEquals(-20, root.euler().y(), EPSILON, "and about Y");
        assertEquals(30, root.euler().z(), EPSILON, "but not about Z");
        final ModelCube cube = model.bone("arm").orElseThrow().cubes().getFirst();
        assertEquals(-5, cube.from().x(), EPSILON, "a cube's lowest X is its far one turned round");
        assertEquals(6, cube.faces().size(), "box UV gives every face a picture");
        assertEquals(6, cube.faces().get(CubeFace.EAST).width(), EPSILON, "east as wide as the cube is deep");
        assertEquals(-1, model.locators().get("tip").offset().x(), EPSILON);
        assertEquals("root", model.childrenOf(null).getFirst().name());
    }

    @Test
    void animations_readKeyframesOfNumbersAndExpressions() {
        final JsonObject file = JsonParser.parseString("""
                {"format_version": "1.8.0", "animations": {"animation.test.spin": {
                  "loop": true, "animation_length": 2,
                  "bones": {"fan": {"rotation": {"0.0": [0, 0, 0], "2.0": [0, 0, 360]},
                                    "position": [0, "math.sin(query.anim_time * 90) * 2", 0],
                                    "scale": {"1.0": {"vector": [2, 2, 2], "easing": "easeInSine"}}}},
                  "sound_effects": {"0.5": {"effect": "click"}},
                  "timeline": {"1.0": "v.done = 1;"}}}}
                """).getAsJsonObject();
        final ModelAnimation spin = BedrockReader.animations("test", file).getFirst();
        assertEquals(ModelAnimation.Loop.LOOP, spin.loop());
        assertEquals(2, spin.length(), EPSILON);
        final ExpressionContext context = new ExpressionContext().animTime(1.0);
        final float[] out = new float[3];
        spin.bones().get("fan").rotation().evaluate(1.0, context, out);
        assertEquals(180, out[2], EPSILON, "halfway round at halfway");
        spin.bones().get("fan").position().evaluate(1.0, context, out);
        assertEquals(2, out[1], EPSILON, "an expression worked out from the animation's time");
        assertEquals(Easing.EASE_IN_SINE, spin.bones().get("fan").scale().keyframes().getFirst().easing());
        assertEquals(2, spin.events().size());
        assertEquals(ModelAnimation.EventKind.SOUND, spin.events().getFirst().kind());
        assertEquals("click", spin.events().getFirst().value());
    }

    private static List<Path> filesEnding(final String suffix) {
        final Path root = Path.of("").toAbsolutePath().getParent();
        final List<Path> out = new ArrayList<>();
        for (final String module : List.of("computers", "industrial", "core", "tests")) {
            final Path resources = root.resolve(module).resolve("src/main/resources");
            if (!Files.isDirectory(resources)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(resources)) {
                files.filter(path -> path.getFileName().toString().endsWith(suffix)).forEach(out::add);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return out;
    }

    private static JsonObject json(final Path file) {
        try {
            return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
