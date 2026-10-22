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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

class ThreeMfReaderTest {

    private static final double EPSILON = 1.0e-4;
    /* A centimetre, in the game's pixels. */
    private static final float CENTIMETRE = 16.0F / 100.0F;
    private static final String RELATIONS = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Target="/3D/pump.model" Id="rel0"
                  Type="http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel"/>
            </Relationships>
            """;
    /* A pump of two parts in centimetres: a blue box placed twice, once 10 cm along X, and a red plate under it. */
    private static final String PUMP = """
            <?xml version="1.0" encoding="UTF-8"?>
            <model unit="centimeter" xmlns="http://schemas.microsoft.com/3dmanufacturing/core/2015/02"
                   xmlns:m="http://schemas.microsoft.com/3dmanufacturing/material/2015/02">
              <resources>
                <basematerials id="1">
                  <base name="Blue" displaycolor="#3060C0"/>
                  <base name="Red" displaycolor="#C03030FF"/>
                </basematerials>
                <object id="2" name="housing" type="model" pid="1" pindex="0">
                  <mesh>
                    <vertices>
                      <vertex x="0" y="0" z="0"/>
                      <vertex x="2" y="0" z="0"/>
                      <vertex x="2" y="2" z="0"/>
                      <vertex x="0" y="0" z="2"/>
                    </vertices>
                    <triangles>
                      <triangle v1="0" v2="1" v3="2"/>
                      <triangle v1="0" v2="1" v3="3" p1="1"/>
                    </triangles>
                  </mesh>
                </object>
                <object id="3" name="pump" type="model">
                  <components>
                    <component objectid="2"/>
                    <component objectid="2" transform="1 0 0 0 1 0 0 0 1 10 0 0"/>
                  </components>
                </object>
              </resources>
              <build>
                <item objectid="3" transform="1 0 0 0 1 0 0 0 1 0 0 5"/>
              </build>
            </model>
            """;

    @Test
    void read_makesEachPartOfTheAssemblyABone() {
        final ModelData pump = read();
        assertEquals(3, pump.bones().size(), "the pump and its two housings");
        assertEquals("pump", pump.bones().getFirst().name());
        assertEquals("pump", pump.bone("housing").orElseThrow().parent());
        assertEquals("pump", pump.bone("housing_2").orElseThrow().parent(), "a second of one name is told apart");
    }

    @Test
    void read_placesAComponentInTheGamesPixels() {
        final ModelBone second = read().bone("housing_2").orElseThrow();
        assertEquals(10 * CENTIMETRE, second.translation().x(), EPSILON, "10 cm along X, in the file's unit");
    }

    @Test
    void read_givesEachColourATextureAndAMesh() {
        final ModelData pump = read();
        final ModelBone housing = pump.bone("housing").orElseThrow();
        assertEquals(2, housing.meshes().size(), "the object's own colour, and the red a triangle names");
        assertEquals(2, pump.textures().size());
        assertTrue(pump.textures().get(1).name().contains("ffc03030"), pump.textures().get(1).name());
        assertEquals(0, housing.meshes().getFirst().uvs().length, "a colour's pictures are laid from the sides later");
    }

    @Test
    void read_standsThePumpOnItsOrigin() {
        final ModelBone root = read().bones().getFirst();
        assertEquals(0, root.translation().y(), EPSILON, "lifted 5 cm by the build, then stood back on 0");
    }

    @Test
    void read_refusesAPackageThatBuildsNothing() {
        final String empty = PUMP.replaceAll("(?s)<build>.*</build>", "<build/>");
        assertThrows(ModelFormatException.class, () -> ThreeMfReader.read("empty", zip(Map.of(
                "_rels/.rels", RELATIONS, "3D/pump.model", empty))));
    }

    private static ModelData read() {
        return ThreeMfReader.read("pump", zip(Map.of("_rels/.rels", RELATIONS, "3D/pump.model", PUMP)));
    }

    private static byte[] zip(final Map<String, String> files) {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (final Map.Entry<String, String> file : files.entrySet()) {
                out.putNextEntry(new ZipEntry(file.getKey()));
                out.write(file.getValue().getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }
}
