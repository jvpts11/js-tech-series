/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.camera;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

/**
 * The camera paths mods declare in code, by id. A resource pack's file under
 * {@code assets/<namespace>/jstech/camera_paths/} wins over a declaration of its id, so a pack re-shoots a scene with
 * no code; the server names a path only by its id, and each player's game plays it as its own files have it.
 */
public final class CameraPaths {

    private static final Map<ResourceLocation, CameraPath> DECLARED = new ConcurrentHashMap<>();

    private CameraPaths() {
    }

    /** Declares {@code path} as {@code id}, as it is while no resource pack says otherwise; gives the id. */
    public static ResourceLocation declare(final ResourceLocation id, final CameraPath path) {
        DECLARED.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(path, "path"));
        return id;
    }

    /** Every path declared in code, by id. */
    public static Map<ResourceLocation, CameraPath> declared() {
        return Collections.unmodifiableMap(DECLARED);
    }
}
