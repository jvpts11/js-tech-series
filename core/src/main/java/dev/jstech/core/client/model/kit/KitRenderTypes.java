/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * The ways the Core's models are drawn: solid with holes, both sides of every face, as the game draws a mob; or see
 * through, every face sorted back to front as the game sorts what it draws see-through, so a part behind shows
 * through one before it. Each comes with the cracks of a block being broken drawn over it, as the game draws them over
 * a block entity's, or without, for a model that is not its block's shape. They are the game's own shaders, so shader
 * packs draw them as they draw mobs.
 */
public final class KitRenderTypes extends RenderType {

    private static final int BUFFER = 1536;
    private static final Function<ResourceLocation, RenderType> CUTOUT_UNCRACKED = Util.memoize(texture -> create(
            "jscore_kit_cutout", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, BUFFER, false, false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(true)));
    private static final Function<ResourceLocation, RenderType> TRANSLUCENT_UNCRACKED = Util.memoize(texture ->
            create("jscore_kit_translucent", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, BUFFER,
                    false, true, CompositeState.builder()
                            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .createCompositeState(true)));

    /* Never made: the class only reaches the game's render states, which only a render type may. */
    private KitRenderTypes(final String name, final VertexFormat format, final VertexFormat.Mode mode,
                           final int bufferSize, final boolean crumbling, final boolean sort, final Runnable setup,
                           final Runnable clear) {
        super(name, format, mode, bufferSize, crumbling, sort, setup, clear);
    }

    /** Solid with holes, both sides drawn, with or without the cracks of being broken. */
    public static RenderType cutout(final ResourceLocation texture, final boolean cracks) {
        return cracks ? RenderType.entityCutoutNoCull(texture) : CUTOUT_UNCRACKED.apply(texture);
    }

    /** See-through, sorted back to front, with or without the cracks of being broken. */
    public static RenderType translucent(final ResourceLocation texture, final boolean cracks) {
        return cracks ? RenderType.entityTranslucent(texture) : TRANSLUCENT_UNCRACKED.apply(texture);
    }
}
