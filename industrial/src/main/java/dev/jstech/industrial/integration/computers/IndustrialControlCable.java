/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.integration.computers;

import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.Lane;
import dev.jstech.core.jstechseries.peripheral.ControlLinks;
import dev.jstech.core.jstechseries.peripheral.PeripheralCableType;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.JsIndustrial;
import net.minecraft.resources.ResourceLocation;

/**
 * The industrial control cable: a line of the series' cable block from an Industrial Controller Computer to the
 * machines it reads, no further than eight blocks of cable. Point to point, as a peripheral cable is: the computer
 * links each machine it reaches while its Fieldbus Cards have a port free, and the machine keeps nothing.
 *
 * <p>Violet, as fieldbus cable is, in the bottom left lane, so it runs beside the energy cable without touching it. At
 * the computer it ends in the plug of its card's connector (an RJ45, the violet D-sub of PROFIBUS, the grey D-sub of
 * RS-485); at a machine in an M12, the screw-locked round connector of factory machines and sensors.
 */
@TextHolder
public final class IndustrialControlCable {

    /** What the cable is for, as its tooltip says it. */
    public static final TextKey JOB = TextKey.of("jsindustrial.cable.industrial_control.job",
            "Links an Industrial Controller Computer to the machines it reads, up to eight blocks of cable away.");
    /* Its thickness in its lane, in pixels: a shielded two-pair cable, thicker than the Research Link Cable. */
    private static final int THICKNESS = 4;
    public static final CableEntry CABLE = IndustrialModule.CONTENT.cable("industrial_control_cable",
                    CableType.builder(ControlLinks.LINE).describe(JOB, null)
                            .grid(ControlLinks.GRID).lane(Lane.BOTTOM_LEFT).thickness(THICKNESS)
                            .carries(0L, PeripheralCableType.INDUSTRIAL_CONTROL.maxLength())
                            .jacket(id("block/cable/industrial_control"))
                            .plug(id("block/cable/plug/m12"))
                            .plug(FieldbusCardSpec.RJ45, id("block/cable/plug/fieldbus_rj45"))
                            .plug(FieldbusCardSpec.PROFIBUS, id("block/cable/plug/profibus"))
                            .plug(FieldbusCardSpec.RS485, id("block/cable/plug/rs485")))
            .named("Industrial Control Cable").tab(IndustrialModule.CONTROLLERS).register();

    private IndustrialControlCable() {
    }

    /** Declares the cable, while the mod is built. */
    static void declare() {
        // Loading the class declares it.
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID, path);
    }
}
