/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.effect;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.effect.EffectDefinition;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.slf4j.Logger;

/**
 * The effects the resource packs hold, read as the game's resources load: every {@code effects/<path>.json} of every
 * namespace, the effect {@code <namespace>:<path>}. One that does not read is left out, the log saying why. A player's
 * game alone reads them; the server names effects and leaves their look to the files.
 */
public final class EffectFiles extends SimplePreparableReloadListener<Map<ResourceLocation, EffectDefinition>> {

    /** The folder effects are kept in. */
    public static final String FOLDER = "effects";
    private static final String ENDING = ".json";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final EffectFiles INSTANCE = new EffectFiles();

    private static volatile Map<ResourceLocation, EffectDefinition> effects = Map.of();

    private EffectFiles() {
    }

    /** Hands the mod's bus the reading of the files, once, from the Core's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterClientReloadListenersEvent.class, event -> event.registerReloadListener(INSTANCE));
    }

    /** The effect {@code id}, if a file holds it. */
    public static Optional<EffectDefinition> get(final ResourceLocation id) {
        return Optional.ofNullable(effects.get(id));
    }

    /** Every effect the files hold, by id. */
    public static Set<ResourceLocation> ids() {
        return effects.keySet();
    }

    /** Has the effect {@code id} be {@code definition} until the files are read again, as a saved edit is. */
    public static void put(final ResourceLocation id, final EffectDefinition definition) {
        final Map<ResourceLocation, EffectDefinition> changed = new HashMap<>(effects);
        changed.put(id, definition);
        effects = Map.copyOf(changed);
    }

    /** The file an effect is kept in, under {@code assets/<namespace>/}. */
    public static String path(final ResourceLocation id) {
        return FOLDER + "/" + id.getPath() + ENDING;
    }

    @Override
    protected Map<ResourceLocation, EffectDefinition> prepare(final ResourceManager manager,
                                                              final ProfilerFiller profiler) {
        final Map<ResourceLocation, EffectDefinition> read = new HashMap<>();
        for (final Map.Entry<ResourceLocation, Resource> file : manager.listResources(FOLDER,
                path -> path.getPath().endsWith(ENDING)).entrySet()) {
            final ResourceLocation location = file.getKey();
            final String path = location.getPath();
            final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(location.getNamespace(),
                    path.substring(FOLDER.length() + 1, path.length() - ENDING.length()));
            try (Reader reader = file.getValue().openAsReader()) {
                final JsonElement json = JsonParser.parseReader(reader);
                EffectDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(problem -> LOGGER.warn("The effect {} is left out: {}", id, problem))
                        .ifPresent(definition -> read.put(id, definition));
            } catch (final IOException | RuntimeException broken) {
                LOGGER.warn("The effect {} is left out: {}", id, broken.getMessage());
            }
        }
        return read;
    }

    @Override
    protected void apply(final Map<ResourceLocation, EffectDefinition> read, final ResourceManager manager,
                         final ProfilerFiller profiler) {
        effects = Map.copyOf(read);
        EffectEngine.filesChanged();
    }
}
