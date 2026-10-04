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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.joml.Matrix4f;

/**
 * The pictures shown in the world, each drawn into a texture of its own and drawn again as often as its viewer's
 * distance calls for.
 *
 * <p>A renderer asks for its picture every frame it is seen, saying what draws it, its size and tube, and how far away
 * it is; it gets back the texture to draw with. The pictures themselves are drawn at the start of the next frame,
 * before the world, a few a frame at most so a room full of screens never costs a frame its time; one nobody has asked
 * for in a while is let go with its textures. Nothing here runs on the server: a picture costs the player who looks.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class LiveScreens {

    /* A picture nobody has asked for in this long gives its textures back. */
    private static final long FORGET_NANOS = 5_000_000_000L;
    /* At most this many pictures are drawn in one frame; the rest are drawn in the frames after. */
    private static final int PAINTS_PER_FRAME = 4;
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
        if (!screen.ready()) {
            return;
        }
        final VertexConsumer quad = buffers.getBuffer(RenderType.text(screen.texture()));
        final Matrix4f at = pose.last().pose();
        quad.addVertex(at, x0, y0, 0.0F).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setLight(light);
        quad.addVertex(at, x0, y1, 0.0F).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setLight(light);
        quad.addVertex(at, x1, y1, 0.0F).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setLight(light);
        quad.addVertex(at, x1, y0, 0.0F).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setLight(light);
    }

    /** How many pictures are held right now, drawn or waiting. */
    public static int held() {
        return SCREENS.size();
    }

    @SubscribeEvent
    public static void onFrame(final RenderFrameEvent.Pre event) {
        if (SCREENS.isEmpty()) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final long now = Util.getNanos();
        final float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        int painted = 0;
        final Iterator<LiveScreen> each = SCREENS.values().iterator();
        while (each.hasNext()) {
            final LiveScreen screen = each.next();
            if (screen.forgotten(now, FORGET_NANOS)) {
                screen.close();
                each.remove();
            } else if (painted < PAINTS_PER_FRAME && screen.due(now)) {
                screen.paint(minecraft, partialTick, now);
                painted++;
            }
        }
    }

    @SubscribeEvent
    public static void onLeave(final ClientPlayerNetworkEvent.LoggingOut event) {
        for (final LiveScreen screen : SCREENS.values()) {
            screen.close();
        }
        SCREENS.clear();
    }
}
