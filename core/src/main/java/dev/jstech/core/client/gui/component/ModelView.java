/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A 3D view of a block, a few blocks, an item or an entity, turned by dragging, brought closer by the wheel, and seen a
 * layer at a time with the layers button; the reset button turns it back. What it shows is read every frame.
 */
public final class ModelView extends UiComponent {

    private final Supplier<Subject> subject;
    private float yaw = START_YAW;
    private float pitch = START_PITCH;
    private float zoom = 1.0f;
    private int layers = Integer.MAX_VALUE;
    private boolean turning;
    private boolean dragging;
    private double lastX;
    private double lastY;

    private static final float START_YAW = 225.0f;
    private static final float START_PITCH = 30.0f;
    private static final float MOST_ZOOM = 4.0f;
    private static final float LEAST_ZOOM = 0.25f;
    private static final float TURN_SPEED = 0.6f;
    private static final int BUTTON = 16;
    private static final float DEPTH = 150.0f;

    public ModelView(final Supplier<Subject> subject) {
        this.subject = Objects.requireNonNull(subject, "subject");
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        ctx.skin().chartGround(g, x(), y(), width(), height());
        if (turning) {
            yaw += TURN_SPEED;
        }
        Draw.pushScissor(g, x() + 1, y() + 1, right() - 1, bottom() - 1);
        try {
            draw(g, subject.get());
        } finally {
            Draw.popScissor(g);
        }
        buttons(g, ctx);
    }

    @Override
    public boolean mouseClicked(final double mx, final double my, final int button) {
        final int bx = right() - 3 * (BUTTON + 2) - 2;
        if (my >= y() + 2 && my < y() + 2 + BUTTON && mx >= bx) {
            final int which = (int) ((mx - bx) / (BUTTON + 2));
            switch (which) {
                case 0 -> turning = !turning;
                case 1 -> layers = layers == Integer.MAX_VALUE ? 1 : layers + 1 > height(subject.get())
                        ? Integer.MAX_VALUE : layers + 1;
                default -> {
                    yaw = START_YAW;
                    pitch = START_PITCH;
                    zoom = 1.0f;
                    layers = Integer.MAX_VALUE;
                }
            }
            return true;
        }
        dragging = true;
        lastX = mx;
        lastY = my;
        return true;
    }

    @Override
    public boolean mouseDragged(final double mx, final double my, final int button) {
        if (!dragging) {
            return false;
        }
        yaw += (float) (mx - lastX);
        pitch = Math.max(-90.0f, Math.min(90.0f, pitch + (float) (my - lastY)));
        lastX = mx;
        lastY = my;
        return true;
    }

    @Override
    public boolean mouseReleased(final double mx, final double my, final int button) {
        dragging = false;
        return true;
    }

    @Override
    public boolean mouseScrolled(final double mx, final double my, final double delta) {
        zoom = Math.max(LEAST_ZOOM, Math.min(MOST_ZOOM, zoom * (delta > 0 ? 1.1f : 1 / 1.1f)));
        return true;
    }

    private void draw(final GuiGraphics g, final Subject shown) {
        final PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x() + width() / 2.0f, y() + height() / 2.0f, DEPTH);
        final float base = Math.min(width(), height()) * 0.45f * zoom;
        RenderSystem.enableDepthTest();
        try {
            switch (shown) {
                case Subject.Blocks blocks -> drawBlocks(g, pose, blocks.states(), base);
                case Subject.Item item -> drawItem(g, pose, item.stack(), base);
                case Subject.Creature creature -> drawEntity(g, creature.entity(), base);
                case Subject.Nothing ignored -> {
                }
            }
        } finally {
            pose.popPose();
            Lighting.setupFor3DItems();
        }
    }

    private void drawBlocks(final GuiGraphics g, final PoseStack pose, final Map<BlockPos, BlockState> states,
                            final float base) {
        if (states.isEmpty()) {
            return;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (final BlockPos pos : states.keySet()) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        final float span = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ)) + 1;
        final float scale = base / span;
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-(minX + maxX + 1) / 2.0f, -(minY + maxY + 1) / 2.0f, -(minZ + maxZ + 1) / 2.0f);
        Lighting.setupForEntityInInventory();
        for (final Map.Entry<BlockPos, BlockState> placed : states.entrySet()) {
            final BlockPos pos = placed.getKey();
            if (pos.getY() - minY >= layers || placed.getValue().getRenderShape() != RenderShape.MODEL) {
                continue;
            }
            pose.pushPose();
            pose.translate(pos.getX(), pos.getY(), pos.getZ());
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(placed.getValue(), pose, g.bufferSource(),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        g.flush();
    }

    private void drawItem(final GuiGraphics g, final PoseStack pose, final ItemStack stack, final float base) {
        if (stack.isEmpty()) {
            return;
        }
        pose.scale(base * 1.4f, -base * 1.4f, base * 1.4f);
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        Lighting.setupForEntityInInventory();
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose, g.bufferSource(),
                Minecraft.getInstance().level, 0);
        g.flush();
    }

    private void drawEntity(final GuiGraphics g, final Entity entity, final float base) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        final float scale = base / Math.max(1.0f, Math.max(living.getBbHeight(), living.getBbWidth()));
        final Quaternionf turn = new Quaternionf().rotateZ((float) Math.PI)
                .rotateX((float) Math.toRadians(pitch)).rotateY((float) Math.toRadians(yaw));
        InventoryScreen.renderEntityInInventory(g, x() + width() / 2.0f, y() + height() / 2.0f + scale
                * living.getBbHeight() / 2.0f, scale, new Vector3f(), turn, null, living);
    }

    private void buttons(final GuiGraphics g, final UiContext ctx) {
        final int by = y() + 2;
        int bx = right() - 3 * (BUTTON + 2) - 2;
        for (int i = 0; i < 3; i++) {
            ctx.skin().button(g, ctx.font(), bx, by, BUTTON, BUTTON, "", ctx.over(bx, by, BUTTON, BUTTON),
                    i == 0 && turning, false);
            glyph(g, ctx, i, bx + 4, by + 4);
            bx += BUTTON + 2;
        }
    }

    /* The marks of the three buttons: an arrow going round, three layers, a cross. */
    private static void glyph(final GuiGraphics g, final UiContext ctx, final int which, final int x, final int y) {
        final int ink = ctx.skin().text();
        switch (which) {
            case 0 -> {
                Draw.outline(g, x + 1, y + 1, 6, 6, ink);
                g.fill(x + 5, y, x + 8, y + 1, ink);
            }
            case 1 -> {
                g.fill(x, y + 1, x + 8, y + 2, ink);
                g.fill(x, y + 4, x + 8, y + 5, ink);
                g.fill(x, y + 7, x + 8, y + 8, ink);
            }
            default -> {
                for (int i = 0; i < 8; i++) {
                    g.fill(x + i, y + i, x + i + 1, y + i + 1, ink);
                    g.fill(x + 7 - i, y + i, x + 8 - i, y + i + 1, ink);
                }
            }
        }
    }

    private static int height(final Subject shown) {
        if (shown instanceof Subject.Blocks blocks && !blocks.states().isEmpty()) {
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (final BlockPos pos : blocks.states().keySet()) {
                min = Math.min(min, pos.getY());
                max = Math.max(max, pos.getY());
            }
            return max - min + 1;
        }
        return 1;
    }

    /** What a 3D view shows. */
    public sealed interface Subject {

        /** Nothing. */
        Subject NOTHING = new Nothing();

        /** One block. */
        static Subject block(final BlockState state) {
            return new Blocks(Map.of(BlockPos.ZERO, state));
        }

        /**
         * A few blocks, each where it stands.
         *
         * @param states the blocks by their places
         */
        record Blocks(Map<BlockPos, BlockState> states) implements Subject {

            public Blocks {
                states = Map.copyOf(states);
            }
        }

        /**
         * An item.
         *
         * @param stack the item
         */
        record Item(ItemStack stack) implements Subject {

            public Item {
                stack = stack.copy();
            }
        }

        /**
         * An entity.
         *
         * @param entity the entity
         */
        record Creature(Entity entity) implements Subject {
        }

        /** Nothing. */
        record Nothing() implements Subject {
        }
    }
}
