/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.AbstractSmallComputerBlock;
import dev.jstech.computers.block.DeviceFront;
import dev.jstech.computers.block.MainframeBlock;
import dev.jstech.computers.blockentity.AbstractSmallComputerBlockEntity;
import dev.jstech.computers.blockentity.CaseFront;
import dev.jstech.computers.hardware.BayDriveSpec;
import dev.jstech.computers.hardware.CaseFrontLayout;
import dev.jstech.computers.hardware.FrontArea;
import dev.jstech.computers.os.media.EjectButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The outline a part of a computer's front gets while the player looks at it, the {@link ButtonOutline part alone}:
 * the power button of a small computer's case or of a Mainframe's cabinet, the eject button of a drive in a bay, a
 * USB port and the hot-swap bay, each measured on the front as a click is, so it stands exactly where a click works
 * it. Looked at anywhere else, the machine is outlined whole.
 */
@EventBusSubscriber(modid = JsComputers.MODID, value = Dist.CLIENT)
public final class CaseFrontOutline {

    /*
     * How far each kind of part stands from the face of the block, in sixty-fourths, nearest and furthest: a case's
     * power button stands out of the face, a Mainframe's out of its sill, and the rest lie on the face.
     */
    private static final int CASE_BUTTON_NEAR = -2;
    private static final int CASE_BUTTON_FAR = 1;
    private static final int SILL_BUTTON_NEAR = -1;
    private static final int SILL_BUTTON_FAR = 2;
    private static final int FLAT_NEAR = 0;
    private static final int FLAT_FAR = 1;

    private CaseFrontOutline() {
    }

    @SubscribeEvent
    public static void onBlockHighlight(final RenderHighlightEvent.Block event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Entity viewer = minecraft.getCameraEntity();
        if (minecraft.level == null || viewer == null) {
            return;
        }
        final BlockHitResult hit = event.getTarget();
        final BlockPos pos = hit.getBlockPos();
        final BlockState state = minecraft.level.getBlockState(pos);
        if (!(state.getBlock() instanceof AbstractSmallComputerBlock<?>) && !(state.getBlock() instanceof MainframeBlock)) {
            return;
        }
        final Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        final double[] at = DeviceFront.point(facing, pos, hit, viewer.getEyePosition(), 0);
        if (at == null) {
            return;
        }
        final AABB box;
        if (state.getBlock() instanceof AbstractSmallComputerBlock<?> block
                && minecraft.level.getBlockEntity(pos) instanceof AbstractSmallComputerBlockEntity computer) {
            box = casePart(block.frontLayout(), computer.front(), facing, at);
        } else {
            box = MainframeBlock.POWER_BUTTON.contains(at[0], at[1])
                    ? DeviceFront.box(MainframeBlock.POWER_BUTTON, SILL_BUTTON_NEAR, SILL_BUTTON_FAR, facing) : null;
        }
        if (box == null) {
            return;
        }
        final Vec3 camera = event.getCamera().getPosition();
        ButtonOutline.draw(event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.lines()),
                box.move(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z));
        event.setCanceled(true);
    }

    /*
     * The part of a case's front at that point, in the order a click finds them: the power button, the eject button of
     * a drive in a bay, a port, then the hot-swap bay; null where none is.
     */
    @Nullable
    private static AABB casePart(final CaseFrontLayout layout, final CaseFront front, final Direction facing,
                                 final double[] at) {
        if (layout.power().contains(at[0], at[1])) {
            return DeviceFront.box(layout.power(), CASE_BUTTON_NEAR, CASE_BUTTON_FAR, facing);
        }
        final int bays = Math.min(front.bays(), layout.bays().size());
        for (int bay = 0; bay < bays; bay++) {
            final BayDriveSpec.Kind kind = front.kind(bay);
            final EjectButton button = kind == null ? null : layout.ejectButton(bay, kind);
            if (button != null && button.pressedAt(at[0], at[1])) {
                return DeviceFront.box(button, facing);
            }
        }
        final int ports = Math.min(front.ports(), layout.ports().size());
        for (int port = 0; port < ports; port++) {
            final FrontArea socket = layout.ports().get(port);
            if (socket.contains(at[0], at[1])) {
                return DeviceFront.box(socket, FLAT_NEAR, FLAT_FAR, facing);
            }
        }
        for (int bay = 0; bay < bays; bay++) {
            final FrontArea area = layout.bays().get(bay);
            if (front.kind(bay) == BayDriveSpec.Kind.HOT_SWAP && area.contains(at[0], at[1])) {
                return DeviceFront.box(area, FLAT_NEAR, FLAT_FAR, facing);
            }
        }
        return null;
    }
}
