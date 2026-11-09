/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.core.jstechseries.network.DataLink;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

/**
 * How a node of the network is joined to it, as the Network Manager shows it: the cable it is plugged into (its line
 * and era, by the cable's serialized name, empty when it is plugged into none the screen can tell), whether it has a
 * fibre port, whether its link is up, how many cables long its cable's run is, the optical router its fibre
 * reaches the network through, and, for a node off the network, why: its fibre bends where no optical router turns it,
 * or a run on its way is longer than its cable reaches.
 *
 * @param link      the serialized name of its cable's link; for a node off the network, the cable that cut it off
 * @param optical   whether it has a fibre port: an Optical Network Card, or a rack's Fibre Optic Interface
 * @param up        whether its link is up
 * @param runLength how many cables long the run of {@code link} is
 * @param reason    why it is off the network: {@link #REASON_NONE}, {@link #REASON_BENDS} or {@link #REASON_TOO_LONG}
 * @param where     where its fibre bends, a block position, or {@link #NO_PLACE}
 * @param router    the optical router its fibre goes through, a block position, or {@link #NO_PLACE}
 * @param wifi      how it is on the network by Wi-Fi, for a computer that joined it through an access point; or null
 */
public record NodeLink(String link, boolean optical, boolean up, int runLength, byte reason, long where,
                       long router, @Nullable Wifi wifi) {

    /** A position no block can have, for a link with no bend or no router to tell of. */
    public static final long NO_PLACE = Long.MIN_VALUE;
    public static final byte REASON_NONE = 0;
    public static final byte REASON_BENDS = 1;
    public static final byte REASON_TOO_LONG = 2;
    /** A node whose link the screen does not tell: a subframe, a supercomputer, a machine on no cable it knows. */
    public static final NodeLink NONE = new NodeLink("", false, true, 0, REASON_NONE, NO_PLACE, NO_PLACE, null);

    public static final StreamCodec<RegistryFriendlyByteBuf, NodeLink> STREAM_CODEC =
            StreamCodec.of(NodeLink::encode, NodeLink::decode);
    /* The longest name a node goes by on the screen. */
    private static final int MOST_NAME = 64;

    /**
     * A computer's Wi-Fi (computers/docs/design/wireless.md): its card's and its access point's, by their serialized
     * names, and the access point it joined through, by the name the screen shows it by.
     */
    public record Wifi(String card, String accessPoint, String through) {
    }

    /** A node on the network over {@code link}, through {@code router} when its fibre passes one. */
    public static NodeLink up(final DataLink link, final boolean optical, final int runLength, final long router) {
        return new NodeLink(link.serializedName(), optical, true, runLength, REASON_NONE, NO_PLACE, router, null);
    }

    /** A node whose fibre bends at {@code where}, with no optical router there to turn it. */
    public static NodeLink bends(final DataLink link, final boolean optical, final long where) {
        return new NodeLink(link.serializedName(), optical, false, 0, REASON_BENDS, where, NO_PLACE, null);
    }

    /** A node cut off by a run of {@code link}, {@code runLength} cables long, longer than the cable reaches. */
    public static NodeLink tooLong(final DataLink link, final boolean optical, final int runLength) {
        return new NodeLink(link.serializedName(), optical, false, runLength, REASON_TOO_LONG, NO_PLACE, NO_PLACE,
                null);
    }

    /** A computer on the network by Wi-Fi, through an access point. */
    public static NodeLink wireless(final Wifi wifi) {
        return new NodeLink("", false, true, 0, REASON_NONE, NO_PLACE, NO_PLACE, wifi);
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
        buf.writeBoolean(link.wifi != null);
        if (link.wifi != null) {
            buf.writeUtf(link.wifi.card(), 32);
            buf.writeUtf(link.wifi.accessPoint(), 32);
            buf.writeUtf(link.wifi.through(), MOST_NAME);
        }
    }

    private static NodeLink decode(final RegistryFriendlyByteBuf buf) {
        final String link = buf.readUtf(32);
        final boolean optical = buf.readBoolean();
        final boolean up = buf.readBoolean();
        final int runLength = buf.readVarInt();
        final byte reason = buf.readByte();
        final long where = buf.readLong();
        final long router = buf.readLong();
        final Wifi wifi = buf.readBoolean() ? new Wifi(buf.readUtf(32), buf.readUtf(32), buf.readUtf(MOST_NAME)) : null;
        return new NodeLink(link, optical, up, runLength, reason, where, router, wifi);
    }
}
