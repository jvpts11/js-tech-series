/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.effect;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.heat.HeatGlowClient;
import dev.jstech.core.effect.EffectSettings;
import dev.jstech.core.heat.Blackbody;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

/**
 * What effects do to the screen: a flash, the view shaking, the ring of a shockwave bending the view as it goes out,
 * the air shimmering over hot things, and a vignette closing in for a hazard (radiation, too little air). The bends
 * are drawn by a shader over the world as it was drawn; the player's settings say how hard the screen shakes, how
 * bright a flash is, and whether the view bends at all. On the player's game only.
 *
 * <pre>{@code
 * ScreenEffects.vignette(rl("radiation"), RADIATION_GREEN, dose);   // each tick the hazard lasts
 * }</pre>
 */
public final class ScreenEffects {

    private static final ResourceLocation SHADER = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "distort");
    private static final int HAZE_POINTS = 4;
    private static final double HAZE_REACH = 24.0;
    /* How hot glows its fullest shimmer, kelvin, and how far a hot block's shimmer reaches on the screen. */
    private static final double SHIMMER_KELVIN = 1_200.0;
    private static final double SHIMMER_SIZE = 1.5;
    private static final double LEAST_SIZE = 0.04;
    private static final double MOST_SIZE = 0.35;
    /* How far a shockwave's ring goes across the screen over its time, and how much a shake turns the view. */
    private static final double SHOCK_SPREAD = 0.9;
    private static final double SHAKE_DEGREES = 2.5;
    private static final int VIGNETTE_TICKS = 10;
    private static final float VIGNETTE_INSET = 0.3F;
    private static final int CHANNEL = 255;
    private static final int ALPHA_SHIFT = 24;
    private static final int WHITE = 0xFFFFFF;
    private static final double TICKS_A_SECOND = 20.0;
    private static final float TIME_SCALE = 1.0F / 20.0F;
    private static final List<Burst> FLASHES = new ArrayList<>();
    private static final List<Burst> SHAKES = new ArrayList<>();
    private static final List<Shock> SHOCKS = new ArrayList<>();
    private static final Map<ResourceLocation, Vignette> VIGNETTES = new HashMap<>();

    private static @Nullable ShaderInstance shader;
    private static int scratch = -1;
    private static int scratchWidth;
    private static int scratchHeight;
    private static Matrix4f view = new Matrix4f();
    private static Matrix4f projection = new Matrix4f();
    private static Vec3 eye = Vec3.ZERO;

    private ScreenEffects() {
    }

    /** Hands the buses what this answers, once, from the Core's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterShadersEvent.class, event -> {
            try {
                event.registerShader(new ShaderInstance(event.getResourceProvider(), SHADER,
                        DefaultVertexFormat.POSITION_TEX), loaded -> shader = loaded);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, event -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
                view = new Matrix4f(event.getModelViewMatrix());
                projection = new Matrix4f(event.getProjectionMatrix());
                eye = event.getCamera().getPosition();
            }
        });
        NeoForge.EVENT_BUS.addListener(RenderGuiEvent.Pre.class, event -> draw(event.getGuiGraphics()));
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeCameraAngles.class, ScreenEffects::shake);
    }

    /**
     * What an effect does to the screen as it starts at {@code at}: a flash of {@code flash}, a shake of
     * {@code shake}, a shockwave of {@code shock}, each 0 to 1, over {@code seconds}.
     */
    public static void fire(final Vec3 at, final float flash, final float shake, final float shock,
                            final float seconds) {
        final long now = now();
        final double ticks = Math.max(1, seconds * TICKS_A_SECOND);
        if (flash > 0) {
            FLASHES.add(new Burst(flash, now, ticks));
        }
        if (shake > 0) {
            SHAKES.add(new Burst(shake, now, ticks));
        }
        if (shock > 0) {
            SHOCKS.add(new Shock(at, shock, now, ticks));
        }
    }

    /**
     * A vignette of {@code argb} closing in by {@code strength} (0 to 1) for the hazard {@code key}: said each tick it
     * lasts, it fades half a second after the last.
     */
    public static void vignette(final ResourceLocation key, final int argb, final float strength) {
        VIGNETTES.put(key, new Vignette(argb, Math.clamp(strength, 0.0F, 1.0F), now() + VIGNETTE_TICKS));
    }

    /** Where {@code at} was on the screen in the last frame, from 0 to 1 across and down; none behind the eye. */
    public static float @Nullable [] toScreen(final Vec3 at) {
        final float[] up = onScreen(at);
        return up == null ? null : new float[] {up[0], 1 - up[1]};
    }

    /** Takes every screen effect away, as the world closes. */
    static void clear() {
        FLASHES.clear();
        SHAKES.clear();
        SHOCKS.clear();
        VIGNETTES.clear();
    }

    /** Whether any screen effect is on now, for a test to look at. */
    public static boolean active() {
        return !FLASHES.isEmpty() || !SHAKES.isEmpty() || !SHOCKS.isEmpty() || !VIGNETTES.isEmpty();
    }

    private static void shake(final ViewportEvent.ComputeCameraAngles event) {
        final double strength = strongest(SHAKES) * EffectSettings.shake();
        if (strength <= 0) {
            return;
        }
        final double t = now() + event.getPartialTick();
        final double degrees = SHAKE_DEGREES * strength;
        event.setYaw((float) (event.getYaw() + degrees * Math.sin(t * 2.1)));
        event.setPitch((float) (event.getPitch() + degrees * Math.sin(t * 2.9 + 1.0)));
        event.setRoll((float) (event.getRoll() + degrees * 0.5 * Math.sin(t * 3.7 + 2.0)));
    }

    private static void draw(final GuiGraphics graphics) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        bend(graphics);
        vignettes(graphics);
        final double flash = strongest(FLASHES) * EffectSettings.flash();
        if (flash > 0) {
            final int alpha = (int) Math.round(Math.min(1, flash) * CHANNEL);
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << ALPHA_SHIFT | WHITE);
        }
    }

    /* The view bent by the shimmer over hot things and the nearest shockwave, through the shader. */
    private static void bend(final GuiGraphics graphics) {
        final ShaderInstance distort = shader;
        if (distort == null || !EffectSettings.distortion()) {
            return;
        }
        final List<float[]> hazes = hazes();
        final float[] shock = shock();
        if (hazes.isEmpty() && shock == null) {
            return;
        }
        graphics.flush();
        final Window window = Minecraft.getInstance().getWindow();
        final int width = window.getWidth();
        final int height = window.getHeight();
        ensureScratch(width, height);
        GlStateManager._bindTexture(scratch);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
        final float u1 = (float) width / scratchWidth;
        final float v1 = (float) height / scratchHeight;
        distort.safeGetUniform("Time").set(now() * TIME_SCALE);
        distort.safeGetUniform("Aspect").set((float) width / height);
        distort.safeGetUniform("Extent").set(u1, v1);
        for (int i = 0; i < HAZE_POINTS; i++) {
            final float[] haze = i < hazes.size() ? hazes.get(i) : new float[4];
            distort.safeGetUniform("Haze" + i).set(haze[0], haze[1], haze[2], haze[3]);
        }
        final float[] ring = shock == null ? new float[4] : shock;
        distort.safeGetUniform("Shock").set(ring[0], ring[1], ring[2], ring[3]);
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShader(() -> distort);
        RenderSystem.setShaderTexture(0, scratch);
        final float w = graphics.guiWidth();
        final float h = graphics.guiHeight();
        final Matrix4f pose = new Matrix4f();
        final BufferBuilder quad = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX);
        // The window's rows count up from its bottom edge, the screen's down from its top.
        quad.addVertex(pose, 0, 0, 0).setUv(0, v1);
        quad.addVertex(pose, 0, h, 0).setUv(0, 0);
        quad.addVertex(pose, w, h, 0).setUv(u1, 0);
        quad.addVertex(pose, w, 0, 0).setUv(u1, v1);
        BufferUploader.drawWithShader(quad.buildOrThrow());
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
    }

    /* The hot things nearest the eye, each where it is on the screen, how far its shimmer reaches and how strong. */
    private static List<float[]> hazes() {
        final List<float[]> out = new ArrayList<>();
        final List<Map.Entry<BlockPos, Double>> hot = new ArrayList<>(HeatGlowClient.hot().entrySet());
        hot.removeIf(entry -> Vec3.atCenterOf(entry.getKey()).distanceTo(eye) > HAZE_REACH);
        hot.sort(Comparator.comparingDouble(entry -> Vec3.atCenterOf(entry.getKey()).distanceTo(eye)));
        for (final Map.Entry<BlockPos, Double> entry : hot) {
            if (out.size() >= HAZE_POINTS) {
                break;
            }
            final Vec3 above = Vec3.atCenterOf(entry.getKey()).add(0, 0.6, 0);
            final float[] screen = onScreen(above);
            if (screen != null) {
                final double away = Math.max(1, above.distanceTo(eye));
                final float size = (float) Math.clamp(SHIMMER_SIZE / away, LEAST_SIZE, MOST_SIZE);
                final float strength = (float) Math.min(1, Blackbody.glow(entry.getValue())
                        + entry.getValue() / SHIMMER_KELVIN * 0.5);
                out.add(new float[] {screen[0], screen[1], size, strength});
            }
        }
        return out;
    }

    /* The strongest shockwave going out now: where it is on the screen, its ring's radius and its strength. */
    private static float @Nullable [] shock() {
        final long now = now();
        float[] best = null;
        final Iterator<Shock> shocks = SHOCKS.iterator();
        while (shocks.hasNext()) {
            final Shock shock = shocks.next();
            final double gone = (now - shock.born()) / shock.ticks();
            if (gone >= 1) {
                shocks.remove();
                continue;
            }
            final float[] screen = onScreen(shock.at());
            final float strength = (float) (shock.strength() * (1 - gone));
            if (screen != null && (best == null || strength > best[3])) {
                best = new float[] {screen[0], screen[1], (float) (gone * SHOCK_SPREAD), strength};
            }
        }
        return best;
    }

    /* The vignettes of the hazards on now: each colour closing in from the screen's edges. */
    private static void vignettes(final GuiGraphics graphics) {
        final long now = now();
        VIGNETTES.values().removeIf(vignette -> vignette.until() < now);
        if (VIGNETTES.isEmpty()) {
            return;
        }
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        final float w = graphics.guiWidth();
        final float h = graphics.guiHeight();
        final Matrix4f pose = graphics.pose().last().pose();
        final BufferBuilder quads = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
        for (final Vignette vignette : VIGNETTES.values()) {
            final int a = (int) Math.round((vignette.argb() >>> ALPHA_SHIFT) * vignette.strength());
            final int r = vignette.argb() >> 16 & CHANNEL;
            final int g = vignette.argb() >> 8 & CHANNEL;
            final int b = vignette.argb() & CHANNEL;
            final float ix = w * VIGNETTE_INSET * vignette.strength();
            final float iy = h * VIGNETTE_INSET * vignette.strength();
            edge(quads, pose, 0, 0, w, 0, w - ix, iy, ix, iy, r, g, b, a);
            edge(quads, pose, w, h, 0, h, ix, h - iy, w - ix, h - iy, r, g, b, a);
            edge(quads, pose, 0, h, 0, 0, ix, iy, ix, h - iy, r, g, b, a);
            edge(quads, pose, w, 0, w, h, w - ix, h - iy, w - ix, iy, r, g, b, a);
        }
        BufferUploader.drawWithShader(quads.buildOrThrow());
    }

    /* One side of a vignette: its outer edge at the colour, its inner edge clear. */
    private static void edge(final BufferBuilder quads, final Matrix4f pose, final float x0, final float y0,
                             final float x1, final float y1, final float x2, final float y2, final float x3,
                             final float y3, final int r, final int g, final int b, final int a) {
        quads.addVertex(pose, x0, y0, 0).setColor(r, g, b, a);
        quads.addVertex(pose, x3, y3, 0).setColor(r, g, b, 0);
        quads.addVertex(pose, x2, y2, 0).setColor(r, g, b, 0);
        quads.addVertex(pose, x1, y1, 0).setColor(r, g, b, a);
    }

    /* Where {@code at} is on the screen, from 0 to 1 across and up; none behind the eye. */
    private static float @Nullable [] onScreen(final Vec3 at) {
        final Vector4f clip = new Vector4f((float) (at.x - eye.x), (float) (at.y - eye.y), (float) (at.z - eye.z),
                1.0F);
        view.transform(clip);
        projection.transform(clip);
        if (clip.w <= 0) {
            return null;
        }
        return new float[] {clip.x / clip.w * 0.5F + 0.5F, clip.y / clip.w * 0.5F + 0.5F};
    }

    /* The strongest of {@code bursts} now, fading over each one's time; those over are let go. */
    private static double strongest(final List<Burst> bursts) {
        final long now = now();
        double best = 0;
        final Iterator<Burst> each = bursts.iterator();
        while (each.hasNext()) {
            final Burst burst = each.next();
            final double gone = (now - burst.born()) / burst.ticks();
            if (gone >= 1) {
                each.remove();
                continue;
            }
            best = Math.max(best, burst.strength() * (1 - gone) * (1 - gone));
        }
        return best;
    }

    private static long now() {
        final ClientLevel level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime();
    }

    private static void ensureScratch(final int width, final int height) {
        if (scratch != -1 && scratchWidth >= width && scratchHeight >= height) {
            return;
        }
        if (scratch == -1) {
            scratch = TextureUtil.generateTextureId();
        }
        scratchWidth = Math.max(width, scratchWidth);
        scratchHeight = Math.max(height, scratchHeight);
        TextureUtil.prepareImage(NativeImage.InternalGlFormat.RGBA, scratch, 0, scratchWidth, scratchHeight);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }

    /* A flash or a shake: how strong, when it began, and over how many ticks it fades. */
    private record Burst(double strength, long born, double ticks) {
    }

    /* A shockwave: where it began, how strong, when, and over how many ticks it goes out. */
    private record Shock(Vec3 at, double strength, long born, double ticks) {
    }

    /* A hazard's vignette: its colour, how far it closes in, and the tick it fades after. */
    private record Vignette(int argb, float strength, long until) {
    }
}
