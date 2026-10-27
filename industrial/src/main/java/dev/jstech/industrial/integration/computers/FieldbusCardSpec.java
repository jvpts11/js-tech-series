/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.integration.computers;

import dev.jstech.computers.hardware.ExpansionBus;
import dev.jstech.computers.hardware.ExpansionCardKind;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.PcieGeneration;
import dev.jstech.core.jstechseries.tier.Era;
import java.util.Objects;

/**
 * A Fieldbus Card: the card an Industrial Controller Computer reads machines with, one field bus of its era, on the
 * slot that era's boards had. It reads as many machines on the industrial control cable as its bus is given, and the
 * cable plugs into it by its bus's connector.
 *
 * @param era      the era it is shown with
 * @param bus      the slot it goes in
 * @param tdpWatts what it draws
 * @param machines how many machines it lets its computer read
 * @param plug     its connector, the kind of port the control cable's plug enters at the computer
 */
public record FieldbusCardSpec(Era era, PcieGeneration bus, int tdpWatts, int machines, String plug)
        implements IExpansionCardSpec {

    /** The RJ45 of the field buses that run on Ethernet: PROFINET, EtherNet/IP and EtherCAT. */
    public static final String RJ45 = "rj45";
    /** PROFIBUS's violet nine-pin D-sub. */
    public static final String PROFIBUS = "profibus";
    /** The grey nine-pin D-sub of an RS-485 line. */
    public static final String RS485 = "rs485";

    public FieldbusCardSpec {
        Objects.requireNonNull(era, "era");
        Objects.requireNonNull(bus, "bus");
        Objects.requireNonNull(plug, "plug");
        if (machines < 1) {
            throw new IllegalArgumentException("a field bus card reads at least one machine");
        }
    }

    @Override
    public ExpansionCardKind kind() {
        return FieldbusCards.FIELDBUS;
    }

    /*
     * A PCI card seats in a PCI, AGP or PCI Express board alike, as J's Computers' PCI cards do: the boards of those
     * years carried PCI slots beside their graphics slot, and the Legacy's boards are AGP or PCI Express, so a PCI
     * card held to PCI boards alone would fit no controller of its era. Every other card keeps to its own bus family.
     */
    @Override
    public boolean fits(final PcieGeneration slot) {
        return bus == PcieGeneration.PCI ? slot.busFamily() != ExpansionBus.ISA : IExpansionCardSpec.super.fits(slot);
    }

    @Override
    public int controlPorts() {
        return machines;
    }

    @Override
    public String controlPlug() {
        return plug;
    }
}
