/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.camera;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.jstech.core.JsCore;
import dev.jstech.core.camera.CameraFeed;
import dev.jstech.core.camera.CameraSettings;
import dev.jstech.core.client.view.WorldViews;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.lwjgl.opengl.GL11;

/**
 * The views of cameras in the world on a player's game, for the screens that show them: a screen asks for a camera's
 * view each time it draws ({@link #show}) and gets the last picture drawn of it. The pictures are drawn before the
 * frame, from the land the game keeps built ({@link WorldViews}), as many at once as the player's setting allows (the
 * nearest asked for first), each drawn again at the setting's rate and as wide as its resolution; one nobody has asked
 * for in a while gives its picture back. Nothing here runs on the server: a view costs the player who looks.
 */
public final class CameraFeeds {

    /* A view nobody has asked for in this long gives its picture back. */
    private static final long FORGET_NANOS = 1_000_000_000L;
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    /* At most this many views are drawn in one frame; the rest in the frames after. */
    private static final int DRAWS_PER_FRAME = 1;
    private static final Map<CameraFeed, Feed> FEEDS = new HashMap<>();
    private static int named;
    private static int drawn;

    private CameraFeeds() {
    }

    /**
     * Shows the view of {@code feed} in the rectangle at ({@code x}, {@code y}), {@code width} by {@code height}: the
     * last picture drawn of it, its middle cut to the rectangle's shape; nothing before its first picture, or while
     * the player's setting draws no views. Asks for it again, so it goes on being drawn; whether a picture showed.
     */
    public static boolean show(final GuiGraphics g, final CameraFeed feed, final int x, final int y, final int width,
                               final int height) {
        if (width <= 0 || height <= 0 || CameraSettings.feeds() <= 0) {
            return false;
        }
        final Feed held = FEEDS.computeIfAbsent(feed, key -> new Feed(
                ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "camera_feed/" + named++)));
        held.asked(Util.getNanos(), width, height);
        if (held.target == null || held.drawnAt == 0) {
            return false;
        }
        final int w = held.target.width;
        final int h = held.target.height;
        /* The framebuffer's rows run upwards: its picture is drawn with its last row first. */
        g.blit(held.id, x, y, width, height, 0.0F, h, w, -h, w, h);
        return true;
    }

    /** How many pictures of views have been drawn since the game started, for the tests. */
    public static int drawn() {
        return drawn;
    }

    /** How many views are held now, drawn or waiting. */
    public static int held() {
        return FEEDS.size();
    }

    static void onFrame(final RenderFrameEvent.Pre event) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (FEEDS.isEmpty() || minecraft.level == null || minecraft.player == null || WorldViews.drawing()) {
            return;
        }
        final long now = Util.getNanos();
        final List<Map.Entry<CameraFeed, Feed>> wanted = new ArrayList<>();
        final Iterator<Map.Entry<CameraFeed, Feed>> each = FEEDS.entrySet().iterator();
        while (each.hasNext()) {
            final Map.Entry<CameraFeed, Feed> entry = each.next();
            if (now - entry.getValue().askedAt > FORGET_NANOS) {
                entry.getValue().close(minecraft);
                each.remove();
            } else {
                wanted.add(entry);
            }
        }
        wanted.sort(Comparator.comparingDouble(entry -> entry.getKey().at().distanceToSqr(
                minecraft.player.position())));
        final long every = NANOS_PER_SECOND / Math.max(1, CameraSettings.feedRate());
        final float partial = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        int draws = 0;
        for (int i = 0; i < Math.min(CameraSettings.feeds(), wanted.size()) && draws < DRAWS_PER_FRAME; i++) {
            final Feed feed = wanted.get(i).getValue();
            if (now - feed.drawnAt >= every) {
                feed.draw(minecraft, wanted.get(i).getKey(), partial, now);
                draws++;
            }
        }
        if (draws > 0) {
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }

    static void onLeave(final ClientPlayerNetworkEvent.LoggingOut event) {
        final Minecraft minecraft = Minecraft.getInstance();
        for (final Feed feed : FEEDS.values()) {
            feed.close(minecraft);
        }
        FEEDS.clear();
    }

    /* One view: its picture, the texture the screens draw it with, and when it was last asked for and drawn. */
    private static final class Feed {

        private final ResourceLocation id;
        private TextureTarget target;
        private long askedAt;
        private long drawnAt;
        private double aspect = 1.0;

        private Feed(final ResourceLocation id) {
            this.id = id;
        }

        void asked(final long now, final int width, final int height) {
            this.askedAt = now;
            this.aspect = (double) width / height;
        }

        void draw(final Minecraft minecraft, final CameraFeed feed, final float partial, final long now) {
            final int width = CameraSettings.feedResolution();
            final int height = Math.max(1, (int) Math.round(width / this.aspect));
            if (this.target == null || this.target.width != width || this.target.height != height) {
                if (this.target != null) {
                    this.target.destroyBuffers();
                }
                this.target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
                final TextureTarget made = this.target;
                minecraft.getTextureManager().register(this.id, new AbstractTexture() {
                    @Override
                    public void load(final ResourceManager manager) {
                        // The picture is drawn, never read from a pack.
                    }

                    @Override
                    public int getId() {
                        return made.getColorTextureId();
                    }

                    @Override
                    public void releaseId() {
                        // The picture's own target gives its texture back.
                    }
                });
            }
            WorldViews.draw(minecraft, minecraft.level, this.target, feed.at(), feed.yaw(), feed.pitch(),
                    WorldViews.projection(minecraft, feed.fov(), width, height), partial, true);
            /*
             * The game clears the sky with nothing for its alpha, which its own screen never shows; a picture drawn
             * on a screen in the world would show the sky as a hole. Every pixel is made opaque.
             */
            this.target.bindWrite(false);
            RenderSystem.colorMask(false, false, false, true);
            RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 1.0F);
            RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.colorMask(true, true, true, true);
            this.drawnAt = now;
            drawn++;
        }

        void close(final Minecraft minecraft) {
            if (this.target != null) {
                minecraft.getTextureManager().release(this.id);
                this.target.destroyBuffers();
                this.target = null;
            }
        }
    }
}
