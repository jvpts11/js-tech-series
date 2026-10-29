/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.terrain;

import dev.jstech.core.log.CoreLogs;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

/**
 * The maps of round worlds a data pack holds, {@code data/<namespace>/world_maps/<path>.png}, each a grey picture
 * laid as a {@link RoundMap}, known by {@code <namespace>:<path>}: a world's maria, its heights, its young craters.
 * They are read as the server loads its data, before any world is made, so a world's generator reads them while it
 * makes the ground ({@code jscore:round_map}).
 */
public final class RoundMaps {

    private static final Logger LOGGER = CoreLogs.WORLD.logger();
    private static final FileToIdConverter FILES = new FileToIdConverter("world_maps", ".png");
    private static final Map<ResourceLocation, RoundMap> MAPS = new ConcurrentHashMap<>();
    /* A grey value's top, which reads as 1. */
    private static final float WHITE = 255F;

    private RoundMaps() {
    }

    /** Hands the game's bus the reading of the maps, once, from the Core's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(AddReloadListenerEvent.class, event -> event.addListener(new Reader()));
    }

    /** The map known as {@code id}, if a data pack holds it. */
    public static Optional<RoundMap> get(final ResourceLocation id) {
        return Optional.ofNullable(MAPS.get(id));
    }

    /** Reads a picture as a map: its first channel, a grey's value, from 0 to 1. */
    public static RoundMap read(final InputStream in) throws IOException {
        final BufferedImage image = ImageIO.read(in);
        if (image == null) {
            throw new IOException("not a picture the game can read");
        }
        final Raster raster = image.getRaster();
        final int width = image.getWidth();
        final int height = image.getHeight();
        final float[] values = new float[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                values[y * width + x] = raster.getSample(x, y, 0) / WHITE;
            }
        }
        return new RoundMap(width, height, values);
    }

    /* Reads every map the data packs hold, and puts them in force at once. */
    private static final class Reader extends SimplePreparableReloadListener<Map<ResourceLocation, RoundMap>> {

        @Override
        protected Map<ResourceLocation, RoundMap> prepare(final ResourceManager resources,
                                                          final ProfilerFiller profiler) {
            final Map<ResourceLocation, RoundMap> read = new HashMap<>();
            for (final Map.Entry<ResourceLocation, Resource> file : FILES.listMatchingResources(resources).entrySet()) {
                final ResourceLocation id = FILES.fileToId(file.getKey());
                try (InputStream in = file.getValue().open()) {
                    read.put(id, RoundMaps.read(in));
                } catch (final IOException | IllegalArgumentException unreadable) {
                    LOGGER.error("The world map {} cannot be read, and is left out: {}", id, unreadable.getMessage());
                }
            }
            return read;
        }

        @Override
        protected void apply(final Map<ResourceLocation, RoundMap> read, final ResourceManager resources,
                             final ProfilerFiller profiler) {
            MAPS.clear();
            MAPS.putAll(read);
        }
    }
}
