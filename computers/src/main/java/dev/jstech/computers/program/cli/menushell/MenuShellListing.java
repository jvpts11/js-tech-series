/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.menushell;

import dev.jstech.computers.os.fs.StoredFile;
import java.util.ArrayList;
import java.util.List;

/**
 * What a text-mode shell of the Vintage systems is told about the machine it runs on: the folder it is looking at and
 * what is in it, the drives, every folder of the system disk for a tree, the programs installed, whether a printer
 * is there, and what a search found. The machine writes it out as lines and the shell reads them back, which is how
 * it comes over: asked for by name, as a file is.
 *
 * <p>Each line is a letter saying what it is and its fields after it, separated by tabs, which no name on these
 * systems can hold.
 *
 * @param dir      the folder looked at, written the way the system writes a path
 * @param found    whether that folder was there to look at
 * @param printer  whether a printer is linked to the machine
 * @param drives   the machine's drives, by letter, on a system that has letters
 * @param tree     every folder of the system disk, each written whole, for a shell that draws a tree
 * @param entries  what the folder holds
 * @param programs the programs installed on the machine
 * @param results  what a search found, each written whole
 */
public record MenuShellListing(String dir, boolean found, boolean printer, List<Drive> drives, List<String> tree,
                               List<Entry> entries, List<Program> programs, List<String> results) {

    /** How a shell names what it asks the machine for, as a file is named. */
    public static final String SCHEME = "menushell:";
    /* What follows the name, the folder looked at or what is searched for. */
    private static final String VIEW = "/view?";
    private static final String SEARCH = "/search?";
    private static final String TAB = "\t";

    public MenuShellListing {
        drives = List.copyOf(drives);
        tree = List.copyOf(tree);
        entries = List.copyOf(entries);
        programs = List.copyOf(programs);
        results = List.copyOf(results);
    }

    /** The name a shell asks for to look at {@code dir}, the root when it is empty. */
    public static String view(final String dir) {
        return SCHEME + VIEW + dir;
    }

    /** The name a shell asks for to search the system disk for names like {@code pattern}. */
    public static String search(final String pattern) {
        return SCHEME + SEARCH + pattern;
    }

    /** Whether a name asked for is one of these. */
    public static boolean names(final String path) {
        return path != null && path.startsWith(SCHEME);
    }

    /** The folder a name asks to look at, or null when it asks something else. */
    public static String viewed(final String path) {
        return what(path, VIEW);
    }

    /** What a name asks to search for, or null when it asks something else. */
    public static String searched(final String path) {
        return what(path, SEARCH);
    }

    /** A listing of nothing, for a machine that could not be asked. */
    public static MenuShellListing empty(final String dir) {
        return new MenuShellListing(dir, false, false, List.of(), List.of(), List.of(), List.of(), List.of());
    }

    /**
     * The listing as the lines it travels in, kept to what a file's content carries: a reply past that is refused
     * whole, and the shell would show nothing. What the folder holds goes first, so a disk of many folders loses the
     * far end of its tree before it loses a file.
     */
    public String write() {
        final StringBuilder out = new StringBuilder();
        boolean room = line(out, "D", dir, found ? "1" : "0", printer ? "1" : "0");
        for (final Drive drive : drives) {
            room = room && line(out, "M", String.valueOf(drive.letter()), drive.ready() ? "1" : "0",
                    Long.toString(drive.capacity()), Long.toString(drive.free()));
        }
        for (final Entry entry : entries) {
            room = room && line(out, "F", entry.name(), entry.ext(), Long.toString(entry.weight()), entry.stamp(),
                    entry.folder() ? "1" : "0", entry.readOnly() ? "1" : "0");
        }
        for (final Program program : programs) {
            room = room && line(out, "G", program.label(), program.command());
        }
        for (final String folder : tree) {
            room = room && line(out, "T", folder);
        }
        for (final String result : results) {
            room = room && line(out, "S", result);
        }
        return out.toString();
    }

    /** Reads a listing back from its lines; a line it cannot read is left out. */
    public static MenuShellListing read(final String text) {
        String dir = "";
        boolean found = false;
        boolean printer = false;
        final List<Drive> drives = new ArrayList<>();
        final List<String> tree = new ArrayList<>();
        final List<Entry> entries = new ArrayList<>();
        final List<Program> programs = new ArrayList<>();
        final List<String> results = new ArrayList<>();
        for (final String line : (text == null ? "" : text).split("\n")) {
            final String[] f = line.split(TAB, -1);
            switch (f[0]) {
                case "D" -> {
                    if (f.length >= 4) {
                        dir = f[1];
                        found = "1".equals(f[2]);
                        printer = "1".equals(f[3]);
                    }
                }
                case "M" -> {
                    if (f.length >= 5 && !f[1].isEmpty()) {
                        drives.add(new Drive(f[1].charAt(0), "1".equals(f[2]), parse(f[3]), parse(f[4])));
                    }
                }
                case "T" -> {
                    if (f.length >= 2) {
                        tree.add(f[1]);
                    }
                }
                case "F" -> {
                    if (f.length >= 7) {
                        entries.add(new Entry(f[1], f[2], parse(f[3]), f[4], "1".equals(f[5]), "1".equals(f[6])));
                    }
                }
                case "G" -> {
                    if (f.length >= 3) {
                        programs.add(new Program(f[1], f[2]));
                    }
                }
                case "S" -> {
                    if (f.length >= 2) {
                        results.add(f[1]);
                    }
                }
                default -> {
                    // A line of a kind this side does not know, from a machine newer than it.
                }
            }
        }
        return new MenuShellListing(dir, found, printer, drives, tree, entries, programs, results);
    }

    private static String what(final String path, final String kind) {
        if (!names(path)) {
            return null;
        }
        final String rest = path.substring(SCHEME.length());
        return rest.startsWith(kind) ? rest.substring(kind.length()) : null;
    }

    /* Adds the line when it fits in what is left; false, and nothing added, when it does not. */
    private static boolean line(final StringBuilder out, final String kind, final String... fields) {
        final StringBuilder line = new StringBuilder(kind);
        for (final String field : fields) {
            // A tab or a line break inside a field would split it, so they become spaces.
            line.append(TAB).append(field == null ? "" : field.replace('\t', ' ').replace('\n', ' '));
        }
        line.append('\n');
        if (out.length() + line.length() > StoredFile.MOST_CHARS) {
            return false;
        }
        out.append(line);
        return true;
    }

    private static long parse(final String number) {
        try {
            return Long.parseLong(number);
        } catch (final NumberFormatException notANumber) {
            return 0L;
        }
    }

    /**
     * A drive by its letter: whether there is something in it to read, and how much it holds and has free, in the
     * system's own unit.
     */
    public record Drive(char letter, boolean ready, long capacity, long free) {
    }

    /**
     * One thing in a folder.
     *
     * @param name     its name, without the extension on a system that keeps one apart
     * @param ext      its extension, or empty
     * @param weight   what it weighs on its disk, in the system's own unit
     * @param stamp    when it was last written, as the system's listing writes it
     * @param folder   whether it is a folder
     * @param readOnly whether it may only be read
     */
    public record Entry(String name, String ext, long weight, String stamp, boolean folder, boolean readOnly) {

        /** Its whole name, the extension after a dot when it has one. */
        public String fullName() {
            return ext.isEmpty() ? name : name + "." + ext;
        }
    }

    /** A program installed on the machine: what it is called and the command that starts it. */
    public record Program(String label, String command) {
    }
}
