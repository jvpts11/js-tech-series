/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.gui.screen.ScreenFile;
import dev.jstech.core.log.CoreLogs;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

/**
 * The screens declared in files, read from the resource packs as they load, {@code
 * assets/<namespace>/jstech/screens/*.json}. A file that cannot be read keeps what it says wrong, which its screen
 * shows in place of itself, so a slip while editing one is seen where the screen is. When the files are read again (a
 * resource pack changed, or a developer's game saw one edited) the open screen is built again from its new file.
 */
public final class ScreenFiles extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().create();
    private static final Logger LOGGER = CoreLogs.INTERFACE.logger();
    private static final ScreenFiles INSTANCE = new ScreenFiles();
    private static final List<Runnable> RELOADED = new CopyOnWriteArrayList<>();
    private static volatile Map<ResourceLocation, ScreenFile> files = Map.of();
    private static volatile Map<ResourceLocation, String> problems = Map.of();

    private ScreenFiles() {
        super(GSON, ScreenFile.FOLDER);
    }

    /** Hands the game the reader, once, from the Core's client constructor. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterClientReloadListenersEvent.class, event -> event.registerReloadListener(INSTANCE));
    }

    /** The screen {@code id} as its file declares it, or none when no pack has it or it cannot be read. */
    public static Optional<ScreenFile> get(final ResourceLocation id) {
        return Optional.ofNullable(files.get(id));
    }

    /** What is wrong with the file of {@code id}, or none. */
    public static Optional<String> problem(final ResourceLocation id) {
        return Optional.ofNullable(problems.get(id));
    }

    /** Every screen read. */
    public static Map<ResourceLocation, ScreenFile> all() {
        return files;
    }

    /** Runs {@code action} each time the files are read again. */
    public static void onReload(final Runnable action) {
        RELOADED.add(action);
    }

    @Override
    protected void apply(final Map<ResourceLocation, JsonElement> read, final ResourceManager resources,
                         final ProfilerFiller profiler) {
        final Map<ResourceLocation, ScreenFile> parsed = new LinkedHashMap<>();
        final Map<ResourceLocation, String> wrong = new LinkedHashMap<>();
        read.forEach((id, json) -> ScreenFile.CODEC.parse(JsonOps.INSTANCE, json).ifSuccess(file -> parsed.put(id,
                file)).ifError(error -> {
                    wrong.put(id, error.message());
                    LOGGER.warn("the screen {} cannot be read: {}", id, error.message());
                }));
        files = Map.copyOf(parsed);
        problems = Map.copyOf(wrong);
        RELOADED.forEach(Runnable::run);
        // The open screen built from a file is built again from what the file says now.
        if (Minecraft.getInstance().screen instanceof IDataScreen open) {
            open.rebuild();
        }
    }
}
