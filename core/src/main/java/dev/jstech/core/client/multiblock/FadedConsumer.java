/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.multiblock;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Vertices passed on as they come, their alpha made {@code alpha} of what it was: a ghost, pale and see-through. */
public final class FadedConsumer implements VertexConsumer {

    private final VertexConsumer inner;
    private final float alpha;

    public FadedConsumer(final VertexConsumer inner, final float alpha) {
        this.inner = inner;
        this.alpha = alpha;
    }

    @Override
    public VertexConsumer addVertex(final float x, final float y, final float z) {
        this.inner.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(final int red, final int green, final int blue, final int alpha) {
        this.inner.setColor(red, green, blue, Math.round(alpha * this.alpha));
        return this;
    }

    @Override
    public VertexConsumer setUv(final float u, final float v) {
        this.inner.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(final int u, final int v) {
        this.inner.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(final int u, final int v) {
        this.inner.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(final float x, final float y, final float z) {
        this.inner.setNormal(x, y, z);
        return this;
    }
}
