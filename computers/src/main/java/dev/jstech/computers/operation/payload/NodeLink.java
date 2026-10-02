/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.core.network.DataLink;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

/**
 * How a node of the network is joined to it, as the Network Manager shows it: the cable it is plugged into (its line
 * and era, by the cable's serialized name, empty when it is plugged into none the screen can tell), whether it holds an
 * Optical Network Card, whether its link is up, how many cables long its cable's run is, the optical router its fibre
 * reaches the network through, and, for a node off the network, why: its fibre bends where no optical router turns it,
 * or a run on its way is longer than its cable reaches.
 *
 * @param link      the serialized name of its cable's link; for a node off the network, the cable that cut it off
 * @param optical   whether it holds an Optical Network Card
 * @param up        whether its link is up
 * @param runLength how many cables long the run of {@code link} is
 * @param reason    why it is off the network: {@link #REASON_NONE}, {@link #REASON_BENDS} or {@link #REASON_TOO_LONG}
 * @param where     where its fibre bends, a block position, or {@link #NO_PLACE}
 * @param router    the optical router its fibre goes through, a block position, or {@link #NO_PLACE}
 */
public record NodeLink(String link, boolean optical, boolean up, int runLength, byte reason, long where,
                       long router) {

    /** A position no block can have, for a link with no bend or no router to tell of. */
    public static final long NO_PLACE = Long.MIN_VALUE;
    public static final byte REASON_NONE = 0;
    public static final byte REASON_BENDS = 1;
    public static final byte REASON_TOO_LONG = 2;
    /** A node whose link the screen does not tell: a subframe, a supercomputer, a machine on no cable it knows. */
    public static final NodeLink NONE = new NodeLink("", false, true, 0, REASON_NONE, NO_PLACE, NO_PLACE);

    public static final StreamCodec<RegistryFriendlyByteBuf, NodeLink> STREAM_CODEC =
            StreamCodec.of(NodeLink::encode, NodeLink::decode);

    /** A node on the network over {@code link}, through {@code router} when its fibre passes one. */
    public static NodeLink up(final DataLink link, final boolean optical, final int runLength, final long router) {
        return new NodeLink(link.serializedName(), optical, true, runLength, REASON_NONE, NO_PLACE, router);
    }

    /** A node whose fibre bends at {@code where}, with no optical router there to turn it. */
    public static NodeLink bends(final DataLink link, final boolean optical, final long where) {
        return new NodeLink(link.serializedName(), optical, false, 0, REASON_BENDS, where, NO_PLACE);
    }

    /** A node cut off by a run of {@code link}, {@code runLength} cables long, longer than the cable reaches. */
    public static NodeLink tooLong(final DataLink link, final boolean optical, final int runLength) {
        return new NodeLink(link.serializedName(), optical, false, runLength, REASON_TOO_LONG, NO_PLACE, NO_PLACE);
    }

    /** The cable's link, or null when the node is plugged into none the screen can tell. */
    @Nullable
    public DataLink dataLink() {
        return link.isEmpty() ? null : DataLink.byName(link);
    }

    private static void encode(final RegistryFriendlyByteBuf buf, final NodeLink link) {
        buf.writeUtf(link.link, 32);
        buf.writeBoolean(link.optical);
        buf.writeBoolean(link.up);
        buf.writeVarInt(link.runLength);
        buf.writeByte(link.reason);
        buf.writeLong(link.where);
        buf.writeLong(link.router);
    }

    private static NodeLink decode(final RegistryFriendlyByteBuf buf) {
        return new NodeLink(buf.readUtf(32), buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readByte(),
                buf.readLong(), buf.readLong());
    }
}
