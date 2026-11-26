/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

/**
 * A bucket, or any container of a liquid, used on a port of a machine: a full one empties into a liquid inlet and an
 * empty one fills from a liquid outlet, through the machine's own tank, and none ever pours into the world or takes
 * from it, on a liquid port or on a port of gas or any other matter. A full bucket used on a Liquid Inlet poured its
 * water out beside the machine.
 */
public final class PortBuckets {

    private PortBuckets() {
    }

    /**
     * What a block standing as a port of {@code kind} does with {@code stack} used on its face {@code side}: a liquid's
     * container is taken whole there, on the player's game too, and moves its liquid through the port on the server;
     * anything else, and a block that is no port, passes on to the block's own use.
     */
    public static ItemInteractionResult use(final ItemStack stack, @Nullable final PortKind kind, final Level level,
                                            final BlockPos pos, final Player player, final InteractionHand hand,
                                            final Direction side) {
        if (kind == null || kind.resource() != PortKind.Resource.FLUID && kind.resource() != PortKind.Resource.MATTER
                || FluidUtil.getFluidHandler(stack).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide() && kind.resource() == PortKind.Resource.FLUID) {
            FluidUtil.interactWithFluidHandler(player, hand, level, pos, side);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
}
