/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.live;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.JsCore;
import dev.jstech.core.gui.Tube;
import dev.jstech.core.live.LiveRate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * The pictures shown in the world, each drawn into a texture of its own and drawn again as often as its viewer's
 * distance calls for.
 *
 * <p>A renderer asks for its picture every frame it is seen, saying what draws it, its size and tube, and how far away
 * it is; it gets back the texture to draw with. The pictures themselves are drawn at the start of the next frame,
 * before the world, a few a frame at most so a room full of screens never costs a frame its time; one nobody has asked
 * for in a while is let go with its textures. Nothing here runs on the server: a picture costs the player who looks.
 */
public final class LiveScreens {

    /* A picture nobody has asked for in this long gives its textures back. */
    private static final long FORGET_NANOS = 5_000_000_000L;
    /* At most this many pictures are drawn in one frame; the rest are drawn in the frames after. */
    private static final int PAINTS_PER_FRAME = 4;
    /* The world's two lights a model's faces are lit by, and how much each gives and none does, as the game's are. */
    private static final Vector3fc FIRST_LIGHT = new Vector3f(0.2F, 1.0F, -0.7F).normalize();
    private static final Vector3fc SECOND_LIGHT = new Vector3f(-0.2F, 1.0F, 0.7F).normalize();
    private static final float LIGHTS = 0.6F;
    private static final float UNLIT = 0.4F;
    private static final Map<Object, LiveScreen> SCREENS = new HashMap<>();
    private static int named;

    private LiveScreens() {
    }

    /**
     * Asks for the picture known as {@code key}, drawn by {@code painter} into an area of that size (in the units it
     * draws in, {@code scale} texture pixels each) and shown through {@code tube}, for a viewer that far away
     * (squared, in blocks). Returns the picture, whose texture is the last one drawn.
     */
    public static LiveScreen ask(final Object key, final ILivePainter painter, final int width, final int height,
                                 final int scale, final Tube tube, final double distanceSquared) {
        final LiveScreen screen = SCREENS.computeIfAbsent(key,
                k -> new LiveScreen(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "live/" + named++)));
        screen.ask(painter, width, height, scale, tube, LiveRate.framesPerSecond(distanceSquared), Util.getNanos());
        return screen;
    }

    /**
     * Draws a picture's texture over a rectangle of the pose's plane, from {@code (x0, y0)} at its top left to
     * {@code (x1, y1)} at its bottom right, lit as given. The texture's top row is the picture's top.
     */
    public static void draw(final PoseStack pose, final MultiBufferSource buffers, final LiveScreen screen,
                            final float x0, final float y0, final float x1, final float y1, final int light) {
        draw(pose, buffers, screen, x0, y0, x1, y1, light, 1.0F);
    }

    /**
     * Draws a picture's texture as {@link #draw(PoseStack, MultiBufferSource, LiveScreen, float, float, float, float,
     * int)} does, darkened to {@code shade}, from 0 to 1: for a picture laid on a face of a model, which the game
     * darkens as it turns from the world's lights ({@link #shade}), so the picture matches the face round it.
     */
    public static void draw(final PoseStack pose, final MultiBufferSource buffers, final LiveScreen screen,
                            final float x0, final float y0, final float x1, final float y1, final int light,
                            final float shade) {
        if (!screen.ready()) {
            return;
        }
        final int grey = Math.round(Math.max(0.0F, Math.min(1.0F, shade)) * 255);
        final VertexConsumer quad = buffers.getBuffer(RenderType.text(screen.texture()));
        final Matrix4f at = pose.last().pose();
        quad.addVertex(at, x0, y0, 0.0F).setColor(grey, grey, grey, 255).setUv(0.0F, 1.0F).setLight(light);
        quad.addVertex(at, x0, y1, 0.0F).setColor(grey, grey, grey, 255).setUv(0.0F, 0.0F).setLight(light);
        quad.addVertex(at, x1, y1, 0.0F).setColor(grey, grey, grey, 255).setUv(1.0F, 0.0F).setLight(light);
        quad.addVertex(at, x1, y0, 0.0F).setColor(grey, grey, grey, 255).setUv(1.0F, 1.0F).setLight(light);
    }

    /**
     * How bright the game draws a face of a model whose normal, in the pose's space, is {@code normal}: as its shader
     * lights an entity's faces, by the world's two lights from above and a floor of light beneath.
     */
    public static float shade(final Vector3fc normal) {
        final float first = Math.max(0.0F, normal.dot(FIRST_LIGHT));
        final float second = Math.max(0.0F, normal.dot(SECOND_LIGHT));
        return Math.min(1.0F, (first + second) * LIGHTS + UNLIT);
    }

    /** How many pictures are held right now, drawn or waiting. */
    public static int held() {
        return SCREENS.size();
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor on a player's game. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(RenderFrameEvent.Pre.class, LiveScreens::onFrame);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, LiveScreens::onLeave);
    }

    public static void onFrame(final RenderFrameEvent.Pre event) {
        if (SCREENS.isEmpty()) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final long now = Util.getNanos();
        final float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        final List<LiveScreen> due = new ArrayList<>();
        final Iterator<LiveScreen> each = SCREENS.values().iterator();
        while (each.hasNext()) {
            final LiveScreen screen = each.next();
            if (screen.forgotten(now, FORGET_NANOS)) {
                screen.close();
                each.remove();
            } else if (screen.due(now)) {
                due.add(screen);
            }
        }
        /*
         * More pictures due than a frame draws: the ones waiting longest go first, those never drawn before all, so
         * a room of many screens draws each in turn rather than the same few every frame and some never at all.
         */
        if (due.size() > PAINTS_PER_FRAME) {
            due.sort(Comparator.comparingLong(LiveScreen::paintedAt));
        }
        for (int i = 0; i < Math.min(PAINTS_PER_FRAME, due.size()); i++) {
            due.get(i).paint(minecraft, partialTick, now);
        }
    }

    public static void onLeave(final ClientPlayerNetworkEvent.LoggingOut event) {
        for (final LiveScreen screen : SCREENS.values()) {
            screen.close();
        }
        SCREENS.clear();
    }
}
