/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain;

import static dev.jstech.computers.chain.ChainParts.part;

import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;

/**
 * The components a motherboard is assembled from (computers/docs/design/recipes.md, "The board's components"): a socket
 * for each processor family, a slot for each memory generation and each expansion bus, the storage and power
 * connectors. Each sits on the chain's shelf of the era a board or a processor of the catalogue first needs it in, and
 * knows what its recipe is reckoned from: its real pins and the mechanism it holds a part with. Quince's three sockets
 * came after the others and are reckoned after them, so none of the others' recipes moves.
 */
public final class BoardParts {

    public static final List<Component> CATALOGUE = List.of(
            expansionSlot(HardwareEra.VINTAGE, "isa", 98).named("ISA Slot"),
            expansionSlot(HardwareEra.VINTAGE, "pci", 120).named("PCI Slot"),
            expansionSlot(HardwareEra.VINTAGE, "agp", 132).named("AGP Slot"),
            memorySlot(HardwareEra.VINTAGE, "simm", 30).named("30-pin SIMM Slot"),
            memorySlot(HardwareEra.VINTAGE, "edo", 72).named("72-pin SIMM Slot"),
            memorySlot(HardwareEra.VINTAGE, "sdram", 168).named("SDRAM DIMM Slot"),
            power(HardwareEra.VINTAGE, "at", 12).named("AT Power Connector"),
            socket(HardwareEra.VINTAGE, "socket_3", 237, Mechanism.ZIF).named("Socket 3"),
            socket(HardwareEra.VINTAGE, "slot_1", 242, Mechanism.SLOT).named("Slot 1"),
            socket(HardwareEra.VINTAGE, "socket_7", 321, Mechanism.ZIF).named("Socket 7"),
            socket(HardwareEra.VINTAGE, "socket_8", 387, Mechanism.ZIF).named("Socket 8"),
            storage(HardwareEra.VINTAGE, "ide", 40).named("IDE Header"),
            expansionSlot(HardwareEra.LEGACY, "pcie", 164).named("PCIe x16 Slot"),
            memorySlot(HardwareEra.LEGACY, "ddr", 184).named("DDR DIMM Slot"),
            memorySlot(HardwareEra.LEGACY, "ddr2", 240).named("DDR2 DIMM Slot"),
            power(HardwareEra.LEGACY, "eps", 8).named("EPS 8-pin Connector"),
            power(HardwareEra.LEGACY, "atx20", 20).named("ATX 20-pin Connector"),
            socket(HardwareEra.LEGACY, "socket_370", 370, Mechanism.ZIF).named("Socket 370"),
            socket(HardwareEra.LEGACY, "socket_a", 462, Mechanism.ZIF).named("Socket A"),
            socket(HardwareEra.LEGACY, "socket_478", 478, Mechanism.ZIF).named("Socket 478"),
            socket(HardwareEra.LEGACY, "socket_604", 604, Mechanism.ZIF).named("Socket 604"),
            socket(HardwareEra.LEGACY, "socket_754", 754, Mechanism.ZIF).named("Socket 754"),
            socket(HardwareEra.LEGACY, "lga_775", 775, Mechanism.LGA).named("LGA 775"),
            socket(HardwareEra.LEGACY, "socket_939", 939, Mechanism.ZIF).named("Socket 939"),
            socket(HardwareEra.LEGACY, "socket_940", 940, Mechanism.ZIF).named("Socket 940"),
            memorySlot(HardwareEra.TRANSITION, "ddr3", 240).named("DDR3 DIMM Slot"),
            power(HardwareEra.TRANSITION, "atx24", 24).named("ATX 24-pin Connector"),
            socket(HardwareEra.TRANSITION, "lga_771", 771, Mechanism.LGA).named("LGA 771"),
            socket(HardwareEra.TRANSITION, "am2", 940, Mechanism.ZIF).named("AM2"),
            socket(HardwareEra.TRANSITION, "am3", 941, Mechanism.ZIF).named("AM3"),
            socket(HardwareEra.TRANSITION, "lga_1156", 1156, Mechanism.LGA).named("LGA 1156"),
            socket(HardwareEra.TRANSITION, "socket_f", 1207, Mechanism.LGA).named("Socket F"),
            socket(HardwareEra.TRANSITION, "lga_1366", 1366, Mechanism.LGA).named("LGA 1366"),
            storage(HardwareEra.TRANSITION, "sata", 7).named("SATA Port"),
            socket(HardwareEra.STANDARD, "fm2_plus", 906, Mechanism.ZIF).named("FM2+"),
            socket(HardwareEra.STANDARD, "am3_plus", 942, Mechanism.ZIF).named("AM3+"),
            socket(HardwareEra.STANDARD, "lga_1150", 1150, Mechanism.LGA).named("LGA 1150"),
            socket(HardwareEra.STANDARD, "lga_1155", 1155, Mechanism.LGA).named("LGA 1155"),
            socket(HardwareEra.STANDARD, "g34", 1944, Mechanism.LGA).named("G34"),
            socket(HardwareEra.STANDARD, "lga_2011", 2011, Mechanism.LGA).named("LGA 2011"),
            memorySlot(HardwareEra.ADVANCED, "ddr4", 288).named("DDR4 DIMM Slot"),
            memorySlot(HardwareEra.ADVANCED, "ddr5", 288).named("DDR5 DIMM Slot"),
            socket(HardwareEra.ADVANCED, "lga_1151", 1151, Mechanism.LGA).named("LGA 1151"),
            socket(HardwareEra.ADVANCED, "lga_1200", 1200, Mechanism.LGA).named("LGA 1200"),
            socket(HardwareEra.ADVANCED, "am4", 1331, Mechanism.ZIF).named("AM4"),
            socket(HardwareEra.ADVANCED, "lga_1700", 1700, Mechanism.LGA).named("LGA 1700"),
            socket(HardwareEra.ADVANCED, "am5", 1718, Mechanism.LGA).named("AM5"),
            socket(HardwareEra.ADVANCED, "lga_1851", 1851, Mechanism.LGA).named("LGA 1851"),
            socket(HardwareEra.ADVANCED, "lga_2066", 2066, Mechanism.LGA).named("LGA 2066"),
            socket(HardwareEra.ADVANCED, "lga_3647", 3647, Mechanism.LGA).named("LGA 3647"),
            socket(HardwareEra.ADVANCED, "str4", 4094, Mechanism.LGA).named("sTR4"),
            socket(HardwareEra.ADVANCED, "sp3", 4094, Mechanism.LGA).named("SP3"),
            socket(HardwareEra.ADVANCED, "lga_4189", 4189, Mechanism.LGA).named("LGA 4189"),
            socket(HardwareEra.ADVANCED, "lga_4677", 4677, Mechanism.LGA).named("LGA 4677"),
            socket(HardwareEra.ADVANCED, "str5", 4844, Mechanism.LGA).named("sTR5"),
            socket(HardwareEra.ADVANCED, "sp5", 6096, Mechanism.LGA).named("SP5"),
            storage(HardwareEra.ADVANCED, "m2", 75).named("M.2 Slot"));

    /** Quince's sockets: the 68000's DIP socket, the G3's and G4's processor card connector, the Q chips' socket. */
    public static final List<Component> QUINCE = List.of(
            socket(HardwareEra.VINTAGE, "quince_dip64", 64, Mechanism.DIP).named("Quince DIP-64 Socket"),
            socket(HardwareEra.LEGACY, "quince_card", 300, Mechanism.CARD).named("Quince Processor Card Connector"),
            socket(HardwareEra.ADVANCED, "quince_q", 2600, Mechanism.LGA).named("Quince Q Socket"));

    private BoardParts() {
    }

    /** Declares the components, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Every component, the catalogue's and then Quince's, in the order their recipes are reckoned. */
    public static List<Component> all() {
        final List<Component> out = new ArrayList<>(CATALOGUE);
        out.addAll(QUINCE);
        return List.copyOf(out);
    }

    /** The component whose item path is {@code path}: "socket_lga_1150". */
    public static Component byPath(final String path) {
        return all().stream().filter(component -> component.item().getId().getPath().equals(path)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no board component " + path));
    }

    private static Declaration socket(final Era era, final String path, final int pins, final Mechanism holds) {
        return new Declaration(Kind.SOCKET, era, path, pins, holds);
    }

    private static Declaration memorySlot(final Era era, final String path, final int pins) {
        return new Declaration(Kind.MEMORY_SLOT, era, path, pins, Mechanism.NONE);
    }

    private static Declaration expansionSlot(final Era era, final String path, final int pins) {
        return new Declaration(Kind.EXPANSION_SLOT, era, path, pins, Mechanism.NONE);
    }

    private static Declaration storage(final Era era, final String path, final int pins) {
        return new Declaration(Kind.STORAGE, era, path, pins, Mechanism.NONE);
    }

    private static Declaration power(final Era era, final String path, final int pins) {
        return new Declaration(Kind.POWER, era, path, pins, Mechanism.NONE);
    }

    /** What a component is, and how many of it one recipe makes: one socket, small connectors by the handful. */
    public enum Kind {
        SOCKET(1),
        MEMORY_SLOT(4),
        EXPANSION_SLOT(2),
        STORAGE(4),
        POWER(2);

        private final int made;

        Kind(final int made) {
            this.made = made;
        }

        /** How many of it one recipe makes. */
        public int made() {
            return made;
        }
    }

    /**
     * How a socket holds its processor, and the metal that takes: a ZIF socket's lever (a rod), a slot's two latches
     * (two rods), an LGA socket's load plate (a plate, two on the biggest, and a rod), a processor card's two rods,
     * a DIP socket nothing; the other components hold nothing.
     */
    public enum Mechanism {
        NONE,
        ZIF,
        SLOT,
        LGA,
        DIP,
        CARD
    }

    /**
     * One component.
     *
     * @param kind  what it is
     * @param era   the era a board or a processor first needs it in
     * @param pins  its real pins, which its contacts are reckoned from
     * @param holds how it holds a processor, for a socket
     * @param item  its item
     */
    public record Component(Kind kind, Era era, int pins, Mechanism holds, ItemEntry<Item> item) {
    }

    /* A component declared by its kind, era, path, pins and mechanism, named last, which registers it. */
    private record Declaration(Kind kind, Era era, String path, int pins, Mechanism holds) {

        Component named(final String english) {
            final String id = switch (kind) {
                case SOCKET -> "socket_" + path;
                case MEMORY_SLOT -> "memory_slot_" + path;
                case EXPANSION_SLOT -> "expansion_slot_" + path;
                case STORAGE -> "storage_" + path;
                case POWER -> "power_" + path;
            };
            return new Component(kind, era, pins, holds, part(id, era).named(english).register());
        }
    }
}
