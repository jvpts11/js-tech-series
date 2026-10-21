/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class BlockModelFilesTest {

    @Test
    void modelOf_takesTheFirstVariantsModel() {
        assertEquals(Optional.of("othermod:block/limestone"), BlockModelFiles.modelOf("""
                {"variants": {"": [{"model": "othermod:block/limestone"}, {"model": "othermod:block/limestone_2"}]}}
                """));
        assertEquals(Optional.of("othermod:block/marble"), BlockModelFiles.modelOf("""
                {"variants": {"axis=y": {"model": "othermod:block/marble"}}}
                """));
    }

    @Test
    void modelOf_readsAMultipartsFirstPart() {
        assertEquals(Optional.of("othermod:block/slate"), BlockModelFiles.modelOf("""
                {"multipart": [{"apply": {"model": "othermod:block/slate"}}]}
                """));
    }

    @Test
    void textureOf_takesTheWholeCubesTextureFirst() {
        assertEquals(Optional.of("othermod:block/limestone"), BlockModelFiles.textureOf("""
                {"parent": "minecraft:block/cube_all", "textures": {"particle": "othermod:block/dust",
                 "all": "othermod:block/limestone"}}
                """));
    }

    @Test
    void textureOf_followsAReferenceToAnotherTexture() {
        assertEquals(Optional.of("othermod:block/marble_side"), BlockModelFiles.textureOf("""
                {"textures": {"side": "#body", "body": "othermod:block/marble_side"}}
                """));
    }

    @Test
    void textureOf_givesNothingForAModelWithNoTextures() {
        assertEquals(Optional.empty(), BlockModelFiles.textureOf("{\"parent\": \"othermod:block/base\"}"));
        assertEquals(Optional.of("othermod:block/base"), BlockModelFiles.parentOf(
                "{\"parent\": \"othermod:block/base\"}"));
        assertEquals(Optional.empty(), BlockModelFiles.textureOf("{\"textures\": {\"all\": \"#all\"}}"));
    }
}
