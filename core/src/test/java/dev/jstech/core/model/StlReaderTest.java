/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class StlReaderTest {

    private static final double EPSILON = 1.0e-4;
    /* A millimetre, in the game's pixels. */
    private static final float MILLIMETRE = 16.0F / 1000.0F;
    private static final String TWO_SOLIDS = """
            solid plate
              facet normal 0 0 1
                outer loop
                  vertex 0 0 0
                  vertex 1000 0 0
                  vertex 1000 1000 0
                endloop
              endfacet
              facet normal 0 0 1
                outer loop
                  vertex 0 0 0
                  vertex 1000 1000 0
                  vertex 0 1000 0
                endloop
              endfacet
            endsolid plate
            solid post
              facet normal 1 0 0
                outer loop
                  vertex 0 0 0
                  vertex 0 0 500
                  vertex 0 100 500
                endloop
              endfacet
            endsolid post
            """;

    @Test
    void read_makesEachSolidOfATextFileABone() {
        final ModelData read = StlReader.read("parts", TWO_SOLIDS.getBytes(StandardCharsets.US_ASCII));
        assertEquals(2, read.bones().size());
        final ModelMesh plate = read.bone("plate").orElseThrow().meshes().getFirst();
        assertEquals(4, plate.corners(), "the two triangles of the plate share their two corners at one place");
        assertEquals(6, plate.triangles().length);
    }

    @Test
    void read_takesMillimetresAndTurnsZUpright() {
        final ModelData read = StlReader.read("parts", TWO_SOLIDS.getBytes(StandardCharsets.US_ASCII));
        final ModelMesh post = read.bone("post").orElseThrow().meshes().getFirst();
        float highest = -Float.MAX_VALUE;
        for (int at = 1; at < post.positions().length; at += 3) {
            highest = Math.max(highest, post.positions()[at]);
        }
        assertEquals(500 * MILLIMETRE, highest, EPSILON, "half a metre up the CAD program's Z is up the game's Y");
    }

    @Test
    void read_standsTheModelOnItsOrigin() {
        final ModelData read = StlReader.read("parts", TWO_SOLIDS.getBytes(StandardCharsets.US_ASCII));
        final ModelBone plate = read.bone("plate").orElseThrow();
        assertEquals(-500 * MILLIMETRE, plate.translation().x(), EPSILON, "the middle of the box across at 0");
        assertEquals(0, plate.translation().y(), EPSILON, "the lowest point at 0");
    }

    @Test
    void read_givesABinaryFilesColoursATextureEach() {
        final ModelData read = StlReader.read("coloured", binary("solid made by a program", 0x8000 | 31 << 10,
                0x8000 | 31));
        final ModelBone model = read.bones().getFirst();
        assertEquals(2, model.meshes().size(), "a mesh for each colour");
        assertEquals(2, read.textures().size());
        assertTrue(read.textures().getFirst().embedded(), "the colour made a texture of its own");
        assertTrue(read.textures().getFirst().name().contains("ff0000"), "red first, as VisCAM writes it");
    }

    @Test
    void read_takesMaterialisesColoursBlueFirst() {
        final ModelData read = StlReader.read("coloured", binary("COLOR= written by Magics", 31, 31));
        assertTrue(read.textures().getFirst().name().contains("ff0000"), "Materialise writes red last");
    }

    @Test
    void read_saysTheLineThatDoesNotRead() {
        final String broken = "solid x\nfacet normal 0 0 1\nouter loop\nvertex 0 zero 0\n";
        final ModelFormatException thrown = assertThrows(ModelFormatException.class,
                () -> StlReader.read("broken", broken.getBytes(StandardCharsets.US_ASCII)));
        assertTrue(thrown.getMessage().contains("line 4"), thrown.getMessage());
    }

    /* A binary file of two triangles, each with its attribute, its header as given. */
    private static byte[] binary(final String header, final int first, final int second) {
        final ByteBuffer out = ByteBuffer.allocate(80 + 4 + 2 * 50).order(ByteOrder.LITTLE_ENDIAN);
        final byte[] written = header.getBytes(StandardCharsets.US_ASCII);
        out.put(written, 0, Math.min(80, written.length));
        out.position(80);
        out.putInt(2);
        for (final int attribute : new int[] {first, second}) {
            out.putFloat(0).putFloat(0).putFloat(1);
            out.putFloat(0).putFloat(0).putFloat(0);
            out.putFloat(10).putFloat(0).putFloat(0);
            out.putFloat(0).putFloat(10).putFloat(attribute == first ? 0 : 5);
            out.putShort((short) attribute);
        }
        return out.array();
    }
}
