/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.energy.Energy;
import dev.jstech.core.energy.EnergyClass;
import dev.jstech.core.text.Text;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * What a machine draws of its energy now, and the port it draws through: what a controller reads of its power.
 *
 * @param energy  the energy it runs on
 * @param perTick how much of it it drew in the last tick
 * @param port    the class of its port, a voltage of the series' energy; none for a machine without one (on FE)
 */
public record PowerDrawn(Energy energy, long perTick, @Nullable EnergyClass port) {

    public PowerDrawn {
        Objects.requireNonNull(energy, "energy");
    }

    /** How much it draws in a second: watts, for the series' energy. */
    public long perSecond() {
        return Energy.perSecond(this.perTick);
    }

    /** The class of its port, when it has one. */
    public Optional<EnergyClass> portClass() {
        return Optional.ofNullable(this.port);
    }

    /** What it draws as a player reads it: "182 kW". */
    public Text text() {
        return this.energy.rate(this.perTick);
    }
}
