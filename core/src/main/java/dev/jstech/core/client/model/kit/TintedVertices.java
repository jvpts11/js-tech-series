/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.FastColor;

/**
 * Vertices passed on with their colour tinted, each channel multiplied by the tint's, its alpha too: a model drawn
 * through it is coloured as a hologram is ({@link KitRenderer#drawTinted}).
 */
final class TintedVertices implements VertexConsumer {

    private final VertexConsumer into;
    private final int red;
    private final int green;
    private final int blue;
    private final int alpha;

    private static final int FULL = 255;

    TintedVertices(final VertexConsumer into, final int argb) {
        this.into = into;
        this.red = FastColor.ARGB32.red(argb);
        this.green = FastColor.ARGB32.green(argb);
        this.blue = FastColor.ARGB32.blue(argb);
        this.alpha = FastColor.ARGB32.alpha(argb);
    }

    @Override
    public VertexConsumer addVertex(final float x, final float y, final float z) {
        this.into.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(final int r, final int g, final int b, final int a) {
        this.into.setColor(r * this.red / FULL, g * this.green / FULL, b * this.blue / FULL, a * this.alpha / FULL);
        return this;
    }

    @Override
    public VertexConsumer setUv(final float u, final float v) {
        this.into.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(final int u, final int v) {
        this.into.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(final int u, final int v) {
        this.into.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(final float x, final float y, final float z) {
        this.into.setNormal(x, y, z);
        return this;
    }
}
