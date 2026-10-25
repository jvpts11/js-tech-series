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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
            assertTrue(ahead.z < held.z - AHEAD, hand + ": the claws point ahead of the grip, away from the eye ("
                    + ahead.z + " against " + held.z + ")");
        }
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
