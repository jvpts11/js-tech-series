/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.sky;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.jstech.core.JsCore;
import dev.jstech.core.sky.SkySpec;
import dev.jstech.core.sky.ZodiacalPicture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Draws a sky's zodiacal light ({@link ZodiacalPicture}) round its first sun, along the sun's path, as the night
 * allows: standing up from the horizon before sunrise and after sunset, where the air is thin enough to show it.
 */
final class SkyZodiacal {

    private static final ResourceLocation PICTURE = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "sky/zodiacal_light");
    /* The picture's texels: a quarter of a degree each. */
    private static final int WIDTH = 880;
    private static final int HEIGHT = 320;
    /* The patch's mesh: so many lengths along the path and across it. */
    private static final int ALONG = 44;
    private static final int ACROSS = 16;
    private static final float DISTANCE = 100F;
    /* How strong its light is beside its picture's whole, at a brightness of 1 at midnight, as the screen shows it. */
    private static final float GLOW = 0.45F;

    private static boolean drawn;

    private SkyZodiacal() {
    }

    /**
     * Draws {@code light} round the sun {@code pose} stands at the top of, the sun's path running round the pose's x
     * axis, as bright as the starlight {@code night}.
     */
    static void render(final SkySpec.ZodiacalLight light, final PoseStack pose, final float night) {
        final float alpha = (float) (light.brightness() * night * GLOW);
        if (alpha <= 0) {
            return;
        }
        picture();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, PICTURE);
        final float[] colour = SkyRenderer.rgb(light.colour());
        final Matrix4f matrix = pose.last().pose();
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < ALONG; i++) {
            final double a0 = -ZodiacalPicture.REACH + 2 * ZodiacalPicture.REACH * i / ALONG;
            final double a1 = -ZodiacalPicture.REACH + 2 * ZodiacalPicture.REACH * (i + 1) / ALONG;
            for (int j = 0; j < ACROSS; j++) {
                final double b0 = -ZodiacalPicture.WIDE + 2 * ZodiacalPicture.WIDE * j / ACROSS;
                final double b1 = -ZodiacalPicture.WIDE + 2 * ZodiacalPicture.WIDE * (j + 1) / ACROSS;
                vertex(builder, matrix, a0, b0, colour, alpha);
                vertex(builder, matrix, a1, b0, colour, alpha);
                vertex(builder, matrix, a1, b1, colour, alpha);
                vertex(builder, matrix, a0, b1, colour, alpha);
            }
        }
        RenderSystem.disableCull();
        SkyRenderer.drawNow(builder);
        RenderSystem.enableCull();
    }

    /* A point {@code along} degrees along the sun's path from the sun, {@code beta} degrees off it. */
    private static void vertex(final BufferBuilder builder, final Matrix4f matrix, final double along,
                               final double beta, final float[] colour, final float alpha) {
        final float a = (float) Math.toRadians(along);
        final float b = (float) Math.toRadians(beta);
        builder.addVertex(matrix, Mth.sin(b) * DISTANCE, Mth.cos(b) * Mth.cos(a) * DISTANCE,
                        Mth.cos(b) * Mth.sin(a) * DISTANCE)
                .setUv((float) ((along + ZodiacalPicture.REACH) / (2 * ZodiacalPicture.REACH)),
                        (float) ((ZodiacalPicture.WIDE - beta) / (2 * ZodiacalPicture.WIDE)))
                .setColor(colour[0], colour[1], colour[2], alpha);
    }

    /* The picture on the card, worked out the first time it is wanted: small, so at once. */
    private static void picture() {
        if (drawn) {
            return;
        }
        final double[] light = ZodiacalPicture.draw(WIDTH, HEIGHT);
        final NativeImage image = new NativeImage(NativeImage.Format.RGBA, WIDTH, HEIGHT, false);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                final int grey = (int) Math.round(light[y * WIDTH + x] * 0xFF);
                image.setPixelRGBA(x, y, 0xFF000000 | grey << 16 | grey << 8 | grey);
            }
        }
        Minecraft.getInstance().getTextureManager().register(PICTURE, new DynamicTexture(image));
        drawn = true;
    }
}
