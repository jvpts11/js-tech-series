/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import java.util.List;
import java.util.Map;

/**
 * What a command reaches of the packages a computer installs over its network's Mirror: the package manager, what the
 * Mirror offers, installing, removing, updating and publishing, and the source builds still compiling.
 *
 * <p>Every member answers as a computer with no Mirror in reach would.
 */
public interface ICliPackages {

    /** The installed OS's package manager; {@code NONE} on media-installed platforms. */
    default dev.jstech.computers.os.PackageManagerKind packageManager() {
        return dev.jstech.computers.os.PackageManagerKind.NONE;
    }

    /** The packages the network mirror offers this computer (empty when no mirror is reachable). */
    default List<ICliComputer.PackageInfo> packagesAvailable() {
        return List.of();
    }

    /**
     * Installs the named package from the network mirror: resolves it, checks the OS/hardware gates, and
     * installs it (or, for a source-based manager, starts the build). The message reads like the package
     * manager's own output; a missing mirror is the classic "could not resolve" failure.
     */
    default ICliComputer.OpResult packageInstall(final String name) {
        return ICliComputer.OpResult.fail("could not resolve mirror://");
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

    /** Whether a network mirror is reachable from this computer right now. */
    default boolean mirrorReachable() {
        return false;
    }

    /** Controls the Mirror service on the network's Mainframe: {@code install|status}. */
    default ICliComputer.OpResult mirrorControl(final String action) {
        return ICliComputer.OpResult.fail("the network has no Mainframe");
    }

    /** Source builds still compiling on this computer: program id to ticks remaining. */
    default Map<String, Long> buildsRemaining() {
        return Map.of();
    }

    /**
     * What the shell prints ahead of the next command, each returned once and then forgotten: what the machine itself
     * has to say (programs a save could not bring back), and one notice per source build that finished since the shell
     * last asked, since a build completes while the player is elsewhere.
     */
    default List<String> drainBuildNotices() {
        return List.of();
    }
}
