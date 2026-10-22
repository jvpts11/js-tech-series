/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class ModelShapingTest {

    private static final double EPSILON = 1.0e-5;

    @Test
    void shape_laysPicturesFromTheSidesOnAMeshThatHasNone() {
        final ModelData shaped = ModelShaping.shape(model(bare()), ModelDefinition.Detail.AS_MADE, false);
        final ModelMesh mesh = shaped.bones().getFirst().meshes().getFirst();
        assertEquals(mesh.corners() * 2, mesh.uvs().length, "every corner has its place on the picture");
        assertEquals(6, mesh.corners(), "each triangle its own corners, so each face is flat");
        assertEquals(1.0, mesh.uvs()[3], EPSILON, "sixteen pixels across a picture, laid from above");
    }

    @Test
    void shape_laysThePicturesAtTheSizeTheModelIsDrawn() {
        final ModelData shaped = ModelShaping.shape(model(bare()), new ModelDefinition.Detail(2.0, 0, 0, 0), false);
        assertEquals(2.0, shaped.bones().getFirst().meshes().getFirst().uvs()[3], EPSILON,
                "drawn twice as big, its pixels stay the game's");
    }

    @Test
    void shape_leavesAModelOfItsOwnPicturesAsItIs() {
        final ModelData read = model(new ModelMesh(new float[] {0, 0, 0, 1, 0, 0, 0, 0, 1}, new float[0],
                new float[] {0, 0, 1, 0, 0, 1}, new int[] {0, 1, 2}, new int[0], new float[0], List.of(), 0));
        assertSame(read, ModelShaping.shape(read, ModelDefinition.Detail.AS_MADE, false));
    }

    @Test
    void shape_bringsTheMeshesDownToTheTrianglesNearAndFar() {
        final ModelData read = model(grid());
        final ModelDefinition.Detail detail = new ModelDefinition.Detail(1.0, 200, 32, 50);
        assertTrue(ModelShaping.triangles(ModelShaping.shape(read, detail, false)) <= 200);
        assertTrue(ModelShaping.triangles(ModelShaping.shape(read, detail, true)) <= 50);
        assertEquals(800, ModelShaping.triangles(read));
    }

    @Test
    void overBudget_stopsAModelOfMoreTrianglesThanItMayHaveUnsaid() {
        final ModelData read = model(grid(101));
        assertTrue(ModelShaping.triangles(read) > ModelDefinition.Detail.MOST_UNSAID);
        assertTrue(ModelShaping.overBudget(read, ModelDefinition.Detail.AS_MADE).orElseThrow()
                .contains(String.valueOf(ModelDefinition.Detail.MOST_UNSAID)));
        assertTrue(ModelShaping.overBudget(read, new ModelDefinition.Detail(1.0, 5_000, 0, 0)).isEmpty(),
                "saying how many it is drawn with lets it through, brought down as it is read");
        assertTrue(ModelShaping.overBudget(model(grid(20)), ModelDefinition.Detail.AS_MADE).isEmpty());
    }

    @Test
    void overBudget_stopsCubesOfMoreTrianglesThanTheModelIsDrawnWith() {
        final List<ModelCube> cubes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            cubes.add(new ModelCube(new Vector3f(i, 0, 0), new Vector3f(1, 1, 1), 0, new Vector3f(), new Vector3f(),
                    Map.of(), false));
        }
        final ModelData read = new ModelData("cubes", 16, 16, List.of(ModelBone.ofEuler("part", null,
                new Vector3f(), new Vector3f(), cubes, List.of(), List.of(), false)), Map.of(), List.of(),
                List.of());
        assertEquals(120, ModelShaping.triangles(read));
        assertTrue(ModelShaping.overBudget(read, new ModelDefinition.Detail(1.0, 100, 0, 0)).isPresent(),
                "cubes are drawn as they are, so they cannot be brought down to a hundred");
        assertTrue(ModelShaping.overBudget(read, new ModelDefinition.Detail(1.0, 200, 16, 60)).isPresent(),
                "nor far away to sixty");
        assertTrue(ModelShaping.overBudget(read, new ModelDefinition.Detail(1.0, 200, 16, 150)).isEmpty());
    }

    /* A square of two triangles lying flat, sixteen pixels a side, with no picture laid on it. */
    private static ModelMesh bare() {
        return new ModelMesh(new float[] {0, 0, 0, 16, 0, 0, 16, 0, 16, 0, 0, 16}, new float[0], new float[0],
                new int[] {0, 3, 1, 1, 3, 2}, new int[0], new float[0], List.of(), 0);
    }

    /* A flat grid of twenty by twenty squares. */
    private static ModelMesh grid() {
        return grid(20);
    }

    /* A wavy grid of {@code squares} by {@code squares} squares, two triangles each. */
    private static ModelMesh grid(final int squares) {
        final FloatList points = new FloatList();
        final IntList triangles = new IntList();
        final int row = squares + 1;
        for (int z = 0; z <= squares; z++) {
            for (int x = 0; x <= squares; x++) {
                points.add(x);
                points.add((float) Math.sin(x * 0.4) * 2);
                points.add(z);
            }
        }
        for (int z = 0; z < squares; z++) {
            for (int x = 0; x < squares; x++) {
                final int a = z * row + x;
                triangles.add(a);
                triangles.add(a + row);
                triangles.add(a + 1);
                triangles.add(a + 1);
                triangles.add(a + row);
                triangles.add(a + row + 1);
            }
        }
        return new ModelMesh(points.toArray(), new float[0], new float[0], triangles.toArray(), new int[0],
                new float[0], List.of(), 0);
    }

    private static ModelData model(final ModelMesh mesh) {
        return new ModelData("test", 16, 16, List.of(ModelBone.ofEuler("part", null, new Vector3f(), new Vector3f(),
                List.of(), List.of(mesh), List.of(), false)), Map.of(), List.of(), List.of());
    }
}
