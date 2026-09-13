/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.IStableName;

/**
 * How an operating system gets onto a disk when its medium is booted from the firmware. A definition names it by
 * its {@link #serializedName()}; the firmware state carries its {@link #id()}.
 */
public enum InstallMode implements IStableId, IStableName {

    /** A guided installer: pick the disk, confirm, done (MC-DOS, the Frames editions, most distributions). */
    GUIDED(0, "guided"),

    /**
     * A live root shell where the player performs the real installation steps by hand (partition, mount,
     * bootstrap the base system, generate fstab, chroot, bootloader, reboot). Arch-style.
     */
    LIVE_MANUAL(1, "live_manual"),

    /** Like {@link #LIVE_MANUAL}, but the base system and kernel are compiled from source first. Gentoo-style. */
    SOURCE(2, "source");

    private final int id;
    private final String serializedName;

    InstallMode(final int id, final String serializedName) {
        this.id = id;
        this.serializedName = serializedName;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }
}
