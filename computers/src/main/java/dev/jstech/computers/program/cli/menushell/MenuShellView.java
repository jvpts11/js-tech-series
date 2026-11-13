/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.menushell;

import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.DosPath;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.cli.Stamps;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.resources.ResourceLocation;

/**
 * The machine's answer to a text-mode shell: what a folder holds and everything round it a shell shows, read the way
 * the system's own commands read it, so the shell lists what DIR or ls would and nothing they would not. Asked for by
 * name, as a file is, and answered as the lines of a {@link MenuShellListing}.
 */
public final class MenuShellView {

    /** The most folders a tree is built of, and how deep it goes, so a disk full of folders answers quickly. */
    private static final int MOST_FOLDERS = 256;
    private static final int MOST_DEPTH = 8;
    /** The most a search reports, and the most folders it reads to find them. */
    private static final int MOST_RESULTS = 128;
    private static final int MOST_SEARCHED = 1024;

    private MenuShellView() {
    }

    /** What the machine answers to {@code path}, one of a shell's names, as the lines a listing travels in. */
    public static String answer(final ServerCliComputer shell, final String path) {
        final String searched = MenuShellListing.searched(path);
        if (searched != null) {
            return search(shell, searched).write();
        }
        final String viewed = MenuShellListing.viewed(path);
        return view(shell, viewed == null ? "" : viewed).write();
    }

    /** A folder and everything round it: the drives, the tree, the programs and the printer. */
    static MenuShellListing view(final ServerCliComputer shell, final String dir) {
        final boolean dos = shell.shellFamily() == ShellFamily.DOS;
        final String asked = dir.isEmpty() ? (dos ? "C:\\" : "/") : dir;
        final ICliComputer.FsResult listed = shell.listDisk(asked);
        final List<MenuShellListing.Entry> entries = new ArrayList<>();
        if (listed.ok() && listed.entries() != null) {
            for (final ICliComputer.FsEntry entry : listed.entries()) {
                entries.add(entry(entry));
            }
        }
        final List<MenuShellListing.Drive> drives = new ArrayList<>();
        if (dos) {
            for (final ICliComputer.MountInfo mount : shell.mounts()) {
                drives.add(new MenuShellListing.Drive(mount.drive(), mount.ready(), mount.capacityMbEq(),
                        mount.freeMbEq()));
            }
        }
        final String written = written(shell, asked, dos);
        return new MenuShellListing(written, listed.ok(), !shell.printers().isEmpty(), drives,
                dos ? tree(shell, written, listed.ok(), entries) : List.of(), entries,
                programs(shell), List.of());
    }

    /**
     * The files on the system disk whose names are like {@code pattern}, written whole: DOS's wildcards, a star for
     * any run of letters and a question mark for one, matched without regard to case. A search reads at most
     * MOST_SEARCHED folders, nearest first, and reports at most MOST_RESULTS files, so on a disk past either it
     * finds what is nearest the root.
     */
    static MenuShellListing search(final ServerCliComputer shell, final String pattern) {
        final boolean dos = shell.shellFamily() == ShellFamily.DOS;
        final String root = dos ? "C:\\" : "/";
        final String like = pattern.isBlank() ? "*" : pattern.trim();
        final List<String> results = new ArrayList<>();
        final EntryVisitor matching = (whole, entry) -> {
            if (!entry.isDir() && Wildcards.matches(like, entry.name()) && results.size() < MOST_RESULTS) {
                results.add(whole);
            }
            return entry.isDir();
        };
        walk(shell, root, dos, Integer.MAX_VALUE, MOST_SEARCHED, () -> results.size() >= MOST_RESULTS, matching);
        return new MenuShellListing(root, true, false, List.of(), List.of(), List.of(), List.of(), results);
    }

    /*
     * One thing in a folder, its extension kept apart from its name the way a DOS listing writes it: what follows
     * the last dot of its name, which is what its name says it is whatever the file holds.
     */
    private static MenuShellListing.Entry entry(final ICliComputer.FsEntry entry) {
        final String whole = entry.name();
        final int dot = whole.lastIndexOf('.');
        final boolean apart = !entry.isDir() && dot > 0 && dot < whole.length() - 1;
        final String name = apart ? whole.substring(0, dot) : whole;
        final String ext = apart ? whole.substring(dot + 1) : "";
        return new MenuShellListing.Entry(name, ext, entry.weightMbEq(), Stamps.of(entry.modified()),
                entry.isDir(), entry.readOnly());
    }

    /*
     * Every folder of the drive {@code looked} is on, each written whole, as far as the limits go, and past them
     * the way down to {@code looked}, when it was {@code found}, and the folders in it ({@code entries}): a disk
     * deeper or fuller than the walk reads still shows, where the player is, every folder there is to open.
     */
    private static List<String> tree(final ServerCliComputer shell, final String looked, final boolean found,
                                     final List<MenuShellListing.Entry> entries) {
        final String root = looked.substring(0, Math.min(3, looked.length()));
        final List<String> out = new ArrayList<>();
        walk(shell, root, true, MOST_DEPTH, MOST_FOLDERS, () -> out.size() >= MOST_FOLDERS, (whole, entry) -> {
            if (entry.isDir() && out.size() < MOST_FOLDERS) {
                out.add(whole);
                return true;
            }
            return false;
        });
        final Set<String> known = new HashSet<>();
        for (final String folder : out) {
            known.add(folder.toUpperCase(Locale.ROOT));
        }
        if (found) {
            String way = root;
            for (final String part : looked.substring(root.length()).split("\\\\")) {
                if (!part.isEmpty()) {
                    way = join(way, part, true);
                    kept(out, known, way);
                }
            }
            for (final MenuShellListing.Entry entry : entries) {
                if (entry.folder()) {
                    kept(out, known, join(looked, entry.fullName(), true));
                }
            }
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    /* Adds {@code folder} to the tree unless it is there already, without regard to case. */
    private static void kept(final List<String> out, final Set<String> known, final String folder) {
        if (known.add(folder.toUpperCase(Locale.ROOT))) {
            out.add(folder);
        }
    }

    /** What a walk of the disk does with each thing it meets; true for a folder it wants the walk to go into. */
    private interface EntryVisitor {
        boolean visit(String whole, ICliComputer.FsEntry entry);
    }

    /*
     * Goes over the disk folder by folder, nearest first, from {@code root}. It stops after {@code mostRead}
     * folders have been read or as soon as {@code done} says so, never goes {@code deepest} levels down, and
     * passes over a folder that cannot be read.
     */
    private static void walk(final ServerCliComputer shell, final String root, final boolean dos, final int deepest,
                             final int mostRead, final BooleanSupplier done, final EntryVisitor visitor) {
        final Deque<String> folders = new ArrayDeque<>();
        final Deque<Integer> depths = new ArrayDeque<>();
        folders.add(root);
        depths.add(0);
        int read = 0;
        while (!folders.isEmpty() && read++ < mostRead && !done.getAsBoolean()) {
            final String folder = folders.poll();
            final int depth = depths.poll();
            final ICliComputer.FsResult listed = shell.listDisk(folder);
            if (!listed.ok() || listed.entries() == null) {
                continue;
            }
            for (final ICliComputer.FsEntry entry : listed.entries()) {
                final String whole = join(folder, entry.name(), dos);
                if (visitor.visit(whole, entry) && entry.isDir() && depth + 1 < deepest) {
                    folders.add(whole);
                    depths.add(depth + 1);
                }
            }
        }
    }

    /* The programs installed, by the names their makers gave them and the commands that start them. */
    private static List<MenuShellListing.Program> programs(final ServerCliComputer shell) {
        final List<MenuShellListing.Program> out = new ArrayList<>();
        for (final ICliComputer.ProgramInfo program : shell.programs()) {
            final ProgramSpec spec = Programs.get(ResourceLocation.tryParse(program.id()));
            out.add(new MenuShellListing.Program(spec == null ? program.name() : spec.displayName(), program.name()));
        }
        return out;
    }

    /* The folder written the way the system writes a path: C:\DOS on DOS, as asked elsewhere. */
    private static String written(final ServerCliComputer shell, final String asked, final boolean dos) {
        return dos ? DosPath.resolve(shell.currentLocation(), asked).dosPath() : asked;
    }

    private static String join(final String folder, final String name, final boolean dos) {
        final String separator = dos ? "\\" : "/";
        return folder.endsWith(separator) ? folder + name : folder + separator + name;
    }
}
