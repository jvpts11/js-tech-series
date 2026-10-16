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

import java.util.List;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class ModelPoseTest {

    private static final double EPSILON = 1.0e-4;

    @Test
    void world_turnsABoneAboutItsPivot() {
        final ModelPose pose = new ModelPose(arm());
        pose.addEuler(pose.index("arm"), 0, 0, 90);
        final Vector3f hand = place(pose, "arm", new Vector3f(2, 4, 0));
        // The arm turns a quarter about Z at its pivot (0, 4, 0): two pixels out along X become two up along Y.
        assertEquals(0, hand.x, EPSILON);
        assertEquals(6, hand.y, EPSILON);
    }

    @Test
    void world_carriesAChildWithItsParent() {
        final ModelPose pose = new ModelPose(arm());
        pose.addMove(pose.index("body"), 0, 10, 0);
        assertEquals(14, place(pose, "arm", new Vector3f(0, 4, 0)).y, EPSILON);
    }

    @Test
    void addEuler_addsToABoneTurnedAtRestByAngles() {
        final ModelPose pose = new ModelPose(arm());
        final int tilted = pose.index("tilted");
        pose.addEuler(tilted, 0, 0, 45);
        final Vector3f tip = place(pose, "tilted", new Vector3f(1, 0, 0));
        // At rest it is turned 45 about Z; another 45 turns it a quarter in all.
        assertEquals(0, tip.x, EPSILON);
        assertEquals(1, tip.y, EPSILON);
    }

    @Test
    void setTurn_takesTheRestTurnsPlace() {
        final ModelPose pose = new ModelPose(arm());
        final int tilted = pose.index("tilted");
        pose.setTurn(tilted, new Quaternionf());
        assertEquals(1, place(pose, "tilted", new Vector3f(1, 0, 0)).x, EPSILON);
    }

    @Test
    void reset_putsEveryBoneBackAtRest() {
        final ModelPose pose = new ModelPose(arm());
        pose.addMove(0, 5, 5, 5);
        pose.multiplySize(0, 2, 2, 2);
        pose.setHidden(0, true);
        pose.setWeight("smile", 1);
        assertTrue(pose.hidden(pose.index("arm")), "a child of a hidden bone is hidden");
        pose.reset();
        assertEquals(0, place(pose, "body", new Vector3f()).y, EPSILON);
        assertFalse(pose.hidden(pose.index("arm")));
        assertEquals(0, pose.weight("smile"), EPSILON);
    }

    @Test
    void locator_isFoundWhereItsBoneTakesIt() {
        final ModelPose pose = new ModelPose(arm());
        pose.addMove(pose.index("body"), 3, 0, 0);
        final Matrix4f[] world = matrices(pose);
        pose.world(world);
        final Vector3f hand = pose.locator("hand", world, new Vector3f());
        assertEquals(3 + 2, hand.x, EPSILON);
        assertEquals(4, hand.y, EPSILON);
    }

    /* A body, an arm hanging from it with its pivot at its shoulder, and a bone turned at rest. */
    private static ModelData arm() {
        final ModelBone body = ModelBone.ofEuler("body", null, new Vector3f(), new Vector3f(), List.of(), List.of(),
                List.of(), false);
        final ModelBone arm = ModelBone.ofEuler("arm", "body", new Vector3f(0, 4, 0), new Vector3f(), List.of(),
                List.of(), List.of(new ModelLocator("hand", new Vector3f(2, 4, 0), new Vector3f())), false);
        final ModelBone tilted = ModelBone.ofEuler("tilted", null, new Vector3f(), new Vector3f(0, 0, 45), List.of(),
                List.of(), List.of(), false);
        return new ModelData("arm", 16, 16, List.of(body, arm, tilted), Map.of(), List.of(), List.of());
    }

    private static Vector3f place(final ModelPose pose, final String bone, final Vector3f point) {
        final Matrix4f[] world = matrices(pose);
        pose.world(world);
        return world[pose.index(bone)].transformPosition(point);
    }

    private static Matrix4f[] matrices(final ModelPose pose) {
        final Matrix4f[] out = new Matrix4f[pose.bones()];
        for (int i = 0; i < out.length; i++) {
            out[i] = new Matrix4f();
        }
        return out;
    }
}
