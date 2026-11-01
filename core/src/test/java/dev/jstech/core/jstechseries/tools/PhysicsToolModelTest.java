/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tools;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.jstech.core.model.BedrockReader;
import dev.jstech.core.model.ModelBone;
import dev.jstech.core.model.ModelCube;
import dev.jstech.core.model.ModelData;
import dev.jstech.core.model.Rotations;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/**
 * The Physics Tool is held the right way round: its claws, at its far end, point ahead of the hand on its grip, away
 * from the eye, in either hand, seen from the eye or from outside. Ahead is the way the game looks along as it draws
 * what is held, its -Z.
 */
class PhysicsToolModelTest {

    private static final Path MODELS = Path.of("src/main/resources/assets/jscore");
    private static final List<String> HANDS = List.of("firstperson_righthand", "firstperson_lefthand",
            "thirdperson_righthand", "thirdperson_lefthand");
    /* How far ahead of the grip the claws are, at least, in the model's pixels before it is scaled. */
    private static final float AHEAD = 10;
    /* The claws, and how near a segment's end must come to its tip's hinge, in pixels. */
    private static final int CLAWS = 3;
    private static final float MEETS = 0.5f;

    @Test
    void heldModel_pointsTheClawsAheadOfTheGripInEveryHand() {
        final ModelData model = BedrockReader.geometry("physics_tool",
                json(MODELS.resolve("jsmodels/physics_tool.geo.json"))).getFirst();
        final Vector3f claws = new Vector3f(Float.MAX_VALUE, 0, 0);
        final Vector3f grip = new Vector3f(0, Float.MAX_VALUE, 0);
        for (final ModelBone bone : model.bones()) {
            for (final ModelCube cube : bone.cubes()) {
                // The claws are the far end along the tool's length; the grip is the box that hangs lowest.
                if (cube.from().x() < claws.x) {
                    claws.set(cube.from().x(), cube.from().y() + cube.size().y() / 2, 0);
                }
                if (cube.from().y() < grip.y) {
                    grip.set(cube.from().x() + cube.size().x() / 2, cube.from().y(), 0);
                }
            }
        }
        final JsonObject display = json(MODELS.resolve("models/held/physics_tool.json")).getAsJsonObject("display");
        for (final String hand : HANDS) {
            final JsonArray turn = display.getAsJsonObject(hand).getAsJsonArray("rotation");
            // The game turns a left hand's transform the other way about Y and Z, as the hand is the right's mirror.
            final float side = hand.endsWith("lefthand") ? -1 : 1;
            final Quaternionf turned = new Quaternionf().rotationXYZ(radians(turn, 0), side * radians(turn, 1),
                    side * radians(turn, 2));
            final Vector3f ahead = turned.transform(new Vector3f(claws));
            final Vector3f held = turned.transform(new Vector3f(grip));
            if (hand.startsWith("firstperson")) {
                assertTrue(ahead.z < held.z - AHEAD, hand + ": the claws point ahead of the grip, away from the eye ("
                        + ahead.z + " against " + held.z + ")");
            } else {
                // In the hand of a player seen, the game's held item points out of the fist along its Y, as an
                // axe's head does; its -Z runs down the arm.
                assertTrue(ahead.y > held.y + AHEAD, hand + ": the claws point out of the fist, ahead of the grip ("
                        + ahead.y + " against " + held.y + ")");
            }
        }
    }

    @Test
    void model_eachClawSegmentReachesItsTip() {
        final ModelData model = BedrockReader.geometry("physics_tool",
                json(MODELS.resolve("jsmodels/physics_tool.geo.json"))).getFirst();
        final List<ModelCube> segments = new ArrayList<>();
        final List<ModelCube> tips = new ArrayList<>();
        for (final ModelBone bone : model.bones()) {
            for (final ModelCube cube : bone.cubes()) {
                // A claw is a turned bar a pixel thick: its segment four long, its tip two.
                if (cube.rotation().z() != 0 && cube.size().y() == 1) {
                    (cube.size().x() > 3 ? segments : tips).add(cube);
                }
            }
        }
        assertTrue(segments.size() == CLAWS && tips.size() == CLAWS, "three claws of a segment and a tip each: "
                + segments.size() + " segments, " + tips.size() + " tips");
        for (final ModelCube segment : segments) {
            final Vector3f end = farEnd(segment);
            float nearest = Float.MAX_VALUE;
            for (final ModelCube tip : tips) {
                nearest = Math.min(nearest, end.distance(new Vector3f(tip.pivot())));
            }
            assertTrue(nearest <= MEETS, "a claw's segment ends where its tip is hinged, as the game draws them: "
                    + nearest + " pixels apart");
        }
    }

    /* The middle of a bar's end away from its pivot, turned about the pivot as the game's baker turns it. */
    private static Vector3f farEnd(final ModelCube cube) {
        final float low = cube.from().x();
        final float high = low + cube.size().x();
        final float x = Math.abs(low - cube.pivot().x()) > Math.abs(high - cube.pivot().x()) ? low : high;
        final Vector3f end = new Vector3f(x, cube.from().y() + cube.size().y() / 2,
                cube.from().z() + cube.size().z() / 2).sub(cube.pivot());
        Rotations.ofEuler(cube.rotation()).transform(end);
        return end.add(cube.pivot());
    }

    private static float radians(final JsonArray turn, final int axis) {
        return (float) Math.toRadians(turn.get(axis).getAsDouble());
    }

    private static JsonObject json(final Path file) {
        try {
            return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (final IOException unread) {
            throw new UncheckedIOException(unread);
        }
    }
}
