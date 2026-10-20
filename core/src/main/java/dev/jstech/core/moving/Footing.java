/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.uuid.StructureUuid;
import java.util.Optional;

/**
 * The moving structure a player stands on, and where on it, in the structure's own places in the yard: kept with
 * the player, so one who leaves the game on a deck that goes on flying comes back standing where they were on it,
 * wherever it is by then.
 *
 * @param structure the structure stood on, or none
 * @param x         the player's place on it, in the yard, east
 * @param y         the player's place on it, in the yard, up
 * @param z         the player's place on it, in the yard, south
 * @param yaw       how far the player looks from the structure's own forward, in degrees
 */
public record Footing(Optional<StructureUuid> structure, double x, double y, double z, float yaw) {

    /** Standing on no structure. */
    public static final Footing NONE = new Footing(Optional.empty(), 0, 0, 0, 0);
    public static final Codec<Footing> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StructureUuid.CODEC.optionalFieldOf("structure").forGetter(Footing::structure),
            Codec.DOUBLE.fieldOf("x").forGetter(Footing::x),
            Codec.DOUBLE.fieldOf("y").forGetter(Footing::y),
            Codec.DOUBLE.fieldOf("z").forGetter(Footing::z),
            Codec.FLOAT.fieldOf("yaw").forGetter(Footing::yaw)
    ).apply(instance, Footing::new));

    /** Standing at {@code (x, y, z)} of the yard on {@code structure}, looking {@code yaw} from its forward. */
    public static Footing on(final StructureUuid structure, final double x, final double y, final double z,
                             final float yaw) {
        return new Footing(Optional.of(structure), x, y, z, yaw);
    }
}
