/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.os.media.DockStationBlockEntity;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.IKitExtras;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitRenderer;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the Dock Station: each tray's window shows the very disk the player put in, small, its face out; the stick
 * stands in the USB port while one is plugged in; the power ring is lit while a computer is on the other end of its
 * cable, and each tray's lamp while it holds a disk the computer can see. Nothing on it moves by a clip.
 */
public final class DockStationRenderer implements IKitDriver<DockStationBlockEntity>,
        IKitExtras<DockStationBlockEntity> {

    /** How big a disk is drawn in its tray's window, of its full size. */
    private static final float DISK_SCALE = 0.16F;

    private DockStationRenderer() {
    }

    /** The renderer of the Dock Station. */
    public static BlockEntityRenderer<DockStationBlockEntity> create(
            final BlockEntityRendererProvider.Context context) {
        final DockStationRenderer drawn = new DockStationRenderer();
        return KitBlockEntityRenderer.<DockStationBlockEntity>look(ComputingLooks.DOCK_STATION)
                .driver(drawn)
                .extras(drawn)
                .build();
    }

    @Override
    public void drive(final DockStationBlockEntity dock, final ModelAnimator animator, final double seconds) {
    }

    @Override
    public void move(final DockStationBlockEntity dock, final ModelPose pose, final double seconds) {
        final boolean linked = dock.ownerPos() != null;
        DeviceLamps.show(pose, ComputingLooks.DOCK_POWER_LAMP, linked);
        for (int bay = 0; bay < DockStationBlockEntity.BAYS; bay++) {
            DeviceLamps.show(pose, ComputingLooks.DOCK_BAY_LAMPS.get(bay), linked && !dock.disk(bay).isEmpty());
        }
    }

    @Override
    public boolean movesBones() {
        return true;
    }

    /*
     * The disks in the trays' windows, each at the bone that marks its tray; and the stick in the port, the stick's
     * own item as the Pattern Encoder draws it, lying as its port does.
     */
    @Override
    public void draw(final DockStationBlockEntity dock, final KitInstance instance, final PoseStack poses,
                     final MultiBufferSource buffers, final int light, final int overlay, final float partialTick) {
        for (int bay = 0; bay < DockStationBlockEntity.BAYS; bay++) {
            final ItemStack disk = dock.disk(bay);
            if (disk.isEmpty()) {
                continue;
            }
            poses.pushPose();
            if (KitRenderer.toBone(instance, ComputingLooks.DOCK_BAYS.get(bay), poses)) {
                poses.scale(DISK_SCALE, DISK_SCALE, DISK_SCALE);
                poses.mulPose(Axis.YP.rotationDegrees(180));
                Minecraft.getInstance().getItemRenderer().renderStatic(disk, ItemDisplayContext.NONE, light, overlay,
                        poses, buffers, dock.getLevel(), 0);
            }
            poses.popPose();
        }
        BayMedium.draw(dock.mediaSlot().getStackInSlot(0), true, instance, poses, buffers, light, overlay,
                dock.getLevel());
    }
}
