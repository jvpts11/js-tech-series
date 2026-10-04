/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.live;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.logging.LogUtils;
import dev.jstech.core.gui.Tube;
import dev.jstech.core.live.LiveRate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;

/**
 * One picture shown in the world: the texture it is drawn into, and when it was last drawn.
 *
 * <p>The picture is drawn in two steps: what the screen shows, into a texture of its own at the size it is laid out
 * for, and then that copied through its tube into the texture the world shows. The world never waits for a picture:
 * it shows the last one drawn, and {@link LiveScreens} draws the next when it is due.
 */
public final class LiveScreen {

    private final ResourceLocation texture;
    @Nullable
    private TextureTarget picture;
    @Nullable
    private TextureTarget shown;
    private long paintedAt = LiveRate.NEVER;
    private long askedAt;
    private boolean painted;
    private boolean failed;
    // What the latest renderer asked for.
    @Nullable
    private ILivePainter painter;
    private int width;
    private int height;
    private int scale = 1;
    private Tube tube = Tube.COLOUR;
    private int framesPerSecond;

    private static final Logger LOG = LogUtils.getLogger();
    /* The units a picture is laid out in sit this far into the depth the game's own screens use. */
    private static final float GUI_NEAR = 1000.0F;
    private static final float GUI_FAR = 21000.0F;
    private static final float GUI_DEPTH = -11000.0F;

    LiveScreen(final ResourceLocation texture) {
        this.texture = texture;
        Minecraft.getInstance().getTextureManager().register(texture, new Shown());
    }

    /** The texture a renderer draws the picture with. */
    public ResourceLocation texture() {
        return texture;
    }

    /** Whether the picture has been drawn at least once, before which there is nothing to show. */
    public boolean ready() {
        return painted;
    }

    void ask(final ILivePainter by, final int areaWidth, final int areaHeight, final int pixelsPerUnit,
             final Tube through, final int fps, final long now) {
        this.painter = by;
        this.width = Math.max(1, areaWidth);
        this.height = Math.max(1, areaHeight);
        this.scale = Math.max(1, pixelsPerUnit);
        this.tube = through;
        this.framesPerSecond = fps;
        this.askedAt = now;
    }

    boolean due(final long now) {
        return painter != null && LiveRate.due(paintedAt, now, framesPerSecond);
    }

    boolean forgotten(final long now, final long after) {
        return now - askedAt > after;
    }

    void paint(final Minecraft minecraft, final float partialTick, final long now) {
        final ILivePainter by = painter;
        if (by == null) {
            return;
        }
        final int pw = width * scale;
        final int ph = height * scale;
        picture = sized(picture, pw, ph);
        shown = sized(shown, pw, ph);
        final RenderTarget main = minecraft.getMainRenderTarget();
        final Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        final VertexSorting sorting = RenderSystem.getVertexSorting();
        final Matrix4fStack view = RenderSystem.getModelViewStack();
        view.pushMatrix();
        try {
            picture.setClearColor(0.0F, 0.0F, 0.0F, 1.0F);
            picture.clear(Minecraft.ON_OSX);
            picture.bindWrite(true);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, width, height, 0.0F, GUI_NEAR, GUI_FAR),
                    VertexSorting.ORTHOGRAPHIC_Z);
            view.translation(0.0F, 0.0F, GUI_DEPTH);
            RenderSystem.applyModelViewMatrix();
            Lighting.setupFor3DItems();
            final LiveGraphics graphics = new LiveGraphics(minecraft, minecraft.renderBuffers().bufferSource(), width,
                    height, scale);
            by.paint(graphics, width, height, partialTick);
            graphics.flush();
            RenderSystem.disableScissor();
            copyThroughTube(pw, ph, view);
            painted = true;
        } catch (final RuntimeException e) {
            // A picture that cannot be drawn shows the last one it had; said once, not every frame.
            if (!failed) {
                LOG.warn("A picture shown in the world could not be drawn", e);
                failed = true;
            }
            RenderSystem.disableScissor();
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorting);
            main.bindWrite(true);
            paintedAt = now;
        }
    }

    void close() {
        Minecraft.getInstance().getTextureManager().release(texture);
        if (picture != null) {
            picture.destroyBuffers();
            picture = null;
        }
        if (shown != null) {
            shown.destroyBuffers();
            shown = null;
        }
    }

    /* The picture as its tube shows it, into the texture the world draws. */
    private void copyThroughTube(final int pw, final int ph, final Matrix4fStack view) {
        if (shown == null || picture == null || !TubeFilter.ready()) {
            return;
        }
        shown.setClearColor(0.0F, 0.0F, 0.0F, 1.0F);
        shown.clear(Minecraft.ON_OSX);
        shown.bindWrite(true);
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, pw, ph, 0.0F, -1.0F, 1.0F),
                VertexSorting.ORTHOGRAPHIC_Z);
        view.identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.disableDepthTest();
        // A texture's first row is its bottom one, so the top of the area samples its last.
        TubeFilter.draw(picture.getColorTextureId(), new Matrix4f(), 0.0F, 0.0F, pw, ph, 0.0F, 0.0F, 1.0F, 1.0F,
                0.0F, tube);
        RenderSystem.enableDepthTest();
    }

    private static TextureTarget sized(@Nullable final TextureTarget target, final int pw, final int ph) {
        if (target != null && target.width == pw && target.height == ph) {
            return target;
        }
        if (target != null) {
            target.destroyBuffers();
        }
        final TextureTarget made = new TextureTarget(pw, ph, true, Minecraft.ON_OSX);
        made.setFilterMode(GL11.GL_LINEAR);
        return made;
    }

    /** The texture the world draws: the copy through the tube, or the picture itself until the shader is there. */
    private final class Shown extends AbstractTexture {

        @Override
        public int getId() {
            final TextureTarget through = TubeFilter.ready() ? shown : picture;
            return through == null ? -1 : through.getColorTextureId();
        }

        @Override
        public void load(final ResourceManager resources) {
            // Nothing to read: the picture is drawn, not loaded.
        }

        @Override
        public void releaseId() {
            // The targets own their textures and free them themselves.
        }
    }
}
