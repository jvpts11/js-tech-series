/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Where a desktop's trash sits on the system disk, what the things in it are called, and how it writes down where
 * each came from.
 *
 * <p>A deleted file is moved, not copied: it keeps taking room on the disk until the trash is emptied, and putting
 * it back is moving it again. What cannot be moved is its old place, so that is written down when it goes in: one
 * index line per file on Frames and on CDE, one small note per file on the other Unix desktops.
 *
 * <p>Pure, with no Minecraft types, so the naming and the records are unit-tested.
 *
 * @param kind the habit this trash follows
 * @param path the trash folder, as a path under the root with no leading slash
 */
public record TrashFolder(TrashKind kind, String path) {

    /** The Frames folder, at the root of the system disk. */
    public static final String RECYCLER_DIR = "RECYCLER";

    /** Where Frames writes down the old place of everything in the bin. */
    public static final String RECYCLER_INDEX = "INFO2";

    /** CDE's index, beside the files it describes. */
    public static final String CDE_INDEX = ".trashinfo";

    /** What a freedesktop note is called after the name of the file it describes. */
    public static final String NOTE_SUFFIX = ".trashinfo";

    /** Separates a stored name from its old place on an index line; a name can hold neither a tab nor a newline. */
    private static final char FIELD = '\t';
    private static final String NOTE_HEADER = "[Trash Info]";
    private static final String NOTE_PATH = "Path=/";

    /** The trash of that kind for a system whose home is {@code home}; Frames keeps it at the root and ignores it. */
    public static TrashFolder of(final TrashKind kind, final String home) {
        return new TrashFolder(kind, switch (kind) {
            case RECYCLER -> RECYCLER_DIR;
            case FREEDESKTOP -> FsPaths.join(home, ".local/share/Trash");
            case CDE -> FsPaths.join(home, ".dt/Trash");
        });
    }

    /** The line an index takes for one deleted thing. */
    public static String indexLine(final String stored, final String original) {
        return stored + FIELD + original + "\n";
    }

    /**
     * The note freedesktop keeps for one deleted thing. It carries no deletion date, since the world keeps no
     * calendar a file could carry.
     */
    public static String note(final String original) {
        return NOTE_HEADER + "\n" + NOTE_PATH + original + "\n";
    }

    /** What an index records, stored name to old place, in the order the lines were written. */
    public static Map<String, String> readIndex(final String content) {
        final Map<String, String> out = new LinkedHashMap<>();
        for (final String line : content.split("\n")) {
            final int field = line.indexOf(FIELD);
            if (field > 0 && field < line.length() - 1) {
                out.put(line.substring(0, field), line.substring(field + 1));
            }
        }
        return out;
    }

    /** The old place a note gives, or empty when it gives none that can be read. */
    public static Optional<String> readNote(final String content) {
        for (final String line : content.split("\n")) {
            if (line.startsWith(NOTE_PATH) && line.length() > NOTE_PATH.length()) {
                return Optional.of(line.substring(NOTE_PATH.length()));
            }
        }
        return Optional.empty();
    }

    /** The name of the thing a file in the info folder is a note about, or empty for a file there that is no note. */
    public static Optional<String> noteSubject(final String fileName) {
        if (fileName.endsWith(NOTE_SUFFIX) && fileName.length() > NOTE_SUFFIX.length()) {
            return Optional.of(fileName.substring(0, fileName.length() - NOTE_SUFFIX.length()));
        }
        return Optional.empty();
    }

    /** An index with the line of {@code stored} taken out. */
    public static String withoutEntry(final String content, final String stored) {
        final StringBuilder out = new StringBuilder();
        for (final String line : content.split("\n")) {
            if (!line.isEmpty() && !line.startsWith(stored + FIELD)) {
                out.append(line).append('\n');
            }
        }
        return out.toString();
    }

    /**
     * The path a thing put back takes: its old place, or that name numbered the way a desktop numbers a new file
     * when something else has taken the place since.
     */
    public static String freePlace(final String original, final Predicate<String> taken) {
        if (!taken.test(original)) {
            return original;
        }
        final String dir = FsPaths.parentDir(original);
        final String name = FsPaths.fileName(original);
        final String ext = extensionOf(name);
        final String stem = name.substring(0, name.length() - ext.length());
        int n = 2;
        String candidate;
        do {
            candidate = FsPaths.join(dir, fitUnder(dir, stem, " (" + n++ + ")" + ext));
        } while (taken.test(candidate));
        return candidate;
    }

    /** The folders the trash needs, parents first, so making them in order never meets a missing parent. */
    public List<String> directories() {
        final List<String> out = new ArrayList<>();
        String built = "";
        for (final String segment : path.split("/")) {
            built = FsPaths.join(built, segment);
            out.add(built);
        }
        if (kind == TrashKind.FREEDESKTOP) {
            out.add(path + "/files");
            out.add(path + "/info");
        }
        return List.copyOf(out);
    }

    /** The folder the deleted things themselves are kept in. */
    public String filesDir() {
        return kind == TrashKind.FREEDESKTOP ? path + "/files" : path;
    }

    /** Where the thing stored under that name is. */
    public String storedPath(final String stored) {
        return filesDir() + "/" + stored;
    }

    /** Whether a path is the trash itself or anything in it. */
    public boolean holds(final String candidate) {
        return candidate.equals(path) || FsPaths.isUnder(path, candidate);
    }

    /** Whether one index records the whole trash, rather than a note for each thing in it. */
    public boolean indexed() {
        return kind != TrashKind.FREEDESKTOP;
    }

    /** The name of the one index, which nothing deleted may take; empty for a trash that keeps notes. */
    public String indexName() {
        return switch (kind) {
            case RECYCLER -> RECYCLER_INDEX;
            case CDE -> CDE_INDEX;
            case FREEDESKTOP -> "";
        };
    }

    /** The file that records the old place of {@code stored}: the one index, or that thing's own note. */
    public String recordPath(final String stored) {
        return indexed() ? path + "/" + indexName() : path + "/info/" + stored + NOTE_SUFFIX;
    }

    /** The folder the notes are in, for a trash that keeps them. */
    public String notesDir() {
        return path + "/info";
    }

    /**
     * The name a thing called {@code name} takes in the trash, where {@code taken} says which names are there
     * already. Frames gives everything a serial name and keeps only its extension, the way its bin always did;
     * the others keep the name, numbered before the extension when one of that name is already in.
     */
    public String storedName(final String name, final Predicate<String> taken) {
        final Predicate<String> busy = candidate -> taken.test(candidate) || candidate.equals(indexName());
        final String ext = extensionOf(name);
        if (kind == TrashKind.RECYCLER) {
            int n = 1;
            while (busy.test("Dc" + n + fitExtension(ext))) {
                n++;
            }
            return "Dc" + n + fitExtension(ext);
        }
        if (!busy.test(name)) {
            return name;
        }
        final String stem = name.substring(0, name.length() - ext.length());
        int n = 2;
        String candidate;
        do {
            candidate = fit(stem, "." + n++ + ext);
        } while (busy.test(candidate));
        return candidate;
    }

    /** The extension of a name with its dot, or empty for none; a name that only starts with a dot has none. */
    private static String extensionOf(final String name) {
        final int dot = name.lastIndexOf('.');
        return dot > 0 && dot < name.length() - 1 ? name.substring(dot) : "";
    }

    /** A stem and what follows it, the stem cut short so the whole stays a name a path can hold. */
    private static String fit(final String stem, final String tail) {
        final int room = Math.max(1, FsPaths.MAX_NAME_LENGTH - tail.length());
        return (stem.length() > room ? stem.substring(0, room) : stem) + tail;
    }

    /**
     * The same, cut to what is left of a whole path once the folder it goes in is counted.
     *
     * <p>Putting a thing back numbers its name, which makes it longer. A name that was already as long as a
     * name may be, in a folder deep enough, would then make a path longer than a path may be, and the thing
     * would quietly fail to come back out of the trash at all.
     */
    private static String fitUnder(final String dir, final String stem, final String tail) {
        final int forName = FsPaths.MAX_NAME_LENGTH - tail.length();
        final int forPath = FsPaths.MAX_PATH_LENGTH - tail.length()
                - (dir.isEmpty() ? 0 : dir.length() + 1);
        final int room = Math.max(1, Math.min(forName, forPath));
        return (stem.length() > room ? stem.substring(0, room) : stem) + tail;
    }

    /** An extension short enough to follow a serial name. */
    private static String fitExtension(final String ext) {
        final int room = FsPaths.MAX_NAME_LENGTH - "Dc".length() - 6;
        return ext.length() > room ? "" : ext;
    }
}
