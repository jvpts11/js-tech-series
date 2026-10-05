/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.GuideCodecs;
import dev.jstech.core.guide.GuideEntry;
import dev.jstech.core.guide.GuideIds;
import dev.jstech.core.guide.GuideManual;
import dev.jstech.core.guide.GuideSection;
import dev.jstech.core.guide.GuideStyle;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

/**
 * Reads every manual from the loaded resource packs, under each namespace's {@code guide/}: {@code chapter.json},
 * and the folders {@code sections}, {@code entries}, {@code manuals} and {@code styles}. The top-most pack's file wins,
 * as for any resource; a file that cannot be read is left out with a line in the log, and the rest of the manuals
 * still open.
 */
public final class GuideReloadListener extends SimplePreparableReloadListener<GuideLibrary> {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ROOT = "guide";

    @Override
    protected GuideLibrary prepare(final ResourceManager resources, final ProfilerFiller profiler) {
        final Map<String, GuideChapter> chapters = new HashMap<>();
        for (final Map.Entry<ResourceLocation, Resource> file : resources.listResources(ROOT,
                path -> path.getPath().equals(ROOT + "/chapter.json")).entrySet()) {
            read(file, GuideCodecs.CHAPTER).ifPresent(chapter -> chapters.put(chapter.namespace(), chapter));
        }
        return new GuideLibrary(chapters,
                readAll(resources, "sections", GuideCodecs.SECTION, GuideSection::id),
                readAll(resources, "entries", GuideCodecs.ENTRY, GuideEntry::id),
                readAll(resources, "manuals", GuideCodecs.MANUAL, GuideManual::id),
                readStyles(resources));
    }

    @Override
    protected void apply(final GuideLibrary library, final ResourceManager resources, final ProfilerFiller profiler) {
        GuideLibrary.use(library);
    }

    private static <T> Map<String, T> readAll(final ResourceManager resources, final String folder,
                                              final Codec<T> codec, final Function<T, String> id) {
        final Map<String, T> read = new HashMap<>();
        for (final Map.Entry<ResourceLocation, Resource> file : resources.listResources(ROOT + "/" + folder,
                path -> path.getPath().endsWith(".json")).entrySet()) {
            read(file, codec).ifPresent(value -> read.put(id.apply(value), value));
        }
        return read;
    }

    /** Styles are named by their file, since a style says nothing of its own id. */
    private static Map<String, GuideStyle> readStyles(final ResourceManager resources) {
        final Map<String, GuideStyle> read = new HashMap<>();
        final String folder = ROOT + "/styles/";
        for (final Map.Entry<ResourceLocation, Resource> file : resources.listResources(ROOT + "/styles",
                path -> path.getPath().endsWith(".json")).entrySet()) {
            final ResourceLocation at = file.getKey();
            final String path = at.getPath().substring(folder.length(), at.getPath().length() - ".json".length());
            read(file, GuideCodecs.STYLE).ifPresent(style -> read.put(GuideIds.of(at.getNamespace(), path), style));
        }
        return read;
    }

    private static <T> Optional<T> read(final Map.Entry<ResourceLocation, Resource> file, final Codec<T> codec) {
        try (Reader reader = file.getValue().openAsReader()) {
            final JsonElement json = JsonParser.parseReader(reader);
            return codec.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(problem -> LOGGER.warn("The manual file {} could not be read: {}", file.getKey(),
                            problem));
        } catch (final IOException | RuntimeException e) {
            LOGGER.warn("The manual file {} could not be read", file.getKey(), e);
            return Optional.empty();
        }
    }
}
