/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;

/**
 * The physical format of a medium, independent of the {@link MediaKind content} it carries.
 *
 * <p>The format decides how much the medium can hold and which {@link MediaDriveType drive} can read
 * it. A medium's content (OS installer, program installer, or a data snapshot) is orthogonal: a
 * floppy can hold an OS installer, a CD can hold a data snapshot, and so on.
 *
 * <p>This enum is free of Minecraft / NeoForge imports so it stays usable from pure-JUnit tests.
 * Capacities are in item-equivalents (1 item = 4 MB) and are tunable balance numbers, not final.
 */
@TextHolder
public enum MediaFormat {

    /** 3.5" floppy disk, the smallest, earliest medium. Read/write. */
    FLOPPY(1_024, TextKey.of("jsc.media.format.floppy", "Floppy")),

    /** CD, the Legacy-era optical medium. 700 MB class. */
    CD(8_192, TextKey.of("jsc.media.format.cd", "CD")),

    /** DVD, the Transition-era optical medium. 4.7 GB class. */
    DVD(65_536, TextKey.of("jsc.media.format.dvd", "DVD")),

    /** USB flash drive, a removable medium four times a DVD. */
    USB(262_144, TextKey.of("jsc.media.format.usb", "USB")),

    /** Blu-ray, the Advanced-era optical medium, four times a USB stick, as each medium is four times the last. */
    BLU_RAY(1_048_576, TextKey.of("jsc.media.format.blu_ray", "Blu-ray"));

    private final int capacityItems;
    /** The format's name as a player reads it. */
    private final TextKey name;

    MediaFormat(final int capacityItems, final TextKey name) {
        this.capacityItems = capacityItems;
        this.name = name;
    }

    /**
     * Returns this format's capacity in item-equivalents (1 item = 4 MB). Bounds how large a data
     * snapshot, OS image, or program a medium of this format can carry.
     */
    public int capacityItems() {
        return capacityItems;
    }

    /** The format's name: "DVD", "Blu-ray". */
    public Text text() {
        return name.text();
    }

    /** The hardware era the format belongs to, which decides what a file's bytes weigh on it. */
    public HardwareEra era() {
        return switch (this) {
            case FLOPPY -> HardwareEra.VINTAGE;
            case CD -> HardwareEra.LEGACY;
            case DVD -> HardwareEra.TRANSITION;
            case USB -> HardwareEra.STANDARD;
            case BLU_RAY -> HardwareEra.ADVANCED;
        };
    }
}
