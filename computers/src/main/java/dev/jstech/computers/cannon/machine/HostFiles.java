/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.machine;

import dev.jstech.computers.cannon.run.Halt;
import dev.jstech.computers.cannon.run.IHost;
import dev.jstech.computers.cannon.run.Values;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.DosPath;
import dev.jstech.computers.program.cli.ICliComputer;
import java.util.List;

/**
 * The machine's own drives, as a program reaches them.
 *
 * <p>It goes through the same door the shell does, so a path means the same thing to a program as it
 * does at the prompt and a file written by one is the file the other opens. Nothing here is free: a disk
 * is slower than adding two numbers, and the prices below are what says so.
 */
public final class HostFiles {

    /** What each of these is worth in instructions. Reading is dear; writing is dearer. */
    private static final int LOOK = dev.jstech.computers.cannon.CannonCosts.GLANCE_NETWORK;
    private static final int READ = dev.jstech.computers.cannon.CannonCosts.READ;
    private static final int WRITE = dev.jstech.computers.cannon.CannonCosts.WRITE;

    /** The system disk, which is the root of a ComputerCraft tree. */
    private static final char SYSTEM_DRIVE = 'C';

    /** What ComputerCraft calls the folder a disk in a drive appears under. */
    private static final String DISK = "disk";

    private HostFiles() {
    }

    /** Whether this is one of the calls handled here. */
    public static boolean handles(final String owner) {
        return "File".equals(owner);
    }

    /** Answers one of them against a real machine. */
    public static IHost.Reply call(final ICliComputer computer, final String member,
                                  final List<Object> arguments, final int line) {
        final String path = arguments.isEmpty() ? "" : String.valueOf(arguments.getFirst());
        return switch (member) {
            case "Exists" -> IHost.Reply.of(computer.readFile(path).ok(), LOOK);
            case "Read" -> {
                final ICliComputer.FsResult read = computer.readFile(path);
                if (!read.ok()) {
                    throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, read.message());
                }
                yield IHost.Reply.of(read.message(), READ);
            }
            case "TryRead" -> {
                // The out parameter comes back beside the answer: found, and what was found.
                final ICliComputer.FsResult read = computer.readFile(path);
                yield new IHost.Reply(read.ok(), List.of(read.ok() ? read.message() : ""), READ);
            }
            case "Write" -> IHost.Reply.of(
                    computer.writeFile(path, text(arguments)).ok(), WRITE);
            case "Append" -> {
                final ICliComputer.FsResult had = computer.readFile(path);
                final String before = had.ok() ? had.message() : "";
                yield IHost.Reply.of(computer.writeFile(path, before + text(arguments)).ok(), WRITE);
            }
            case "Delete" -> IHost.Reply.of(computer.deleteFile(path).ok(), WRITE);
            case "MkDir" -> IHost.Reply.of(computer.makeDir(path).ok(), WRITE);
            case "List" -> {
                final Values.ListValue names = new Values.ListValue();
                final ICliComputer.FsResult listing = computer.listDisk(path);
                if (listing.ok()) {
                    for (final ICliComputer.FsEntry entry : listing.entries()) {
                        /*
                         * The name is the whole last segment of the path, extension included, so a name
                         * a program is handed is a name it can turn round and open. A folder ends in a
                         * slash, because a program walking a tree has to be able to tell which is which
                         * and asking it to try opening each one to find out would be a poor answer.
                         */
                        names.items().add(entry.isDir() ? entry.name() + "/" : entry.name());
                    }
                }
                yield IHost.Reply.of(names, READ);
            }
            // What follows is what a Lua program's fs asks, by ComputerCraft paths rather than the shell's.
            case "Entries" -> IHost.Reply.of(entries(computer, path), READ);
            case "Stat" -> {
                final Values.ListValue row = stat(computer, path);
                yield IHost.Reply.of(row, row != null && !isFolder(row) ? READ : LOOK);
            }
            case "Text" -> {
                final ICliComputer.FsResult read = computer.readFile(machinePath(computer, path));
                yield IHost.Reply.of(read.ok() ? read.message() : null, READ);
            }
            case "Put" -> IHost.Reply.of(wrong(computer.writeFile(machinePath(computer, path), text(arguments))), WRITE);
            case "MakeDir" -> IHost.Reply.of(wrong(computer.makeDir(machinePath(computer, path))), WRITE);
            case "Remove" -> IHost.Reply.of(remove(computer, path), WRITE);
            case "Free" -> IHost.Reply.of(computer instanceof ServerCliComputer shell
                    ? shell.freeBytes(machinePath(computer, path)) : 0L, LOOK);
            case "Capacity" -> IHost.Reply.of(computer instanceof ServerCliComputer shell
                    ? shell.capacityBytes(machinePath(computer, path)) : 0L, LOOK);
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, "File has no " + member);
        };
    }

    /*
     * ComputerCraft paths. A ComputerCraft computer has one tree: its own disk at the root and each disk
     * put in a drive under a folder of its own, "disk", then "disk2" and on. The machine's system disk is
     * the root here, and each of its other drives with something in it is one of those folders.
     */

    /** The shell's path for a ComputerCraft one. */
    static String machinePath(final ICliComputer computer, final String path) {
        final String clean = clean(path);
        final int slash = clean.indexOf('/');
        final String first = slash < 0 ? clean : clean.substring(0, slash);
        final char drive = driveOf(computer, first);
        final String rest = drive == SYSTEM_DRIVE ? clean : (slash < 0 ? "" : clean.substring(slash + 1));
        return drive + ":\\" + rest.replace('/', '\\');
    }

    /** A place on the machine as a ComputerCraft path; empty for a drive that has no place in the tree. */
    public static String luaPath(final ICliComputer computer, final DosPath.Location where) {
        final String inside = where.storagePath();
        if (where.drive() == SYSTEM_DRIVE) {
            return inside;
        }
        final int index = otherDrives(computer).indexOf(where.drive());
        if (index < 0) {
            return "";
        }
        return inside.isEmpty() ? diskName(index) : diskName(index) + "/" + inside;
    }

    /** The drive a first folder name stands for: one of the other drives, or the system disk. */
    private static char driveOf(final ICliComputer computer, final String first) {
        if (!first.toLowerCase(java.util.Locale.ROOT).startsWith(DISK)) {
            return SYSTEM_DRIVE;
        }
        final List<Character> drives = otherDrives(computer);
        for (int i = 0; i < drives.size(); i++) {
            if (first.equalsIgnoreCase(diskName(i))) {
                return drives.get(i);
            }
        }
        return SYSTEM_DRIVE;
    }

    /** The drives other than the system disk that hold something, in the order the machine lists them. */
    private static List<Character> otherDrives(final ICliComputer computer) {
        final List<Character> drives = new java.util.ArrayList<>();
        for (final ICliComputer.MountInfo mount : computer.mounts()) {
            if (mount.ready() && Character.toUpperCase(mount.drive()) != SYSTEM_DRIVE) {
                drives.add(Character.toUpperCase(mount.drive()));
            }
        }
        return drives;
    }

    private static String diskName(final int index) {
        return index == 0 ? DISK : DISK + (index + 1);
    }

    private static String clean(final String path) {
        String clean = path == null ? "" : path.replace('\\', '/');
        while (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }

    /** A row for each thing in a folder, null when it is not one; the root shows the other drives too. */
    private static Values.ListValue entries(final ICliComputer computer, final String path) {
        final String clean = clean(path);
        if (!clean.isEmpty()) {
            if (!isFolder(stat(computer, clean))) {
                return null;
            }
        }
        final ICliComputer.FsResult listing = computer.listDisk(machinePath(computer, clean));
        if (!listing.ok() || listing.entries() == null) {
            return null;
        }
        final Values.ListValue rows = new Values.ListValue();
        for (final ICliComputer.FsEntry entry : listing.entries()) {
            rows.items().add(row(entry.name(), entry.isDir(), 0L, entry.readOnly(), entry.modified()));
        }
        if (clean.isEmpty()) {
            for (int i = 0; i < otherDrives(computer).size(); i++) {
                rows.items().add(row(diskName(i), true, 0L, false, 0L));
            }
        }
        return rows;
    }

    /**
     * What is at a path, as a row of name, whether it is a folder, size in bytes, whether it is
     * read-only and when it last changed; null when nothing is there.
     */
    private static Values.ListValue stat(final ICliComputer computer, final String path) {
        final String clean = clean(path);
        if (clean.isEmpty()) {
            return row("", true, 0L, false, 0L);
        }
        final int slash = clean.lastIndexOf('/');
        final String parent = slash < 0 ? "" : clean.substring(0, slash);
        final String name = slash < 0 ? clean : clean.substring(slash + 1);
        if (parent.isEmpty()) {
            final List<Character> drives = otherDrives(computer);
            for (int i = 0; i < drives.size(); i++) {
                if (diskName(i).equalsIgnoreCase(name)) {
                    return row(diskName(i), true, 0L, false, 0L);
                }
            }
        }
        final ICliComputer.FsResult listing = computer.listDisk(machinePath(computer, parent));
        if (!listing.ok() || listing.entries() == null) {
            return null;
        }
        for (final ICliComputer.FsEntry entry : listing.entries()) {
            if (!entry.name().equalsIgnoreCase(name)) {
                continue;
            }
            long size = 0L;
            if (!entry.isDir()) {
                // A file's size is what its text takes as bytes, which is what ComputerCraft reports.
                final ICliComputer.FsResult read = computer.readFile(machinePath(computer, clean));
                size = read.ok() && read.message() != null
                        ? read.message().getBytes(java.nio.charset.StandardCharsets.UTF_8).length : 0L;
            }
            return row(entry.name(), entry.isDir(), size, entry.readOnly(), entry.modified());
        }
        return null;
    }

    private static Values.ListValue row(final String name, final boolean directory, final long size,
                                        final boolean readOnly, final long modified) {
        final Values.ListValue row = new Values.ListValue();
        row.items().add(name);
        row.items().add(directory);
        row.items().add(size);
        row.items().add(readOnly);
        row.items().add(modified);
        return row;
    }

    /** Takes a file or an empty folder away, by whichever of the shell's two it is. */
    private static String remove(final ICliComputer computer, final String path) {
        final Values.ListValue what = stat(computer, path);
        if (what == null) {
            return "";
        }
        final String at = machinePath(computer, path);
        return wrong(isFolder(what) ? computer.removeDir(at) : computer.deleteFile(at));
    }

    private static boolean isFolder(final Values.ListValue row) {
        return row != null && Boolean.TRUE.equals(row.items().get(1));
    }

    /** Nothing when it went right, and the shell's words when it did not. */
    private static String wrong(final ICliComputer.FsResult result) {
        return result.ok() ? "" : (result.message() == null ? "failed" : result.message());
    }

    /** The second argument of a write, which is the text to put there. */
    private static String text(final List<Object> arguments) {
        return arguments.size() < 2 ? "" : String.valueOf(arguments.get(1));
    }
}
