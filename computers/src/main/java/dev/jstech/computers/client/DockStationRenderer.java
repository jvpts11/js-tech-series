/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.os.media.DockStationBlockEntity;
import dev.jstech.core.client.geo.LookGeoModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Draws the Dock Station: each tray's window shows the very disk the player put in, small, its face out; the stick
 * stands in the USB port while one is plugged in; the power ring is lit while a computer is on the other end of its
 * cable, and each tray's lamp while it holds a disk the computer can see.
 */
public final class DockStationRenderer extends GeoBlockRenderer<DockStationBlockEntity> {

    /** How big a disk is drawn in its tray's window, of its full size. */
    private static final float DISK_SCALE = 0.16F;

    public DockStationRenderer(final BlockEntityRendererProvider.Context context) {
        super(new LookGeoModel<>(ComputingLooks.DOCK_STATION));
        addRenderLayer(new TrayDisks(this));
    }

    @Override
    public void preRender(final PoseStack poseStack, final DockStationBlockEntity dock, final BakedGeoModel model,
                          final MultiBufferSource bufferSource, final VertexConsumer buffer, final boolean isReRender,
                          final float partialTick, final int packedLight, final int packedOverlay, final int colour) {
        super.preRender(poseStack, dock, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);
        final boolean linked = dock.ownerPos() != null;
        DeviceLamps.show(model, ComputingLooks.DOCK_POWER_LAMP, linked);
        for (int bay = 0; bay < DockStationBlockEntity.BAYS; bay++) {
            DeviceLamps.show(model, ComputingLooks.DOCK_BAY_LAMPS.get(bay), linked && !dock.disk(bay).isEmpty());
        }
        DeviceLamps.show(model, ComputingLooks.DOCK_STICK, !dock.mediaSlot().getStackInSlot(0).isEmpty());
    }

    /** The disks in the trays' windows, each at the bone that marks its tray. */
    private static final class TrayDisks extends BlockAndItemGeoLayer<DockStationBlockEntity> {

        TrayDisks(final DockStationRenderer renderer) {
            super(renderer);
        }

        @Override
        @Nullable
        protected ItemStack getStackForBone(final GeoBone bone, final DockStationBlockEntity dock) {
            final int bay = ComputingLooks.DOCK_BAYS.indexOf(bone.getName());
            if (bay < 0) {
                return null;
            }
            final ItemStack disk = dock.disk(bay);
            return disk.isEmpty() ? null : disk;
        }

        @Override
        protected void renderStackForBone(final PoseStack poseStack, final GeoBone bone, final ItemStack stack,
                                          final DockStationBlockEntity dock, final MultiBufferSource bufferSource,
                                          final float partialTick, final int packedLight, final int packedOverlay) {
            poseStack.pushPose();
            poseStack.scale(DISK_SCALE, DISK_SCALE, DISK_SCALE);
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            super.renderStackForBone(poseStack, bone, stack, dock, bufferSource, partialTick, packedLight,
                    packedOverlay);
            poseStack.popPose();
        }
    }
}
