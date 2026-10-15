/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * How a clamp meter is set, kept in the item: its dial (off, the energy's own unit, or FE) and its four keys: HOLD keeps
 * the reading still, MAX MIN shows the highest and the lowest since it was clamped, SHOW puts the reading beside the
 * crosshair while it is held, and LIGHT lights the display.
 *
 * @param dial   the dial's position
 * @param hold   whether the reading is held still
 * @param maxMin whether the highest and the lowest are shown
 * @param show   whether the reading shows beside the crosshair
 * @param light  whether the display is lit
 */
public record MeterState(Dial dial, boolean hold, boolean maxMin, boolean show, boolean light) {

    /** How a meter comes: reading the energy's own unit, on the HUD, the rest off. */
    public static final MeterState DEFAULT = new MeterState(Dial.OWN, false, false, true, false);

    public static final Codec<MeterState> CODEC = RecordCodecBuilder.create(state -> state.group(
            StableCodecs.byName(Dial.class).fieldOf("dial").forGetter(MeterState::dial),
            Codec.BOOL.fieldOf("hold").forGetter(MeterState::hold),
            Codec.BOOL.fieldOf("max_min").forGetter(MeterState::maxMin),
            Codec.BOOL.fieldOf("show").forGetter(MeterState::show),
            Codec.BOOL.fieldOf("light").forGetter(MeterState::light)
    ).apply(state, MeterState::new));

    public static final StreamCodec<ByteBuf, MeterState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodec(StableCodecs.byName(Dial.class)), MeterState::dial,
            ByteBufCodecs.BOOL, MeterState::hold,
            ByteBufCodecs.BOOL, MeterState::maxMin,
            ByteBufCodecs.BOOL, MeterState::show,
            ByteBufCodecs.BOOL, MeterState::light,
            MeterState::new);

    /** The same, its dial turned to the next position. */
    public MeterState turned() {
        return new MeterState(this.dial.next(), this.hold, this.maxMin, this.show, this.light);
    }

    /** The same, its HOLD key pressed. */
    public MeterState toggleHold() {
        return new MeterState(this.dial, !this.hold, this.maxMin, this.show, this.light);
    }

    /** The same, its MAX MIN key pressed. */
    public MeterState toggleMaxMin() {
        return new MeterState(this.dial, this.hold, !this.maxMin, this.show, this.light);
    }

    /** The same, its SHOW key pressed. */
    public MeterState toggleShow() {
        return new MeterState(this.dial, this.hold, this.maxMin, !this.show, this.light);
    }

    /** The same, its LIGHT key pressed. */
    public MeterState toggleLight() {
        return new MeterState(this.dial, this.hold, this.maxMin, this.show, !this.light);
    }

    /** Where the dial points. */
    public enum Dial implements IStableName {
        /** Off: the meter reads nothing. */
        OFF("off"),
        /** The energy's own unit: joules and watts for J's Energy. */
        OWN("own"),
        /** FE, whatever the energy. */
        FE("fe");

        private final String serializedName;

        Dial(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }

        /** The next position, clockwise, back to off after FE. */
        public Dial next() {
            return switch (this) {
                case OFF -> OWN;
                case OWN -> FE;
                case FE -> OFF;
            };
        }
    }
}
