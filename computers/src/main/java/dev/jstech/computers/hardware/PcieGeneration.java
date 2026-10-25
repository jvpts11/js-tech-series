/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.text.Text;
import dev.jstech.core.util.DeclarationError;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A generation of expansion slot, which a card is made for and a motherboard offers: the old ISA, PCI and AGP buses,
 * then the PCI Express line.
 *
 * <p>An id rather than a closed set, as a socket is, so a mod bringing an era of its own brings its slot too. This
 * mod's own, oldest to newest, are the constants here.
 *
 * @param id         the generation's id, {@code namespace:path}: {@code jsc:pcie_3_0}
 * @param generation its place in the line, oldest first: one step is roughly a doubling of bandwidth
 * @param label      the bus as a part's sheet names it, the same in every language: "PCI-e 3.0"
 * @param slotName   the slot as its specifications name it, the same in every language: "PCI Express 3.0 x16"
 * @param family     the physical bus family it belongs to, which decides which cards it takes
 */
@ApiStatus.Experimental
public record PcieGeneration(String id, int generation, Text label, Text slotName, ExpansionBus family) {

    private static final DeclaredKinds<PcieGeneration> KINDS = new DeclaredKinds<>("slot generation",
            PcieGeneration::id);
    /* The least of its throughput a card keeps however old the slot: an ancient board is slow, not a wall. */
    private static final double FLOOR = 0.125;

    public static final PcieGeneration ISA = own("isa", 0, "ISA", "ISA", ExpansionBus.ISA);
    public static final PcieGeneration PCI = own("pci", 1, "PCI", "PCI", ExpansionBus.PCI);
    /** The first AGP, of the Slot 1 boards and the cards of 1998. */
    public static final PcieGeneration AGP_2X = own("agp_2x", 2, "AGP 2x", "AGP 2x", ExpansionBus.AGP);
    public static final PcieGeneration AGP_4X = own("agp_4x", 3, "AGP 4x", "AGP 4x", ExpansionBus.AGP);
    public static final PcieGeneration AGP_8X = own("agp_8x", 4, "AGP 8x", "AGP 8x", ExpansionBus.AGP);
    public static final PcieGeneration PCIE_1_0 = express("pcie_1_0", 5, "1.0");
    public static final PcieGeneration PCIE_2_0 = express("pcie_2_0", 6, "2.0");
    public static final PcieGeneration PCIE_3_0 = express("pcie_3_0", 7, "3.0");
    public static final PcieGeneration PCIE_4_0 = express("pcie_4_0", 8, "4.0");
    public static final PcieGeneration PCIE_5_0 = express("pcie_5_0", 9, "5.0");
    public static final PcieGeneration PCIE_6_0 = express("pcie_6_0", 10, "6.0");

    public PcieGeneration {
        HardwareIds.requireNamespaced(id, "a slot generation");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(slotName, "slotName");
        Objects.requireNonNull(family, "family");
    }

    /**
     * Declares a generation an add-on brings, while the game loads: what the register event does.
     *
     * @throws DeclarationError when one of the same id is declared already, or the game has loaded
     */
    public static PcieGeneration declare(final PcieGeneration generation) {
        return KINDS.declare(generation);
    }

    /** The generation of {@code id}, or null when none is declared; a bare name is one of this mod's own. */
    public static @Nullable PcieGeneration byId(final String id) {
        return KINDS.byId(id);
    }

    /** Every generation, this mod's own first, oldest to newest. */
    public static List<PcieGeneration> all() {
        return KINDS.all();
    }

    /** The physical bus family this generation belongs to. */
    public ExpansionBus busFamily() {
        return family;
    }

    /**
     * Whether this card bus is compatible with the given motherboard slot. ISA, PCI, and AGP are physically distinct
     * and reject each other; all PCIe generations are cross-compatible.
     *
     * <p>A newer card in an older slot still runs, and is simply held to the older slot's bandwidth. See
     * {@link #bandwidthFactorIn}.
     */
    public boolean compatibleWith(final PcieGeneration slot) {
        return this.family == slot.family;
    }

    /**
     * How much of this card's throughput survives in {@code slot}, as a fraction of 1.0. Each bus generation carries
     * roughly twice the bandwidth of the one before it, so a card seated in a slot one generation older gets about
     * half its lane bandwidth, two generations older about a quarter, and so on. A card in a slot of its own
     * generation or newer runs at full speed.
     *
     * <p>The floor keeps an extreme mismatch slow rather than useless: an ancient board is a bad home for a modern
     * card, not a brick wall.
     */
    public double bandwidthFactorIn(final PcieGeneration slot) {
        final int behind = this.generation - slot.generation;
        if (behind <= 0) {
            return 1.0;
        }
        return Math.max(FLOOR, Math.pow(0.5, behind));
    }

    /** Closes the list once the game has loaded; from the mod. */
    static void close() {
        KINDS.close();
    }

    private static PcieGeneration express(final String path, final int generation, final String version) {
        return KINDS.declare(new PcieGeneration(HardwareIds.own(path), generation, Text.literal("PCI-e " + version),
                Text.literal("PCI Express " + version + " x16"), ExpansionBus.PCIE));
    }

    private static PcieGeneration own(final String path, final int generation, final String label,
                                      final String slotName, final ExpansionBus family) {
        return KINDS.declare(new PcieGeneration(HardwareIds.own(path), generation, Text.literal(label),
                Text.literal(slotName), family));
    }
}
