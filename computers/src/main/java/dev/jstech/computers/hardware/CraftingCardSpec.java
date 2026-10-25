/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.computers.crafting.CraftingEras;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.IndustrialTier;

import java.util.Objects;

/**
 * A Crafting Card: a crafting-accelerator FPGA on an expansion card of its era. It "compiles" recipes into hardware
 * pipelines and runs them, offloading a Crafting Computer's crafting work from the CPU. Its stats: {@code threads},
 * how many recipe/stage pipelines it can run in parallel (the ceiling on a computer's concurrent crafting stages),
 * {@code cpuFactor}, the crafting throughput it delivers, as a multiple of the host CPU's capacity, and by its era
 * how many Crafting Interfaces it drives and how many bench recipes its own ROM keeps.
 */
public record CraftingCardSpec(Era era, IndustrialTier tier, PcieGeneration bus, double cpuFactor,
                               int threads, int tdpWatts) implements IExpansionCardSpec {

    public CraftingCardSpec {
        Objects.requireNonNull(era, "era must not be null");
        Objects.requireNonNull(tier, "tier must not be null");
        Objects.requireNonNull(bus, "bus must not be null");
        if (cpuFactor <= 0) {
            throw new IllegalArgumentException("cpuFactor must be > 0; got " + cpuFactor);
        }
        if (threads < 1) {
            throw new IllegalArgumentException("threads must be >= 1; got " + threads);
        }
        if (tdpWatts < 0) {
            throw new IllegalArgumentException("tdpWatts must be >= 0; got " + tdpWatts);
        }
    }

    @Override
    public ExpansionCardKind kind() {
        return ExpansionCardKind.CRAFTING;
    }

    /*
     * A PCI card seats in a PCI, AGP or PCI Express board alike, as the personal-use cards do: the boards of those
     * years carried PCI slots beside their graphics slot, and the Legacy's own boards are AGP or PCI Express, so a
     * PCI card held to PCI boards alone would fit no machine of the era it is made for. Every other card keeps to its
     * own bus family.
     */
    @Override
    public boolean fits(final PcieGeneration slot) {
        return bus == PcieGeneration.PCI ? slot.busFamily() != ExpansionBus.ISA
                : IExpansionCardSpec.super.fits(slot);
    }

    /** How many Crafting Interfaces the card drives. */
    public int interfaces() {
        return CraftingEras.interfacesPerCard(era);
    }

    /** How many bench recipes the card's ROM keeps. */
    public int romSize() {
        return CraftingEras.romPerCard(era);
    }
}
