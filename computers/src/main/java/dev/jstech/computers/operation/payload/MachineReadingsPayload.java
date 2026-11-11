/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: a machine's power and temperature for its system monitors, in reply to
 * {@link RequestMachineReadingsPayload}: over the period asked, what its parts drew, what the line gave and its
 * temperature, a mean a sample, oldest first ({@code MachineReadings.NONE} for a sample with no reading); and now, what
 * its parts draw against its supply's rating, what the line gives, its temperature and its surroundings', and the
 * energy it took from the line today.
 *
 * @param hostPos        the machine
 * @param hours          the period: 1, 24 or 168
 * @param draw           what its parts drew, in watts
 * @param line           what the line gave, in watts
 * @param temperature    its temperature, in °C
 * @param drawNow        what its parts draw now, in watts
 * @param rating         its supply's rating, in watts; 0 with none
 * @param lineNow        what the line gives now, in watts
 * @param celsius        its temperature now, in °C
 * @param ambient        its surroundings' temperature, in °C
 * @param hasTemperature whether it has a temperature of its own (one cooled from outside it has none)
 * @param joulesToday    the energy it took from the line in the game day, in joules
 */
public record MachineReadingsPayload(BlockPos hostPos, int hours, int[] draw, int[] line, int[] temperature,
                                     int drawNow, int rating, int lineNow, int celsius, int ambient,
                                     boolean hasTemperature, long joulesToday) implements CustomPacketPayload {

    /* More samples than the longest period has, so a reply is never cut. */
    private static final int MOST_SAMPLES = 256;

    public static final CustomPacketPayload.Type<MachineReadingsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "machine_readings"));

    // Hand-written because the record has more components than StreamCodec.composite carries.
    public static final StreamCodec<RegistryFriendlyByteBuf, MachineReadingsPayload> STREAM_CODEC =
            StreamCodec.of(MachineReadingsPayload::encode, MachineReadingsPayload::decode);

    /* Copied on the way in, so what the client is handed cannot change under it after it arrives. */
    public MachineReadingsPayload {
        draw = draw.clone();
        line = line.clone();
        temperature = temperature.clone();
    }

    @Override
    public CustomPacketPayload.Type<MachineReadingsPayload> type() {
        return TYPE;
    }

    private static void encode(final RegistryFriendlyByteBuf buf, final MachineReadingsPayload payload) {
        BlockPos.STREAM_CODEC.encode(buf, payload.hostPos);
        buf.writeVarInt(payload.hours);
        buf.writeVarIntArray(payload.draw);
        buf.writeVarIntArray(payload.line);
        buf.writeVarIntArray(payload.temperature);
        buf.writeVarInt(payload.drawNow);
        buf.writeVarInt(payload.rating);
        buf.writeVarInt(payload.lineNow);
        buf.writeVarInt(payload.celsius);
        buf.writeVarInt(payload.ambient);
        buf.writeBoolean(payload.hasTemperature);
        buf.writeVarLong(payload.joulesToday);
    }

    private static MachineReadingsPayload decode(final RegistryFriendlyByteBuf buf) {
        return new MachineReadingsPayload(BlockPos.STREAM_CODEC.decode(buf), buf.readVarInt(),
                buf.readVarIntArray(MOST_SAMPLES), buf.readVarIntArray(MOST_SAMPLES),
                buf.readVarIntArray(MOST_SAMPLES), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readVarLong());
    }
}
