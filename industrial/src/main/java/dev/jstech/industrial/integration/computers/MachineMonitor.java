/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.integration.computers;

import dev.jstech.computers.api.ComputersRegisterEvent;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ProgramKind;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.SoftwareHouse;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.industrial.JsIndustrial;
import java.util.EnumSet;
import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceLocation;

/**
 * The Machine Monitor, by Wonderwork Systems: a plant's alarm screen, on any computer of the network, showing the
 * alerts of every Industrial Controller Computer on it, newest first, each machine's reading beside it, and letting an
 * operator acknowledge them. It runs on the systems with windows from the Legacy on, and installs as any program does,
 * from its disc.
 */
public final class MachineMonitor {

    /** Who wrote it: the series' maker of a plant's screens and alarms. */
    public static final SoftwareHouse WONDERWORK = new SoftwareHouse("Wonderwork Systems", "Wonderwork Systems Inc.");
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID,
            "machine_monitor");
    /* What it takes of the disk, in megabytes: a plant screen of the late 1990s. */
    private static final int DISK_MB = 24;
    public static final ProgramSpec SPEC = ProgramSpec.of(ID, "machinemonitor", false,
                    EnumSet.of(Platform.FRAMES, Platform.LINUX, Platform.FREEBSD), DISK_MB, ProgramKind.APP, 0,
                    HostScope.ANY)
            .named("Machine Monitor")
            .described("Shows the alerts of every Industrial Controller Computer on the network, to acknowledge them.")
            .withMinEra(HardwareEra.LEGACY).withEra(HardwareEra.LEGACY).withHouse(WONDERWORK);

    private MachineMonitor() {
    }

    /** Registers the program; from J's Computers' register event. */
    static void register(final ComputersRegisterEvent event) {
        event.program(SPEC);
    }

    /** Its name and what it does, for the language file; from the data generation. */
    static void names(final BiConsumer<String, String> add) {
        add.accept(SPEC.titleKey(), SPEC.displayName());
        add.accept(SPEC.descriptionKey(), SPEC.description());
    }
}
