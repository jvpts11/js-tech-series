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
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.CraftFile;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import static dev.jstech.computers.operation.payload.files.FileAccess.filesystemKindOf;

/**
 * The pattern files a Crafting Computer keeps in the crafts folder of its system disk.
 */
public final class CraftFilesOnDisk {

    /** The folder on a Crafting Computer's system disk that mirrors its loaded {@code .craft} files. */
    private static final String CRAFTS_DIR = "crafts";

    /** How many numbered variants of one base name are searched when looking for a recipe's mirror. */
    private static final int MAX_MIRROR_VARIANTS = 64;

    private CraftFilesOnDisk() {
    }

    /**
     * Mirrors a {@code .craft} onto the Crafting Computer's system disk under {@code crafts/} so the
     * loaded recipes are visible (and copyable) in the Files app. A flat filesystem keeps them at the
     * root; a hierarchical one nests them in {@code crafts/}. A recipe whose content is already mirrored
     * stays where it is; otherwise it takes {@code base.craft}, or {@code base_2.craft}, ... when that name
     * holds a different recipe, so two recipes for the same result never share a file. A no-op when there is
     * no system disk.
     */
    static void writeCraftToDisk(final CraftingComputerBlockEntity cc, final String base, final String content) {
        if (mirrorPath(cc, base + ".craft") == null) {
            return;
        }
        final ItemStack disk = cc.systemDisk();
        final FilesystemKind kind = filesystemKindOf(cc);
        if (kind == FilesystemKind.HIERARCHICAL) {
            DiskFilesystem.mkdir(disk, CRAFTS_DIR, kind);
        }
        if (findMirror(cc, base, content) != null) {
            return;
        }
        DiskFilesystem.write(disk, firstFreeMirror(cc, base), FileType.CRAFT, content, cc.systemDiskFreeWeight(),
                kind, cc.getLevel() == null ? 0L : cc.getLevel().getGameTime());
    }

    /**
     * Deletes the mirror of one recipe from the Crafting Computer's system disk, if present. Only the file that
     * holds exactly this recipe goes; a same-named file of another recipe stays.
     */
    static void deleteCraftFromDisk(final CraftingComputerBlockEntity cc, final String base, final String content) {
        final String path = findMirror(cc, base, content);
        if (path != null) {
            DiskFilesystem.delete(cc.systemDisk(), path);
        }
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
            final Optional<String> content = CraftFile.serialize(pattern, level.registryAccess());
            if (content.isEmpty()) {
                continue;
            }
            final String base = craftFileNameFor(pattern);
            if (findMirror(cc, base, content.get()) == null) {
                writeCraftToDisk(cc, base, content.get());
                wrote = true;
            }
        }
        if (wrote) {
            cc.setChanged();
        }
    }

    /**
     * The file base-name a recipe is mirrored and saved under: a bench recipe by its pattern's name, any other by
     * its sanitized display name.
     */
    static String mirrorFileName(final NetworkRecipe recipe) {
        return recipe.bench().isPresent() ? craftFileNameFor(recipe.bench().get())
                : sanitizeFileBase(recipe.displayName());
    }

    /* The mirror path holding exactly this content under the base name or its numbered variants, or null. */
    @Nullable
    private static String findMirror(final CraftingComputerBlockEntity cc, final String base, final String content) {
        for (int n = 1; n <= MAX_MIRROR_VARIANTS; n++) {
            final String path = mirrorPath(cc, variantName(base, n));
            if (path != null && DiskFilesystem.read(cc.systemDisk(), path).map(content::equals).orElse(false)) {
                return path;
            }
        }
        return null;
    }

    /* The first mirror path under the base name or its numbered variants that no file occupies. */
    private static String firstFreeMirror(final CraftingComputerBlockEntity cc, final String base) {
        String path = mirrorPath(cc, variantName(base, 1));
        for (int n = 2; n <= MAX_MIRROR_VARIANTS && DiskFilesystem.exists(cc.systemDisk(), path); n++) {
            path = mirrorPath(cc, variantName(base, n));
        }
        return path;
    }

    private static String variantName(final String base, final int n) {
        return (n == 1 ? base : base + "_" + n) + ".craft";
    }

    /* Where a mirrored file lives on the system disk, or null when there is no disk or no filesystem on it. */
    @Nullable
    private static String mirrorPath(final CraftingComputerBlockEntity cc, final String fileName) {
        if (cc.systemDisk().isEmpty()) {
            return null;
        }
        final FilesystemKind kind = filesystemKindOf(cc);
        if (kind == FilesystemKind.NONE) {
            return null;
        }
        return kind == FilesystemKind.HIERARCHICAL ? CRAFTS_DIR + "/" + fileName : fileName;
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
        final ResourceLocation key =
                BuiltInRegistries.ITEM.getKey(result.getItem());
        return sanitizeFileBase(key == null ? "pattern" : key.getPath());
    }

    /** Clamps a display or registry name to a safe file base (letters/digits/underscore, max 32 chars). */
    public static String sanitizeFileBase(final String base) {
        final StringBuilder sb = new StringBuilder();
        for (final char c : base.toLowerCase(Locale.ROOT).toCharArray()) {
            sb.append(Character.isLetterOrDigit(c) || c == '_' ? c : '_');
            if (sb.length() >= 32) {
                break;
            }
        }
        return sb.isEmpty() ? "pattern" : sb.toString();
    }
}
