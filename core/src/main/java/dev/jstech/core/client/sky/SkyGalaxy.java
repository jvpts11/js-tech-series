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
import dev.jstech.core.sky.GalaxyPicture;
import dev.jstech.core.sky.SkySpec;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws a sky's galaxy as its picture shows it ({@link GalaxyPicture}), laid round the sky along the band's great
 * circle, its heart where the sky says, its light added to what is behind it as the night allows. The picture is
 * worked out once for the galaxy's colours, away from the game's frame, and drawn from when it is ready.
 */
final class SkyGalaxy {

    private static final ResourceLocation PICTURE = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "sky/galaxy");
    /* The picture's texels: about a ninth of a degree each, a pixel or two of the game's frame. */
    private static final int WIDTH = 4096;
    private static final int HEIGHT = 546;
    private static final long SEED = 7L;
    /* The band's mesh: so many lengths round and across. */
    private static final int ROUND = 128;
    private static final int ACROSS = 8;
    private static final float DISTANCE = 100F;
    private static final double CROSSING = 90.0;
    /* How strong its light is beside its picture's whole, at a brightness of 1 at midnight, as the screen shows it. */
    private static final float GLOW = 0.65F;

    private static @Nullable Object drawnFor;
    private static @Nullable Object wantedFor;
    private static @Nullable CompletableFuture<int[]> drawing;

    private SkyGalaxy() {
    }

    /** Draws {@code galaxy} on the sky as {@code pose} turns it, as bright as the starlight {@code night}. */
    static void render(final SkySpec.Galaxy galaxy, final PoseStack pose, final float night) {
        final float alpha = (float) (galaxy.brightness() * night * GLOW);
        if (alpha <= 0 || !ready(galaxy)) {
            return;
        }
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, PICTURE);
        final Matrix4f matrix = pose.last().pose();
        final Quaternionf lean = new Quaternionf().rotateX((float) Math.toRadians(galaxy.tilt()));
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < ROUND; i++) {
            final double l0 = -180 + 360.0 * i / ROUND;
            final double l1 = -180 + 360.0 * (i + 1) / ROUND;
            for (int j = 0; j < ACROSS; j++) {
                final double b0 = -GalaxyPicture.REACH + 2 * GalaxyPicture.REACH * j / ACROSS;
                final double b1 = -GalaxyPicture.REACH + 2 * GalaxyPicture.REACH * (j + 1) / ACROSS;
                vertex(builder, matrix, galaxy, lean, l0, b0, alpha);
                vertex(builder, matrix, galaxy, lean, l1, b0, alpha);
                vertex(builder, matrix, galaxy, lean, l1, b1, alpha);
                vertex(builder, matrix, galaxy, lean, l0, b1, alpha);
            }
        }
        // Seen from inside the sphere it is laid on, whichever way the band runs: neither side may be culled.
        RenderSystem.disableCull();
        SkyRenderer.drawNow(builder);
        RenderSystem.enableCull();
    }

    /**
     * Where galactic longitude {@code l} and latitude {@code b}, in degrees, lie on the sky of {@code galaxy}, as a
     * direction before the sky's turning: the band's great circle leaning by its tilt, its heart turned round to its
     * centre.
     */
    static Vector3f direction(final SkySpec.Galaxy galaxy, final Quaternionf lean, final double l, final double b) {
        // A quarter of the way round the band from the sky's pole, where the band crosses the sun's path.
        final float round = (float) Math.toRadians(l + galaxy.centre() + CROSSING);
        final float off = (float) Math.toRadians(b);
        return new Vector3f(Mth.cos(round) * Mth.cos(off), Mth.sin(off), Mth.sin(round) * Mth.cos(off)).rotate(lean);
    }

    private static void vertex(final BufferBuilder builder, final Matrix4f matrix, final SkySpec.Galaxy galaxy,
                               final Quaternionf lean, final double l, final double b, final float alpha) {
        final Vector3f at = direction(galaxy, lean, l, b).mul(DISTANCE);
        builder.addVertex(matrix, at.x, at.y, at.z).setUv((float) ((l + 180) / 360),
                (float) ((GalaxyPicture.REACH - b) / (2 * GalaxyPicture.REACH))).setColor(1F, 1F, 1F, alpha);
    }

    /*
     * Whether the picture for {@code galaxy}'s colours is on the card: its working out begun the first time it is
     * wanted, away from the frame, and put on the card once done.
     */
    private static boolean ready(final SkySpec.Galaxy galaxy) {
        final Object wanted = galaxy.colour() + "/" + galaxy.heartColour();
        if (wanted.equals(drawnFor)) {
            return true;
        }
        if (!wanted.equals(wantedFor)) {
            wantedFor = wanted;
            final int rim = galaxy.colour();
            final int heart = galaxy.heartColour();
            drawing = CompletableFuture.supplyAsync(() -> GalaxyPicture.draw(SEED, WIDTH, HEIGHT, rim, heart));
        }
        final CompletableFuture<int[]> pending = drawing;
        if (pending == null || !pending.isDone() || pending.isCompletedExceptionally()) {
            return false;
        }
        final int[] picture = pending.join();
        final NativeImage image = new NativeImage(NativeImage.Format.RGBA, WIDTH, HEIGHT, false);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                final int rgb = picture[y * WIDTH + x];
                // The game's pictures hold their texels as alpha, blue, green and red.
                image.setPixelRGBA(x, y, 0xFF000000 | (rgb & 0xFF) << 16 | (rgb & 0xFF00) | (rgb >> 16 & 0xFF));
            }
        }
        Minecraft.getInstance().getTextureManager().register(PICTURE, new DynamicTexture(image));
        drawnFor = wanted;
        drawing = null;
        return true;
    }
}
