/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.machine;

import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.machine.IMachineDamage;
import dev.jstech.core.machine.MachineEnergy;
import dev.jstech.core.machine.MachineTransfers;
import dev.jstech.industrial.JsIndustrial;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * The rules every machine of J's Industrial shares, over J's Core's machine kit (industrial/docs/design/machines.md):
 * its upgrade slots by its tier, few so a machine is fast or thrifty and hardly both; what damage does to it; the
 * store of its port, a couple of seconds of its work; and the upgrades that hold its radiation in.
 */
public final class IndustrialMachines {

    /** A damaged machine runs at half speed and draws a quarter more, its upgrades idle until it is repaired. */
    public static final IMachineDamage DAMAGE = IMachineDamage.flat(0.5, 1.25);
    /** The upgrades that hold a machine's radiation in while it works, as a Lead Shielding does. */
    public static final TagKey<Item> SHIELDS_RADIATION = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID, "upgrades/shields_radiation"));
    /**
     * How many ticks of its work a machine's port holds, and how many it takes in at once: a round of what a generator
     * against it pushes, which the Core's machines do every {@link MachineTransfers#EVERY} ticks.
     */
    public static final int STORE_TICKS = 40;
    public static final int TAKES_TICKS = MachineTransfers.EVERY;
    private static final int TICKS_A_SECOND = 20;
    private static final int FEW_SLOTS = 2;
    private static final int SOME_SLOTS = 3;
    private static final int MOST_SLOTS = 4;

    private IndustrialMachines() {
    }

    /**
     * The upgrade slots of a machine of {@code tier}: 2 on a T1 machine, 3 on T2 and T3, 4 from T4.
     *
     * @throws IllegalArgumentException for the T0, whose tools come before any machine
     */
    public static int upgradeSlots(final IndustrialTier tier) {
        if (tier.isAtMost(IndustrialTier.T0)) {
            throw new IllegalArgumentException("the T0 is worked by hand: it has no machine to fit an upgrade in");
        }
        if (tier.isAtMost(IndustrialTier.T1)) {
            return FEW_SLOTS;
        }
        return tier.isAtMost(IndustrialTier.T3) ? SOME_SLOTS : MOST_SLOTS;
    }

    /** The joules a tick of {@code watts}: a twentieth of them, rounded up. */
    public static long joulesPerTick(final long watts) {
        return Math.ceilDiv(watts, TICKS_A_SECOND);
    }

    /**
     * The port of a machine of {@code rating}, of its class of J's Energy: holding {@link #STORE_TICKS} ticks of its
     * work, so a short gap in the line is bridged, and taking in at most {@link #TAKES_TICKS} of them at once.
     */
    public static MachineEnergy energy(final MachineRating rating) {
        final long perTick = rating.joulesPerTick();
        return MachineEnergy.of(JsEnergy.ENERGY, rating.portClass(), perTick * STORE_TICKS, perTick * TAKES_TICKS);
    }

    /**
     * The port of a generator of {@code rating}: a round of its pushing, {@link MachineTransfers#EVERY} ticks of its
     * power, all of which it gives at once, so a machine against it is fed as fully as one on a cable.
     */
    public static MachineEnergy generatorEnergy(final MachineRating rating) {
        final long round = rating.joulesPerTick() * MachineTransfers.EVERY;
        return MachineEnergy.of(JsEnergy.ENERGY, rating.portClass(), round, 0L).giving(round);
    }
}
