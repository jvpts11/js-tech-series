/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class GeometryBakerTest {

    @Test
    void bake_keepsSeeThroughFacesApartFromTheSolidOnesOfTheirTexture() {
        final ModelData model = new ModelData("canopy", 16, 16, List.of(ModelBone.ofEuler("frame", null,
                new Vector3f(), new Vector3f(), List.of(), List.of(triangle(false), triangle(true), triangle(false)),
                List.of(), false)), Map.of(), List.of(), List.of());
        final List<BakedGeometry.Part> parts = GeometryBaker.bake(model, Set.of()).partsOf(0);
        assertEquals(2, parts.size(), "the solid faces together, the glass apart");
        assertEquals(List.of(false, true), parts.stream().map(BakedGeometry.Part::seeThrough).toList());
        assertEquals(List.of(2, 1), parts.stream().map(BakedGeometry.Part::faces).toList());
        assertEquals(List.of(0, 0), parts.stream().map(BakedGeometry.Part::texture).toList(),
                "both painted with the one texture");
    }

    /* One triangle on the first texture, see-through or not. */
    private static ModelMesh triangle(final boolean seeThrough) {
        return new ModelMesh(new float[] {0, 0, 0, 16, 0, 0, 0, 16, 0}, new float[0], new float[6],
                new int[] {0, 1, 2}, new int[0], new float[0], List.of(), 0, seeThrough);
    }
}
