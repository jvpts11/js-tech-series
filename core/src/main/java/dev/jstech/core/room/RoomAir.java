/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.air.AirMath;
import dev.jstech.core.air.AirMix;
import dev.jstech.core.dimension.Climate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * The air of a room at a moment: how much of each gas it holds, in moles, by the gas's material's id, in how many
 * cubic metres, at what warmth. Its pressure follows from them as an ideal gas's; whether it is breathed, from its
 * oxygen's own pressure.
 *
 * @param moles  each gas's amount, in moles
 * @param volume the room's volume the gas fills, in cubic metres
 * @param kelvin its warmth, in kelvin
 */
public record RoomAir(Map<ResourceLocation, Double> moles, double volume, double kelvin) {

    /** No air. */
    public static final RoomAir NONE = new RoomAir(Map.of(), 0, GasMath.TEMPERED);
    /* Water freezes at this many kelvin: a climate reads in degrees Celsius. */
    private static final double ZERO_CELSIUS = 273.15;
    public static final Codec<RoomAir> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.DOUBLE).fieldOf("moles").forGetter(RoomAir::moles),
            Codec.DOUBLE.fieldOf("volume").forGetter(RoomAir::volume),
            Codec.DOUBLE.fieldOf("kelvin").forGetter(RoomAir::kelvin))
            .apply(instance, RoomAir::new));

    public RoomAir {
        moles = Map.copyOf(new LinkedHashMap<>(moles));
    }

    /** Its pressure, in kilopascals. */
    public double pressure() {
        return GasMath.pressure(total(), this.volume, this.kelvin);
    }

    /** How many moles of gas it holds in all. */
    public double total() {
        double sum = 0;
        for (final double amount : this.moles.values()) {
            sum += Math.max(0, amount);
        }
        return sum;
    }

    /** The share of it {@code gas} makes up, 0 to 1. */
    public double share(final ResourceLocation gas) {
        final double total = total();
        return total <= 0 ? 0 : Math.max(0, this.moles.getOrDefault(gas, 0.0)) / total;
    }

    /** The partial pressure of {@code gas} in it, in kilopascals. */
    public double partial(final ResourceLocation gas) {
        return AirMath.partial(share(gas), pressure());
    }

    /** Whether it can be breathed: its oxygen's own pressure is enough. */
    public boolean breathable() {
        return AirMath.breathable(share(AirMix.OXYGEN), pressure());
    }

    /** What it is made of, each gas by its share. */
    public AirMix mix() {
        final Map<ResourceLocation, Double> shares = new LinkedHashMap<>();
        for (final ResourceLocation gas : this.moles.keySet()) {
            final double share = share(gas);
            if (share > 0) {
                shares.put(gas, share);
            }
        }
        return new AirMix(shares);
    }

    /** It as a climate: its pressure, its warmth, its air, and whether it is breathed. */
    public Climate climate() {
        return new Climate(pressure(), this.kelvin - ZERO_CELSIUS, Optional.of(mix()), breathable());
    }
}
