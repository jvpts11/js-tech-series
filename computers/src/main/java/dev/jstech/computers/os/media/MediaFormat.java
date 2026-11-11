/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import org.jetbrains.annotations.ApiStatus;

/**
 * The physical format of a medium, independent of the {@link MediaKind content} it carries.
 *
 * <p>The format decides how much the medium can hold and which {@link MediaDriveType drive} can read
 * it. A medium's content (OS installer, program installer, or a data snapshot) is orthogonal: a
 * floppy can hold an OS installer, a CD can hold a data snapshot, and so on.
 *
 * <p>This enum is free of Minecraft / NeoForge imports so it stays usable from pure-JUnit tests.
 * Capacities are real bytes, as each medium was sold: a file costs on a medium what its bytes weigh on the disks of
 * the format's era ({@code EraStorage}).
 */
@TextHolder
@ApiStatus.Experimental
public enum MediaFormat {

    /** 3.5" floppy disk, the smallest, earliest medium: 1.44 MB, that is 1,440 KB of 1,024 bytes. Read/write. */
    FLOPPY(1_474_560L, TextKey.of("jsc.media.format.floppy", "Floppy")),

    /** CD, the Legacy-era optical medium: 700 MB, 360,000 sectors of 2,048 bytes. */
    CD(737_280_000L, TextKey.of("jsc.media.format.cd", "CD")),

    /** DVD, the Transition-era optical medium: 4.7 GB, 2,295,104 sectors of 2,048 bytes. */
    DVD(4_700_372_992L, TextKey.of("jsc.media.format.dvd", "DVD")),

    /** USB flash drive: a stick holds what its model says on the box, and this is the 32 GB one's. */
    USB(32_000_000_000L, TextKey.of("jsc.media.format.usb", "USB")),

    /** Blu-ray, the Advanced-era optical medium: 25 GB, 12,219,392 sectors of 2,048 bytes. */
    BLU_RAY(25_025_314_816L, TextKey.of("jsc.media.format.blu_ray", "Blu-ray"));

    private final long capacityBytes;
    /** The format's name as a player reads it. */
    private final TextKey name;

    MediaFormat(final long capacityBytes, final TextKey name) {
        this.capacityBytes = capacityBytes;
        this.name = name;
    }

    /**
     * The bytes a medium of this format holds, a USB stick's being its model's: what the files and patterns written
     * on it may come to. An installer always fits, since what a system takes counts on the disk it goes to.
     */
    public long capacityBytes() {
        return capacityBytes;
    }

    /** The format's name: "DVD", "Blu-ray". */
    public Text text() {
        return name.text();
    }

    /** The hardware era the format belongs to, which decides what a file's bytes weigh on it. */
    public Era era() {
        return switch (this) {
            case FLOPPY -> HardwareEra.VINTAGE;
            case CD -> HardwareEra.LEGACY;
            case DVD -> HardwareEra.TRANSITION;
            case USB -> HardwareEra.STANDARD;
            case BLU_RAY -> HardwareEra.ADVANCED;
        };
    }

    /**
     * Whether a medium of this format lies on a disc tray, which the player opens to put it on and take it off, rather
     * than going through a slot or into a port by itself: the optical discs.
     */
    public boolean onTray() {
        return switch (this) {
            case CD, DVD, BLU_RAY -> true;
            case FLOPPY, USB -> false;
        };
    }
}
