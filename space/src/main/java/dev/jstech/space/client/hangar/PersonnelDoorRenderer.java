/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.hangar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jstech.space.hangar.HangarBlocks;
import dev.jstech.space.hangar.PersonnelDoorBlock;
import dev.jstech.space.hangar.PersonnelDoorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws a personnel door's leaf of the Rocket Hangar turning about its hinge as it swings, as the approved storyboard
 * has it: the shut leaf's model, turned toward the open one by how far its block entity says it has swung, the way
 * its block faces.
 */
public final class PersonnelDoorRenderer implements BlockEntityRenderer<PersonnelDoorBlockEntity> {

    /*
     * The hinge, in blocks as the door faces north: the shut leaf turned a quarter turn about it, its far edge toward
     * the room, stands as the open leaf's model draws it.
     */
    private static final double HINGE_X = 1.5 / 16;
    private static final double HINGE_Z = 8.0 / 16;
    private static final double HALF = 0.5;
    private static final float HALF_TURN = 180;
    private static final float WHOLE_TURN = 360;

    public PersonnelDoorRenderer(final BlockEntityRendererProvider.Context context) {
        // Nothing to keep: the leaf's model is drawn as it is.
    }

    /** Hands the mod's bus the renderer, once, from the mod's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> event.registerBlockEntityRenderer(
                HangarBlocks.PERSONNEL_DOOR_ENTITY.get(), PersonnelDoorRenderer::new));
    }

    @Override
    public void render(final PersonnelDoorBlockEntity door, final float partialTick, final PoseStack poses,
                       final MultiBufferSource buffers, final int light, final int overlay) {
        final BlockState state = door.getBlockState();
        if (!state.is(HangarBlocks.PERSONNEL_DOOR.get())) {
            return;
        }
        // The shut leaf as it faces north, turned the way the block faces as its model's state turns it.
        final BlockState shut = state.setValue(PersonnelDoorBlock.OPEN, false)
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        final float facing = (state.getValue(HorizontalDirectionalBlock.FACING).toYRot() + HALF_TURN) % WHOLE_TURN;
        final BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        poses.pushPose();
        poses.translate(HALF, 0, HALF);
        poses.mulPose(Axis.YP.rotationDegrees(-facing));
        poses.translate(-HALF, 0, -HALF);
        poses.translate(HINGE_X, 0, HINGE_Z);
        poses.mulPose(Axis.YP.rotationDegrees((float) -door.angle(partialTick)));
        poses.translate(-HINGE_X, 0, -HINGE_Z);
        blocks.getModelRenderer().renderModel(poses.last(), buffers.getBuffer(RenderType.cutout()), shut,
                blocks.getBlockModel(shut), 1, 1, 1, light, overlay, ModelData.EMPTY, RenderType.cutout());
        poses.popPose();
    }
}
