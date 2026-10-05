/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.guide.GuideCodecs;
import dev.jstech.core.guide.GuideEntry;
import dev.jstech.core.guide.GuideIds;
import dev.jstech.core.guide.GuideManual;
import dev.jstech.core.guide.GuideSection;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.ModGuide;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

/**
 * Writes what a mod declared for the manuals to {@code assets/<mod>/guide/}: its chapter, its sections and entries,
 * and its manuals and styles, one file each. These are the files a manual is read from, so a resource pack can
 * replace any of them, and a pack maker can write a manual of their own the same way with no code at all.
 */
public final class GuideProvider implements DataProvider {

    private final PackOutput output;
    private final ModGuide guide;

    public GuideProvider(final PackOutput output, final ModGuide guide) {
        this.output = output;
        this.guide = guide;
    }

    @Override
    public String getName() {
        return this.guide.namespace() + ":guide";
    }

    @Override
    public CompletableFuture<?> run(final CachedOutput cache) {
        final List<CompletableFuture<?>> written = new ArrayList<>();
        final String namespace = this.guide.namespace();
        if (this.guide.declaredChapter() != null) {
            written.add(this.save(cache, GuideCodecs.CHAPTER, this.guide.declaredChapter(), "guide",
                    GuideIds.of(namespace, "chapter")));
        }
        for (final GuideSection section : this.guide.declaredSections()) {
            written.add(this.save(cache, GuideCodecs.SECTION, section, "guide/sections", section.id()));
        }
        for (final GuideEntry entry : this.guide.declaredEntries()) {
            written.add(this.save(cache, GuideCodecs.ENTRY, entry, "guide/entries", entry.id()));
        }
        for (final GuideManual manual : this.guide.declaredManuals()) {
            written.add(this.save(cache, GuideCodecs.MANUAL, manual, "guide/manuals", manual.id()));
        }
        for (final Map.Entry<String, GuideStyle> style : this.guide.declaredStyles().entrySet()) {
            written.add(this.save(cache, GuideCodecs.STYLE, style.getValue(), "guide/styles",
                    GuideIds.of(namespace, style.getKey())));
        }
        return CompletableFuture.allOf(written.toArray(CompletableFuture[]::new));
    }

    private <T> CompletableFuture<?> save(final CachedOutput cache, final Codec<T> codec, final T value,
                                          final String folder, final String id) {
        final JsonElement json = codec.encodeStart(JsonOps.INSTANCE, value)
                .getOrThrow(problem -> new IllegalStateException(id + ": " + problem));
        final PackOutput.PathProvider paths = this.output.createPathProvider(PackOutput.Target.RESOURCE_PACK, folder);
        return DataProvider.saveStable(cache, json, paths.json(ResourceLocation.parse(id)));
    }
}
