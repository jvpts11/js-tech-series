/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

/**
 * Where a face of a cube takes its picture from on the model's texture, in the texture's pixels: its corner, its
 * width and height (negative to flip it), and how far the picture is turned on the face, in quarter turns.
 *
 * @param u        the left of the picture
 * @param v        the top of the picture
 * @param width    how wide it is; negative when it is flipped left to right
 * @param height   how tall it is; negative when it is flipped top to bottom
 * @param rotation the picture's turn on the face: 0, 90, 180 or 270 degrees
 * @param texture  which of the model's textures it is on, from 0
 */
public record FaceUv(float u, float v, float width, float height, int rotation, int texture) {

    /** A face's picture on the model's first texture, unturned. */
    public static FaceUv of(final float u, final float v, final float width, final float height) {
        return new FaceUv(u, v, width, height, 0, 0);
    }
}
