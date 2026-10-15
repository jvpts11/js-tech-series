/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.heat.HeatBalance;
import dev.jstech.core.heat.HeatBody;
import dev.jstech.core.heat.HeatMath;
import dev.jstech.core.heat.Thermal;

/**
 * A machine's heat: its casing, a body that holds heat ({@link HeatBody}) and offers it through the faces its Sides
 * tab opens to heat, the heat its work gives off while it runs, times what its upgrades say, and the hottest it stands.
 * Past that it stops to cool, and loses health while it stays past it unless the server says otherwise; it starts
 * again once ten kelvin cooler. What takes its heat away is the place around it and what it touches: a radiator, a
 * heat pipe, a cooler.
 *
 * <pre>{@code
 * holdsHeat(MachineHeat.of(Thermal.of(steel, false), 0.5, 2_000, 600));   // in the constructor: 2 kW while it works
 * }</pre>
 */
public final class MachineHeat {

    private final HeatBody body;
    private final double watts;
    private final double limit;
    private boolean cutOut;
    private double owed;

    /** How far below its limit a machine that stopped to cool starts again, in kelvin. */
    public static final double STARTS_BELOW = 10.0;
    /* Each so many kelvin past its limit costs a machine a point of health more a second. */
    private static final double KELVIN_A_POINT = 50.0;

    private MachineHeat(final HeatBody body, final double watts, final double limit) {
        if (!(watts >= 0.0) || !(limit > 0.0)) {
            throw new IllegalArgumentException("a machine gives off no less than no heat and stands a temperature "
                    + "above nothing");
        }
        this.body = body;
        this.watts = watts;
        this.limit = limit;
    }

    /**
     * A casing of {@code cubicMetres} of a material of {@code casing}, giving off {@code wattsWhileWorking} as it
     * works and standing up to {@code limitKelvin}.
     */
    public static MachineHeat of(final Thermal casing, final double cubicMetres, final double wattsWhileWorking,
                                 final double limitKelvin) {
        return new MachineHeat(HeatBody.of(casing, cubicMetres), wattsWhileWorking, limitKelvin);
    }

    /** The same with {@code squareMetres} of fins besides its faces, as a machine cooled by its own radiator. */
    public MachineHeat exposed(final double squareMetres) {
        this.body.exposed(squareMetres);
        return this;
    }

    /** Its casing's heat. */
    public HeatBody body() {
        return this.body;
    }

    /** What its work gives off while it runs, before upgrades, in watts. */
    public double watts() {
        return this.watts;
    }

    /** The hottest it stands, in kelvin. */
    public double limit() {
        return this.limit;
    }

    /** Whether it stopped to cool, past its limit and not yet ten kelvin below it. */
    public boolean cutOut() {
        return this.cutOut;
    }

    /**
     * One tick: the heat its work gave off, if it worked, times {@code heatFactor} from its upgrades; whether it is
     * past its limit; and the points of health it loses for it, if any.
     */
    int tick(final boolean working, final double heatFactor) {
        if (!this.body.known()) {
            return 0;
        }
        if (working && this.watts > 0.0) {
            this.body.exchange(this.watts * Math.max(0.0, heatFactor) * HeatMath.TICK_SECONDS);
        }
        final double kelvin = this.body.temperature();
        if (kelvin > this.limit) {
            this.cutOut = true;
        } else if (kelvin < this.limit - STARTS_BELOW) {
            this.cutOut = false;
        }
        if (!(kelvin > this.limit) || !HeatBalance.overheatingDamages()) {
            return 0;
        }
        this.owed += HeatMath.TICK_SECONDS * (1.0 + (kelvin - this.limit) / KELVIN_A_POINT);
        final int points = (int) this.owed;
        this.owed -= points;
        return points;
    }
}
