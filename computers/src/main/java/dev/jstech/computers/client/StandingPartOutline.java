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
import dev.jstech.core.content.IStandingParts;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

/**
 * The outline a part standing out of a device gets while the player looks at it, a stick in a case's port or a
 * drive's open tray: the {@link ButtonOutline part alone}, as the buttons of a front are outlined, so the player sees
 * that the part itself is what a click works. Asked before the outlines of the front, which only know its face.
 */
@EventBusSubscriber(modid = JsComputers.MODID, value = Dist.CLIENT)
public final class StandingPartOutline {

    private StandingPartOutline() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockHighlight(final RenderHighlightEvent.Block event) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        final BlockHitResult hit = event.getTarget();
        final BlockPos pos = hit.getBlockPos();
        final BlockState state = minecraft.level.getBlockState(pos);
        if (!(state.getBlock() instanceof IStandingParts device)) {
            return;
        }
        final List<AABB> parts = device.standingParts(state, minecraft.level, pos);
        final int part = DeviceFront.partAt(parts, pos, hit.getLocation());
        if (part < 0) {
            return;
        }
        final Vec3 camera = event.getCamera().getPosition();
        ButtonOutline.draw(event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.lines()),
                parts.get(part).move(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z));
        event.setCanceled(true);
    }
}
