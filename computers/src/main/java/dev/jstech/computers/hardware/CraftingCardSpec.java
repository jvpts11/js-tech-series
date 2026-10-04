/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.computers.crafting.CraftingEras;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.tier.IndustrialTier;

import java.util.Objects;

/**
 * A Crafting Card: a crafting-accelerator FPGA on an expansion card of its era. It "compiles" recipes into hardware
 * pipelines and runs them, offloading a Crafting Computer's crafting work from the CPU. Its stats: {@code threads},
 * how many recipe/stage pipelines it can run in parallel (the ceiling on a computer's concurrent crafting stages),
 * {@code cpuFactor}, the crafting throughput it delivers, as a multiple of the host CPU's capacity, and by its era
 * how many Crafting Interfaces it drives and how many bench recipes its own ROM keeps.
 */
public record CraftingCardSpec(HardwareEra era, IndustrialTier tier, PcieGeneration bus, double cpuFactor,
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

    /** How many Crafting Interfaces the card drives. */
    public int interfaces() {
        return CraftingEras.interfacesPerCard(era);
    }

    /** How many bench recipes the card's ROM keeps. */
    public int romSize() {
        return CraftingEras.romPerCard(era);
    }
}
