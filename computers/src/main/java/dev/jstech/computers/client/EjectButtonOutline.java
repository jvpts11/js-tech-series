/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.DeviceFront;
import dev.jstech.computers.os.media.EjectButton;
import dev.jstech.computers.os.media.ITrayBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

/**
 * The outline the eject button of a drive or a Pattern Encoder gets while the player looks at it: the
 * {@link ButtonOutline button alone}, measured on the front as a click is, so it stands exactly where a click opens
 * and closes the tray. Looked at anywhere else, the device is outlined whole.
 */
@EventBusSubscriber(modid = JsComputers.MODID, value = Dist.CLIENT)
public final class EjectButtonOutline {

    private EjectButtonOutline() {
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
        if (!(state.getBlock() instanceof ITrayBlock device)) {
            return;
        }
        final EjectButton button = device.ejectButton();
        if (button == null || !DeviceFront.presses(button, state, pos, hit, viewer.getEyePosition())) {
            return;
        }
        final Vec3 camera = event.getCamera().getPosition();
        ButtonOutline.draw(event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.lines()),
                DeviceFront.box(button, state.getValue(HorizontalDirectionalBlock.FACING))
                        .move(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z));
        event.setCanceled(true);
    }
}
