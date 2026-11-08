/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.storage;

import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.boot.SystemIntegrity;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.StoredFile;
import dev.jstech.computers.registry.ComputingComponents;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Corrupting a real disk (computers/docs/design/storage.md): a share of the items within reach leave its contents for
 * the corrupted ones, and as many of its system files leave its file system the same way, so a system missing the file
 * that starts it no longer starts. Both wait for the Defrag. A disk whose damage passes nine tenths dies, and takes its
 * files and systems with it.
 *
 * <p>Corruption never happens in normal play, and never when the server's settings turn it off.
 */
public final class DiskCorruption {

    private DiskCorruption() {
    }

    /**
     * Corrupts {@code share} of what can be reached on {@code disk}.
     *
     * @param exposed     the items within reach (a Personal Computer's public part), or null for all the disk holds
     * @param systemFiles whether its system files are within reach too
     * @return whether anything was corrupted
     */
    public static boolean corrupt(final ItemStack disk, final double share,
                                  @Nullable final Map<StorageKey, Long> exposed, final boolean systemFiles,
                                  final RandomSource random) {
        if (!ComputersServerConfig.corruption() || !DriveVolumes.isDisk(disk) || DriveVolumes.health(disk).dead()) {
            return false;
        }
        final StorageVolume volume = DriveVolumes.of(disk);
        boolean hit = volume.corrupt(exposed == null ? volume.items() : exposed, share, random::nextDouble) > 0L;
        if (systemFiles && !volume.dead()) {
            hit |= corruptSystemFiles(disk, volume, share, random);
        }
        if (volume.dead()) {
            // Nothing on a dead disk can be read: its file system and the systems on it go with it.
            disk.remove(ComputingComponents.FILESYSTEM.get());
            disk.remove(ComputingComponents.DISK_SYSTEMS.get());
            hit = true;
        }
        DriveVolumes.refreshUsage(disk, volume);
        return hit;
    }

    /**
     * Each file of the systems on {@code disk}, the one that starts it among them, is corrupted at a chance of
     * {@code share}: taken out of the file system and put aside on the volume.
     */
    private static boolean corruptSystemFiles(final ItemStack disk, final StorageVolume volume, final double share,
                                              final RandomSource random) {
        final List<String> folders = new ArrayList<>();
        final List<String> loaders = new ArrayList<>();
        for (final ResourceLocation id : OsDisks.systemsOn(disk).ids()) {
            final OsDef os = OsRegistry.getOs(id);
            if (os != null) {
                folders.add(SystemIntegrity.folderOf(os));
                loaders.add(SystemIntegrity.loaderOf(os));
            }
        }
        if (folders.isEmpty()) {
            return false;
        }
        FilesystemContents files = disk.getOrDefault(ComputingComponents.FILESYSTEM.get(), FilesystemContents.EMPTY);
        boolean hit = false;
        for (final StoredFile file : new ArrayList<>(files.files().values())) {
            if (isSystemFile(file.path(), folders, loaders) && random.nextDouble() < share) {
                volume.corruptFile(file);
                files = files.without(file.path());
                hit = true;
            }
        }
        if (hit) {
            disk.set(ComputingComponents.FILESYSTEM.get(), files);
        }
        return hit;
    }

    private static boolean isSystemFile(final String path, final List<String> folders, final List<String> loaders) {
        for (final String loader : loaders) {
            if (loader.equalsIgnoreCase(path)) {
                return true;
            }
        }
        for (final String folder : folders) {
            if (!folder.isEmpty() && FsPaths.isUnder(folder, path)) {
                return true;
            }
        }
        return false;
    }
}
