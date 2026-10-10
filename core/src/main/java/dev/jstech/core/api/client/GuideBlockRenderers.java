/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.api.client;

import com.mojang.logging.LogUtils;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Where a mod says what draws its kinds of special block in the manuals, once, from its client setup:
 *
 * <pre>{@code
 * GuideBlockRenderers.register(ResourceLocation.fromNamespaceAndPath("myaddon", "structure"), new StructureRenderer());
 * }</pre>
 *
 * <p>A kind is registered once: a second renderer for a kind already taken is refused, since which of the two drew it
 * would otherwise depend on the order the mods loaded in.
 */
@ApiStatus.Experimental
public final class GuideBlockRenderers {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<ResourceLocation, IGuideBlockRenderer> RENDERERS = new ConcurrentHashMap<>();

    private GuideBlockRenderers() {
    }

    /**
     * Says what draws a kind of special block.
     *
     * @return whether it was taken; false when the kind already has a renderer
     */
    public static boolean register(final ResourceLocation kind, final IGuideBlockRenderer renderer) {
        final boolean taken = RENDERERS.putIfAbsent(kind, renderer) == null;
        if (!taken) {
            LOGGER.warn("Guide block kind {} already has a renderer; the second one is refused", kind);
        }
        return taken;
    }

    /** What draws that kind, or null when no mod registered one. */
    @Nullable
    public static IGuideBlockRenderer get(final ResourceLocation kind) {
        return RENDERERS.get(kind);
    }
}
