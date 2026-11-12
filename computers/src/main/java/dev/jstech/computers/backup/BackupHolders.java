/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.backup;

import dev.jstech.computers.hardware.EraStorage;
import dev.jstech.computers.os.fs.BackupFile;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.fs.StoredFile;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Where a network's backups are: on the tapes of its Tape Libraries and in the backup files on its computers' drives.
 * What the Restore tab lists, newest first, and what a restore looks for its backup on.
 */
public final class BackupHolders {

    private BackupHolders() {
    }

    /**
     * One backup the network holds, where it is held.
     *
     * @param id      the backup
     * @param job     the job that made it
     * @param clock   the world's clock when it was made
     * @param parts   what it keeps
     * @param items   how many of the network's items it keeps
     * @param sizeMb  what it takes where it is held, in megabytes
     * @param onTape  whether a tape holds it rather than a drive
     * @param tape    the tape's slot in its library, from 0, when a tape holds it
     * @param library the library's rack number and row, as the network names it, when a tape holds it
     * @param machine   the computer whose drive holds it, when a drive does
     * @param drive     that drive among the computer's, from 0
     * @param mbPerItem what an item weighs where it is held, in megabytes
     */
    public record Held(UUID id, String job, long clock, Set<BackupPart> parts, long items, long sizeMb,
                       boolean onTape, int tape, String library, String machine, int drive, long mbPerItem) {
    }

    /** Every backup {@code network} holds, the newest first; one held twice is listed where it is first found. */
    public static List<Held> of(final ServerLevel level, final NetworkUuid network) {
        final BackupArchive archive = BackupArchive.of(level);
        final List<Held> out = new ArrayList<>();
        final List<UUID> seen = new ArrayList<>();
        for (final TapeLibrary library : TapeLibrary.of(level, network)) {
            final long mbPerItem = EraStorage.mbPerItem(library.format().era());
            for (int n = 0; n < library.slots(); n++) {
                for (final TapeContents.Spot spot : library.contents(n).spots()) {
                    final BackupArchive.Backup backup = archive.get(spot.backup());
                    if (backup != null && !seen.contains(spot.backup())) {
                        seen.add(spot.backup());
                        out.add(new Held(backup.id(), backup.job(), backup.clock(), backup.parts(), backup.items(),
                                spot.size() * mbPerItem, true, n, libraryName(library), "", -1, mbPerItem));
                    }
                }
            }
        }
        for (final NetworkMachines.Machine machine : NetworkMachines.of(level, network)) {
            final List<ItemStack> drives = machine.host().diskStacks();
            for (int d = 0; d < drives.size(); d++) {
                for (final BackupFile file : filesOn(drives.get(d))) {
                    final BackupArchive.Backup backup = archive.get(file.backup());
                    if (backup != null && !seen.contains(file.backup())) {
                        seen.add(file.backup());
                        final long mbPerItem = EraStorage.mbPerItem(DiskFilesystem.eraOf(drives.get(d)));
                        out.add(new Held(backup.id(), backup.job(), backup.clock(), backup.parts(), backup.items(),
                                Math.ceilDiv(backup.weight() * mbPerItem, StorageKey.MB_EQ_PER_ITEM), false, -1, "",
                                machine.name(), d, mbPerItem));
                    }
                }
            }
        }
        out.sort(Comparator.comparingLong(Held::clock).reversed());
        return out;
    }

    /**
     * Lets the world's archive go of each of {@code ids} that no tape or drive of {@code network} holds any more: a
     * backup another tape or a copy of its file on a drive still holds keeps what it holds, so that copy restores.
     */
    public static void forgetUnheld(final ServerLevel level, final NetworkUuid network, final Collection<UUID> ids) {
        final Set<UUID> held = new HashSet<>();
        for (final Held one : of(level, network)) {
            held.add(one.id());
        }
        final BackupArchive archive = BackupArchive.of(level);
        for (final UUID id : ids) {
            if (!held.contains(id)) {
                archive.remove(id);
            }
        }
    }

    /** The backup {@code id} where a drive of {@code network} holds it, or null when no drive does. */
    @Nullable
    public static Held onDrive(final ServerLevel level, final NetworkUuid network, final UUID id) {
        for (final Held held : of(level, network)) {
            if (!held.onTape() && held.id().equals(id)) {
                return held;
            }
        }
        return null;
    }

    /** The backup {@code id} wherever {@code network} holds it, or null when nothing does. */
    @Nullable
    public static Held find(final ServerLevel level, final NetworkUuid network, final UUID id) {
        for (final Held held : of(level, network)) {
            if (held.id().equals(id)) {
                return held;
            }
        }
        return null;
    }

    /** How the network names a library: its rack's number and the row it is topped at, from 1, as "2:5". */
    public static String libraryName(final TapeLibrary library) {
        return library.rackNumber() + ":" + (library.row() + 1);
    }

    private static List<BackupFile> filesOn(final ItemStack drive) {
        if (drive.isEmpty()) {
            return List.of();
        }
        final FilesystemContents fs = drive.getOrDefault(ComputingComponents.FILESYSTEM.get(),
                FilesystemContents.EMPTY);
        final List<BackupFile> out = new ArrayList<>();
        for (final StoredFile file : fs.files().values()) {
            if (file.type() == FileType.BAK) {
                final BackupFile backup = BackupFile.read(file.content());
                if (backup != null) {
                    out.add(backup);
                }
            }
        }
        return out;
    }
}
