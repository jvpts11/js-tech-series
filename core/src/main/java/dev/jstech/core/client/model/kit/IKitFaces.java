/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import java.util.function.Function;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** The faces of a model just drawn, as a layer laid over it redraws them ({@link IKitLayer}). */
@FunctionalInterface
public interface IKitFaces {

    /**
     * Draws every face shown this frame again, each into the way {@code type} gives for its texture, skipping a face
     * whose texture it gives none for, at {@code light}.
     */
    void into(Function<ResourceLocation, @Nullable RenderType> type, int light, int overlay);
}
