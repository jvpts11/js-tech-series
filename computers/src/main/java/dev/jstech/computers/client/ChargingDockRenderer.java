/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.block.DataWires;
import dev.jstech.computers.blockentity.ChargingDockBlockEntity;
import dev.jstech.computers.client.portable.MobileDisplay;
import dev.jstech.computers.client.portable.PortableBodies;
import dev.jstech.computers.portable.PortableBattery;
import dev.jstech.computers.portable.PortableItem;
import dev.jstech.computers.portable.Portables;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * Draws a Mobile Device Charging Dock as the one of its era, with the portable it holds standing in it by its own
 * model, its screen showing the battery filling, the bolt over it while it charges. The lamp is lit while a device
 * stands in it and charges or is full (the Legacy cradle's amber while charging, its green when full), and a hookup
 * shows behind it where its energy line comes in.
 */
public final class ChargingDockRenderer implements IKitDriver<ChargingDockBlockEntity> {

    private static final String[] HOOKUPS = BackHookups.bones(ComputingLooks.COMPUTER_HOOKUP);

    private ChargingDockRenderer() {
    }

    /** The renderer of the docks. */
    public static BlockEntityRenderer<ChargingDockBlockEntity> create(
            final BlockEntityRendererProvider.Context context) {
        return KitBlockEntityRenderer.<ChargingDockBlockEntity>look(ComputingLooks.CHARGING_DOCK)
                .driver(new ChargingDockRenderer())
                .extras(ChargingDockRenderer::device)
                .build();
    }

    /* Nothing on it moves: its lamps and hookups are bones shown and hidden. */
    @Override
    public void drive(final ChargingDockBlockEntity dock, final ModelAnimator animator, final double seconds) {
    }

    @Override
    public void move(final ChargingDockBlockEntity dock, final ModelPose pose, final double seconds) {
        final boolean full = full(dock.held());
        final boolean charging = dock.isCharging();
        if (dock.era().isAtMost(HardwareEra.LEGACY)) {
            DeviceLamps.show(pose, ComputingLooks.CHARGING_DOCK_LAMP, charging);
            DeviceLamps.show(pose, ComputingLooks.CHARGING_DOCK_FULL_LAMP, full);
        } else {
            DeviceLamps.show(pose, ComputingLooks.CHARGING_DOCK_LAMP, charging || full);
        }
        final boolean[] plugged = BackHookups.plugged(dock.getLevel(), dock.getBlockPos(),
                dock.getBlockState().getValue(HorizontalDirectionalBlock.FACING), wire -> !DataWires.isNetwork(wire));
        for (int k = 0; k < HOOKUPS.length; k++) {
            DeviceLamps.show(pose, HOOKUPS[k], plugged[k]);
        }
    }

    @Override
    public boolean movesBones() {
        return true;
    }

    /* The portable it holds, on its seat, its screen showing the battery. */
    private static void device(final ChargingDockBlockEntity dock, final KitInstance instance,
                               final PoseStack poses, final MultiBufferSource buffers, final int light,
                               final int overlay, final float partialTick) {
        final ItemStack held = dock.held();
        if (!(held.getItem() instanceof PortableItem item)) {
            return;
        }
        final int percent = PortableBattery.percent(Portables.battery(held, item.model()),
                PortableBattery.capacity(item.model()));
        final boolean filling = dock.isCharging();
        poses.pushPose();
        PortableBodies.seat(poses, dock.era(), item.model());
        PortableBodies.draw(held, item.model(), ItemDisplayContext.NONE, poses, buffers, light, overlay,
                new Docked(dock.getBlockPos()), (g, width, height, partial) -> MobileDisplay.charging(g, item.model(),
                        percent, filling));
        poses.popPose();
    }

    /* Whether the device in {@code held} has a full battery; false for none. */
    private static boolean full(final ItemStack held) {
        return held.getItem() instanceof PortableItem item
                && Portables.battery(held, item.model()) >= PortableBattery.capacity(item.model());
    }

    /* The picture on the screen of the device in the dock at {@code pos}. */
    private record Docked(BlockPos pos) {
    }
}
