/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.effect;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;

/**
 * The layers effects are drawn in: particles from the game's particle sheet laid over what is behind them, or added
 * to it as light; ribbons, beams and arcs of colour laid over or added; and marks on the blocks, drawn just off their
 * faces. None writes depth, so what is see-through overlaps cleanly; walls in front hide them all. Every particle of
 * a layer goes to the graphics card in one batch.
 */
public final class EffectRenderTypes extends RenderType {

    private static final int BUFFER = 1 << 18;
    private static final int SMALL_BUFFER = 1536;
    private static final ShaderStateShard PARTICLE_SHADER = new ShaderStateShard(GameRenderer::getParticleShader);
    /** Particles laid over what is behind them. */
    public static final RenderType PARTICLES = create("jscore_effect_particles", DefaultVertexFormat.PARTICLE,
            VertexFormat.Mode.QUADS, BUFFER, false, false, CompositeState.builder()
                    .setShaderState(PARTICLE_SHADER)
                    .setTextureState(new TextureStateShard(TextureAtlas.LOCATION_PARTICLES, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .createCompositeState(false));
    /** Particles added to what is behind them, as light. */
    public static final RenderType GLOWING = create("jscore_effect_glowing", DefaultVertexFormat.PARTICLE,
            VertexFormat.Mode.QUADS, BUFFER, false, false, CompositeState.builder()
                    .setShaderState(PARTICLE_SHADER)
                    .setTextureState(new TextureStateShard(TextureAtlas.LOCATION_PARTICLES, false, false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .createCompositeState(false));
    /** Ribbons of colour laid over what is behind them: trails of smoke. */
    public static final RenderType RIBBONS = create("jscore_effect_ribbons", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, SMALL_BUFFER, false, false, CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .createCompositeState(false));
    /** Marks on the blocks, just off their faces. */
    public static final RenderType MARKS = create("jscore_effect_marks", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, SMALL_BUFFER, false, false, CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .createCompositeState(false));

    /* Never made: the class only reaches the game's render states, which only a render type may. */
    private EffectRenderTypes() {
        super("jscore_effect", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 0, false, false, () -> { },
                () -> { });
    }
}
