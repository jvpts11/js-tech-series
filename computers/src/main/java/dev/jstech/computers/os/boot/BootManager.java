/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import dev.jstech.computers.os.Platform;

/**
 * The boot manager a family of systems brings with it.
 *
 * <p>A machine with more than one system on it has to be asked which one, and who asks is the system family:
 * the Linux distributions bring GRUB, and the Frames editions bring the Midsoft Boot Manager, whose file on the
 * disk is {@code kickmgr}. They are the same job in two voices, which is exactly what they were.
 *
 * <p>The families with no manager of their own boot what they are pointed at and say nothing: a system of the
 * first age had one disk and no question to ask.
 */
public enum BootManager {

    /** No manager: the machine boots what the firmware points it at. */
    NONE("", ""),

    /** The Linux family's, listed by device and entered from the firmware settings. */
    GRUB("GNU GRUB  version 2.12", "Firmware Settings"),

    /** The Frames family's, which names its editions rather than the devices they sit on. */
    KICKMGR("Midsoft Boot Manager", "Change firmware settings");

    private final String title;
    private final String firmwareLabel;

    BootManager(final String title, final String firmwareLabel) {
        this.title = title;
        this.firmwareLabel = firmwareLabel;
    }

    /** The manager a system of that family brings, or {@link #NONE} for a family that brings none. */
    public static BootManager of(final Platform platform) {
        return switch (platform) {
            case LINUX -> GRUB;
            case FRAMES -> KICKMGR;
            default -> NONE;
        };
    }

    /** What the manager writes across the top of its list. */
    public String title() {
        return this.title;
    }

    /**
     * Whether this manager stops the machine when there is only one system to stop it for.
     *
     * <p>The Frames one never did: with a single installation it went straight through, and a player only ever
     * saw it once there was a second system to choose between. That is the whole of what it was for. GRUB is
     * left stopping either way, which is how the machines here have always behaved and is not this change's to
     * decide.
     */
    public boolean stopsForOne() {
        return this != KICKMGR;
    }

    /** What it calls the way into the firmware's own setup. */
    public String firmwareLabel() {
        return this.firmwareLabel;
    }

    /**
     * How it names a system that is not the one running.
     *
     * <p>The two families differ in what they think is worth saying. GRUB names the device, because on a real
     * machine that is how you tell two installations apart. The Frames manager names the edition and the disk
     * it is on, because that is what its own list said.
     */
    public String label(final String systemName, final String device, final int slot) {
        return switch (this) {
            case GRUB -> systemName + " Boot Manager (on " + device + ")";
            case KICKMGR -> systemName + " (Disk " + slot + ")";
            default -> systemName;
        };
    }
}
