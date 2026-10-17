/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.atmosphere;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * What an Atmosphere Detector read last: the pressure and how fast it changes, each gas and its share, and, in a sealed
 * room, the room's size and its leaks, each where it is in the room's box.
 *
 * @param pressure the pressure, in kilopascals
 * @param change   how fast it changes, in kilopascals a second
 * @param gases    each gas, the largest share first
 * @param room     whether a sealed room holds the air it reads
 * @param volume   the room's size, in blocks
 * @param size     the size of the room's box, across, up and along
 * @param leaks    how many blocks the room leaks through
 * @param spots    where the first of its leaks are, in its box
 */
public record AtmosphereReadings(double pressure, double change, List<Gas> gases, boolean room, int volume,
                                 BlockPos size, int leaks, List<BlockPos> spots) {

    /** Nothing read yet. */
    public static final AtmosphereReadings NONE = new AtmosphereReadings(0, 0, List.of(), false, 0, BlockPos.ZERO, 0,
            List.of());
    /** The most leaks marked on its drawing of the room, and the most gases on its display. */
    public static final int MOST_SPOTS = 8;
    public static final int MOST_GASES = 4;
    public static final Codec<AtmosphereReadings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("pressure").forGetter(AtmosphereReadings::pressure),
            Codec.DOUBLE.fieldOf("change").forGetter(AtmosphereReadings::change),
            Gas.CODEC.listOf().fieldOf("gases").forGetter(AtmosphereReadings::gases),
            Codec.BOOL.fieldOf("room").forGetter(AtmosphereReadings::room),
            Codec.INT.fieldOf("volume").forGetter(AtmosphereReadings::volume),
            BlockPos.CODEC.fieldOf("size").forGetter(AtmosphereReadings::size),
            Codec.INT.fieldOf("leaks").forGetter(AtmosphereReadings::leaks),
            BlockPos.CODEC.listOf().fieldOf("spots").forGetter(AtmosphereReadings::spots))
            .apply(instance, AtmosphereReadings::new));

    public AtmosphereReadings {
        gases = List.copyOf(gases);
        spots = List.copyOf(spots);
    }

    /** The share of the air {@code gas} makes up. */
    public double share(final ResourceLocation gas) {
        for (final Gas each : this.gases) {
            if (each.id().equals(gas)) {
                return each.share();
            }
        }
        return 0;
    }

    /**
     * A gas of the air.
     *
     * @param id    its material's id
     * @param share its share of the air, 0 to 1
     */
    public record Gas(ResourceLocation id, double share) {

        public static final Codec<Gas> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(Gas::id),
                Codec.DOUBLE.fieldOf("share").forGetter(Gas::share))
                .apply(instance, Gas::new));
    }
}
