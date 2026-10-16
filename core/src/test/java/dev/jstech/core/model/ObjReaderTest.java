/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ObjReaderTest {

    private static final double EPSILON = 1.0e-5;
    private static final String CRATE = """
            # a crate's lid and its base
            mtllib crate.mtl
            v 0 0 0
            v 1 0 0
            v 1 0 1
            v 0 0 1
            vt 0 0
            vt 1 1
            vn 0 1 0
            o lid
            usemtl wood
            f 1/1/1 2/2/1 3/1/1 4/2/1
            o base
            usemtl metal
            f -4/1 -3/1 -2/2
            """;
    private static final String LIBRARY = """
            newmtl wood
            map_Kd -s 1 1 1 textures/wood.png
            newmtl metal
            map_Kd textures\\metal.png
            """;

    @Test
    void read_makesEachObjectABoneAndEachFaceTriangles() {
        final ModelData crate = read();
        assertEquals(2, crate.bones().size());
        final ModelMesh lid = crate.bone("lid").orElseThrow().meshes().getFirst();
        assertArrayEquals(new int[] {0, 1, 2, 0, 2, 3}, lid.triangles(), "a face of four corners as two triangles");
        assertEquals(16, lid.positions()[3], EPSILON, "a unit of OBJ is a metre, sixteen pixels");
        assertEquals(1, lid.uvs()[1], EPSILON, "a picture's height counted from its top");
        assertEquals(1, lid.normals()[1], EPSILON);
    }

    @Test
    void read_countsNegativeReferencesBackFromTheLastPoint() {
        final ModelMesh base = read().bone("base").orElseThrow().meshes().getFirst();
        assertEquals(0, base.positions()[0], EPSILON, "-4 is the first of four points");
        assertEquals(16, base.positions()[6], EPSILON, "-2 the third");
        assertEquals(0, base.normals().length, "a face of no normals has them worked out");
    }

    @Test
    void read_paintsEachMaterialWithItsLibrarysTexture() {
        final ModelData crate = read();
        assertEquals("textures/wood.png", crate.textures().getFirst().file(), "past the options before the file");
        assertEquals("textures/metal.png", crate.textures().get(1).file());
        assertEquals(1, crate.bone("base").orElseThrow().meshes().getFirst().texture());
    }

    @Test
    void read_saysWhichLineItCannotRead() {
        final ModelFormatException refused = assertThrows(ModelFormatException.class, () -> ObjReader.read("bad",
                "v 0 zero 0\n".getBytes(StandardCharsets.UTF_8), file -> null));
        assertEquals(true, refused.getMessage().contains("line 1"));
    }

    private static ModelData read() {
        return ObjReader.read("crate", CRATE.getBytes(StandardCharsets.UTF_8),
                file -> file.equals("crate.mtl") ? LIBRARY.getBytes(StandardCharsets.UTF_8) : null);
    }
}
