/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.network.NetworkLookup;
import dev.jstech.computers.storage.IDataSink;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.uuid.NetworkUuid;
import net.minecraft.server.level.ServerLevel;

/**
 * Where a machine step draws its inputs from and returns its outputs to. A standalone processing operation uses
 * the network (the default). A machine step run inside a recursive craft uses that craft's isolated pool, so
 * several steps of one craft can run at once and pipeline through the pool without racing on shared network
 * stock, and the network is touched only for the craft's raws and its final result.
 */
public interface ICraftIo {

    /** Moves up to {@code amount} of {@code key} from this source into {@code into}; returns how much moved. */
    long select(StorageKey key, long amount, IDataSink into);

    /** Deposits {@code amount} of {@code key} into this sink; returns how much was stored. */
    long insert(StorageKey key, long amount);

    /**
     * The default I/O: the network's own storage. It takes only what no running operation holds reserved, so a job of
     * its own, such as an order a machine runs straight from stock, never takes a craft's reserved ingredient and
     * leaves that craft's step waiting for what is gone.
     */
    static ICraftIo network(final ServerLevel level, final NetworkUuid network) {
        return new ICraftIo() {
            @Override
            public long select(final StorageKey key, final long amount, final IDataSink into) {
                final NetworkStorage storage = NetworkStorage.of(level, network);
                final MainframeBlockEntity mainframe = NetworkLookup.resolveMainframe(level, network);
                return mainframe == null ? storage.select(key, amount, into)
                        : storage.selectUnreserved(key, amount, into, mainframe.networkIndex());
            }

            @Override
            public long insert(final StorageKey key, final long amount) {
                return NetworkStorage.of(level, network).insert(key, amount);
            }
        };
    }
}
