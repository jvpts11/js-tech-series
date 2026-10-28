/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.sky;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.jstech.core.sky.Skies;
import dev.jstech.core.sky.SkySpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * A sky's own clouds: a layer at their height, in their colour and as thick as they say, drifting as the game's
 * clouds drift and lit by the time of day. The game's cloud map gives their shapes, a block of it twelve blocks wide.
 */
final class SkyClouds {

    private static final ResourceLocation CLOUDS = ResourceLocation.withDefaultNamespace(
            "textures/environment/clouds.png");
    private static final float BLOCKS_A_PIXEL = 12F;
    private static final float MAP = 256F;
    private static final float DRIFT = 0.03F;
    private static final float NIGHT = 0.15F;
    private static boolean wrapped;

    private SkyClouds() {
    }

    /** Draws {@code clouds} round the camera at {@code camX, camY, camZ}; whether drawn in the game's place. */
    static boolean render(final ClientLevel level, final SkySpec.Clouds clouds, final int ticks,
                          final float partialTick, final double camX, final double camY, final double camZ,
                          final Matrix4f modelView, final Matrix4f projection) {
        if (clouds.density() <= 0) {
            return true;
        }
        final PoseStack pose = new PoseStack();
        pose.mulPose(modelView);
        final Matrix4f matrix = pose.last().pose();
        final float reach = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16F * 2F;
        final float drift = (ticks + partialTick) * DRIFT;
        final float daylight = Math.max(NIGHT, SkyPlace.at(level, Skies.of(level), camX, camZ).skyLight());
        final float[] colour = SkyRenderer.rgb(clouds.colour());
        final float alpha = (float) clouds.density() * 0.8F;
        final float height = (float) (clouds.height() - camY);
        final float u0 = (float) ((camX + drift - reach) / BLOCKS_A_PIXEL / MAP);
        final float u1 = (float) ((camX + drift + reach) / BLOCKS_A_PIXEL / MAP);
        final float v0 = (float) ((camZ - reach) / BLOCKS_A_PIXEL / MAP);
        final float v1 = (float) ((camZ + reach) / BLOCKS_A_PIXEL / MAP);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, CLOUDS);
        Minecraft.getInstance().getTextureManager().getTexture(CLOUDS).bind();
        if (!wrapped) {
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
            wrapped = true;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR);
        final float red = colour[0] * daylight;
        final float green = colour[1] * daylight;
        final float blue = colour[2] * daylight;
        builder.addVertex(matrix, -reach, height, -reach).setUv(u0, v0).setColor(red, green, blue, alpha);
        builder.addVertex(matrix, -reach, height, reach).setUv(u0, v1).setColor(red, green, blue, alpha);
        builder.addVertex(matrix, reach, height, reach).setUv(u1, v1).setColor(red, green, blue, alpha);
        builder.addVertex(matrix, reach, height, -reach).setUv(u1, v0).setColor(red, green, blue, alpha);
        SkyRenderer.drawNow(builder);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        return true;
    }
}
