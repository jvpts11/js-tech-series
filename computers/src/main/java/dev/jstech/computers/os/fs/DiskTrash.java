/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.core.tier.HardwareEra;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

/**
 * The trash of a system disk: putting a file or folder into it, reading what is in it, putting a thing back where it
 * came from, and destroying it for good.
 *
 * <p>Everything here is a move or a delete on the disk the machine already has, laid out by a {@link TrashFolder}.
 * Nothing is kept anywhere else, so a player who looks into the trash folder with a file manager or at a prompt finds
 * exactly what the trash window lists, and the room the trash takes is the room its files take.
 */
public final class DiskTrash {

    /** Only a hierarchical disk has folders, and only a desktop has a trash, which only such a disk carries. */
    private static final FilesystemKind TREE = FilesystemKind.HIERARCHICAL;

    /**
     * One thing in the trash.
     *
     * @param stored    the name it is kept under in the trash
     * @param original  where it was when it was deleted, as a path under the root
     * @param directory whether it is a folder
     * @param weight    the room it takes, with everything in it for a folder
     */
    public record Entry(String stored, String original, boolean directory, long weight) {
    }

    /** How putting something in the trash went. */
    public enum Outcome {
        /** It is in the trash. */
        DONE,
        /** Nothing is at that path. */
        MISSING,
        /** It is the trash, or a folder the trash is in, which cannot go into itself. */
        HOLDS_TRASH,
        /** The disk has no room left to write down where it came from, so it stayed where it was. */
        NO_ROOM
    }

    private DiskTrash() {
    }

    /**
     * Moves the file or folder at {@code path} into the trash and writes down where it came from.
     *
     * @param freeWeight the room left on the disk, which the record of the old place has to fit in
     * @param now        the world time the record is stamped with
     */
    public static Outcome put(final ItemStack disk, final TrashFolder trash, final String path, final long freeWeight,
                              final long now) {
        if (path.isEmpty() || trash.holds(path) || FsPaths.isUnder(path, trash.path())) {
            return Outcome.HOLDS_TRASH;
        }
        if (!present(disk, path)) {
            return Outcome.MISSING;
        }
        for (final String dir : trash.directories()) {
            if (!present(disk, dir)) {
                DiskFilesystem.mkdir(disk, dir, TREE);
            }
        }
        final String stored = trash.storedName(FsPaths.fileName(path), name -> present(disk, trash.storedPath(name)));
        if (!DiskFilesystem.rename(disk, path, trash.storedPath(stored), TREE)) {
            return Outcome.MISSING;
        }
        if (record(disk, trash, stored, path, freeWeight, now)) {
            return Outcome.DONE;
        }
        // No room to write down where it came from: it goes back, so nothing sits in the trash with no way home.
        DiskFilesystem.rename(disk, trash.storedPath(stored), path, TREE);
        return Outcome.NO_ROOM;
    }

    /** What is in the trash, in the order it went in; a thing without a record, or a record without it, is left out. */
    public static List<Entry> list(final ItemStack disk, final TrashFolder trash) {
        final FilesystemContents fs = contents(disk);
        final HardwareEra era = DiskFilesystem.eraOf(disk);
        final List<Entry> out = new ArrayList<>();
        for (final Map.Entry<String, String> one : records(disk, trash).entrySet()) {
            final String at = trash.storedPath(one.getKey());
            final StoredFile file = fs.files().get(at);
            if (file != null) {
                out.add(new Entry(one.getKey(), one.getValue(), false, file.weight(era)));
            } else if (isDirectory(fs, at)) {
                out.add(new Entry(one.getKey(), one.getValue(), true, weightUnder(fs, at, era)));
            }
        }
        return out;
    }

    /** Whether anything at all is in the trash, which is what its picture shows. */
    public static boolean holdsAnything(final ItemStack disk, final TrashFolder trash) {
        return !list(disk, trash).isEmpty();
    }

    /**
     * Puts the thing kept under {@code stored} back where it came from, making again any folder on the way there that
     * has gone since. When something else has taken its place since, it comes back beside it under a numbered name.
     *
     * @return whether it went back
     */
    public static boolean restore(final ItemStack disk, final TrashFolder trash, final String stored, final long now) {
        final String original = records(disk, trash).get(stored);
        if (original == null || !present(disk, trash.storedPath(stored))) {
            return false;
        }
        final String place = TrashFolder.freePlace(original, candidate -> present(disk, candidate));
        if (!makeFoldersTo(disk, FsPaths.parentDir(place))
                || !DiskFilesystem.rename(disk, trash.storedPath(stored), place, TREE)) {
            return false;
        }
        forget(disk, trash, stored, now);
        return true;
    }

    /** Deletes for good the thing kept under {@code stored}, and its record with it. */
    public static boolean shred(final ItemStack disk, final TrashFolder trash, final String stored, final long now) {
        final String at = trash.storedPath(stored);
        final boolean gone = DiskFilesystem.delete(disk, at) || DiskFilesystem.rmdir(disk, at, TREE);
        // A record whose thing is already gone goes too, so it never lists something that is not there.
        forget(disk, trash, stored, now);
        return gone;
    }

    /** Deletes everything in the trash for good, the folder and its records included; it is made again when needed. */
    public static boolean empty(final ItemStack disk, final TrashFolder trash) {
        return DiskFilesystem.rmdir(disk, trash.path(), TREE);
    }

    /**
     * Deletes for good something that is already inside the trash folder, reached by its path rather than from the
     * trash window: the folder itself empties the trash, one of the things kept there is shredded with its record,
     * and anything else is simply removed.
     */
    public static boolean destroyWithin(final ItemStack disk, final TrashFolder trash, final String path,
                                        final long now) {
        if (path.equals(trash.path())) {
            return empty(disk, trash);
        }
        if (trash.filesDir().equals(FsPaths.parentDir(path))) {
            return shred(disk, trash, FsPaths.fileName(path), now);
        }
        return DiskFilesystem.delete(disk, path) || DiskFilesystem.rmdir(disk, path, TREE);
    }

    /** Where each thing in the trash came from, by the name it is kept under, in the order it was written down. */
    private static Map<String, String> records(final ItemStack disk, final TrashFolder trash) {
        if (trash.indexed()) {
            return DiskFilesystem.read(disk, trash.recordPath("")).map(TrashFolder::readIndex).orElse(Map.of());
        }
        final Map<String, String> out = new LinkedHashMap<>();
        for (final DiskFilesystem.FileEntry note : DiskFilesystem.list(disk, trash.notesDir(), TREE)) {
            TrashFolder.noteSubject(FsPaths.fileName(note.path())).ifPresent(stored ->
                    DiskFilesystem.read(disk, note.path()).flatMap(TrashFolder::readNote)
                            .ifPresent(original -> out.put(stored, original)));
        }
        return out;
    }

    /** Writes down that {@code stored} came from {@code original}: a line on the index, or a note of its own. */
    private static boolean record(final ItemStack disk, final TrashFolder trash, final String stored,
                                  final String original, final long freeWeight, final long now) {
        final DiskFilesystem.WriteResult written = trash.indexed()
                ? DiskFilesystem.append(disk, trash.recordPath(stored), FileType.OTHER,
                        TrashFolder.indexLine(stored, original), freeWeight, TREE, now)
                : DiskFilesystem.write(disk, trash.recordPath(stored), FileType.OTHER, TrashFolder.note(original),
                        freeWeight, TREE, now);
        return written == DiskFilesystem.WriteResult.OK;
    }

    /** Takes out the record of {@code stored}; an index left with nothing on it goes too. */
    private static void forget(final ItemStack disk, final TrashFolder trash, final String stored, final long now) {
        final String record = trash.recordPath(stored);
        if (!trash.indexed()) {
            DiskFilesystem.delete(disk, record);
            return;
        }
        DiskFilesystem.read(disk, record).ifPresent(content -> {
            final String rest = TrashFolder.withoutEntry(content, stored);
            if (rest.isEmpty()) {
                DiskFilesystem.delete(disk, record);
            } else {
                // The index only shrinks here, so no amount of room it needs can be missing.
                DiskFilesystem.write(disk, record, FileType.OTHER, rest, Long.MAX_VALUE, TREE, now);
            }
        });
    }

    /**
     * Makes every folder on the way to {@code dir} that is not there.
     *
     * @return false when a file stands where one of those folders should be, which nothing can be put under
     */
    private static boolean makeFoldersTo(final ItemStack disk, final String dir) {
        String built = "";
        for (final String segment : dir.isEmpty() ? new String[0] : dir.split("/")) {
            built = FsPaths.join(built, segment);
            if (DiskFilesystem.exists(disk, built)) {
                return false;
            }
            if (!isDirectory(contents(disk), built)) {
                DiskFilesystem.mkdir(disk, built, TREE);
            }
        }
        return true;
    }

    /** Whether a file or a folder is at that path. */
    private static boolean present(final ItemStack disk, final String path) {
        final FilesystemContents fs = contents(disk);
        return fs.files().containsKey(path) || isDirectory(fs, path);
    }

    /** Whether a folder is at that path, made on its own or standing under a file that is in it. */
    private static boolean isDirectory(final FilesystemContents fs, final String path) {
        if (fs.hasDir(path)) {
            return true;
        }
        for (final String file : fs.files().keySet()) {
            if (FsPaths.isUnder(path, file)) {
                return true;
            }
        }
        return false;
    }

    /** The room everything under a folder takes. */
    private static long weightUnder(final FilesystemContents fs, final String dir, final HardwareEra era) {
        long total = 0L;
        for (final StoredFile file : fs.files().values()) {
            if (FsPaths.isUnder(dir, file.path())) {
                total += file.weight(era);
            }
        }
        return total;
    }

    private static FilesystemContents contents(final ItemStack disk) {
        return disk.getOrDefault(ComputingModule.FILESYSTEM.get(), FilesystemContents.EMPTY);
    }
}
