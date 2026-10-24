/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.machine;

import dev.jstech.core.energy.CoreEnergy;
import dev.jstech.core.energy.EnergyClass;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import java.util.Objects;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * What a battery of J's Industrial, an item, says of itself (industrial/docs/design/energy.md): its tier, the class of
 * J's Energy it is made for, low voltage as every battery's, the energy it holds and the power it charges and
 * discharges at, the two measures of real storage. A machine's battery slot takes it, charging it from the grid and
 * drawing on it when the grid goes down.
 *
 * <pre>{@code
 * public static final BatteryRating LEAD_ACID = BatteryRating.of(IndustrialTier.T2, 2_500_000L, POWER);
 * // from the mod's RegisterCapabilitiesEvent:
 * LEAD_ACID.holdIn(event, LEAD_ACID_BATTERY.get());
 * }</pre>
 *
 * @param tier      its tier
 * @param portClass the class it is made for, of J's Energy
 * @param joules    the energy it holds at most
 * @param watts     the power it charges and discharges at
 */
public record BatteryRating(IndustrialTier tier, EnergyClass portClass, long joules, long watts) {

    public BatteryRating {
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(portClass, "portClass");
        if (!JsEnergy.ENERGY.classes().contains(portClass)) {
            throw new IllegalArgumentException("a battery of J's Industrial holds J's Energy, not of "
                    + portClass.path());
        }
        if (joules <= 0 || watts <= 0) {
            throw new IllegalArgumentException("a battery holds energy and moves it, above nothing; got " + joules
                    + " J at " + watts + " W");
        }
    }

    /** A battery of {@code tier} at low voltage, holding {@code joules}, charging and discharging at {@code watts}. */
    public static BatteryRating of(final IndustrialTier tier, final long joules, final long watts) {
        return new BatteryRating(tier, JsEnergy.LOW, joules, watts);
    }

    /** The most it charges or discharges in a tick, in joules. */
    public long joulesPerTick() {
        return IndustrialMachines.joulesPerTick(this.watts);
    }

    /** Makes {@code items} batteries of it: each holds its energy in itself, as J's Core's item energy does. */
    public void holdIn(final RegisterCapabilitiesEvent event, final ItemLike items) {
        CoreEnergy.holdsPort(event, items, JsEnergy.ENERGY, this.portClass, this.joules, joulesPerTick(),
                joulesPerTick());
    }
}
