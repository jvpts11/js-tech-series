/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jstech.core.client.model.kit.KitArmorModel;
import dev.jstech.core.multiblock.shape.CellSpace;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.SuitLockerEntity;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Vector3f;

/**
 * A suit locker's suit on its rack, as the approved locker draws it: each of its pieces only while the locker holds it,
 * a little smaller than worn, facing out of the locker. It is the game's own suit, each piece drawn as it is worn, so
 * a visor or a module fitted to one shows on it too. Where the rack stands is read from the airlock's locker
 * ({@link ShipLockers}).
 */
public final class SuitLockerRenderer implements BlockEntityRenderer<SuitLockerEntity> {

    private static final double HALF = 0.5;
    /* How far over its feet a living thing's model is drawn from, as the game's renderer of one sets it. */
    private static final float MODEL_ORIGIN = 1.501F;
    private static final float HALF_TURN = 180.0F;
    /* Each of the rack's places, and the slot its piece is worn in. */
    private static final Map<SuitLockerEntity.Place, EquipmentSlot> WORN = Map.of(SuitLockerEntity.Place.HELMET,
            EquipmentSlot.HEAD, SuitLockerEntity.Place.SUIT, EquipmentSlot.CHEST, SuitLockerEntity.Place.TROUSERS,
            EquipmentSlot.LEGS, SuitLockerEntity.Place.BOOTS, EquipmentSlot.FEET);

    /** Hands the game the locker's renderer, once, from the mod's client entry point. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> event.registerBlockEntityRenderer(
                ShipKitBlocks.SUIT_LOCKER_ENTITY.get(), context -> new SuitLockerRenderer()));
    }

    @Override
    public void render(final SuitLockerEntity locker, final float partialTick, final PoseStack poses,
                       final MultiBufferSource buffers, final int light, final int overlay) {
        if (locker.places().isEmpty()) {
            return;
        }
        final FormedPlace place = locker.places().getFirst();
        final Optional<ShipLockers.Rig> rig = ShipLockers.rigOf(place.model());
        if (rig.isEmpty()) {
            return;
        }
        // Where the rack stands from the locker's block, and which way the suit faces, as the model stands turned.
        final Vector3f rack = rig.get().rack();
        final Vector3f facing = rig.get().facing();
        final double[] at = new double[3];
        CellSpace.toWorld(new double[] {rack.x() - place.x(), rack.y() - place.y(), rack.z() - place.z()},
                place.turn(), at);
        final double[] ahead = new double[3];
        CellSpace.toWorld(new double[] {HALF + facing.x(), HALF + facing.y(), HALF + facing.z()}, place.turn(), ahead);
        final float body = (float) Math.toDegrees(Math.atan2(-(ahead[0] - HALF), ahead[2] - HALF));
        final float scale = rig.get().scale();
        poses.pushPose();
        poses.translate(at[0], at[1], at[2]);
        poses.scale(scale, scale, scale);
        // As the game turns and lays out a living thing facing that way before its model.
        poses.mulPose(Axis.YP.rotationDegrees(HALF_TURN - body));
        poses.scale(-1, -1, 1);
        poses.translate(0, -MODEL_ORIGIN, 0);
        for (final Map.Entry<SuitLockerEntity.Place, EquipmentSlot> piece : WORN.entrySet()) {
            final ItemStack stack = locker.piece(piece.getKey());
            if (!stack.isEmpty()) {
                KitArmorModel.drawStanding(stack, piece.getValue(), poses, light);
            }
        }
        poses.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(final SuitLockerEntity locker) {
        // The rack stands beside the locker's block, in the block before it.
        return true;
    }
}
