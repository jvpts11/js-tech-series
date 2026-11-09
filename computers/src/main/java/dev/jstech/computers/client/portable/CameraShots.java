/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.portable.PortableGallery;
import dev.jstech.computers.portable.PortableModel;
import dev.jstech.core.client.audio.media.MediaUploader;
import dev.jstech.core.text.Text;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * A portable's camera on the player's game (computers/docs/design/wireless.md): what the player sees, as the world is
 * drawn before the hand and the HUD go over it. While the camera app is open the world is copied, every frame, into
 * the viewfinder its screen shows; the shutter takes the next frame at the camera's own resolution, cut to four by
 * three about its middle, and brings the PNG to the server's store for the device's gallery.
 */
public final class CameraShots {

    /** The viewfinder, as a texture to draw: the world as the camera sees it, stored upside down as frames are. */
    public static final ResourceLocation VIEWFINDER = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "portable_viewfinder");

    @Nullable
    private static TextureTarget viewfinder;
    private static boolean registered;
    private static boolean viewing;
    @Nullable
    private static Shot wanted;

    private CameraShots() {
    }

    /** Hands the game's bus the moment the world is drawn, once, as the game starts. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, CameraShots::stage);
    }

    /** The camera app is on the screen this frame: the next frames keep its viewfinder coming. */
    static void viewing() {
        viewing = true;
    }

    /** How wide the viewfinder is, in its texels; 0 before it was first made. */
    static int width() {
        return viewfinder == null ? 0 : viewfinder.width;
    }

    /** How tall. */
    static int height() {
        return viewfinder == null ? 0 : viewfinder.height;
    }

    /** Takes a picture with the camera of the device {@code device}, of {@code model}, with the next frame. */
    static void take(final UUID device, final PortableModel model, final MobileAppState state) {
        if (wanted == null && model.cameraWidth() > 0) {
            wanted = new Shot(device, model, state);
        }
    }

    /** Lets the viewfinder go, as the player leaves the world. */
    static void clear() {
        if (viewfinder != null) {
            viewfinder.destroyBuffers();
            viewfinder = null;
        }
        wanted = null;
        viewing = false;
    }

    /* The world is drawn, before the hand and the HUD: the viewfinder and a picture wanted are taken from it. */
    private static void stage(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        final RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        if (viewing) {
            viewing = false;
            copy(main);
        }
        final Shot shot = wanted;
        if (shot != null) {
            wanted = null;
            take(main, shot);
        }
    }

    /* Copies the world into the viewfinder, at half the window's size, which is all a device's screen shows. */
    private static void copy(final RenderTarget main) {
        final int w = Math.max(1, main.width / 2);
        final int h = Math.max(1, main.height / 2);
        if (viewfinder == null) {
            viewfinder = new TextureTarget(w, h, false, Minecraft.ON_OSX);
            viewfinder.setFilterMode(GL11.GL_LINEAR);
        } else if (viewfinder.width != w || viewfinder.height != h) {
            viewfinder.resize(w, h, Minecraft.ON_OSX);
            viewfinder.setFilterMode(GL11.GL_LINEAR);
        }
        if (!registered) {
            Minecraft.getInstance().getTextureManager().register(VIEWFINDER, new ViewfinderTexture());
            registered = true;
        }
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, viewfinder.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT,
                GL11.GL_LINEAR);
        main.bindWrite(false);
    }

    /*
     * Reads the frame, then off the game's thread cuts it to four by three, brings it to the camera's resolution and
     * writes it as a PNG; back on the game's thread the PNG goes to the server, the camera saying how far it has got.
     */
    private static void take(final RenderTarget main, final Shot shot) {
        final NativeImage frame = Screenshot.takeScreenshot(main);
        final int width = shot.model().cameraWidth();
        final int height = shot.model().cameraHeight();
        CompletableFuture.supplyAsync(() -> picture(frame, width, height), Util.backgroundExecutor())
                .thenAcceptAsync(png -> {
                    if (png == null) {
                        return;
                    }
                    MediaUploader.upload(png, "picture.png", PortableGallery.PURPOSE, shot.device().toString(),
                            new MediaUploader.IListener() {
                                @Override
                                public void progress(final long sent, final long total) {
                                    shot.state().cameraNote(AppTexts.SENDING.with(total <= 0 ? 0
                                            : Math.round(100.0F * sent / total)));
                                }

                                @Override
                                public void finished(final boolean ok, final Text message) {
                                    shot.state().cameraNote(message);
                                    shot.state().askSoon();
                                }
                            });
                }, Minecraft.getInstance());
    }

    /* The frame cut about its middle to four by three and brought to {@code width} by {@code height}, as a PNG. */
    @Nullable
    private static byte[] picture(final NativeImage frame, final int width, final int height) {
        try (frame; NativeImage out = new NativeImage(width, height, false)) {
            final int cutW = Math.min(frame.getWidth(), frame.getHeight() * 4 / 3);
            final int cutH = cutW * 3 / 4;
            frame.resizeSubRectTo((frame.getWidth() - cutW) / 2, (frame.getHeight() - cutH) / 2, cutW, cutH, out);
            return out.asByteArray();
        } catch (final IOException unwritable) {
            return null;
        }
    }

    /* A picture wanted: by which device, of what model, and the app it is told in. */
    private record Shot(UUID device, PortableModel model, MobileAppState state) {
    }

    /* The viewfinder's colour as a texture the screens can draw by name. */
    private static final class ViewfinderTexture extends AbstractTexture {

        @Override
        public void load(final ResourceManager manager) {
            // Nothing to load: the frames are copied in as the world is drawn.
        }

        @Override
        public int getId() {
            return viewfinder == null ? super.getId() : viewfinder.getColorTextureId();
        }

        @Override
        public void releaseId() {
            // The viewfinder's texture is its frame's, let go with the frame.
        }
    }
}
