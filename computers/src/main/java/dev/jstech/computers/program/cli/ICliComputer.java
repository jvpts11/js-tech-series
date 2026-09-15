/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

/**
 * The view of a computer and its network that a CLI command operates on, made of one part for each thing a command
 * reaches: the machine, its drives, the network it reads, the network's work, packages, installation, settings and
 * shares, remote shells and running programs. The server backs it with the real machine and its network, while tests
 * back it with a fake, so command logic is unit-testable without a running game.
 *
 * <p>Every part answers each of its members as a computer without that part would, so a fake writes only what its
 * commands reach. Every effecting call returns an {@link OpResult} describing what happened, never throwing for an
 * ordinary failure (unknown item, no network), so commands can print a clean message.
 */
public interface ICliComputer extends ICliMachine, ICliFiles, ICliNetwork, ICliOperations, ICliPackages,
        ICliInstallation, ICliConfig, ICliRemote, ICliProcesses {

    /**
     * One server of the network: what it is called, how many items it is holding and how many it could.
     *
     * <p>{@code capacity} counts only what the network may fill, so a personal computer that published a
     * tenth of its disks contributes a tenth of them and no more.
     */
    record ServerUse(String name, long stored, long capacity) {
    }

    /**
     * One mounted drive for {@code df}: its letter, device name, capacity and free space, and whether a medium is
     * present.
     */
    record MountInfo(char drive, String device, long capacityMbEq, long freeMbEq, boolean ready) {
    }

    /**
     * Everything {@code screenfetch} shows about this machine: the distribution and its id (picks the
     * ASCII logo), kernel line, host and shell, the desktop environment (or the TTY), the hardware, disk
     * usage, installed package count and the world's uptime in ticks.
     */
    record SystemInfo(String distroId, String os, String kernel, String hostname, String shell,
                      String desktop, String cpu, int ramMb, long diskUsedMb, long diskTotalMb,
                      int packages, long uptimeTicks) {
    }

    /**
     * One package on the shelf: its name, description, and whether this computer already has it.
     *
     * <p>{@code community} marks one a player wrote and published, as against one that came with the
     * machines. Both install the same way; a player is entitled to know which is which.
     */
    record PackageInfo(String name, String description, boolean installed, boolean building,
                       boolean community) {

        public PackageInfo(final String name, final String description, final boolean installed,
                           final boolean building) {
            this(name, description, installed, building, false);
        }
    }

    /**
     * One entry returned by {@link ICliFiles#listDisk}: a file or a subdirectory of the listed directory.
     *
     * @param name     the entry's leaf name (a file name with its extension, or a directory name)
     * @param ext      the file extension without the dot, or {@code ""} for a directory
     * @param weightMbEq the file's disk weight in mB-equivalents ({@code 0} for a directory)
     * @param readOnly true if the entry cannot be written or deleted via the shell
     * @param isDir    true if this entry is a subdirectory rather than a file
     * @param modified the world game time the file was last written; {@code 0} means unknown
     */
    record FsEntry(String name, String ext, long weightMbEq, boolean readOnly, boolean isDir, long modified) {}

    /**
     * The result of a filesystem operation: either a success carrying optional entries + an optional
     * text payload + an optional {@link OpResult}, or a failure carrying an error message.
     */
    final class FsResult {

        private final boolean ok;
        private final String message;
        private final java.util.List<FsEntry> entries;
        private final OpResult opResult;

        private FsResult(final boolean ok, final String message, final java.util.List<FsEntry> entries,
                         final OpResult opResult) {
            this.ok = ok;
            this.message = message;
            this.entries = entries != null ? entries : java.util.List.of();
            this.opResult = opResult;
        }

        /** A listing result carrying one or more directory entries. */
        public static FsResult listing(final java.util.List<FsEntry> entries) {
            return new FsResult(true, "", entries, null);
        }

        /** A text-content result (for {@code type} / {@code cat}). */
        public static FsResult content(final String text) {
            return new FsResult(true, text, null, null);
        }

        /** A simple confirmation message (for {@code del}). */
        public static FsResult ok(final String message) {
            return new FsResult(true, message, null, null);
        }

        /** An IQL execution result forwarded from the dispatcher. */
        public static FsResult iqlResult(final OpResult result) {
            return new FsResult(result.ok(), result.message(), null, result);
        }

        /** A standard failure with a human-readable message. */
        public static FsResult fail(final String message) {
            return new FsResult(false, message, null, null);
        }

        /** Returned when no OS or system disk is present. */
        public static FsResult noOs() {
            return fail("no system disk or OS installed");
        }

        public boolean ok() { return ok; }
        public String message() { return message; }
        public java.util.List<FsEntry> entries() { return entries; }
        /** The forwarded IQL result when this is an {@link #iqlResult}, or {@code null} otherwise. */
        public OpResult opResult() { return opResult; }
    }

    /** Counts that describe the network at a glance. */
    record NetSummary(boolean linked, int servers, int personalComputers, int subframes,
                      int indexedTypes, boolean mainframePresent) {
    }

    /**
     * One machine a remote shell can reach. A machine can be addressed by any of these: the host
     * name it answers to, the name its owner gave it, or the head of its node id, whichever the
     * player has in front of them.
     *
     * @param hostname the shell host name (what the remote prompt shows)
     * @param name     the player-given machine name, or {@code ""} when unnamed
     * @param nodeId   the short node identifier
     * @param os       the operating system it runs, or {@code ""} when it has none
     * @param type     the kind of machine (Mainframe, Server, Personal Computer, ...)
     * @param running  whether it is powered on right now
     */
    record RemoteHost(String hostname, String name, String nodeId, String os, String type,
                      boolean running) {
    }

    /**
     * One folder a machine opens to the others on its network.
     *
     * @param name     what the others reach it by: {@code \\host\name}
     * @param path     where it is on that machine, as a DOS path
     * @param writable whether the others may write into it as well as read
     */
    record ShareInfo(String name, String path, boolean writable) {
    }

    /** A folder another machine on this network shares, under that machine's host name. */
    record NetworkShare(String hostname, ShareInfo share) {
    }

    /** A line in a storage listing: a name, a quantity, and an optional detail (e.g. where the item lives). */
    record StoredItem(String name, long quantity, String detail) {

        public StoredItem(final String name, final long quantity) {
            this(name, quantity, "");
        }
    }

    /** How much of an item one server holds. */
    record Holding(String server, long quantity) {
    }

    /** A snapshot of one in-flight operation; {@code id} is the short id {@code cancel} takes. */
    record ActiveOp(String id, String type, String item, long progress, long total, String status,
                    String priority) {
    }

    /**
     * One operation type's last hour on the network: how many settled, the mean ticks waited and run, the
     * share that fell short in whole percent, and what the type moved.
     */
    record OperationStat(String type, int count, int averageWait, int averageRun, int shortfallPercent, long moved) {
    }

    /** A program installed on the computer: its short command name and full id, for listing. */
    record ProgramInfo(String name, String id) {
    }

    /**
     * One program this computer is running: what it is called, how it is getting on, how much of the
     * room it was given it is holding, and the file it was started from, which says which runtime runs it.
     */
    record SigmaProcess(int id, String name, String state, long heldBytes, long heapBytes, String file) {

        public SigmaProcess(final int id, final String name, final String state, final long heldBytes,
                             final long heapBytes) {
            this(id, name, state, heldBytes, heapBytes, name);
        }
    }

    /** A service the computer hosts and its state, for the process/service manager (e.g. "running"). */
    record ServiceStatus(String name, String state) {
    }

    /** The outcome of an effecting command: whether it was accepted, and a message to print. */
    record OpResult(boolean ok, String message) {

        public static OpResult ok(final String message) {
            return new OpResult(true, message);
        }

        public static OpResult fail(final String message) {
            return new OpResult(false, message);
        }
    }
}
