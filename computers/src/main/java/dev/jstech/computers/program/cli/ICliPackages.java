/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.computers.program.tty.ITtyProcess;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * What a command reaches of the packages a computer installs over its network's Mirror: the package manager, what the
 * Mirror offers, installing, removing, updating and publishing.
 *
 * <p>Every member answers as a computer with no Mirror in reach would.
 */
public interface ICliPackages {

    /**
     * What came of asking for a package.
     *
     * @param said what the package manager said at once, in its own words
     * @param tool what it left running in front of the terminal, for a manager whose installing takes the
     *             terminal while it happens; null when it was all said at once
     */
    record Installing(ICliComputer.OpResult said, @Nullable ITtyProcess tool) {

        /** Everything there was to say has been said. */
        public static Installing said(final ICliComputer.OpResult said) {
            return new Installing(said, null);
        }

        /** Nothing to say yet: the tool says it, from here until it ends. */
        public static Installing running(final ITtyProcess tool) {
            return new Installing(ICliComputer.OpResult.ok(""), tool);
        }

        public boolean ok() {
            return this.said.ok();
        }

        public String message() {
            return this.said.message();
        }
    }

    /** The installed OS's package manager; {@code NONE} on media-installed platforms. */
    default PackageManagerKind packageManager() {
        return PackageManagerKind.NONE;
    }

    /** The packages the network mirror offers this computer (empty when no mirror is reachable). */
    default List<ICliComputer.PackageInfo> packagesAvailable() {
        return List.of();
    }

    /**
     * Installs the named package from the network mirror: resolves it, checks the OS/hardware gates, and
     * installs it, or, for a manager that builds from source, leaves the build running in front of the
     * terminal. The message reads like the package manager's own output; a missing mirror is the classic
     * "could not resolve" failure.
     *
     * @param ask whether the manager was told to list what it would do and ask before doing it
     */
    default Installing packageInstall(final String name, final boolean ask) {
        return Installing.said(ICliComputer.OpResult.fail("could not resolve mirror://"));
    }

    /**
     * Removes the named installed program (a package manager's remove verb, or the DOS-family
     * {@code uninstall} command). Mainframe services turn their agent off; a removed desktop
     * environment drops the computer back to the TTY on its next boot.
     */
    default ICliComputer.OpResult packageRemove(final String name) {
        return ICliComputer.OpResult.fail("unable to locate package " + name);
    }

    /**
     * Brings every installed package up to the current build. Packages installed before a mod update
     * carry the version they were installed at, so this is what reconciles a repository that moved on
     * without the machine; it never installs anything new.
     */
    default ICliComputer.OpResult packageUpdate() {
        return ICliComputer.OpResult.fail("could not resolve mirror://");
    }

    /**
     * Puts a built package on the network's Mirror, for anyone on the network to install.
     *
     * @param path the package file on this computer's disk
     */
    default ICliComputer.OpResult publishPackage(final String path) {
        return ICliComputer.OpResult.fail("could not resolve mirror://");
    }

    /** Takes one back off the Mirror. */
    default ICliComputer.OpResult unpublishPackage(final String name) {
        return ICliComputer.OpResult.fail("could not resolve mirror://");
    }

    /**
     * FreeBSD's portsnap: fetches the ports tree from the network's Mirror as one snapshot, lays it out under
     * {@code /usr/ports}, or brings a tree already there up to date, as the commands ask, leaving the work running
     * in front of the terminal.
     *
     * @param commands {@code fetch}, {@code extract}, {@code update} or {@code auto}, in the order typed
     */
    default Installing portsnap(final List<String> commands) {
        return Installing.said(ICliComputer.OpResult.fail("portsnap: this system keeps no ports tree"));
    }

    /**
     * make, in the folder the shell stands in: in a port's folder, builds the port on this machine and installs,
     * cleans or removes it as the targets ask; anywhere else, there is nothing to make.
     *
     * @param targets what make was asked to make, in the order typed; none means build
     */
    default Installing makePort(final List<String> targets) {
        return Installing.said(ICliComputer.OpResult.fail("make: no target to make."));
    }

    /** Whether a network mirror is reachable from this computer right now. */
    default boolean mirrorReachable() {
        return false;
    }

    /** Controls the Mirror service on the network's Mainframe: {@code install|status}. */
    default ICliComputer.OpResult mirrorControl(final String action) {
        return ICliComputer.OpResult.fail("the network has no Mainframe");
    }

    /**
     * What the shell prints ahead of the next command, each returned once and then forgotten: what the machine
     * itself has to say, such as the programs a save could not bring back.
     */
    default List<String> drainNotices() {
        return List.of();
    }
}
