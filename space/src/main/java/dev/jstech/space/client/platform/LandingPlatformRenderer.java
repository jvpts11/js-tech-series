/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.space.JsSpace;
import dev.jstech.space.platform.LandingPlatform;
import dev.jstech.space.platform.LandingPlatformBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Paints a formed landing platform's target across its pads: the yellow ring and the white cross a stage aims its
 * engine at, with the hazard stripes round the edge, drawn as one picture over the whole slab, as large as the platform
 * stands. Nothing is painted while it is not formed.
 */
public final class LandingPlatformRenderer implements BlockEntityRenderer<LandingPlatformBlockEntity> {

    /* The target of the platforms of nine and of fifteen blocks, each sixteen pixels a block. */
    private static final ResourceLocation SMALL = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "textures/block/landing_platform/marking_9.png");
    private static final ResourceLocation LARGE = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "textures/block/landing_platform/marking_15.png");
    private static final int LARGE_SIDE = 15;
    /* Just above the pads' tops, so the paint never flickers with them. */
    private static final float PAINT = 1.002F;
    private static final float MIDDLE = 0.5F;
    /* How far it is drawn from, in blocks: as far as a stage coming down is seen. */
    private static final int SEEN_FROM = 160;

    public LandingPlatformRenderer(final BlockEntityRendererProvider.Context context) {
        // Nothing to keep: the picture is drawn as it is.
    }

    /** Hands the mod's bus the renderer, once, from the mod's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event ->
                event.registerBlockEntityRenderer(LandingPlatform.ENTITY.get(), LandingPlatformRenderer::new));
    }

    @Override
    public void render(final LandingPlatformBlockEntity platform, final float partialTick, final PoseStack pose,
                       final MultiBufferSource buffers, final int light, final int overlay) {
        final int side = platform.shownSide();
        if (side <= 0 || platform.getLevel() == null) {
            return;
        }
        final VertexConsumer paint = buffers.getBuffer(RenderType.entityCutout(side >= LARGE_SIDE ? LARGE : SMALL));
        final int lit = LevelRenderer.getLightColor(platform.getLevel(), platform.getBlockPos().above());
        final float half = side / 2.0F;
        final PoseStack.Pose at = pose.last();
        final float lo = MIDDLE - half;
        final float hi = MIDDLE + half;
        vertex(paint, at, lo, hi, 0, 1, lit);
        vertex(paint, at, hi, hi, 1, 1, lit);
        vertex(paint, at, hi, lo, 1, 0, lit);
        vertex(paint, at, lo, lo, 0, 0, lit);
    }

    @Override
    public AABB getRenderBoundingBox(final LandingPlatformBlockEntity platform) {
        final double half = platform.shownSide() / 2.0;
        return new AABB(platform.getBlockPos()).inflate(half, 0, half);
    }

    @Override
    public int getViewDistance() {
        return SEEN_FROM;
    }

    private static void vertex(final VertexConsumer paint, final PoseStack.Pose at, final float x, final float z,
                               final float u, final float v, final int light) {
        paint.addVertex(at, x, PAINT, z).setColor(255, 255, 255, 255).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(at, 0, 1, 0);
    }
}
