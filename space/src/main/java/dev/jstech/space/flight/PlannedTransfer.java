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
import dev.jstech.space.orbit.Rocketry;
import dev.jstech.space.orbit.Transfers;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import net.minecraft.resources.ResourceLocation;

/**
 * A transfer as planned on the XFER page, as the computer will fly it: the world it goes to, when it leaves, the
 * delta-v of its transfer burn and of its capture on arrival, how long the cruise takes, and whether it swings past a
 * world for a gravity assist and aerobrakes on arrival. A gravity assist spends less and takes longer, as Cassini's
 * swings past Venus, the Earth and Jupiter did; spending more than the least shortens the cruise; aerobraking, where
 * the destination has air and the rocket a heat shield, saves the capture burn.
 *
 * @param to          the world it goes to, by its id among the worlds
 * @param leaves      the game's tick it leaves at
 * @param burn        the delta-v of its transfer burn, in metres a second
 * @param capture     the delta-v of its capture burn on arrival; nought aerobraking
 * @param travel      how long the cruise takes, in ticks
 * @param assisted    whether it swings past a world for a gravity assist
 * @param aerobraking whether it aerobrakes on arrival
 */
public record PlannedTransfer(ResourceLocation to, long leaves, double burn, double capture, long travel,
                              boolean assisted, boolean aerobraking) {

    public static final Codec<PlannedTransfer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("to").forGetter(PlannedTransfer::to),
            Codec.LONG.fieldOf("leaves").forGetter(PlannedTransfer::leaves),
            Codec.DOUBLE.fieldOf("burn").forGetter(PlannedTransfer::burn),
            Codec.DOUBLE.fieldOf("capture").forGetter(PlannedTransfer::capture),
            Codec.LONG.fieldOf("travel").forGetter(PlannedTransfer::travel),
            Codec.BOOL.fieldOf("assisted").forGetter(PlannedTransfer::assisted),
            Codec.BOOL.fieldOf("aerobraking").forGetter(PlannedTransfer::aerobraking)
    ).apply(instance, PlannedTransfer::new));
    /*
     * What a gravity assist saves of the transfer burn, and how much longer it makes the cruise: estimates, to be set
     * by playing.
     */
    private static final double ASSIST_SAVES = 0.85;
    private static final double ASSIST_LONGER = 1.6;

    /**
     * {@code plan}, the transfer as it stands at the tick {@code tick}, flown as {@code choice} says by a rocket that
     * has a heat shield, {@code shield}, or not: a gravity assist only for a transfer with windows, aerobraking only
     * where the destination has air and the rocket a shield.
     */
    public static PlannedTransfer of(final TransferPlan plan, final TransferChoice choice, final boolean shield,
                                     final long tick) {
        final long leaves;
        double least;
        double usual = plan.travel();
        if (!plan.windowed() || choice.window() == 0) {
            leaves = tick;
            least = plan.deltaVNow();
        } else {
            leaves = tick + plan.toWindow() + (long) (choice.window() - 1) * plan.between();
            least = plan.deltaVWindow();
        }
        final boolean assisted = choice.assist() && plan.windowed();
        if (assisted) {
            least *= ASSIST_SAVES;
            usual *= ASSIST_LONGER;
        }
        final double burn = least * choice.spendShare();
        final boolean aerobraking = choice.aero() && plan.aerobraking() && shield;
        return new PlannedTransfer(SolarSystemWorlds.worldOf(plan.to()), leaves, burn,
                aerobraking ? 0 : captureAt(plan.to()), Math.round(Transfers.travelTicks(usual, least, burn)),
                assisted, aerobraking);
    }

    /** The same with no cruise, arriving at once, as the server's fast travel flies it. */
    public PlannedTransfer atOnce() {
        return new PlannedTransfer(this.to, this.leaves, this.burn, this.capture, 0, this.assisted, this.aerobraking);
    }

    /** The delta-v it needs in all: its transfer burn and its capture. */
    public double needed() {
        return this.burn + this.capture;
    }

    /**
     * The capture burn it takes on arriving, its rocket's heat shield whole or not, {@code shield}: aerobraking with a
     * shield takes none; one breached on the way cannot, and it burns the capture it meant to save.
     */
    public double captureBurn(final boolean shield) {
        if (!this.aerobraking || shield) {
            return this.capture;
        }
        return SolarSystemWorlds.bodyOf(this.to).map(PlannedTransfer::captureAt).orElse(0.0);
    }

    /* The capture burn into {@code to}'s orbit with no aerobraking: the Moon's is in its transfer burn already. */
    private static double captureAt(final SolarBody to) {
        return switch (to) {
            case MARS -> Rocketry.MARS_CAPTURE;
            case OVERWORLD -> Rocketry.OVERWORLD_CAPTURE;
            default -> 0;
        };
    }
}
