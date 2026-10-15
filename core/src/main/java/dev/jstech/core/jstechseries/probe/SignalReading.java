/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.probe;

import dev.jstech.core.JsCore;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.signal.Band;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * What a signal probe read, sent to the player holding it a few times a second: the redstone on the block it is linked
 * to, now and its peak since it was linked, and every frequency heard where the player stands, with those of their
 * team that are not heard there.
 *
 * @param linked   whether it is linked to a block it can read now
 * @param wire     where that block is, as a block position's long
 * @param strength the redstone on it now, 0 to 15
 * @param peak     the strongest it has been since the probe was linked to it
 * @param rows     the frequencies, by band and number
 */
public record SignalReading(boolean linked, long wire, int strength, int peak, List<Row> rows)
        implements CustomPacketPayload {

    /** The most frequencies a reading carries. */
    public static final int MOST_ROWS = 32;
    /** The longest name of an owner, or of a dimension, a reading carries. */
    public static final int LONGEST_NAME = 64;

    public static final Type<SignalReading> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "signal_reading"));
    public static final StreamCodec<FriendlyByteBuf, SignalReading> STREAM_CODEC = StreamCodec.of(
            (buf, reading) -> reading.write(buf), SignalReading::read);

    private static final StreamCodec<ByteBuf, Band> BAND_CODEC = ByteBufCodecs.fromCodec(
            StableCodecs.byName(Band.class));

    public SignalReading {
        Objects.requireNonNull(rows, "rows");
        rows = List.copyOf(rows.size() > MOST_ROWS ? rows.subList(0, MOST_ROWS) : rows);
    }

    /** Nothing linked and nothing heard. */
    public static SignalReading nothing() {
        return new SignalReading(false, 0L, 0, 0, List.of());
    }

    /** The rows heard here, those of the team not heard left out. */
    public List<Row> heard() {
        return this.rows.stream().filter(Row::heard).toList();
    }

    private void write(final FriendlyByteBuf buf) {
        buf.writeBoolean(this.linked);
        buf.writeLong(this.wire);
        buf.writeVarInt(this.strength);
        buf.writeVarInt(this.peak);
        buf.writeVarInt(this.rows.size());
        for (final Row row : this.rows) {
            BAND_CODEC.encode(buf, row.band());
            buf.writeVarInt(row.number());
            buf.writeUtf(row.owner(), LONGEST_NAME);
            buf.writeLong(row.from());
            buf.writeUtf(row.dimension(), LONGEST_NAME);
            buf.writeDouble(row.distance());
            buf.writeBoolean(row.relayed());
            buf.writeVarInt(row.bars());
            buf.writeVarInt(row.value());
            buf.writeBoolean(row.clash());
        }
    }

    private static SignalReading read(final FriendlyByteBuf buf) {
        final boolean linked = buf.readBoolean();
        final long wire = buf.readLong();
        final int strength = buf.readVarInt();
        final int peak = buf.readVarInt();
        final int count = Math.min(MOST_ROWS, buf.readVarInt());
        final List<Row> rows = new ArrayList<>(count);
        for (int k = 0; k < count; k++) {
            rows.add(new Row(BAND_CODEC.decode(buf), buf.readVarInt(), buf.readUtf(LONGEST_NAME), buf.readLong(),
                    buf.readUtf(LONGEST_NAME), buf.readDouble(), buf.readBoolean(), buf.readVarInt(),
                    buf.readVarInt(), buf.readBoolean()));
        }
        return new SignalReading(linked, wire, strength, peak, rows);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * A frequency as the probe shows it.
     *
     * @param band      its band
     * @param number    its number; 0 for a pair
     * @param owner     the name of whose transmitter it is
     * @param from      where it is heard from, as a block position's long: its transmitter, or a relay
     * @param dimension the dimension its transmitter is in, when it is not the player's; empty otherwise
     * @param distance  how far that is from the player, in blocks
     * @param relayed   whether it is heard from a relay
     * @param bars      how well it arrives, 0 (not heard here) to 4
     * @param value     the value it carries, 0 to 15
     * @param clash     whether two transmitters clash on it
     */
    public record Row(Band band, int number, String owner, long from, String dimension, double distance,
                      boolean relayed, int bars, int value, boolean clash) {

        public Row {
            Objects.requireNonNull(band, "band");
            owner = cut(owner);
            dimension = cut(dimension);
        }

        /** Whether it is heard here. */
        public boolean heard() {
            return this.bars > 0;
        }

        private static String cut(final String name) {
            return name.length() > LONGEST_NAME ? name.substring(0, LONGEST_NAME) : name;
        }
    }
}
