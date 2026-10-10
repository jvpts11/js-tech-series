/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.operation.OperationPriority;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Wire codecs that several payloads share, so none of them borrows another payload's internals. */
public final class PayloadCodecs {

    /** A priority level as one byte, its id; an unknown id reads as the default, so a stale value never throws. */
    public static final StreamCodec<ByteBuf, OperationPriority> PRIORITY =
            StableCodecs.byId(OperationPriority.class, OperationPriority.DEFAULT);

    private PayloadCodecs() {
    }
}
