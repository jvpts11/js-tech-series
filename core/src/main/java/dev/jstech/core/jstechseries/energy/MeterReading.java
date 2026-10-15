/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import dev.jstech.core.JsCore;
import dev.jstech.core.energy.EnergyReadings;
import dev.jstech.core.energy.EnergyRatio;
import dev.jstech.core.id.StableCodecs;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * What a clamp meter reads, sent to the player holding it a few times a second: of a line, what crossed the clamped
 * wire in the last tick, which way, its ceiling, and what crossed it since it was clamped with the highest and the
 * lowest; of a block, what it holds and what went through each face. Amounts are in the energy's own unit, with the
 * energy's worth in FE at the server, for a meter turned to FE.
 *
 * @param present   whether the meter reads anything: false once what it was clamped on is gone
 * @param line      whether it is clamped on a line rather than a block
 * @param energy    the energy read
 * @param lineClass the class of the line or the block's port; empty for an energy with no classes
 * @param subject   the item that lays the wire's cable, or the block
 * @param ratioUnits how many of the energy's unit are worth {@code ratioFe} FE
 * @param ratioFe   see above
 * @param perTick   what crossed the wire in the last tick
 * @param toward    the face beyond which most of it went
 * @param ceiling   the most the wire carries in a tick
 * @param total     what crossed it since it was clamped
 * @param ticks     how long ago it was clamped, in ticks
 * @param most      the most that crossed it in a tick since
 * @param least     the least
 * @param stored    what the block holds
 * @param capacity  the most it holds
 * @param faces     what went through each of its faces
 */
public record MeterReading(boolean present, boolean line, ResourceLocation energy, String lineClass,
                           ResourceLocation subject, long ratioUnits, long ratioFe, long perTick,
                           Optional<Direction> toward, long ceiling, long total, long ticks, long most, long least,
                           long stored, long capacity, List<Face> faces) implements CustomPacketPayload {

    public static final Type<MeterReading> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "meter_reading"));
    /** The most faces a reading carries: a block's six. */
    public static final int MOST_FACES = 6;
    public static final StreamCodec<RegistryFriendlyByteBuf, MeterReading> STREAM_CODEC = StreamCodec.of(
            MeterReading::write, MeterReading::read);
    private static final StreamCodec<ByteBuf, EnergyReadings.FaceReading.Kind> KIND =
            ByteBufCodecs.fromCodec(StableCodecs.byName(EnergyReadings.FaceReading.Kind.class));

    public MeterReading {
        faces = List.copyOf(faces);
        if (faces.size() > MOST_FACES) {
            throw new IllegalArgumentException("a block has six faces, not " + faces.size());
        }
    }

    /** What a meter clamped on nothing that is still there reads. */
    public static MeterReading nothing() {
        final ResourceLocation none = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "none");
        return new MeterReading(false, false, none, "", none, 1L, 1L, 0L, Optional.empty(), 0L, 0L, 0L, 0L, 0L,
                0L, 0L, List.of());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** The energy's worth in FE at the server, when it was read. */
    public EnergyRatio ratio() {
        return new EnergyRatio(this.ratioUnits, this.ratioFe);
    }

    private static void write(final RegistryFriendlyByteBuf buf, final MeterReading reading) {
        buf.writeBoolean(reading.present);
        buf.writeBoolean(reading.line);
        ResourceLocation.STREAM_CODEC.encode(buf, reading.energy);
        buf.writeUtf(reading.lineClass);
        ResourceLocation.STREAM_CODEC.encode(buf, reading.subject);
        buf.writeVarLong(reading.ratioUnits);
        buf.writeVarLong(reading.ratioFe);
        buf.writeVarLong(reading.perTick);
        ByteBufCodecs.optional(Direction.STREAM_CODEC).encode(buf, reading.toward);
        buf.writeLong(reading.ceiling);
        buf.writeVarLong(reading.total);
        buf.writeVarLong(reading.ticks);
        buf.writeVarLong(reading.most);
        buf.writeVarLong(reading.least);
        buf.writeVarLong(reading.stored);
        buf.writeVarLong(reading.capacity);
        buf.writeVarInt(reading.faces.size());
        for (final Face face : reading.faces) {
            Direction.STREAM_CODEC.encode(buf, face.face());
            KIND.encode(buf, face.kind());
            buf.writeVarLong(face.perTick());
        }
    }

    private static MeterReading read(final RegistryFriendlyByteBuf buf) {
        final boolean present = buf.readBoolean();
        final boolean line = buf.readBoolean();
        final ResourceLocation energy = ResourceLocation.STREAM_CODEC.decode(buf);
        final String lineClass = buf.readUtf();
        final ResourceLocation subject = ResourceLocation.STREAM_CODEC.decode(buf);
        final long ratioUnits = buf.readVarLong();
        final long ratioFe = buf.readVarLong();
        final long perTick = buf.readVarLong();
        final Optional<Direction> toward = ByteBufCodecs.optional(Direction.STREAM_CODEC).decode(buf);
        final long ceiling = buf.readLong();
        final long total = buf.readVarLong();
        final long ticks = buf.readVarLong();
        final long most = buf.readVarLong();
        final long least = buf.readVarLong();
        final long stored = buf.readVarLong();
        final long capacity = buf.readVarLong();
        final int count = Math.min(MOST_FACES, buf.readVarInt());
        final List<Face> faces = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            faces.add(new Face(Direction.STREAM_CODEC.decode(buf), KIND.decode(buf), buf.readVarLong()));
        }
        return new MeterReading(present, line, energy, lineClass, subject, ratioUnits, ratioFe, perTick, toward,
                ceiling, total, ticks, most, least, stored, capacity, faces);
    }

    /**
     * What went through one face of the block read.
     *
     * @param face    the face
     * @param kind    in, out, nothing, or no port
     * @param perTick how much, in the energy's own unit
     */
    public record Face(Direction face, EnergyReadings.FaceReading.Kind kind, long perTick) {
    }
}
