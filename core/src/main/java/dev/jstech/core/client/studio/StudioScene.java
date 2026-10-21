/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.jstech.core.api.client.GpuSurface;
import dev.jstech.core.client.guide.ScenePainters;
import dev.jstech.core.client.model.kit.SceneRenderer;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.SceneProjection;
import dev.jstech.core.gui.layout.StudioLayout;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A few blocks drawn in a small world of their own, as a manual draws its scenes: turned round and brought near by the
 * pointer, drawn again each frame so what moves in the world moves here. The studio's bench, its structure caught and
 * the land round its dearest machines are drawn with it.
 */
final class StudioScene {

    private @Nullable GuideBlock.Scene scene;
    private SceneRenderer.@Nullable IScenePainter painter;
    private @Nullable GpuSurface surface;
    private float yaw = GuideBlock.SceneView.CORNER.yaw();
    private float pitch = GuideBlock.SceneView.CORNER.pitch();
    private float zoom = 1.0F;

    /** How many degrees a pixel of dragging turns it, and how much nearer a turn of the wheel. */
    private static final float TURN = 0.8F;
    private static final float ZOOM_STEP = 1.1F;
    private static final int SCENE_HEIGHT = 120;

    /** Shows {@code blocks}, or nothing for none. */
    void show(final List<GuideBlock.SceneBlock> blocks) {
        if (blocks.isEmpty()) {
            this.scene = null;
            this.painter = null;
            return;
        }
        this.scene = new GuideBlock.Scene(blocks, GuideBlock.SceneView.CORNER, List.of(), false, SCENE_HEIGHT, "");
        this.painter = ScenePainters.of(this.scene);
    }

    /** Whether it shows anything. */
    boolean showing() {
        return this.scene != null;
    }

    /** The scene shown, if any. */
    @Nullable
    GuideBlock.Scene scene() {
        return this.scene;
    }

    /** Turns it round by a drag of the pointer. */
    void turn(final double dx, final double dy) {
        this.yaw += (float) dx * TURN;
        this.pitch = Math.max(-GuideBlock.SceneView.MAX_PITCH, Math.min(GuideBlock.SceneView.MAX_PITCH,
                this.pitch + (float) dy * TURN));
    }

    /** Brings it nearer, or farther, by turns of the wheel. */
    void zoom(final double turns) {
        this.zoom = Math.max(GuideBlock.SceneView.MIN_ZOOM, Math.min(GuideBlock.SceneView.MAX_ZOOM,
                this.zoom * (float) Math.pow(ZOOM_STEP, turns)));
    }

    /** How a block of it stands on the screen, inside {@code area}: for marks drawn over it. */
    SceneProjection projection(final StudioLayout.Rect area) {
        return new SceneProjection(this.scene, view(), area.width(), area.height());
    }

    /** Draws it into {@code area}, its picture made again first. */
    void draw(final GuiGraphics g, final StudioLayout.Rect area) {
        if (this.scene == null || this.painter == null) {
            return;
        }
        final double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        final int pixelsWide = Math.max(1, (int) Math.round(area.width() * guiScale));
        final int pixelsTall = Math.max(1, (int) Math.round(area.height() * guiScale));
        if (this.surface == null || this.surface.width() != pixelsWide || this.surface.height() != pixelsTall) {
            this.close();
            this.surface = new GpuSurface(pixelsWide, pixelsTall);
        }
        // What the screen has drawn so far goes onto it before the scene's own target is drawn into.
        g.flush();
        final GuideBlock.SceneView view = view();
        final SceneProjection projection = new SceneProjection(this.scene, view, pixelsWide, pixelsTall);
        SceneRenderer.draw(this.surface, SceneRenderer.View.of(projection.middle(), view.yaw(), view.pitch(),
                (float) projection.scale()), this.painter);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        this.surface.blit(g, area.x(), area.y(), area.width(), area.height());
        RenderSystem.disableBlend();
    }

    /** Lets its picture on the graphics card go. */
    void close() {
        if (this.surface != null) {
            this.surface.close();
            this.surface = null;
        }
    }

    /**
     * The blocks of {@code level} between {@code from} and {@code to}, air left out, as a scene holds them: at most a
     * scene's grid along each side, from the lower corner.
     */
    static List<GuideBlock.SceneBlock> blocksOf(final ClientLevel level, final BlockPos from, final BlockPos to) {
        final BlockPos min = new BlockPos(Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()),
                Math.min(from.getZ(), to.getZ()));
        final int grid = GuideBlock.Scene.GRID;
        final int sizeX = Math.min(grid, Math.abs(from.getX() - to.getX()) + 1);
        final int sizeY = Math.min(grid, Math.abs(from.getY() - to.getY()) + 1);
        final int sizeZ = Math.min(grid, Math.abs(from.getZ() - to.getZ()) + 1);
        final List<GuideBlock.SceneBlock> out = new ArrayList<>();
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    final BlockState state = level.getBlockState(min.offset(x, y, z));
                    if (!state.isAir()) {
                        out.add(new GuideBlock.SceneBlock(x, y, z, BlockStateParser.serialize(state), ""));
                    }
                }
            }
        }
        return out;
    }

    private GuideBlock.SceneView view() {
        return new GuideBlock.SceneView(this.yaw, this.pitch, this.zoom);
    }
}
