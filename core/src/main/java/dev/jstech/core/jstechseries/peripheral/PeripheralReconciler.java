/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.peripheral;

import dev.jstech.core.moving.BlockMove;
import dev.jstech.core.moving.StructureBlocksMovedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;

/**
 * Frees the ports an owner holds for peripherals that went away while its chunk was not loaded to be told. A peripheral
 * broken far from its owner can only tell the owner when the owner is loaded, so the owner checks for itself now and
 * then: a port is never left taken by a peripheral that is no longer there. An owner moved with a moving structure
 * keeps its ports where its peripherals now are.
 */
public final class PeripheralReconciler {

    /** How often, in ticks, an owner checks that the peripherals it holds ports for are still there. */
    public static final long INTERVAL_TICKS = 100L;

    private PeripheralReconciler() {
    }

    /**
     * Hands the game's bus what follows moving structures, once, from the Core's constructor: an owner of the Core's
     * kind ({@link IPeripheralOwnerSupport}) that moved keeps every port, hub and disabled flag, at the places the
     * peripherals that moved with it are now. An owner of its own kind follows them from its own move hook.
     */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(StructureBlocksMovedEvent.class, event -> {
            final BlockMove move = event.move();
            for (final BlockPos now : move.moved().values()) {
                if (move.to().getBlockEntity(now) instanceof IPeripheralOwnerSupport owner) {
                    owner.peripheralPorts().moved(pos -> move.where(BlockPos.of(pos)).asLong());
                    owner.markPeripheralChange();
                }
            }
        });
    }

    /**
     * Once every {@link #INTERVAL_TICKS} ticks, frees the port of each linked peripheral whose chunk is loaded and
     * whose block is no longer a peripheral linked to an owner. A position in an unloaded chunk is left alone, as
     * nothing can be asked of it. The owners are spread over the interval by their position. A machine on a port of
     * {@link PortKind#CONTROL} is left to {@link ControlLinks#keep}: it keeps nothing of its controller, so it would
     * never answer as linked back, and the walk of the controller's cables already lets go of what they no longer
     * reach.
     *
     * @param ownerPos the owner's own position, packed
     */
    public static void reconcile(final IPeripheralOwnerSupport owner, final ServerLevel level, final long ownerPos) {
        if (Math.floorMod(level.getGameTime() + ownerPos, INTERVAL_TICKS) != 0L) {
            return;
        }
        for (final long pos : owner.linkedEndpoints()) {
            final BlockPos at = BlockPos.of(pos);
            if (owner.kindOf(pos) == PortKind.CONTROL || !level.isLoaded(at)) {
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
