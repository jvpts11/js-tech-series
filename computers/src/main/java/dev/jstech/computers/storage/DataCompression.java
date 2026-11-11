/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.storage;

import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NodeUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * The Data Compressor, as a server's storage feels it: a bay whose server runs it holds a fifth more items on the same
 * drives, and every read of it waits two ticks longer. Good on a hard disk, debatable on an SSD and bad on NVMe, whose
 * one tick it triples; a trade the player weighs (computers/docs/design/programs.md). Both figures are estimates, to be
 * weighed in play.
 */
public final class DataCompression {

    /** What a compressing bay's drives hold, in hundredths of what they hold plain. */
    public static final int ROOM_PERCENT = 120;
    /** The ticks every read of a compressing bay waits beyond its drives' own. */
    public static final int READ_WAIT_TICKS = 2;

    private DataCompression() {
    }

    /** Whether the server in {@code slot} of that rack compresses what its bay keeps. */
    public static boolean on(final ServerRackBlockEntity rack, final int slot) {
        return rack.serviceUp(slot, Programs.DATA_COMPRESSOR);
    }

    /** What that server's drives hold, in hundredths of their plain room. */
    public static int roomPercent(final ServerRackBlockEntity rack, final int slot) {
        return on(rack, slot) ? ROOM_PERCENT : LocalStore.PLAIN_ROOM;
    }

    /** The ticks a read of that server waits for the compressor, beyond its drives' own. */
    public static int readWait(final ServerRackBlockEntity rack, final int slot) {
        return on(rack, slot) ? READ_WAIT_TICKS : 0;
    }

    /** The same for the server with that node, wherever it is mounted; nothing for a node that is no server. */
    public static int readWait(final ServerLevel level, final NodeUuid server) {
        return NetworkSystem.get(level).locationOf(server)
                .map(at -> Loaded.blockEntity(level, BlockPos.of(at.rackPos())) instanceof ServerRackBlockEntity rack
                        ? readWait(rack, at.slot()) : 0)
                .orElse(0);
    }
}
