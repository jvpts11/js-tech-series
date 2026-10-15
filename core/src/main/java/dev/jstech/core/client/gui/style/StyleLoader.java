/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.style;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.gui.style.StyleFile;
import dev.jstech.core.log.CoreLogs;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

/**
 * Reads the styles from the resource packs as they load, {@code assets/<namespace>/jstech/styles/*.json}: a pack higher
 * in the list replaces a style of the same name, and a style it adds joins the others.
 */
public final class StyleLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().create();
    private static final Logger LOGGER = CoreLogs.INTERFACE.logger();
    private static final StyleLoader INSTANCE = new StyleLoader();

    private StyleLoader() {
        super(GSON, StyleFile.FOLDER);
    }

    /** Hands the game the reader, once, from the Core's client constructor. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterClientReloadListenersEvent.class, event -> event.registerReloadListener(INSTANCE));
    }

    @Override
    protected void apply(final Map<ResourceLocation, JsonElement> read, final ResourceManager resources,
                         final ProfilerFiller profiler) {
        final Map<ResourceLocation, StyleFile> files = new LinkedHashMap<>();
        read.forEach((id, json) -> StyleFile.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(problem -> LOGGER.warn("the style {} cannot be read: {}", id, problem))
                .ifPresent(file -> files.put(id, file)));
        Styles.load(files);
    }
}
