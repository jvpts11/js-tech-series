/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a converter's screen shows of it, worked out on the server and carried to the screen as it changes: its class,
 * whether it is a fixed one, on or off, which way it works, the limit on its thumbwheels, what it moved in the last tick
 * and against which limit, and its lamps.
 *
 * @param voltage the rank of its class, 1 for the low class to 5
 * @param fixed   whether it is a converter of one class
 * @param on      whether its switch is on
 * @param toJe    whether it works from FE to J's Energy; from J's Energy to FE otherwise
 * @param digits  what its digit drums show
 * @param power   the power of ten its prefix drum stands for
 * @param movedFe what crossed it in the last tick, in FE
 * @param movedJ  the same, in joules
 * @param limitFe what it moves at most in a tick, in FE: its limit, under its class's ceiling
 * @param limitJ  the same, in joules
 * @param limited whether it is held at its limit
 * @param fault   whether a line of the wrong class, a blown fuse or a burn stops it
 */
public record ConverterView(int voltage, boolean fixed, boolean on, boolean toJe, int digits, int power, long movedFe,
                            long movedJ, long limitFe, long limitJ, boolean limited, boolean fault) {

    /** Nothing yet, until the server's first word. */
    public static final ConverterView NONE = new ConverterView(1, false, false, true, 0, ConverterLimit.KILO, 0L, 0L,
            0L, 0L, false, false);
    public static final StreamCodec<ByteBuf, ConverterView> STREAM_CODEC = StreamCodec.of(ConverterView::write,
            ConverterView::read);

    /** Its class. */
    public VoltageClass voltageClass() {
        return VoltageClass.ofRank(this.voltage).orElse(VoltageClass.LOW);
    }

    /** The limit on its thumbwheels. */
    public ConverterLimit limit() {
        return new ConverterLimit(this.digits, this.power);
    }

    /** Whether it moved anything in the last tick. */
    public boolean running() {
        return this.movedFe > 0;
    }

    private static void write(final ByteBuf buf, final ConverterView view) {
        ByteBufCodecs.VAR_INT.encode(buf, view.voltage);
        ByteBufCodecs.BOOL.encode(buf, view.fixed);
        ByteBufCodecs.BOOL.encode(buf, view.on);
        ByteBufCodecs.BOOL.encode(buf, view.toJe);
        ByteBufCodecs.VAR_INT.encode(buf, view.digits);
        ByteBufCodecs.VAR_INT.encode(buf, view.power);
        ByteBufCodecs.VAR_LONG.encode(buf, view.movedFe);
        ByteBufCodecs.VAR_LONG.encode(buf, view.movedJ);
        ByteBufCodecs.VAR_LONG.encode(buf, view.limitFe);
        ByteBufCodecs.VAR_LONG.encode(buf, view.limitJ);
        ByteBufCodecs.BOOL.encode(buf, view.limited);
        ByteBufCodecs.BOOL.encode(buf, view.fault);
    }

    private static ConverterView read(final ByteBuf buf) {
        final int voltage = ByteBufCodecs.VAR_INT.decode(buf);
        final boolean fixed = ByteBufCodecs.BOOL.decode(buf);
        final boolean on = ByteBufCodecs.BOOL.decode(buf);
        final boolean toJe = ByteBufCodecs.BOOL.decode(buf);
        final int digits = Math.max(0, Math.min(ConverterLimit.MOST_DIGITS, ByteBufCodecs.VAR_INT.decode(buf)));
        final int power = ByteBufCodecs.VAR_INT.decode(buf);
        final int safePower = power == ConverterLimit.MEGA || power == ConverterLimit.GIGA ? power
                : ConverterLimit.KILO;
        return new ConverterView(voltage, fixed, on, toJe, digits, safePower, ByteBufCodecs.VAR_LONG.decode(buf),
                ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf),
                ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.BOOL.decode(buf));
    }
}
