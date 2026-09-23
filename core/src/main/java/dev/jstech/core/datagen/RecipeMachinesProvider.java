/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.content.RecipeMachineFiles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

/**
 * Writes which of a mod's declared blocks run which recipe types, the file {@link RecipeMachineFiles} describes.
 * A mod that declares no machine writes nothing.
 */
public final class RecipeMachinesProvider implements DataProvider {

    private final PackOutput output;
    private final ModContent content;

    public RecipeMachinesProvider(final PackOutput output, final ModContent content) {
        this.output = output;
        this.content = content;
    }

    @Override
    public String getName() {
        return content.modid() + ":" + RecipeMachineFiles.FOLDER;
    }

    @Override
    public CompletableFuture<?> run(final CachedOutput cache) {
        // Sorted by recipe type, so the file reads the same on every run; machines keep their declaration order.
        final Map<String, List<String>> machines = new TreeMap<>();
        for (final BlockEntry<?> entry : content.declaredBlocks()) {
            for (final ResourceLocation type : entry.recipeTypes()) {
                machines.computeIfAbsent(type.toString(), key -> new ArrayList<>()).add(entry.getId().toString());
            }
        }
        if (machines.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        final JsonObject root = new JsonObject();
        machines.forEach((type, blocks) -> {
            final JsonArray array = new JsonArray();
            blocks.forEach(array::add);
            root.add(type, array);
        });
        return DataProvider.saveStable(cache, root, output.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve(content.modid()).resolve(RecipeMachineFiles.FOLDER).resolve("machines.json"));
    }
}
