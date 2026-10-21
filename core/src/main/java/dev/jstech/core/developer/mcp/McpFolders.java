/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import dev.jstech.core.developer.DevSources;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jetbrains.annotations.Nullable;

/**
 * The folders an agent's files are kept to: each mod's own resources in its source, in a development run
 * ({@code mod:<id>}, and {@code generated:<id>} beside it), the world's data packs, and, on the player's game, its
 * resource packs and KubeJS's folder.
 */
public final class McpFolders {

    private McpFolders() {
    }

    /** The folders, with the world's data packs when {@code server} runs one and the game's when given its folder. */
    public static List<McpFiles.Folder> of(@Nullable final MinecraftServer server, @Nullable final Path game) {
        final List<McpFiles.Folder> out = new ArrayList<>();
        for (final IModInfo mod : ModList.get().getMods()) {
            final Optional<Path> resources = DevSources.resourcesOf(mod.getModId());
            if (resources.isEmpty()) {
                continue;
            }
            out.add(new McpFiles.Folder("mod:" + mod.getModId(), resources.get()));
            // The data generator writes beside the hand-written resources: <project>/src/generated/resources.
            final Path generated = resources.get().getParent().getParent().resolve("generated").resolve("resources");
            if (Files.isDirectory(generated)) {
                out.add(new McpFiles.Folder("generated:" + mod.getModId(), generated));
            }
        }
        if (server != null) {
            out.add(new McpFiles.Folder("datapacks", server.getWorldPath(LevelResource.DATAPACK_DIR)));
        }
        if (game != null) {
            for (final String name : List.of("resourcepacks", "kubejs")) {
                final Path folder = game.resolve(name);
                if (Files.isDirectory(folder)) {
                    out.add(new McpFiles.Folder(name, folder));
                }
            }
        }
        return out;
    }
}
