/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.light;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/**
 * The layer light's glow is drawn in: light added to what lies behind it, as glow does, behind walls hidden, writing
 * no depth so glows over glows add. Coloured corners and no picture, so no shader's cut-off of faint pixels shows an
 * edge where a glow fades out.
 */
public final class LightRenderTypes extends RenderType {

    private static final int BUFFER = 1536;
    /** Glow, light added: halos and the shafts of spotlights. */
    public static final RenderType GLOW = create("jscore_light_glow", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, BUFFER, false, true, CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .createCompositeState(false));

    /* Never made: the class only reaches the game's render states, which only a render type may. */
    private LightRenderTypes() {
        super("jscore_light", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 0, false, false, () -> { },
                () -> { });
    }
}
