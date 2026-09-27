/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.List;
import java.util.function.Function;

/**
 * Whether a player may send a payload. A client is not trusted to say what it is using: a modified one can put any
 * position in any payload, so the server asks the gate, on its own thread and before anything is done, whether the
 * sender really has that screen open. The menu the server opened is the proof, reach included.
 *
 * @param <P> the payload
 */
@FunctionalInterface
public interface IPayloadGate<P> {

    /** In a menu of that kind, still open for the player, for a payload that names nothing the menu does not hold. */
    static <P> IPayloadGate<P> menu(final Class<? extends AbstractContainerMenu> kind) {
        return (player, payload) -> kind.isInstance(player.containerMenu) && player.containerMenu.stillValid(player);
    }

    /** In a menu of that kind, still open for the player, on the block the payload names. */
    static <P, M extends AbstractContainerMenu> IPayloadGate<P> menu(final Class<M> kind,
                                                                    final Function<M, BlockPos> at,
                                                                    final Function<P, BlockPos> pos) {
        return (player, payload) -> kind.isInstance(player.containerMenu)
                && at.apply(kind.cast(player.containerMenu)).equals(pos.apply(payload))
                && player.containerMenu.stillValid(player);
    }

    /** Admitted by any one of these. */
    @SafeVarargs
    static <P> IPayloadGate<P> anyOf(final IPayloadGate<P>... gates) {
        final List<IPayloadGate<P>> all = List.of(gates);
        return (player, payload) -> {
            for (final IPayloadGate<P> gate : all) {
                if (gate.admits(player, payload)) {
                    return true;
                }
            }
            return false;
        };
    }

    boolean admits(ServerPlayer player, P payload);
}
