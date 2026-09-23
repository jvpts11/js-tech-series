/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.machine;

import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * The names shown for the system a machine runs.
 */
public final class MachineLabels {

    private MachineLabels() {
    }

    /**
     * The name the system under an installed OS id goes by, or {@code none} when no OS is installed. An id the
     * registry does not know shows as itself, which is all there is to say about it.
     */
    public static String osLabelOf(final ResourceLocation osId) {
        if (osId == null) {
            return "none";
        }
        final OsDef os = OsRegistry.getOs(osId);
        return os == null ? osId.getPath() : os.displayName();
    }

    public static String osLabel(final IOsHost host) {
        final ResourceLocation osId = host.installedOsId();
        final OsDef os = osId == null ? null : OsRegistry.getOs(osId);
        return os == null ? "" : os.displayName();
    }
}
