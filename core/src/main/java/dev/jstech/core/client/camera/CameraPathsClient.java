/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.camera;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.JsCore;
import dev.jstech.core.camera.CameraPath;
import dev.jstech.core.camera.CameraPathCodecs;
import dev.jstech.core.camera.CameraPaths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * The camera paths on a player's game: those declared in code, with the files of the resource packs under
 * {@code assets/<namespace>/jstech/camera_paths/} over them, read again when the packs are.
 */
public final class CameraPathsClient extends SimpleJsonResourceReloadListener {

    private static final String FOLDER = "jstech/camera_paths";
    private static volatile Map<ResourceLocation, CameraPath> files = Map.of();

    CameraPathsClient() {
        super(new Gson(), FOLDER);
    }

    /** The path {@code id}: a resource pack's file of it, else its declaration, else none. */
    public static Optional<CameraPath> get(final ResourceLocation id) {
        final CameraPath file = files.get(id);
        return file != null ? Optional.of(file) : Optional.ofNullable(CameraPaths.declared().get(id));
    }

    @Override
    protected void apply(final Map<ResourceLocation, JsonElement> read, final ResourceManager manager,
                         final ProfilerFiller profiler) {
        final Map<ResourceLocation, CameraPath> paths = new HashMap<>();
        for (final Map.Entry<ResourceLocation, JsonElement> file : read.entrySet()) {
            final DataResult<CameraPath> path = CameraPathCodecs.PATH.parse(JsonOps.INSTANCE, file.getValue());
            path.resultOrPartial(problem -> JsCore.LOGGER.warn("The camera path {} is left out: {}", file.getKey(),
                    problem)).ifPresent(found -> paths.put(file.getKey(), found));
        }
        files = Map.copyOf(paths);
    }
}
