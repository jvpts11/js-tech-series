/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

/**
 * Writes every multiblock pattern a mod declares in code to {@code data/<mod>/multiblock/}, one file each: the file a
 * datapack puts its own in place of to change the structure.
 */
public final class MultiblockPatternProvider implements DataProvider {

    private final PackOutput.PathProvider paths;
    private final String modid;

    public MultiblockPatternProvider(final PackOutput output, final String modid) {
        this.paths = output.createPathProvider(PackOutput.Target.DATA_PACK, "multiblock");
        this.modid = modid;
    }

    @Override
    public String getName() {
        return modid + ":multiblock";
    }

    @Override
    public CompletableFuture<?> run(final CachedOutput cache) {
        final List<CompletableFuture<?>> written = new ArrayList<>();
        for (final Map.Entry<ResourceLocation, MultiblockPattern> pattern
                : MultiblockPatterns.declaredBy(modid).entrySet()) {
            final JsonElement json = MultiblockPatterns.CODEC.encodeStart(JsonOps.INSTANCE, pattern.getValue())
                    .getOrThrow(problem -> new IllegalStateException(pattern.getKey() + ": " + problem));
            written.add(DataProvider.saveStable(cache, json, paths.json(pattern.getKey())));
        }
        return CompletableFuture.allOf(written.toArray(CompletableFuture[]::new));
    }
}
