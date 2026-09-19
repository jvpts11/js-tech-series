/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import dev.jstech.computers.os.Platform;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableNames;

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
public enum BootManager implements IStableName {

    /** No manager: the machine boots what the firmware points it at. */
    NONE("none", "", ""),

    /** The Linux family's, listed by device and entered from the firmware settings. */
    GRUB("grub", "GNU GRUB  version 2.12", "Firmware Settings"),

    /** The Frames family's, which names its editions rather than the devices they sit on. */
    KICKMGR("kickmgr", "Midsoft Boot Manager", "Change firmware settings"),

    /**
     * FreeBSD's loader, which is no chooser of systems at all: it boots the one it belongs to, counting down on
     * every start, and offers what else can be done from there. It lists only what this machine can really do,
     * which leaves the boot itself, starting over, and the firmware where the firmware is reached that way.
     */
    LOADER("loader", "Welcome to FreeBSD", "Firmware settings");

    private static final StableNames<BootManager> NAMES = StableNames.of(BootManager.class);

    private final String serializedName;
    private final String title;
    private final String firmwareLabel;

    BootManager(final String serializedName, final String title, final String firmwareLabel) {
        this.serializedName = serializedName;
        this.title = title;
        this.firmwareLabel = firmwareLabel;
    }

    /** The manager a system of that family brings, or {@link #NONE} for a family that brings none. */
    public static BootManager of(final Platform platform) {
        return switch (platform) {
            case LINUX -> GRUB;
            case FRAMES -> KICKMGR;
            case FREEBSD -> LOADER;
            default -> NONE;
        };
    }

    /** The manager written under that name, or {@link #NONE} for a name nobody has. */
    public static BootManager named(final String name) {
        final BootManager found = NAMES.find(name);
        return found == null ? NONE : found;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /**
     * Whether this manager lists the systems on the machine's disks for one of them to be chosen. The loader does
     * not: another system on the same machine is reached from the firmware's own boot menu, as it really is.
     */
    public boolean listsSystems() {
        return this != LOADER;
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
