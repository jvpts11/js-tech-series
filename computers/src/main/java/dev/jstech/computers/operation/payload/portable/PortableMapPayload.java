/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.portable;

import dev.jstech.computers.JsComputers;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what a portable's Maps app marks on J's Core's map (computers/docs/design/wireless.md), beyond the
 * player and the Core's own markers: the Mainframe of the network it is on, and that network's access points, each
 * with how far it reaches.
 *
 * @param device    the device's id
 * @param mainframe where the network's Mainframe stands; empty on no network or one without it in this dimension
 * @param points    the network's access points in this dimension
 */
public record PortableMapPayload(UUID device, Optional<BlockPos> mainframe, List<Point> points)
        implements CustomPacketPayload {

    /** The most access points marked. */
    public static final int MOST_POINTS = 32;
    private static final int MOST_NAME = 16;

    public static final CustomPacketPayload.Type<PortableMapPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "portable_map"));

    public static final StreamCodec<ByteBuf, PortableMapPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, PortableMapPayload::device,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), PortableMapPayload::mainframe,
            Point.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_POINTS)), PortableMapPayload::points,
            PortableMapPayload::new);

    public PortableMapPayload {
        points = List.copyOf(points.subList(0, Math.min(MOST_POINTS, points.size())));
    }

    @Override
    public CustomPacketPayload.Type<PortableMapPayload> type() {
        return TYPE;
    }

    /**
     * An access point on the map.
     *
     * @param pos   where it stands
     * @param name  what the network calls it
     * @param reach how far it reaches, in blocks
     */
    public record Point(BlockPos pos, String name, int reach) {

        public static final StreamCodec<ByteBuf, Point> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Point::pos,
                ByteBufCodecs.stringUtf8(MOST_NAME), Point::name,
                ByteBufCodecs.VAR_INT, Point::reach,
                Point::new);
    }
}
