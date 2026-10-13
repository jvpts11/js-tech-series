/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.peripheral;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Frees the ports an owner holds for peripherals that went away while its chunk was not loaded to be told. A peripheral
 * broken far from its owner can only tell the owner when the owner is loaded, so the owner checks for itself now and
 * then: a port is never left taken by a peripheral that is no longer there.
 */
public final class PeripheralReconciler {

    /** How often, in ticks, an owner checks that the peripherals it holds ports for are still there. */
    public static final long INTERVAL_TICKS = 100L;

    private PeripheralReconciler() {
    }

    /**
     * Once every {@link #INTERVAL_TICKS} ticks, frees the port of each linked peripheral whose chunk is loaded and
     * whose block is no longer a peripheral linked to an owner. A position in an unloaded chunk is left alone, as
     * nothing can be asked of it. The owners are spread over the interval by their position.
     *
     * @param ownerPos the owner's own position, packed
     */
    public static void reconcile(final IPeripheralOwnerSupport owner, final ServerLevel level, final long ownerPos) {
        if (Math.floorMod(level.getGameTime() + ownerPos, INTERVAL_TICKS) != 0L) {
            return;
        }
        for (final long pos : owner.linkedEndpoints()) {
            final BlockPos at = BlockPos.of(pos);
            if (!level.isLoaded(at)) {
                continue;
            }
            final boolean linkedBack = level.getBlockEntity(at) instanceof IPeripheralEndpoint endpoint
                    && endpoint.linkedOwner().isPresent();
            if (!linkedBack) {
                owner.onEndpointUnlinked(pos);
            }
        }
    }
}
