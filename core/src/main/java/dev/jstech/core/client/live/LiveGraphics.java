/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.live;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;

/**
 * The graphics of a picture drawn into a texture of its own rather than onto the game's window.
 *
 * <p>The game's graphics size everything after its window: how wide the area is, and where a clipped region falls,
 * which it works out from the window's height and scale. Drawn into a texture those are the texture's, so this answers
 * them for the texture instead and leaves every other call as the game makes it.
 *
 * <p>A painter whose picture is part of a bigger area it lays itself out in (a desktop laid out round a monitor's
 * glass, of which only the glass is wanted) can {@link #shift} the area, and a clip it asks for moves with it.
 */
public final class LiveGraphics extends GuiGraphics {

    private final Deque<ScreenRectangle> scissors = new ArrayDeque<>();
    private final int width;
    private final int height;
    /** Texture pixels to each unit drawn in. */
    private final int scale;
    /* Where the area's own origin falls on the texture, after any shift. */
    private int originX;
    private int originY;

    LiveGraphics(final Minecraft minecraft, final MultiBufferSource.BufferSource buffers, final int width,
                 final int height, final int scale) {
        super(minecraft, buffers);
        this.width = width;
        this.height = height;
        this.scale = scale;
    }

    /**
     * Moves what is drawn from now on by {@code (dx, dy)}, clips included: a painter laying itself out in a bigger area
     * shifts it so the part it wants falls on the texture.
     */
    public void shift(final int dx, final int dy) {
        pose().translate(dx, dy, 0.0F);
        originX += dx;
        originY += dy;
    }

    @Override
    public int guiWidth() {
        return width;
    }

    @Override
    public int guiHeight() {
        return height;
    }

    @Override
    public void enableScissor(final int minX, final int minY, final int maxX, final int maxY) {
        final ScreenRectangle asked = new ScreenRectangle(minX + originX, minY + originY, maxX - minX, maxY - minY);
        final ScreenRectangle within = scissors.peekLast();
        final ScreenRectangle region = within == null ? asked
                : Objects.requireNonNullElse(asked.intersection(within), ScreenRectangle.empty());
        scissors.addLast(region);
        apply(region);
    }

    @Override
    public void disableScissor() {
        if (scissors.isEmpty()) {
            throw new IllegalStateException("Scissor stack underflow");
        }
        scissors.removeLast();
        apply(scissors.peekLast());
    }

    @Override
    public boolean containsPointInScissor(final int x, final int y) {
        final ScreenRectangle region = scissors.peekLast();
        return region == null || region.containsPoint(x + originX, y + originY);
    }

    /* What was drawn before a clip changes is drawn under the clip it was drawn under. */
    private void apply(@Nullable final ScreenRectangle region) {
        flush();
        if (region == null) {
            RenderSystem.disableScissor();
            return;
        }
        RenderSystem.enableScissor(region.left() * scale, (height - region.bottom()) * scale,
                Math.max(0, region.width() * scale), Math.max(0, region.height() * scale));
    }
}
