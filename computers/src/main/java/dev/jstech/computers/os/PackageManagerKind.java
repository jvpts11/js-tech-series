/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.core.id.IStableName;

/**
 * The package manager an operating system ships with: the command the player uses to install programs
 * and desktop environments from the network's mirror. {@link #NONE} means the OS installs programs from
 * physical install media instead (MC-DOS, the Frames editions).
 */
public enum PackageManagerKind implements IStableName {

    /** No package manager: programs come from install media in a linked drive. */
    NONE("none", "", ""),

    /** Debian/Ubuntu-style {@code apt}. */
    APT("apt", "apt", "install"),

    /** Fedora-style {@code dnf}. */
    DNF("dnf", "dnf", "install"),

    /** Arch-style {@code pacman} ({@code pacman -S}). */
    PACMAN("pacman", "pacman", "-S"),

    /** Gentoo-style {@code emerge}, which compiles what it installs. */
    EMERGE("emerge", "emerge", ""),

    /** FreeBSD's {@code pkg}, which installs what the Mirror has already built. */
    PKG("pkg", "pkg", "install"),

    /**
     * The Frames package manager: every edition ships with it, so a player who never touches a Linux
     * distribution still installs software from the network Mirror instead of hunting install discs.
     */
    PCKMGR("pckmgr", "pckmgr", "install");

    private final String serializedName;
    private final String command;
    private final String installVerb;

    PackageManagerKind(final String serializedName, final String command, final String installVerb) {
        this.serializedName = serializedName;
        this.command = command;
        this.installVerb = installVerb;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }

    /** The command word the player types ({@code apt}, {@code dnf}, ...), or {@code ""} for none. */
    public String command() {
        return command;
    }

    /** The install sub-verb ({@code install}, {@code -S}), or {@code ""} when the command installs directly. */
    public String installVerb() {
        return installVerb;
    }

    /** Whether this manager compiles packages from source (a real wait that scales with the CPU). */
    public boolean compilesFromSource() {
        return this == EMERGE;
    }
}
