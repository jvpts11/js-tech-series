/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.operation.NetworkInsertOperation;
import dev.jstech.computers.operation.payload.network.NetworkLookup;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.uuid.NetworkUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Where a block's rare item goes (computers/docs/design/teracoin.md, "Blocks"): into the network storage of the machine
 * whose work found the block, written in as any deposit is; what the network has no room for, or a machine on no
 * network, drops at the machine, so nothing found is lost.
 */
public final class RareItems {

    /* What the network's log calls the write. */
    private static final String LABEL = "Teracoin";

    private RareItems() {
    }

    /** Puts {@code item} into the storage of {@code network}, or drops it at {@code at}. */
    public static void deposit(final ServerLevel level, @Nullable final NetworkUuid network, final BlockPos at,
                               final ItemStack item) {
        final MainframeBlockEntity mainframe = network == null ? null : NetworkLookup.resolveMainframe(level, network);
        if (mainframe == null) {
            Block.popResource(level, at.above(), item);
            return;
        }
        final ItemStack inFlight = item.copy();
        final NetworkInsertOperation op = mainframe.networkOperations().push(StorageKey.of(inFlight),
                inFlight.getCount(), LABEL);
        if (op == null) {
            Block.popResource(level, at.above(), item);
            return;
        }
        op.onSettle(() -> {
            final long leftover = op.leftover();
            if (leftover > 0L) {
                Block.popResource(level, at.above(), inFlight.copyWithCount((int) Math.min(leftover,
                        inFlight.getCount())));
            }
        });
    }
}
