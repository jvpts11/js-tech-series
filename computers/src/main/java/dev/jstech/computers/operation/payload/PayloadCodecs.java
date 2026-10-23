/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.jstechseries.operation.OperationPriority;
import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Wire codecs that several payloads share, so none of them borrows another payload's internals. */
public final class PayloadCodecs {

    /** A priority level as one byte, its id; an unknown id reads as the default, so a stale value never throws. */
    public static final StreamCodec<ByteBuf, OperationPriority> PRIORITY =
            StableCodecs.byId(OperationPriority.class, OperationPriority.DEFAULT);
    /** The longest path the wire carries: a disk's longest, with the key of the medium it is on in front of it. */
    public static final int MOST_PATH = FsPaths.MAX_PATH_LENGTH + 40;
    /** What a path too long to carry is sent as: one no disk can hold, since no name has a control character in it. */
    public static final String NO_PATH = "\u0000";
    /**
     * A path on a disk or a medium. One longer than the wire carries, which a program can make by joining a long
     * folder and a long name (a file pasted into a deep folder, a build's output), goes as {@link #NO_PATH} rather
     * than throwing: a message longer than its cap is not cut on the way but drops the player from the game, where a
     * path that names nothing only finds nothing and writes nothing.
     */
    public static final StreamCodec<ByteBuf, String> PATH = ByteBufCodecs.stringUtf8(MOST_PATH)
            .map(Function.identity(), path -> path.length() > MOST_PATH ? NO_PATH : path);

    private PayloadCodecs() {
    }
}
