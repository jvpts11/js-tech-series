/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.surface;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.jstech.core.JsCore;
import dev.jstech.core.api.client.GpuSurface;
import dev.jstech.core.api.client.ISkin;
import dev.jstech.core.api.client.ISurface;
import dev.jstech.core.api.client.PixelSurface;
import dev.jstech.core.api.client.SurfaceRenderer;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.gui.SurfaceTexts;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Runs one renderer inside a rectangle of a screen: makes its surface, asks it for frames no faster than it allows and
 * only while it is drawn, puts its picture on the screen and hands it the input that lands on it.
 *
 * <p>What it shows instead of the picture is the placeholder: to a viewer who is not the one the renderer runs for,
 * unless the renderer is safe to show anywhere, and for good once the renderer has failed, which is written to the log
 * once.
 */
public final class SurfaceHost {

    private final SurfaceRenderer renderer;
    private final String owner;
    private ISurface surface;
    private long lastFrameNanos;
    private boolean failed;
    /** Where the picture was last drawn, so input can be turned into the surface's own pixels. */
    private int drawnX;
    private int drawnY;
    private int drawnW;
    private int drawnH;

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    /**
     * @param owner what the renderer belongs to, for the one line the log is told if it fails: a mod's program
     */
    public SurfaceHost(final SurfaceRenderer renderer, final String owner) {
        this.renderer = renderer;
        this.owner = owner;
    }

    /** Whether the renderer failed and will not be asked again. */
    public boolean failed() {
        return this.failed;
    }

    /** The renderer it runs. */
    public SurfaceRenderer renderer() {
        return this.renderer;
    }

    /**
     * Draws the renderer's picture into that rectangle, asking it for a frame first when its cap allows; or draws the
     * placeholder, when {@code forOpener} is false and the renderer is not safe to show anywhere, or once it failed.
     */
    public void draw(final GuiGraphics g, final Font font, final ISkin skin, final int x, final int y, final int w,
                     final int h, final boolean forOpener) {
        if (this.failed) {
            placeholder(g, font, skin, x, y, w, h, SurfaceTexts.FAILED);
            return;
        }
        if (!forOpener && !this.renderer.worldSafe()) {
            placeholder(g, font, skin, x, y, w, h, SurfaceTexts.ONLY_OPENER);
            return;
        }
        try {
            this.frameIfDue(g);
            this.place(x, y, w, h);
            switch (this.surface) {
                case PixelSurface pixels -> pixels.blit(g, this.drawnX, this.drawnY, this.drawnW, this.drawnH);
                case GpuSurface gpu -> gpu.blit(g, this.drawnX, this.drawnY, this.drawnW, this.drawnH);
            }
        } catch (final RuntimeException fault) {
            this.fail(fault);
            placeholder(g, font, skin, x, y, w, h, SurfaceTexts.FAILED);
        }
    }

    /** A click on the screen, handed on in the surface's own pixels when it lands on the picture. */
    public boolean mouseClicked(final double x, final double y, final int button) {
        return this.live() && this.inside(x, y) && this.guard(() -> this.renderer.mouseClicked(this.surfaceX(x),
                this.surfaceY(y), button));
    }

    /** A button let go, handed on in the surface's own pixels. */
    public boolean mouseReleased(final double x, final double y, final int button) {
        return this.live() && this.guard(() -> this.renderer.mouseReleased(this.surfaceX(x), this.surfaceY(y),
                button));
    }

    public boolean mouseScrolled(final double delta) {
        return this.live() && this.guard(() -> this.renderer.mouseScrolled(delta));
    }

    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        return this.live() && this.guard(() -> this.renderer.keyPressed(key, scanCode, modifiers));
    }

    public boolean keyReleased(final int key, final int scanCode, final int modifiers) {
        return this.live() && this.guard(() -> this.renderer.keyReleased(key, scanCode, modifiers));
    }

    public boolean charTyped(final char typed) {
        return this.live() && this.guard(() -> this.renderer.charTyped(typed));
    }

    /** Its window has closed: the renderer is told and the surface let go. */
    public void close() {
        if (!this.failed) {
            this.guard(() -> {
                this.renderer.closed();
                return true;
            });
        }
        switch (this.surface) {
            case PixelSurface pixels -> pixels.close();
            case GpuSurface gpu -> gpu.close();
            case null -> {
                // Never drawn, so there is nothing to let go.
            }
        }
        this.surface = null;
    }

    /** Draws the placeholder a window shows in place of a picture it cannot show this viewer. */
    public static void placeholder(final GuiGraphics g, final Font font, final ISkin skin, final int x, final int y,
                                   final int w, final int h, final TextKey why) {
        skin.panel(g, x, y, w, h);
        final String said = font.plainSubstrByWidth(GameText.resolve(why), Math.max(0, w - 8));
        Draw.textCentered(g, font, said, x + w / 2, y + (h - font.lineHeight) / 2, skin.dim());
    }

    /* A frame is due once a frame's worth of time has passed at the renderer's cap; the first is always due. */
    private void frameIfDue(final GuiGraphics g) {
        if (this.surface == null) {
            this.surface = this.renderer.gpu() ? new GpuSurface(this.renderer.width(), this.renderer.height())
                    : new PixelSurface(this.renderer.width(), this.renderer.height());
        }
        final long now = System.nanoTime();
        final long every = NANOS_PER_SECOND / Math.max(1, this.renderer.frameCap());
        if (this.lastFrameNanos != 0 && now - this.lastFrameNanos < every) {
            return;
        }
        final double seconds = this.lastFrameNanos == 0 ? 0 : (now - this.lastFrameNanos) / (double) NANOS_PER_SECOND;
        this.lastFrameNanos = now;
        if (this.surface instanceof GpuSurface gpu) {
            // What the screen has queued goes first, so the renderer's drawing lands on its own target alone.
            g.flush();
            final RenderTarget target = gpu.target();
            target.bindWrite(true);
            try {
                this.renderer.frame(gpu, seconds);
            } finally {
                Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
            }
        } else {
            this.renderer.frame(this.surface, seconds);
        }
    }

    /* Scales the surface by whole steps where it fits, else to fit, and centres it in the rectangle. */
    private void place(final int x, final int y, final int w, final int h) {
        final int sw = this.renderer.width();
        final int sh = this.renderer.height();
        final int whole = Math.min(w / Math.max(1, sw), h / Math.max(1, sh));
        if (whole >= 1) {
            this.drawnW = sw * whole;
            this.drawnH = sh * whole;
        } else {
            final double fit = Math.min(w / (double) sw, h / (double) sh);
            this.drawnW = Math.max(1, (int) (sw * fit));
            this.drawnH = Math.max(1, (int) (sh * fit));
        }
        this.drawnX = x + (w - this.drawnW) / 2;
        this.drawnY = y + (h - this.drawnH) / 2;
    }

    private boolean inside(final double x, final double y) {
        return x >= this.drawnX && y >= this.drawnY && x < this.drawnX + this.drawnW && y < this.drawnY + this.drawnH;
    }

    private double surfaceX(final double x) {
        return (x - this.drawnX) * this.renderer.width() / Math.max(1, this.drawnW);
    }

    private double surfaceY(final double y) {
        return (y - this.drawnY) * this.renderer.height() / Math.max(1, this.drawnH);
    }

    private boolean live() {
        return !this.failed && this.surface != null;
    }

    /* Runs a call into the renderer, which may throw like its drawing: a fault stops it for good. */
    private boolean guard(final IRendererCall call) {
        try {
            return call.run();
        } catch (final RuntimeException fault) {
            this.fail(fault);
            return false;
        }
    }

    private void fail(final RuntimeException fault) {
        if (!this.failed) {
            this.failed = true;
            JsCore.LOGGER.warn("The picture of {} failed to draw and is shown as a placeholder from now on",
                    this.owner, fault);
        }
    }

    /** One call into a renderer. */
    @FunctionalInterface
    private interface IRendererCall {
        boolean run();
    }
}
