/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.wan;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraTable;

/**
 * Nouvell BorderLink, after the real BorderManager: the service a WAN Gateway Computer carries its long-distance lines
 * with, one version for each era, the gateway's own (computers/docs/design/network.md). Without it a gateway's lines
 * stay down, whatever cards and cables it has.
 */
public final class BorderLink {

    /* Each era's version, from the Vintage's to the Advanced's; an era past the Advanced runs the Advanced's. */
    private static final EraTable<String> VERSIONS = EraTable.ofFive("1.0", "2.1", "3.8", "4.6", "5.2");

    private BorderLink() {
    }

    /** The version of BorderLink a gateway of {@code era} runs. */
    public static String version(final Era era) {
        return VERSIONS.get(era);
    }

    /** Whether BorderLink is installed on {@code gateway}. */
    public static boolean installed(final AbstractComputerBlockEntity gateway) {
        return gateway.console() != null && gateway.console().isInstalled(Programs.BORDERLINK.toString());
    }

    /** Whether BorderLink runs on {@code gateway}: installed, the machine up, and not ended this run. */
    public static boolean running(final AbstractComputerBlockEntity gateway) {
        final ProgramSpec spec = OsRegistry.getProgram(Programs.BORDERLINK);
        return gateway.isRunning() && gateway.hasOs() && installed(gateway)
                && (spec == null || gateway.serviceRunning(spec));
    }
}
