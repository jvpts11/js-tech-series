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
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The thing on a bench: a block standing on a floor of stone in a small world of its own, a machine working, idle or
 * jammed as its block entity is told, drawn again each frame so it moves as it does in a world; a creature standing;
 * anything else held up large. Dragging walks round it and the wheel brings it closer.
 */
final class StudioBench {

    private @Nullable StudioThing thing;
    private @Nullable GuideBlock.Scene scene;
    private SceneRenderer.@Nullable IScenePainter painter;
    private @Nullable GpuSurface surface;
    private @Nullable LivingEntity creature;
    private BenchState state = BenchState.WORKING;
    private float yaw = GuideBlock.SceneView.CORNER.yaw();
    private float pitch = GuideBlock.SceneView.CORNER.pitch();
    private float zoom = 1.0F;

    /** How far the floor reaches each way from the thing, and how tall the scene is drawn. */
    private static final int FLOOR = 3;
    private static final int SCENE_HEIGHT = 120;
    private static final String FLOOR_BLOCK = "minecraft:smooth_stone";
    /** How many degrees a pixel of dragging turns it, and how much nearer a turn of the wheel. */
    private static final float TURN = 0.8F;
    private static final float ZOOM_STEP = 1.1F;
    /** How large a thing held up is drawn, and a creature. */
    private static final float HELD = 4.0F;
    private static final int CREATURE = 30;
    private static final int ITEM = 16;
    /** How far a creature's turn is read from the bench's, as the inventory's drawing reads the pointer. */
    private static final float CREATURE_TURN = 20.0F;
    private static final float CREATURE_OFFSET = 0.0625F;

    /** Puts {@code shown} on the bench, or nothing for none, as it was seen last. */
    void show(final @Nullable StudioThing shown) {
        this.thing = shown;
        this.creature = null;
        this.rebuild();
        if (shown != null && shown.entity() != null && Minecraft.getInstance().level != null) {
            final Entity made = shown.entity().create(Minecraft.getInstance().level);
            this.creature = made instanceof LivingEntity living ? living : null;
        }
    }

    /** Whether the thing on it is a machine, whose states the bench shows. */
    boolean machine() {
        return this.thing != null && this.thing.kind() == ThingKind.MACHINES && this.scene != null;
    }

    /** Whether what is on it stands in a world: a block, or a creature. */
    boolean standing() {
        return this.scene != null || this.creature != null;
    }

    /** The state a machine on it is in. */
    BenchState state() {
        return this.state;
    }

    /** Has a machine on it be in {@code state}. */
    void state(final BenchState next) {
        if (next != this.state) {
            this.state = next;
            this.rebuild();
        }
    }

    /** Walks round it by a drag of the pointer. */
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

    /** Draws what is on it inside {@code area}. */
    void draw(final GuiGraphics g, final StudioLayout.Rect area) {
        if (this.scene != null && this.painter != null) {
            this.scene(g, area);
        } else if (this.creature != null) {
            InventoryScreen.renderEntityInInventoryFollowsAngle(g, area.x(), area.y(), area.right(), area.bottom(),
                    CREATURE, CREATURE_OFFSET, -this.yaw / CREATURE_TURN, this.pitch / CREATURE_TURN,
                    this.creature);
        } else if (this.thing != null && !this.thing.icon().isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(area.x() + area.width() / 2.0F, area.y() + area.height() / 2.0F, 0.0F);
            g.pose().scale(HELD * this.zoom, HELD * this.zoom, 1.0F);
            g.renderItem(this.thing.icon(), -ITEM / 2, -ITEM / 2);
            g.pose().popPose();
        }
    }

    /** Lets the picture on the graphics card go, for a studio being closed. */
    void close() {
        if (this.surface != null) {
            this.surface.close();
            this.surface = null;
        }
    }

    /* The scene, drawn again each frame into its picture, which is put on the screen. */
    private void scene(final GuiGraphics g, final StudioLayout.Rect area) {
        final double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        final int pixelsWide = Math.max(1, (int) Math.round(area.width() * guiScale));
        final int pixelsTall = Math.max(1, (int) Math.round(area.height() * guiScale));
        if (this.surface == null || this.surface.width() != pixelsWide || this.surface.height() != pixelsTall) {
            this.close();
            this.surface = new GpuSurface(pixelsWide, pixelsTall);
        }
        // What the screen has drawn so far goes onto it before the bench's own target is drawn into.
        g.flush();
        final GuideBlock.SceneView view = new GuideBlock.SceneView(this.yaw, this.pitch, this.zoom);
        final SceneProjection projection = new SceneProjection(this.scene, view, pixelsWide, pixelsTall);
        SceneRenderer.draw(this.surface, SceneRenderer.View.of(projection.middle(), view.yaw(), view.pitch(),
                (float) projection.scale()), this.painter);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        this.surface.blit(g, area.x(), area.y(), area.width(), area.height());
        RenderSystem.disableBlend();
    }

    /* A block's scene: the floor, and the block in the middle of it, a machine's block entity told its state. */
    private void rebuild() {
        this.scene = null;
        this.painter = null;
        if (this.thing == null || this.thing.block() == null || this.thing.kind() == ThingKind.FLUIDS) {
            return;
        }
        final List<GuideBlock.SceneBlock> blocks = new ArrayList<>();
        for (int x = 0; x < FLOOR; x++) {
            for (int z = 0; z < FLOOR; z++) {
                blocks.add(new GuideBlock.SceneBlock(x, 0, z, FLOOR_BLOCK, ""));
            }
        }
        final String data = this.thing.kind() == ThingKind.MACHINES ? this.state.data() : "";
        blocks.add(new GuideBlock.SceneBlock(FLOOR / 2, 1, FLOOR / 2,
                BuiltInRegistries.BLOCK.getKey(this.thing.block()).toString(), data));
        this.scene = new GuideBlock.Scene(blocks, GuideBlock.SceneView.CORNER, List.of(), false, SCENE_HEIGHT, "");
        this.painter = ScenePainters.of(this.scene);
    }
}
