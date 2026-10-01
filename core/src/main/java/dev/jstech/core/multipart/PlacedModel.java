/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * One standalone block model drawn as part of a block: a part on a face, a piece of a wire in its lane. The model is
 * authored facing north and is turned to {@code facing}, then moved by the offset, in pixels.
 *
 * @param model  the standalone block model
 * @param facing the way it is turned, north being as authored
 * @param dx     how far it is moved east, in pixels
 * @param dy     how far it is moved up, in pixels
 * @param dz     how far it is moved south, in pixels
 */
public record PlacedModel(ResourceLocation model, Direction facing, float dx, float dy, float dz) {

    public PlacedModel {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(facing, "facing");
    }

    /** {@code model} turned to {@code facing}, where it stands. */
    public static PlacedModel facing(final ResourceLocation model, final Direction facing) {
        return new PlacedModel(model, facing, 0F, 0F, 0F);
    }
}
