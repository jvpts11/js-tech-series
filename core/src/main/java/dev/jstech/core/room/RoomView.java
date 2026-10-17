/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * A room as a player sees it through goggles or a detector: its outline, its leaks, and what its air reads.
 *
 * @param room      its number; -1 for no room shown
 * @param outline   its outline
 * @param leaks     its leaks, the blocks gas crosses
 * @param volume    how many blocks it holds
 * @param pressure  its pressure, in kilopascals
 * @param change    how fast its pressure changes, in kilopascals a second
 * @param shares    each gas's share of its air
 * @param source    who shows it: goggles, or a detector
 */
public record RoomView(long room, List<RoomOutline.Segment> outline, List<Leak> leaks, int volume, double pressure,
                       double change, Map<ResourceLocation, Double> shares, Source source) {

    /** No room shown. */
    public static final RoomView NONE = new RoomView(-1, List.of(), List.of(), 0, 0, 0, Map.of(), Source.GOGGLES);
    /** The most leaks a view holds. */
    public static final int MOST_LEAKS = 64;
    public static final StreamCodec<ByteBuf, RoomView> STREAM_CODEC = StreamCodec.of(RoomView::write, RoomView::read);

    public RoomView {
        outline = List.copyOf(outline);
        leaks = List.copyOf(leaks);
        shares = Map.copyOf(new LinkedHashMap<>(shares));
    }

    /** Whether it shows a room. */
    public boolean shown() {
        return this.room >= 0;
    }

    /** The share of the air {@code gas} makes up. */
    public double share(final ResourceLocation gas) {
        return this.shares.getOrDefault(gas, 0.0);
    }

    private static void write(final ByteBuf buffer, final RoomView view) {
        final FriendlyByteBuf out = new FriendlyByteBuf(buffer);
        out.writeLong(view.room);
        out.writeVarInt(view.outline.size());
        for (final RoomOutline.Segment segment : view.outline) {
            out.writeVarInt(segment.x1());
            out.writeVarInt(segment.y1());
            out.writeVarInt(segment.z1());
            out.writeVarInt(segment.x2());
            out.writeVarInt(segment.y2());
            out.writeVarInt(segment.z2());
        }
        out.writeVarInt(view.leaks.size());
        for (final Leak leak : view.leaks) {
            out.writeBlockPos(leak.block());
            out.writeBoolean(leak.hole());
            out.writeBoolean(leak.door());
            out.writeVarInt(leak.side().get3DDataValue());
            out.writeResourceLocation(leak.name());
        }
        out.writeVarInt(view.volume);
        out.writeDouble(view.pressure);
        out.writeDouble(view.change);
        out.writeVarInt(view.shares.size());
        for (final Map.Entry<ResourceLocation, Double> share : view.shares.entrySet()) {
            out.writeResourceLocation(share.getKey());
            out.writeDouble(share.getValue());
        }
        out.writeBoolean(view.source == Source.DETECTOR);
    }

    private static RoomView read(final ByteBuf buffer) {
        final FriendlyByteBuf in = new FriendlyByteBuf(buffer);
        final long room = in.readLong();
        final int segments = Math.min(in.readVarInt(), RoomOutline.MOST);
        final List<RoomOutline.Segment> outline = new ArrayList<>(segments);
        for (int i = 0; i < segments; i++) {
            outline.add(new RoomOutline.Segment(in.readVarInt(), in.readVarInt(), in.readVarInt(), in.readVarInt(),
                    in.readVarInt(), in.readVarInt()));
        }
        final int count = Math.min(in.readVarInt(), MOST_LEAKS);
        final List<Leak> leaks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            leaks.add(new Leak(in.readBlockPos(), in.readBoolean(), in.readBoolean(),
                    Direction.from3DDataValue(in.readVarInt()), in.readResourceLocation()));
        }
        final int volume = in.readVarInt();
        final double pressure = in.readDouble();
        final double change = in.readDouble();
        final int gases = in.readVarInt();
        final Map<ResourceLocation, Double> shares = new LinkedHashMap<>();
        for (int i = 0; i < gases; i++) {
            shares.put(in.readResourceLocation(), in.readDouble());
        }
        return new RoomView(room, outline, leaks, volume, pressure, change, shares,
                in.readBoolean() ? Source.DETECTOR : Source.GOGGLES);
    }

    /**
     * A leak of a room: a block gas crosses.
     *
     * @param block where it is
     * @param hole  whether it is a hole, no block at all
     * @param door  whether it is a door
     * @param side  the way from the room out through it: up for a hole in the roof
     * @param name  the id of the block, to name it by
     */
    public record Leak(BlockPos block, boolean hole, boolean door, Direction side, ResourceLocation name) {
    }

    /** Who shows a room. */
    public enum Source {
        /** Something worn or held that sees rooms. */
        GOGGLES,
        /** An Atmosphere Detector. */
        DETECTOR
    }
}
