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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ModelReadersTest {

    @Test
    void readsModel_knowsEveryFormatByItsEnding() {
        for (final String file : new String[] {"a.geo.json", "a.bbmodel", "a.gltf", "a.GLB", "a.fbx", "a.obj"}) {
            assertTrue(ModelReaders.readsModel(file), file);
        }
        assertFalse(ModelReaders.readsModel("a.animation.json"), "an animation file is no model");
        assertFalse(ModelReaders.readsModel("a.png"));
    }

    @Test
    void stem_takesTheWholeEndingOff() {
        assertEquals("fan", ModelReaders.stem("models/fan.geo.json"));
        assertEquals("fan", ModelReaders.stem("animations\\fan.animation.json"));
        assertEquals("crate", ModelReaders.stem("crate.obj"));
    }

    @Test
    void read_choosesTheReaderByTheEnding() {
        final byte[] obj = "v 0 0 0\nv 1 0 0\nv 0 1 0\nf 1 2 3\n".getBytes(StandardCharsets.UTF_8);
        assertEquals("tri", ModelReaders.read("tri.obj", obj, file -> null).getFirst().name());
        final byte[] geo = """
                {"format_version": "1.12.0", "minecraft:geometry": [
                  {"description": {"identifier": "geometry.one"}, "bones": [{"name": "a"}]},
                  {"description": {"identifier": "geometry.two"}, "bones": [{"name": "b"}]}]}
                """.getBytes(StandardCharsets.UTF_8);
        assertEquals(2, ModelReaders.read("pair.geo.json", geo, file -> null).size(),
                "a geometry file can hold several models");
    }

    @Test
    void read_readsEveryModelFileOfTheSeriesKit() throws IOException {
        final Path root = Path.of("").toAbsolutePath().getParent();
        final List<Path> files = new ArrayList<>();
        for (final String module : List.of("core", "computers", "industrial", "space", "tests")) {
            final Path resources = root.resolve(module).resolve("src/main/resources/assets");
            if (Files.isDirectory(resources)) {
                try (Stream<Path> walked = Files.walk(resources)) {
                    walked.filter(path -> path.toString().contains("jsmodels")
                            && ModelReaders.readsModel(path.getFileName().toString())).forEach(files::add);
                }
            }
        }
        assertTrue(files.size() >= 4, "the tests' models are found: " + files);
        for (final Path file : files) {
            final Path folder = file.getParent();
            final List<ModelData> models = ModelReaders.read(file.getFileName().toString(), Files.readAllBytes(file),
                    beside -> {
                        try {
                            return Files.readAllBytes(folder.resolve(beside));
                        } catch (final IOException missing) {
                            return null;
                        }
                    });
            assertFalse(models.getFirst().bones().isEmpty(), file + " has bones");
        }
    }

    @Test
    void read_saysWhatBrokeInAFileOfTheRightEnding() {
        final byte[] broken = "{\"elements\": [".getBytes(StandardCharsets.UTF_8);
        final ModelFormatException refused = assertThrows(ModelFormatException.class,
                () -> ModelReaders.read("box.bbmodel", broken, file -> null));
        assertTrue(refused.getMessage().contains("box.bbmodel"), refused.getMessage());
        assertThrows(ModelFormatException.class, () -> ModelReaders.read("box.png", broken, file -> null),
                "and refuses a file of no format it reads");
    }
}
