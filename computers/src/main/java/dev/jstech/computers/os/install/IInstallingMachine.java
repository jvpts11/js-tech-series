/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A machine a system can be installed on: the installer it sits in, the copy under way, and where the new system
 * goes.
 */
public interface IInstallingMachine {

    /** The value of {@link #pendingInstallSlot()} when no installation is waiting for its reboot. */
    int NO_PENDING_INSTALL = -2;

    /**
     * The disk slot a guided installer has just written a system to ({@code -1} for the default
     * disk), while the machine still sits in that installer waiting for the reboot that will boot
     * it; {@link #NO_PENDING_INSTALL} otherwise. A real machine does not become the new system the
     * moment the files are on the disk: until it restarts, the installer is what is running, so
     * leaving the monitor and coming back must find the installer's "reboot" prompt, not a booted
     * desktop. Any restart or power change clears it.
     */
    int pendingInstallSlot();

    void setPendingInstallSlot(int slot);

    /** The disk slot a guided OS install targets by default. */
    int defaultInstallSlot();

    /** Installs the given OS onto {@code preferredSlot} (or the default target); true on success. */
    boolean installOs(ResourceLocation osId, int preferredSlot);

    /** Installs the given OS onto the default target slot; true on success. */
    boolean installOs(ResourceLocation osId);

    /**
     * A system being copied onto this machine's disks right now, or nothing.
     *
     * <p>A machine that cannot hold one answers nothing and is installed the moment it is asked, which is what
     * a host with nowhere to keep the work has to do.
     */
    @Nullable
    default OsInstallJob installing() {
        return null;
    }

    /** Starts, replaces or ends the copy this machine is doing; a host that keeps none does nothing. */
    default void setInstalling(@Nullable final OsInstallJob job) {
    }

    /** The installer this machine is in: the page it is on and what has been answered so far. */
    @Nullable
    default InstallerFlow installer() {
        return null;
    }

    /** Puts the machine in an installer, or takes it out of one. */
    default void setInstaller(@Nullable final InstallerFlow flow) {
    }

    /** Whether this machine can hold a copy of its own rather than being written to there and then. */
    default boolean keepsInstalls() {
        return false;
    }
}
