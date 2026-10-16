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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class BlockbenchReaderTest {

    private static final double EPSILON = 1.0e-4;
    /* A PNG of one pixel. */
    private static final String PIXEL = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQ"
            + "AAAABJRU5ErkJggg==";

    @Test
    void read_movesAJavaBlockModelToStandOnItsMiddle() {
        final ModelData model = BlockbenchReader.read("rack", json("""
                {"meta": {"format_version": "4.5", "model_format": "java_block"},
                 "resolution": {"width": 16, "height": 16},
                 "elements": [{"name": "body", "type": "cube", "uuid": "c1", "from": [0, 0, 0], "to": [16, 16, 13],
                   "origin": [8, 8, 8], "faces": {
                     "north": {"uv": [0, 0, 16, 16], "texture": 0},
                     "south": {"uv": [0, 0, 0, 0], "texture": -1}}}],
                 "outliner": ["c1"],
                 "textures": [{"name": "rack", "uuid": "t1", "relative_path": "../textures/rack.png"}]}
                """));
        final ModelCube body = model.bones().getFirst().cubes().getFirst();
        assertEquals(-8, body.from().x(), EPSILON, "the block's corner moved to its middle");
        assertEquals(-8, body.from().z(), EPSILON);
        assertEquals(0, body.from().y(), EPSILON, "but not lifted");
        assertEquals(13, body.size().z(), EPSILON);
        assertEquals(1, body.faces().size(), "a face of no texture is not drawn");
        assertEquals("../textures/rack.png", model.textures().getFirst().file());
    }

    @Test
    void read_takesGroupsAsBonesWithTheirPivotsAndTurns() {
        final ModelData model = BlockbenchReader.read("fan", json("""
                {"meta": {"format_version": "4.5", "model_format": "bedrock"},
                 "elements": [{"name": "blade", "type": "cube", "uuid": "c1", "from": [-1, 0, -1], "to": [1, 1, 1],
                               "box_uv": true, "uv_offset": [4, 2]},
                              {"name": "tip", "type": "locator", "uuid": "l1", "position": [0, 3, 0]}],
                 "outliner": [{"name": "body", "uuid": "g1", "origin": [0, 2, 0], "rotation": [0, 45, 0],
                               "children": [{"name": "rotor", "uuid": "g2", "origin": [0, 4, 0],
                                             "children": ["c1", "l1"]}]}]}
                """));
        assertEquals(2, model.bones().size());
        final ModelBone body = model.bone("body").orElseThrow();
        assertEquals(45, body.euler().y(), EPSILON, "Blockbench's space is the Core's: nothing turns round");
        assertEquals(2, body.pivot().y(), EPSILON);
        final ModelBone rotor = model.bone("rotor").orElseThrow();
        assertEquals("body", rotor.parent());
        assertEquals(6, rotor.cubes().getFirst().faces().size(), "box UV pictures every face");
        assertEquals(4 + 2, rotor.cubes().getFirst().faces().get(CubeFace.NORTH).u(), EPSILON,
                "north starts past the cube's depth from its offset");
        assertEquals(3, model.locators().get("tip").offset().y(), EPSILON);
    }

    @Test
    void read_carriesTheTexturesTheFileHolds() {
        final ModelData model = BlockbenchReader.read("chip", json("""
                {"meta": {"format_version": "4.5", "model_format": "free"},
                 "elements": [],
                 "textures": [{"name": "chip", "uuid": "t1", "uv_width": 32, "uv_height": 64,
                   "source": "data:image/png;base64,%s"}]}
                """.formatted(PIXEL)));
        final ModelTexture texture = model.textures().getFirst();
        assertTrue(texture.embedded(), "the picture is in the file");
        assertEquals((byte) 0x89, texture.bytes()[0], "and it is a PNG");
        assertEquals(32, texture.width());
        assertEquals(64, texture.height());
    }

    @Test
    void read_walksAFaceOfFourCornersRoundAsBlockbenchDoes() {
        // The corners are written across the square: a, c, b, d with a and b opposite.
        final ModelData model = BlockbenchReader.read("plate", json("""
                {"meta": {"format_version": "4.5", "model_format": "free"},
                 "elements": [{"name": "plate", "type": "mesh", "uuid": "m1", "origin": [0, 0, 0],
                   "vertices": {"a": [0, 0, 0], "b": [4, 0, 4], "c": [4, 0, 0], "d": [0, 0, 4]},
                   "faces": {"f1": {"vertices": ["a", "b", "c", "d"], "texture": 0,
                                    "uv": {"a": [0, 0], "b": [16, 16], "c": [16, 0], "d": [0, 16]}}}}],
                 "outliner": ["m1"]}
                """));
        final ModelMesh plate = model.bones().getFirst().meshes().getFirst();
        assertEquals(6, plate.triangles().length, "two triangles");
        assertEquals(16, area(plate), EPSILON, "covering the square once, not crossing over it");
        assertEquals(1, plate.uvs()[2], EPSILON, "a picture counted in pixels comes to the Core from 0 to 1");
    }

    @Test
    void read_readsAnimationsWithTheirLoopsEasingsAndEvents() {
        final ModelData model = BlockbenchReader.read("door", json("""
                {"meta": {"format_version": "4.5", "model_format": "bedrock"},
                 "elements": [],
                 "outliner": [{"name": "hinge", "uuid": "g1", "origin": [0, 0, 0], "children": []}],
                 "animations": [{"name": "open", "loop": "hold", "length": 1.5, "animators": {
                   "g1": {"name": "hinge", "type": "bone", "keyframes": [
                     {"channel": "rotation", "time": 0, "interpolation": "linear",
                      "data_points": [{"x": 0, "y": 0, "z": 0}]},
                     {"channel": "rotation", "time": 1.5, "interpolation": "bezier",
                      "bezier_left_time": [-0.1, -0.1, -0.1], "bezier_left_value": [0, 0, 0],
                      "bezier_right_time": [0.1, 0.1, 0.1], "bezier_right_value": [0, 0, 0],
                      "data_points": [{"x": 0, "y": "90 * 1", "z": 0}]}]},
                   "effects": {"name": "Effects", "type": "effect", "keyframes": [
                     {"channel": "sound", "time": 0.2, "data_points": [{"effect": "creak", "locator": ""}]}]}}}]}
                """));
        final ModelAnimation open = model.animations().getFirst();
        assertEquals(ModelAnimation.Loop.HOLD, open.loop());
        assertEquals(1.5, open.length(), EPSILON);
        final AnimationChannel rotation = open.bones().get("hinge").rotation();
        assertEquals(Easing.BEZIER, rotation.keyframes().getLast().easing());
        final float[] out = new float[3];
        rotation.evaluate(1.5, new ExpressionContext(), out);
        assertEquals(90, out[1], EPSILON, "an expression of a keyframe is worked out");
        assertFalse(open.events().isEmpty());
        assertEquals("creak", open.events().getFirst().value());
    }

    private static double area(final ModelMesh mesh) {
        double out = 0;
        final float[] p = mesh.positions();
        final int[] t = mesh.triangles();
        for (int i = 0; i < t.length; i += 3) {
            final Vector3f a = new Vector3f(p[t[i] * 3], p[t[i] * 3 + 1], p[t[i] * 3 + 2]);
            final Vector3f b = new Vector3f(p[t[i + 1] * 3], p[t[i + 1] * 3 + 1], p[t[i + 1] * 3 + 2]).sub(a);
            final Vector3f c = new Vector3f(p[t[i + 2] * 3], p[t[i + 2] * 3 + 1], p[t[i + 2] * 3 + 2]).sub(a);
            out += b.cross(c).length() / 2;
        }
        return out;
    }

    private static JsonObject json(final String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }
}
