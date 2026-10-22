/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide.markdown;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;

/**
 * Every manual's Markdown file the installed mods carry in their own jars, read where no resource pack is loaded, on a
 * server and in its tests: a player's game reads them through its resource packs instead, so a pack can rewrite one.
 */
public final class ModManualFiles {

    private static final String ASSETS = "assets";

    private ModManualFiles() {
    }

    /** Every manual's file of every installed mod, in every language. */
    public static List<ManualFile> all() {
        final List<ManualFile> files = new ArrayList<>();
        for (final IModFileInfo mod : ModList.get().getModFiles()) {
            final Path assets = mod.getFile().findResource(ASSETS);
            if (!Files.isDirectory(assets)) {
                continue;
            }
            for (final Path namespace : listed(assets)) {
                final Path root = namespace.resolve(ManualFile.ROOT);
                if (Files.isDirectory(root)) {
                    gather(namespace.getFileName().toString().replace("/", ""), namespace, root, files);
                }
            }
        }
        return files;
    }

    /* Every Markdown file under {@code folder}, each read into {@code into}. */
    private static void gather(final String namespace, final Path under, final Path folder,
                               final List<ManualFile> into) {
        for (final Path child : listed(folder)) {
            if (Files.isDirectory(child)) {
                gather(namespace, under, child, into);
            } else if (child.getFileName().toString().endsWith(ManualFile.EXTENSION)) {
                final String location = under.relativize(child).toString().replace('\\', '/');
                try {
                    ManualFile.at(namespace, location, Files.readString(child, StandardCharsets.UTF_8))
                            .ifPresent(into::add);
                } catch (final IOException unread) {
                    // A file that cannot be read is left out; the rest of the manuals still read.
                }
            }
        }
    }

    /* What a folder holds, listed one level down, which every file system of a mod's jar allows. */
    private static List<Path> listed(final Path folder) {
        try (Stream<Path> children = Files.list(folder)) {
            return children.sorted().toList();
        } catch (final IOException unlisted) {
            return List.of();
        }
    }
}
