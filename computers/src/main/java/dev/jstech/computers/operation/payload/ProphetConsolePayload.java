/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.engine.prophet.ProphetEngine;
import dev.jstech.computers.engine.prophet.ProphetStates;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what the Prophet Reactive Console window {@code window} shows. The engine with its version, the
 * network, what the last request came to, the states kept with their levels seen lately, the watches, what the engine
 * did lately, its settings, and the game time now, which the graphs and the ages are read against.
 *
 * @param ok        whether what was asked went through
 * @param engine    the engine with its version
 * @param network   the network's name
 * @param message   what the last request came to, or empty
 * @param states    the states kept
 * @param watches   the watches set
 * @param reactions what it did lately, the newest first
 * @param settings  how it looks and reacts
 * @param now       the game time now
 */
public record ProphetConsolePayload(int window, boolean ok, Text engine, Text network, Text message,
                                    List<ProphetEngine.StateRow> states, List<ProphetEngine.WatchRow> watches,
                                    List<ProphetEngine.ReactionRow> reactions, ProphetEngine.Settings settings,
                                    long now) implements CustomPacketPayload {

    /** The most rows of each list sent. */
    public static final int MAX_ROWS = 32;

    public static final CustomPacketPayload.Type<ProphetConsolePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "prophet_console"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProphetConsolePayload> STREAM_CODEC = StreamCodec.of(
            ProphetConsolePayload::write, ProphetConsolePayload::read);

    public ProphetConsolePayload {
        states = List.copyOf(states.subList(0, Math.min(states.size(), MAX_ROWS)));
        watches = List.copyOf(watches.subList(0, Math.min(watches.size(), MAX_ROWS)));
        reactions = List.copyOf(reactions.subList(0, Math.min(reactions.size(), MAX_ROWS)));
    }

    @Override
    public CustomPacketPayload.Type<ProphetConsolePayload> type() {
        return TYPE;
    }

    private static void write(final RegistryFriendlyByteBuf buf, final ProphetConsolePayload payload) {
        buf.writeVarInt(payload.window());
        buf.writeBoolean(payload.ok());
        TextCodecs.STREAM_CODEC.encode(buf, payload.engine());
        TextCodecs.STREAM_CODEC.encode(buf, payload.network());
        TextCodecs.STREAM_CODEC.encode(buf, payload.message());
        buf.writeVarInt(payload.states().size());
        for (final ProphetEngine.StateRow row : payload.states()) {
            ByteBufCodecs.STRING_UTF8.encode(buf, row.item());
            TextCodecs.STREAM_CODEC.encode(buf, row.name());
            ByteBufCodecs.STRING_UTF8.encode(buf, row.written());
            TextCodecs.STREAM_CODEC.encode(buf, row.status());
            buf.writeByte(row.tone());
            buf.writeVarLong(row.held());
            buf.writeVarLong(row.inFlight());
            buf.writeVarLong(row.lower());
            buf.writeLong(row.upper());
            TextCodecs.STREAM_CODEC.encode(buf, row.last());
            buf.writeVarInt(row.samples().size());
            for (final ProphetStates.Sample sample : row.samples()) {
                buf.writeVarLong(sample.at());
                buf.writeVarLong(sample.held());
                buf.writeVarLong(sample.inFlight());
            }
            buf.writeVarInt(row.operations().size());
            row.operations().forEach(line -> TextCodecs.STREAM_CODEC.encode(buf, line));
        }
        buf.writeVarInt(payload.watches().size());
        for (final ProphetEngine.WatchRow row : payload.watches()) {
            buf.writeVarInt(row.number());
            ByteBufCodecs.STRING_UTF8.encode(buf, row.written());
            buf.writeBoolean(row.fired());
            buf.writeBoolean(row.armed());
            buf.writeVarInt(row.times());
        }
        buf.writeVarInt(payload.reactions().size());
        for (final ProphetEngine.ReactionRow row : payload.reactions()) {
            buf.writeVarLong(row.at());
            TextCodecs.STREAM_CODEC.encode(buf, row.what());
        }
        buf.writeVarInt(payload.settings().interval());
        buf.writeVarLong(payload.settings().maxBatch());
        buf.writeBoolean(payload.settings().reacting());
        buf.writeVarLong(payload.now());
    }

    private static ProphetConsolePayload read(final RegistryFriendlyByteBuf buf) {
        final int window = buf.readVarInt();
        final boolean ok = buf.readBoolean();
        final Text engine = TextCodecs.STREAM_CODEC.decode(buf);
        final Text network = TextCodecs.STREAM_CODEC.decode(buf);
        final Text message = TextCodecs.STREAM_CODEC.decode(buf);
        final int stateCount = Math.min(buf.readVarInt(), MAX_ROWS);
        final List<ProphetEngine.StateRow> states = new ArrayList<>(stateCount);
        for (int i = 0; i < stateCount; i++) {
            final String item = ByteBufCodecs.STRING_UTF8.decode(buf);
            final Text name = TextCodecs.STREAM_CODEC.decode(buf);
            final String written = ByteBufCodecs.STRING_UTF8.decode(buf);
            final Text status = TextCodecs.STREAM_CODEC.decode(buf);
            final byte tone = buf.readByte();
            final long held = buf.readVarLong();
            final long inFlight = buf.readVarLong();
            final long lower = buf.readVarLong();
            final long upper = buf.readLong();
            final Text last = TextCodecs.STREAM_CODEC.decode(buf);
            final int sampleCount = Math.min(buf.readVarInt(), ProphetStates.SAMPLES);
            final List<ProphetStates.Sample> samples = new ArrayList<>(sampleCount);
            for (int s = 0; s < sampleCount; s++) {
                samples.add(new ProphetStates.Sample(buf.readVarLong(), buf.readVarLong(), buf.readVarLong()));
            }
            final int operationCount = Math.min(buf.readVarInt(), MAX_ROWS);
            final List<Text> operations = new ArrayList<>(operationCount);
            for (int o = 0; o < operationCount; o++) {
                operations.add(TextCodecs.STREAM_CODEC.decode(buf));
            }
            states.add(new ProphetEngine.StateRow(item, name, written, status, tone, held, inFlight, lower, upper,
                    last, samples, operations));
        }
        final int watchCount = Math.min(buf.readVarInt(), MAX_ROWS);
        final List<ProphetEngine.WatchRow> watches = new ArrayList<>(watchCount);
        for (int i = 0; i < watchCount; i++) {
            watches.add(new ProphetEngine.WatchRow(buf.readVarInt(), ByteBufCodecs.STRING_UTF8.decode(buf),
                    buf.readBoolean(), buf.readBoolean(), buf.readVarInt()));
        }
        final int reactionCount = Math.min(buf.readVarInt(), MAX_ROWS);
        final List<ProphetEngine.ReactionRow> reactions = new ArrayList<>(reactionCount);
        for (int i = 0; i < reactionCount; i++) {
            reactions.add(new ProphetEngine.ReactionRow(buf.readVarLong(), TextCodecs.STREAM_CODEC.decode(buf)));
        }
        final ProphetEngine.Settings settings = new ProphetEngine.Settings(buf.readVarInt(), buf.readVarLong(),
                buf.readBoolean());
        final long now = buf.readVarLong();
        return new ProphetConsolePayload(window, ok, engine, network, message, states, watches, reactions, settings,
                now);
    }
}
