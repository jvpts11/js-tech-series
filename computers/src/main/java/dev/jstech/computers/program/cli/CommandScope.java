/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.computers.os.Platform;
import java.util.EnumSet;
import java.util.Set;

/**
 * Where a command exists: which systems answer to it, and what the machine must have for it to be there at all.
 *
 * <p>Nothing is available everywhere by default. A command says this for itself, and everything that shows commands
 * to a player reads the same answer: the list of commands, the manual, what a name completes to, and whether a typed
 * word is found at all. A machine therefore never offers what it cannot do.
 *
 * <p>Pure, with no Minecraft types, so what a command claims is unit-tested; {@link CommandAccess} is what asks a
 * real machine.
 *
 * @param platforms  the systems that have it, as a family: MC-DOS, MC-NET, Frames, Linux, UNIX, FreeBSD
 * @param minOsRank  the oldest edition of a family that has it, counting from one; zero for every edition
 * @param hostScope  the computer it may live on, the way a program declares one
 * @param needs      what the machine must have: a filesystem, a network under it, ports for peripherals
 * @param manager    the package manager this command IS, or null when it is no package manager's
 * @param packageId  the package that brings it, as {@code namespace:path}, or empty when the system brings it
 */
public record CommandScope(Set<Platform> platforms, int minOsRank, HostScope hostScope, Set<Need> needs,
                           PackageManagerKind manager, String packageId) {

    /** What a machine must have for a command to exist on it. */
    public enum Need {
        /** A filesystem to keep files in, which MC-NET has none of. */
        FILES,
        /** A data network under it. */
        NETWORK,
        /** Ports for peripherals. */
        PORTS
    }

    /** Every system there is, which is what a command of the machines themselves reaches. */
    public static final Set<Platform> EVERY_SYSTEM = Set.of(Platform.values());

    /** The DOS-speaking systems: MC-DOS, MC-NET and every Frames edition. */
    public static final Set<Platform> DOS_SYSTEMS = Set.of(Platform.MC_DOS, Platform.MC_NET, Platform.FRAMES);

    /** The Unix-speaking systems: the distributions, UNIX and FreeBSD. */
    public static final Set<Platform> UNIX_SYSTEMS = Set.of(Platform.LINUX, Platform.UNIX, Platform.FREEBSD);

    /** A command no machine has, which is what a command that declares nothing is. */
    public static final CommandScope NOWHERE = new CommandScope(Set.of(), 0, HostScope.ANY, Set.of(), null, "");

    public CommandScope {
        platforms = Set.copyOf(platforms);
        needs = needs.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(needs));
        packageId = packageId == null ? "" : packageId;
    }

    /** A command every system has. */
    public static CommandScope everywhere() {
        return on(EVERY_SYSTEM);
    }

    /** A command the systems of those families have. */
    public static CommandScope on(final Platform... platforms) {
        return on(Set.of(platforms));
    }

    public static CommandScope on(final Set<Platform> platforms) {
        return new CommandScope(platforms, 0, HostScope.ANY, Set.of(), null, "");
    }

    /** The same, from that edition of its family onwards, counting from one. */
    public CommandScope fromEdition(final int rank) {
        return new CommandScope(this.platforms, rank, this.hostScope, this.needs, this.manager, this.packageId);
    }

    /** The same, only on that kind of computer. */
    public CommandScope onHost(final HostScope scope) {
        return new CommandScope(this.platforms, this.minOsRank, scope, this.needs, this.manager, this.packageId);
    }

    /** The same, only on a machine that has all of those. */
    public CommandScope needing(final Need... needs) {
        return new CommandScope(this.platforms, this.minOsRank, this.hostScope, Set.of(needs), this.manager,
                this.packageId);
    }

    /** The same, only on a machine whose package manager is that one, which is what a manager's own words are. */
    public CommandScope forManager(final PackageManagerKind manager) {
        return new CommandScope(this.platforms, this.minOsRank, this.hostScope, this.needs, manager, this.packageId);
    }

    /** The same, only once that package is installed. */
    public CommandScope fromPackage(final String packageId) {
        return new CommandScope(this.platforms, this.minOsRank, this.hostScope, this.needs, this.manager, packageId);
    }

    /** Whether a system of that family and edition has it at all, before anything about the machine is asked. */
    public boolean onSystem(final Platform platform, final int edition) {
        return this.platforms.contains(platform) && (this.minOsRank == 0 || edition == 0 || edition >= this.minOsRank);
    }

    /** Whether the machine must have that. */
    public boolean needs(final Need need) {
        return this.needs.contains(need);
    }

    /** Whether a package has to be installed for it. */
    public boolean fromAPackage() {
        return !this.packageId.isEmpty();
    }
}
