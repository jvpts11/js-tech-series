/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.shape;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.multiblock.Orientation;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;

/**
 * Which block of which formed model a merged block is, and how its structure stands: what it takes its shape from.
 *
 * @param model       the formed model
 * @param orientation how its structure stands, by its place among {@link FormedShapes#ORIENTATIONS}
 * @param x           the block's x among the model's, its terminal's at 0
 * @param y           its y
 * @param z           its z
 */
public record FormedPlace(ResourceLocation model, int orientation, int x, int y, int z) {

    /** How a merged block keeps it. */
    public static final Codec<FormedPlace> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("model").forGetter(FormedPlace::model),
            Codec.INT.fieldOf("orientation").forGetter(FormedPlace::orientation),
            Codec.INT.fieldOf("x").forGetter(FormedPlace::x),
            Codec.INT.fieldOf("y").forGetter(FormedPlace::y),
            Codec.INT.fieldOf("z").forGetter(FormedPlace::z)).apply(instance, FormedPlace::new));

    public FormedPlace {
        Objects.requireNonNull(model, "model");
        if (orientation < 0 || orientation >= FormedShapes.ORIENTATIONS.size()) {
            throw new IllegalArgumentException("a structure stands one of " + FormedShapes.ORIENTATIONS.size()
                    + " ways, not " + orientation);
        }
    }

    /** How its structure stands. */
    public Orientation turn() {
        return FormedShapes.ORIENTATIONS.get(this.orientation);
    }

    /** The key of its block among the model's, as {@link MeshCells#key} makes them. */
    public long cell() {
        return MeshCells.key(this.x, this.y, this.z);
    }

    /** The same block once its structure has turned {@code rotation} about the up axis. */
    public FormedPlace turned(final Rotation rotation) {
        final Orientation by = switch (rotation) {
            case CLOCKWISE_90 -> Orientation.EAST;
            case CLOCKWISE_180 -> Orientation.SOUTH;
            case COUNTERCLOCKWISE_90 -> Orientation.WEST;
            case NONE -> Orientation.NORTH;
        };
        return by == Orientation.NORTH ? this : new FormedPlace(this.model,
                FormedShapes.ORIENTATIONS.indexOf(turn().then(by)), this.x, this.y, this.z);
    }
}
