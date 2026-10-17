/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * How a hazard device is set, kept on the item: the Geiger counter's range and whether it clicks, the dosimeter's
 * light, and the hazard the map device shows.
 *
 * @param range the Geiger counter's range, 0 for its scale as printed, each step ten times it
 * @param sound whether the Geiger counter clicks
 * @param light whether the dosimeter's face is lit
 * @param shown the id of the hazard the map device shows; empty for the first the map knows
 */
public record DeviceState(int range, boolean sound, boolean light, String shown) {

    /** How a device comes: on its first range, clicking, unlit, on the first hazard. */
    public static final DeviceState DEFAULT = new DeviceState(0, true, false, "");
    /** How many ranges the Geiger counter has: as printed, ten times and a hundred times. */
    public static final int RANGES = 3;

    public static final Codec<DeviceState> CODEC = RecordCodecBuilder.create(state -> state.group(
            Codec.intRange(0, RANGES - 1).optionalFieldOf("range", 0).forGetter(DeviceState::range),
            Codec.BOOL.optionalFieldOf("sound", true).forGetter(DeviceState::sound),
            Codec.BOOL.optionalFieldOf("light", false).forGetter(DeviceState::light),
            Codec.STRING.optionalFieldOf("shown", "").forGetter(DeviceState::shown)
    ).apply(state, DeviceState::new));

    public static final StreamCodec<ByteBuf, DeviceState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DeviceState::range,
            ByteBufCodecs.BOOL, DeviceState::sound,
            ByteBufCodecs.BOOL, DeviceState::light,
            ByteBufCodecs.STRING_UTF8, DeviceState::shown,
            DeviceState::new);

    public DeviceState {
        range = Math.floorMod(range, RANGES);
    }

    /** The same, its RANGE button pressed: the next range, back to the first after the last. */
    public DeviceState nextRange() {
        return new DeviceState(this.range + 1, this.sound, this.light, this.shown);
    }

    /** The same, its SOUND button pressed. */
    public DeviceState toggleSound() {
        return new DeviceState(this.range, !this.sound, this.light, this.shown);
    }

    /** The same, its LIGHT button pressed. */
    public DeviceState toggleLight() {
        return new DeviceState(this.range, this.sound, !this.light, this.shown);
    }

    /** The same, showing the hazard {@code id}. */
    public DeviceState showing(final String id) {
        return new DeviceState(this.range, this.sound, this.light, id);
    }

    /** How many times its scale as printed the Geiger counter's range reads: 1, 10 or 100. */
    public int multiplier() {
        int times = 1;
        for (int i = 0; i < this.range; i++) {
            times *= 10;
        }
        return times;
    }
}
