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
 * authored facing north; it is turned about its face by quarter turns, as a wrench turns a part, then to
 * {@code facing}, then moved by the offset, in pixels.
 *
 * @param model  the standalone block model
 * @param facing the way it is turned, north being as authored
 * @param turns  the quarter turns about its face, from 0 to 3 ({@link PartBox#turnedAbout})
 * @param dx     how far it is moved east, in pixels
 * @param dy     how far it is moved up, in pixels
 * @param dz     how far it is moved south, in pixels
 */
public record PlacedModel(ResourceLocation model, Direction facing, int turns, float dx, float dy, float dz) {

    private static final int QUARTERS = 4;

    public PlacedModel {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(facing, "facing");
        turns = Math.floorMod(turns, QUARTERS);
    }

    /** {@code model} turned to {@code facing}, not turned about its face, and moved by the offset. */
    public PlacedModel(final ResourceLocation model, final Direction facing, final float dx, final float dy,
                       final float dz) {
        this(model, facing, 0, dx, dy, dz);
    }

    /** {@code model} turned to {@code facing}, where it stands. */
    public static PlacedModel facing(final ResourceLocation model, final Direction facing) {
        return new PlacedModel(model, facing, 0, 0F, 0F, 0F);
    }

    /** {@code model} turned {@code turns} quarter turns about its face, then to {@code facing}, where it stands. */
    public static PlacedModel facing(final ResourceLocation model, final Direction facing, final int turns) {
        return new PlacedModel(model, facing, turns, 0F, 0F, 0F);
    }
}
