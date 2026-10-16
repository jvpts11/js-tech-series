/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import dev.jstech.core.machine.MachineWrench;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * A wrench of any mod on a part of a cable or a space of parts: used, it turns the part looked at, when it turns; used
 * sneaking, it takes the part off into the player's hands. A part that does not turn is used as by hand, and a wire
 * looked at is left to the wrench's own work on wires. A place the player may not touch is left alone, as claims say.
 */
public final class PartWrenching {

    private PartWrenching() {
    }

    /** Hands the game's bus the wrench's work on parts, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, PartWrenching::onRightClick);
    }

    /** The place of the part {@code player} looks at in the space at {@code pos}, or none. */
    public static int aimed(final Level level, final BlockPos pos, final Player player) {
        if (!(level.getBlockEntity(pos) instanceof ISpaceHolder holder)) {
            return ISpaceHolder.NO_PART;
        }
        final Vec3 start = player.getEyePosition();
        final Vec3 end = start.add(player.getViewVector(1.0F).scale(player.blockInteractionRange() + 1.0));
        return holder.aimedPart(start, end);
    }

    /**
     * Takes the part in {@code slot} of the space at {@code pos} off into {@code player}'s hands, as a wrench does;
     * on the server.
     *
     * @return whether a part came off
     */
    public static boolean takeOff(final ServerLevel level, final BlockPos pos, final int slot, final Player player) {
        if (!(level.getBlockEntity(pos) instanceof ISpaceHolder holder)) {
            return false;
        }
        final ItemStack back = SpaceBreaking.takeOut(level, holder.parts(), slot);
        if (back.isEmpty()) {
            return false;
        }
        if (!player.getInventory().add(back)) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), back);
        }
        return true;
    }

    private static void onRightClick(final PlayerInteractEvent.RightClickBlock event) {
        final Player player = event.getEntity();
        final Level level = event.getLevel();
        if (event.getHand() != InteractionHand.MAIN_HAND || !MachineWrench.isWrench(event.getItemStack())) {
            return;
        }
        final int slot = aimed(level, event.getPos(), player);
        if (slot == ISpaceHolder.NO_PART || !(level.getBlockEntity(event.getPos()) instanceof ISpaceHolder holder)) {
            return;
        }
        if (player.isShiftKeyDown()) {
            if (level instanceof ServerLevel server) {
                takeOff(server, event.getPos(), slot, player);
            }
        } else if (holder.parts().canTurn(slot)) {
            if (!level.isClientSide()) {
                holder.parts().turn(slot);
            }
        } else {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
    }
}
