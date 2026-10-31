/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

/**
 * How a block seals a room, as data: a wall's leak, the share of a whole open face its cracks let through (0 for none,
 * 1 for all of it), and the difference in pressure it stands before it bursts; a door, a wall that leaks as it says
 * while shut and lets everything through while open, and that keeps the rooms on its two sides apart.
 *
 * <pre>{@code
 * { "values": { "mymod:pressure_window": { "leak": 0, "rating": 2000 }, "#c:glass_blocks": { "leak": 0.0005,
 *   "rating": 120 }, "mymod:airlock_door": { "door": true, "leak": 0, "rating": 2000 } } }
 * }</pre>
 *
 * @param wall   whether it is a wall at all: a block that is not lets air through and is part of the room it is in
 * @param leak   the share of a whole open face its cracks let through, 0 to 1
 * @param rating the difference in pressure it stands, in kilopascals, before it bursts; none for one reckoned from how
 *               hard it is to blow up
 * @param door   whether it is a door: shut while its block says so, open otherwise
 */
public record Seal(boolean wall, double leak, Optional<Double> rating, boolean door) {

    /** A block that is no wall: air passes it, and it is part of the room it stands in. */
    public static final Seal PASSES = new Seal(false, 1.0, Optional.empty(), false);
    /** A whole solid block with no seal said: it does not leak, and stands as hard as it is to blow up. */
    public static final Seal SOLID = new Seal(true, 0.0, Optional.empty(), false);
    /** The most any block stands, in kilopascals. */
    public static final double STRONGEST = 1_000_000.0;
    /**
     * A seal's fields, to be written beside others: a wall unless said it is none, as furniture standing in a room is,
     * its leak none unless said.
     */
    public static final MapCodec<Seal> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("wall", true).forGetter(Seal::wall),
            Codec.doubleRange(0, 1).optionalFieldOf("leak", 0.0).forGetter(Seal::leak),
            Codec.doubleRange(0, STRONGEST).optionalFieldOf("rating").forGetter(Seal::rating),
            Codec.BOOL.optionalFieldOf("door", false).forGetter(Seal::door))
            .apply(instance, Seal::new));
    /** How a block's seal is written in the data map. */
    public static final Codec<Seal> CODEC = MAP_CODEC.codec();

    public Seal {
        leak = Math.clamp(leak, 0.0, 1.0);
    }

    /** A wall that leaks {@code leak} of a face and stands {@code rating} kilopascals. */
    public static Seal wall(final double leak, final double rating) {
        return new Seal(true, leak, Optional.of(rating), false);
    }

    /** A door that leaks {@code leak} of a face while shut and stands {@code rating} kilopascals. */
    public static Seal door(final double leak, final double rating) {
        return new Seal(true, leak, Optional.of(rating), true);
    }
}
