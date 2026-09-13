/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.machine;

import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;

/**
 * The names shown for the system a machine runs.
 */
public final class MachineLabels {

    private MachineLabels() {
    }

    /** A short, friendly label for an installed OS id, or {@code none} when no OS is installed. */
    public static String osLabelOf(final net.minecraft.resources.ResourceLocation osId) {
        if (osId == null) {
            return "none";
        }
        return switch (osId.getPath()) {
            case "frames_95" -> "Frames 95";
            case "frames_xp" -> "Frames XP";
            case "frames_11" -> "Frames 11";
            case "mc_dos" -> "MC-DOS";
            case "mc_net" -> "MC-NET";
            default -> osId.getPath();
        };
    }

    public static String osLabel(final dev.jstech.computers.os.IOsHost host) {
        final net.minecraft.resources.ResourceLocation osId = host.installedOsId();
        final OsDef os = osId == null ? null : OsRegistry.getOs(osId);
        return os == null ? "" : os.displayName();
    }
}
