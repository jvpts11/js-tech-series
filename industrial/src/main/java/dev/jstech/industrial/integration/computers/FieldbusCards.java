/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.integration.computers;

import dev.jstech.computers.api.ComputersRegisterEvent;
import dev.jstech.computers.hardware.ExpansionCardKind;
import dev.jstech.computers.hardware.PcieGeneration;
import dev.jstech.core.content.ItemBuilder;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.industrial.IndustrialModule;
import java.util.List;

/**
 * The Fieldbus Cards, one field bus of each era, which only an Industrial Controller Computer seats: an RS-485 line
 * speaking Modbus, PROFIBUS, PROFINET, EtherNet/IP and EtherCAT. A controller reads as many machines on the industrial
 * control cable as its cards add up to; how many fit is how many slots its board has.
 *
 * <p>The machines each card reads and what it draws are first estimates, to be tuned in play.
 */
@TextHolder
public final class FieldbusCards {

    /** What keeps a Fieldbus Card out of any other computer. */
    public static final TextKey FIELDBUS_ONLY = TextKey.of("jsindustrial.part_refusal.fieldbus_card",
            "Only an Industrial Controller Computer seats a Fieldbus Card");
    /** The kind every Fieldbus Card is, made for the Industrial Controller Computer alone. */
    public static final ExpansionCardKind FIELDBUS = new ExpansionCardKind("jsindustrial:fieldbus")
            .onlyIn(FIELDBUS_ONLY, IndustrialControllers.MACHINE);

    public static final ItemEntry<FieldbusCardItem> RS485 = card("fieldbus_card_rs485",
            new FieldbusCardSpec(HardwareEra.VINTAGE, PcieGeneration.ISA, 3, 2, FieldbusCardSpec.RS485))
            .named("RS-485 Fieldbus Card").register();
    public static final ItemEntry<FieldbusCardItem> PROFIBUS = card("fieldbus_card_profibus",
            new FieldbusCardSpec(HardwareEra.LEGACY, PcieGeneration.PCI, 4, 4, FieldbusCardSpec.PROFIBUS))
            .named("PROFIBUS Fieldbus Card").register();
    public static final ItemEntry<FieldbusCardItem> PROFINET = card("fieldbus_card_profinet",
            new FieldbusCardSpec(HardwareEra.TRANSITION, PcieGeneration.PCI, 5, 6, FieldbusCardSpec.RJ45))
            .named("PROFINET Fieldbus Card").register();
    public static final ItemEntry<FieldbusCardItem> ETHERNET_IP = card("fieldbus_card_ethernet_ip",
            new FieldbusCardSpec(HardwareEra.STANDARD, PcieGeneration.PCIE_2_0, 5, 8, FieldbusCardSpec.RJ45))
            .named("EtherNet/IP Fieldbus Card").register();
    public static final ItemEntry<FieldbusCardItem> ETHERCAT = card("fieldbus_card_ethercat",
            new FieldbusCardSpec(HardwareEra.ADVANCED, PcieGeneration.PCIE_3_0, 6, 12, FieldbusCardSpec.RJ45))
            .named("EtherCAT Fieldbus Card").register();
    /** Every Fieldbus Card, the oldest first. */
    public static final List<ItemEntry<FieldbusCardItem>> ALL = List.of(RS485, PROFIBUS, PROFINET, ETHERNET_IP,
            ETHERCAT);

    private FieldbusCards() {
    }

    /** Declares the cards, while the mod is built. */
    static void declare() {
        // Loading the class declares them.
    }

    /** Makes each card the part its sheet says; from the register event. */
    static void register(final ComputersRegisterEvent event) {
        event.cardKind(FIELDBUS);
        for (final ItemEntry<FieldbusCardItem> card : ALL) {
            final FieldbusCardSpec spec = card.get().spec();
            event.expansionCard(card.get(), spec, spec.era());
        }
    }

    private static ItemBuilder<FieldbusCardItem> card(final String id, final FieldbusCardSpec spec) {
        return IndustrialModule.CONTENT.item(id, properties -> new FieldbusCardItem(properties, spec))
                .tab(IndustrialModule.CONTROLLERS);
    }
}
