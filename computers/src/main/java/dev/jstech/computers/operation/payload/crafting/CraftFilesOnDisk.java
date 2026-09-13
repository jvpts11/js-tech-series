/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.crafting;

import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.os.fs.CraftFile;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import static dev.jstech.computers.operation.payload.files.FileAccess.filesystemKindOf;

/**
 * The pattern files a Crafting Computer keeps in the crafts folder of its system disk.
 */
public final class CraftFilesOnDisk {

    private CraftFilesOnDisk() {
    }

    /** The folder on a Crafting Computer's system disk that mirrors its loaded {@code .craft} files. */
    private static final String CRAFTS_DIR = "crafts";

    /**
     * Mirrors a {@code .craft} onto the Crafting Computer's system disk under {@code crafts/} so the
     * loaded recipes are visible (and copyable) in the Files app. A flat filesystem keeps them at the
     * root; a hierarchical one nests them in {@code crafts/}. A no-op when there is no system disk.
     */
    static void writeCraftToDisk(final CraftingComputerBlockEntity cc, final String fileName,
                                         final String content) {
        final net.minecraft.world.item.ItemStack disk = cc.systemDisk();
        if (disk.isEmpty()) {
            return;
        }
        final dev.jstech.computers.os.FilesystemKind kind = filesystemKindOf(cc);
        if (kind == dev.jstech.computers.os.FilesystemKind.NONE) {
            return;
        }
        final String path;
        if (kind == dev.jstech.computers.os.FilesystemKind.HIERARCHICAL) {
            DiskFilesystem.mkdir(disk, CRAFTS_DIR, kind);
            path = CRAFTS_DIR + "/" + fileName;
        } else {
            path = fileName;
        }
        DiskFilesystem.write(disk, path, FileType.CRAFT, content, cc.systemDiskFreeWeight(), kind,
                cc.getLevel() == null ? 0L : cc.getLevel().getGameTime());
    }

    /** Deletes a mirrored {@code .craft} from the Crafting Computer's system disk, if present. */
    static void deleteCraftFromDisk(final CraftingComputerBlockEntity cc, final String fileName) {
        final net.minecraft.world.item.ItemStack disk = cc.systemDisk();
        if (disk.isEmpty()) {
            return;
        }
        final dev.jstech.computers.os.FilesystemKind kind = filesystemKindOf(cc);
        if (kind == dev.jstech.computers.os.FilesystemKind.NONE) {
            return;
        }
        final String path = kind == dev.jstech.computers.os.FilesystemKind.HIERARCHICAL
                ? CRAFTS_DIR + "/" + fileName : fileName;
        DiskFilesystem.delete(disk, path);
    }

    /**
     * Reconciles the {@code crafts/} folder on the system disk against the Recipe ROM: writes a
     * {@code .craft} for any ROM pattern missing its mirror file. This self-heals a disk whose ROM
     * predates the mirror, so opening the Crafting Manager always presents a consistent view.
     */
    public static void reconcileCraftsFolder(final CraftingComputerBlockEntity cc, final ServerLevel level) {
        if (cc.systemDisk().isEmpty()) {
            return;
        }
        boolean wrote = false;
        for (final CraftingPattern pattern : cc.romPatterns()) {
            final String diskName = craftFileNameFor(pattern) + ".craft";
            if (craftFileExistsOnDisk(cc, diskName)) {
                continue;
            }
            final java.util.Optional<String> content = CraftFile.serialize(pattern, level.registryAccess());
            if (content.isPresent()) {
                writeCraftToDisk(cc, diskName, content.get());
                wrote = true;
            }
        }
        if (wrote) {
            cc.setChanged();
        }
    }

    /** Reports whether a mirrored {@code .craft} of the given name already exists on the system disk. */
    static boolean craftFileExistsOnDisk(final CraftingComputerBlockEntity cc, final String fileName) {
        final net.minecraft.world.item.ItemStack disk = cc.systemDisk();
        if (disk.isEmpty()) {
            return false;
        }
        final dev.jstech.computers.os.FilesystemKind kind = filesystemKindOf(cc);
        if (kind == dev.jstech.computers.os.FilesystemKind.NONE) {
            return false;
        }
        final String path = kind == dev.jstech.computers.os.FilesystemKind.HIERARCHICAL
                ? CRAFTS_DIR + "/" + fileName : fileName;
        return DiskFilesystem.read(disk, path).isPresent();
    }

    /**
     * The file base-name a bench pattern goes by: the name its author gave it, sanitized, or its result's
     * registry path when it has none (what every pattern was called before names existed).
     */
    static String craftFileNameFor(final CraftingPattern pattern) {
        return pattern.name().isEmpty() ? craftFileNameFor(pattern.result()) : sanitizeFileBase(pattern.name());
    }

    /** Derives a safe file base-name from the result {@link ItemStack}'s registry path. */
    static String craftFileNameFor(final ItemStack result) {
        final net.minecraft.resources.ResourceLocation key =
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem());
        return sanitizeFileBase(key == null ? "pattern" : key.getPath());
    }

    /** Clamps a display or registry name to a safe file base (letters/digits/underscore, max 32 chars). */
    public static String sanitizeFileBase(final String base) {
        final StringBuilder sb = new StringBuilder();
        for (final char c : base.toLowerCase(java.util.Locale.ROOT).toCharArray()) {
            sb.append(Character.isLetterOrDigit(c) || c == '_' ? c : '_');
            if (sb.length() >= 32) {
                break;
            }
        }
        return sb.isEmpty() ? "pattern" : sb.toString();
    }
}
