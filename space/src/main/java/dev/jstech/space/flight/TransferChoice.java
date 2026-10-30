/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.flight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * How a player plans a transfer on the Flight Computer's XFER page: which window it leaves in, now or one of those to
 * come; how much more delta-v than the least it spends, to be there sooner; whether it swings past a world on the way
 * for a gravity assist, spending less and taking longer; and whether it aerobrakes on arrival rather than burning.
 *
 * @param window which window it leaves in: 0 now, 1 the next, 2 the one after, and so on
 * @param spend  which of {@link #SPENDS} it spends, from the least
 * @param assist whether it swings past a world for a gravity assist
 * @param aero   whether it aerobrakes on arrival where it can
 */
public record TransferChoice(int window, int spend, boolean assist, boolean aero) {

    public static final Codec<TransferChoice> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("window", 1).forGetter(TransferChoice::window),
            Codec.INT.optionalFieldOf("spend", 0).forGetter(TransferChoice::spend),
            Codec.BOOL.optionalFieldOf("assist", false).forGetter(TransferChoice::assist),
            Codec.BOOL.optionalFieldOf("aero", true).forGetter(TransferChoice::aero)
    ).apply(instance, TransferChoice::new));
    /** How much of the least delta-v each step spends, the trip shorter as it spends more. */
    public static final double[] SPENDS = {1.0, 1.25, 1.5, 2.0};
    /** The farthest window ahead it plans for. */
    public static final int FARTHEST_WINDOW = 4;
    /** The next window, the least delta-v, no assist, aerobraking where it can: the cheapest plan. */
    public static final TransferChoice CHEAPEST = new TransferChoice(1, 0, false, true);

    public TransferChoice {
        window = Math.max(0, Math.min(FARTHEST_WINDOW, window));
        spend = Math.max(0, Math.min(SPENDS.length - 1, spend));
    }

    /** The same, leaving {@code by} windows later, or earlier. */
    public TransferChoice windowBy(final int by) {
        return new TransferChoice(this.window + by, this.spend, this.assist, this.aero);
    }

    /** The same, spending {@code by} steps more delta-v, or less. */
    public TransferChoice spendBy(final int by) {
        return new TransferChoice(this.window, this.spend + by, this.assist, this.aero);
    }

    /** The same, with or without its gravity assist. */
    public TransferChoice assisted(final boolean on) {
        return new TransferChoice(this.window, this.spend, on, this.aero);
    }

    /** The same, aerobraking or burning on arrival. */
    public TransferChoice aerobraking(final boolean on) {
        return new TransferChoice(this.window, this.spend, this.assist, on);
    }

    /** How much of the least delta-v it spends. */
    public double spendShare() {
        return SPENDS[this.spend];
    }
}
