/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A unit a mod counts energy in, with what it is worth in FE. Machines, cables and items work in FE among themselves,
 * as every mod of the game does; a unit is what a mod shows its players and keeps its numbers in, turned to FE and back
 * by its ratio wherever energy crosses into the game's own. Registered in {@link CoreEnergy#UNITS} by each mod.
 */
public final class EnergyUnit {

    private final TextKey name;
    private final TextKey symbol;
    private final EnergyRatio ratio;

    /**
     * @param name   what a player calls the unit
     * @param symbol the short mark written after an amount
     * @param ratio  how many of the unit are worth how many FE
     */
    public EnergyUnit(final TextKey name, final TextKey symbol, final EnergyRatio ratio) {
        this.name = Objects.requireNonNull(name, "name");
        this.symbol = Objects.requireNonNull(symbol, "symbol");
        this.ratio = Objects.requireNonNull(ratio, "ratio");
    }

    /** What a player calls the unit. */
    public Text name() {
        return this.name.text();
    }

    /** The short mark written after an amount. */
    public Text symbol() {
        return this.symbol.text();
    }

    /** How many of the unit are worth how many FE. */
    public EnergyRatio ratio() {
        return this.ratio;
    }

    /** How many FE {@code amount} of this unit is worth, rounded down. */
    public long toFe(final long amount) {
        return this.ratio.toFe(amount);
    }

    /** How many of this unit {@code amount} FE is worth, rounded down. */
    public long fromFe(final long amount) {
        return this.ratio.fromFe(amount);
    }

    /** {@code amount} of {@code from} in this unit, through FE, rounded down. */
    public long from(final EnergyUnit from, final long amount) {
        return from == this ? amount : fromFe(from.toFe(amount));
    }

    /** The id this unit is registered under, or null when it is not registered. */
    public @Nullable ResourceLocation id() {
        return CoreEnergy.UNITS.getKey(this);
    }
}
