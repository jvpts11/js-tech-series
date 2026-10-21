/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;

/**
 * The files of a tag as the installed mods' own jars carry them, the game's jar and NeoForge's among them, read before
 * the game has read any tag: what is made of other mods' content as the game registers its things is found this way,
 * since a data pack's tags come too late for that. What they hold is read by {@link TagFiles}.
 */
public final class ModTagFiles {

    private ModTagFiles() {
    }

    /** Every id the block tag {@code tag} holds in the mods' jars, its own and those of the tags it names. */
    public static Set<String> blocks(final String tag) {
        return TagFiles.members(tag, name -> filesOf("block", name));
    }

    /** The tags the block tag {@code tag} names in the mods' jars, as {@code c:storage_blocks/tin}. */
    public static Set<String> blockTagsIn(final String tag) {
        return TagFiles.tagsIn(tag, name -> filesOf("block", name));
    }

    /** Whether the item tag {@code tag} holds anything in the mods' jars. */
    public static boolean anyItems(final String tag) {
        return !TagFiles.members(tag, name -> filesOf("item", name)).isEmpty();
    }

    /* The text of every file of {@code tag} of the registry {@code registry} the installed mods carry. */
    private static List<String> filesOf(final String registry, final String tag) {
        final ResourceLocation id = ResourceLocation.tryParse(tag);
        final List<String> out = new ArrayList<>();
        if (id == null) {
            return out;
        }
        for (final IModFileInfo mod : ModList.get().getModFiles()) {
            final Path file = mod.getFile().findResource("data", id.getNamespace(), "tags", registry,
                    id.getPath() + ".json");
            if (Files.isRegularFile(file)) {
                try {
                    out.add(Files.readString(file, StandardCharsets.UTF_8));
                } catch (final IOException unreadable) {
                    // A file that cannot be read gives nothing; the rest still do.
                }
            }
        }
        return out;
    }
}
