/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.JsCore;
import dev.jstech.core.multipart.IFacePart;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Breaking a cable block takes out the one wire or part the player looks at and leaves the rest standing; the block
 * itself goes only with its last piece, and then gives that piece back as any block gives its drops.
 */
@EventBusSubscriber(modid = JsCore.MODID)
public final class CableBreaking {

    private CableBreaking() {
    }

    @SubscribeEvent
    public static void onBlockBreak(final BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getState().getBlock() instanceof CableBlock)) {
            return;
        }
        final BlockPos pos = event.getPos();
        if (!(level.getBlockEntity(pos) instanceof CableBlockEntity cable) || cable.pieces() < 2) {
            return;
        }
        final Player player = event.getPlayer();
        final CableBlockEntity.Aim aim = CableBlock.aimOf(level, pos, player);
        if (aim.isNothing()) {
            return;
        }
        event.setCanceled(true);
        final ItemStack back;
        if (aim.part() != null) {
            final IFacePart part = cable.removePart(aim.part());
            if (part == null) {
                return;
            }
            part.dropContents(level);
            back = part.partItem();
        } else {
            if (!cable.take(aim.wire().type())) {
                return;
            }
            back = aim.wire().type().stack();
        }
        level.playSound(null, pos, SoundType.WOOL.getBreakSound(), SoundSource.BLOCKS, 1.0F, 0.9F);
        if (!player.getAbilities().instabuild && !player.getInventory().add(back)) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), back);
        }
    }
}
