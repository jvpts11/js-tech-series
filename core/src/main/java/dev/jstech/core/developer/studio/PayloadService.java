/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import dev.jstech.core.diagnostic.Bandwidth;
import dev.jstech.core.diagnostic.TickLedger;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * What the server sent, tick by tick, over the last hundred ticks: to the player who asks, or to everyone, each tick
 * with every kind of packet it sent, how many and how big.
 */
final class PayloadService implements IStudioService {

    static final String NAME = "payloads";

    @Override
    public void answer(final ServerPlayer player, final CompoundTag args, final Consumer<CompoundTag> reply) {
        final Optional<TickLedger> ledger = args.getBoolean(StudioKeys.EVERYONE) ? Optional.of(
                Bandwidth.everyoneByTick()) : Bandwidth.byTick(player.getUUID());
        final ListTag ticks = new ListTag();
        ledger.ifPresent(found -> {
            for (final TickLedger.Tick tick : found.ticks()) {
                final CompoundTag written = new CompoundTag();
                written.putLong(StudioKeys.TICK, tick.tick());
                final ListTag sent = new ListTag();
                for (final TickLedger.Sent kind : tick.sent()) {
                    final CompoundTag one = new CompoundTag();
                    one.putString(StudioKeys.KIND, kind.kind());
                    one.putLong(StudioKeys.PACKETS, kind.packets());
                    one.putLong(StudioKeys.BYTES, kind.bytes());
                    one.putLong(StudioKeys.MOST, kind.biggest());
                    sent.add(one);
                }
                written.put(StudioKeys.SENT, sent);
                ticks.add(written);
            }
        });
        final CompoundTag out = new CompoundTag();
        out.put(StudioKeys.TICKS, ticks);
        reply.accept(out);
    }
}
