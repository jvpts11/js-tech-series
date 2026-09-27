/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * How the client finds what a menu was opened on. The server opens a menu on a block by writing the block's position
 * after it; the client reads it back and takes the block entity standing there in its own world.
 */
public final class MenuOpening {

    private MenuOpening() {
    }

    /**
     * The block entity of type {@code type} at the position the server wrote.
     *
     * @throws IllegalStateException when the client has no such block entity there
     */
    public static <E extends BlockEntity> E blockEntity(final Inventory inventory, final RegistryFriendlyByteBuf buf,
                                                        final Class<E> type) {
        final BlockPos pos = buf.readBlockPos();
        final BlockEntity found = inventory.player.level().getBlockEntity(pos);
        if (!type.isInstance(found)) {
            throw new IllegalStateException("a menu was opened on a " + type.getSimpleName() + " at " + pos
                    + " the client does not have");
        }
        return type.cast(found);
    }
}
