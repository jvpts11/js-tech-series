/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.multiblock;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.client.model.kit.KitDamage;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.model.kit.KitRenderer;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.FormedParts;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.shape.CellSpace;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.FormedShape;
import dev.jstech.core.multiblock.shape.FormedShapes;
import java.util.Optional;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/**
 * Draws the whole formed model of a merged block told to draw it ({@link FormedParts#drawFrom}): a piece no owner
 * draws any more, a rocket's spent stage falling on its own, is drawn from that one block of each of its parts, the
 * model standing where its shape file and the block's place in it put it, turned as its structure stands, lit by the
 * light round the block.
 */
public final class FormedPartRenderer implements BlockEntityRenderer<FormedPartBlockEntity> {

    /* How far it is drawn from, in blocks. */
    private static final int SEEN_FROM = 256;

    public FormedPartRenderer(final BlockEntityRendererProvider.Context context) {
        // Nothing to keep: each model is an instance of its own, kept with the block that draws it.
    }

    @Override
    public void render(final FormedPartBlockEntity part, final float partialTick, final PoseStack poses,
                       final MultiBufferSource buffers, final int light, final int overlay) {
        final FormedPlace place = part.place();
        final Level level = part.getLevel();
        if (!part.drawsPart() || place == null || level == null) {
            return;
        }
        final Optional<FormedShape> shape = FormedShapes.get(level, place.model());
        final Optional<KitModel> model = KitModels.get(place.model());
        if (shape.isEmpty() || model.isEmpty()) {
            return;
        }
        final double[] origin = originFrom(shape.get(), place);
        final KitInstance instance = KitAnimations.of(part, model.get());
        // Drawn as the strikes laid on it left it, the instance moving on as it was.
        final KitModel drawn = KitDamage.of(model.get(), KitDamage.strikes(level, part.getBlockPos(), place));
        final int lit = KitBlockEntityRenderer.lightAround(level, part.getBlockPos(), light);
        poses.pushPose();
        poses.translate(origin[0], origin[1], origin[2]);
        poses.mulPose(turn(place.turn()));
        if (drawn != model.get()) {
            KitRenderer.draw(instance, drawn, ModelDefinition.Variant.NONE, part, poses, buffers, lit, overlay, null,
                    level, true);
        } else {
            KitRenderer.draw(instance, ModelDefinition.Variant.NONE, part, poses, buffers, lit, overlay, null, level,
                    true);
        }
        poses.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(final FormedPartBlockEntity part) {
        return part.drawsPart();
    }

    /* Round its model however it stands, so the model is drawn while any of it is in sight. */
    @Override
    public AABB getRenderBoundingBox(final FormedPartBlockEntity part) {
        final BlockPos pos = part.getBlockPos();
        final FormedPlace place = part.place();
        final Level level = part.getLevel();
        if (!part.drawsPart() || place == null || level == null) {
            return new AABB(pos);
        }
        final Optional<FormedShape> shape = FormedShapes.get(level, place.model());
        final Optional<KitModel> model = KitModels.get(place.model());
        if (shape.isEmpty() || model.isEmpty()) {
            return new AABB(pos);
        }
        final double[] origin = originFrom(shape.get(), place);
        final AABB box = model.get().bounds();
        final double reach = Math.max(Math.max(Math.max(Math.abs(box.minX), Math.abs(box.maxX)),
                Math.max(Math.abs(box.minZ), Math.abs(box.maxZ))), Math.max(Math.abs(box.minY), Math.abs(box.maxY)));
        final double x = pos.getX() + origin[0];
        final double y = pos.getY() + origin[1];
        final double z = pos.getZ() + origin[2];
        return new AABB(x - reach, y - reach, z - reach, x + reach, y + reach, z + reach).minmax(new AABB(pos));
    }

    @Override
    public int getViewDistance() {
        return SEEN_FROM;
    }

    /*
     * Where its model's own origin stands from the lowest corner of the block {@code place} is, as its structure
     * stands: the model's origin is the shape file's origin taken back from the block's place in the model.
     */
    private static double[] originFrom(final FormedShape shape, final FormedPlace place) {
        final double[] corner = shape.origin();
        final double[] local = {-corner[0] - place.x(), -corner[1] - place.y(), -corner[2] - place.z()};
        final double[] out = new double[local.length];
        CellSpace.toWorld(local, place.turn(), out);
        return out;
    }

    /* The model's axes as the world has them, its structure standing {@code orientation}. */
    private static Matrix4f turn(final Orientation orientation) {
        final Matrix4f turn = new Matrix4f().zero();
        turn.set(orientation.xFrom(), 0, orientation.xSign());
        turn.set(orientation.yFrom(), 1, orientation.ySign());
        turn.set(orientation.zFrom(), 2, orientation.zSign());
        turn.m33(1);
        return turn;
    }
}
