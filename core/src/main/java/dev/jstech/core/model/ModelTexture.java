/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.Objects;

/**
 * A texture a model's file names or carries: its name, the file it is in, beside the model or in the mod's textures,
 * or its picture itself, as Blockbench and glTF can hold it in the model's own file; and the size its faces' pictures
 * are counted on, when it is not the model's.
 *
 * @param name   its name in the model
 * @param file   the file it is in, as the model writes it; empty for one the model carries
 * @param bytes  the picture, as a PNG file's bytes, for one the model carries; empty for one in a file
 * @param width  how wide the faces' pictures on it are counted, in pixels; 0 for the model's own width
 * @param height how tall; 0 for the model's own height
 */
public record ModelTexture(String name, String file, byte[] bytes, int width, int height) {

    public ModelTexture {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(bytes, "bytes");
    }

    /** A texture in a file, counted on the model's own size. */
    public static ModelTexture inFile(final String name, final String file) {
        return new ModelTexture(name, file, new byte[0], 0, 0);
    }

    /** Whether the model carries the picture itself. */
    public boolean embedded() {
        return this.bytes.length > 0;
    }
}
