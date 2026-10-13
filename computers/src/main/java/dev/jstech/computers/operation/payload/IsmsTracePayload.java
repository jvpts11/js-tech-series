/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.trace.TraceEvent;
import dev.jstech.computers.trace.TraceEventClass;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextBounds;
import dev.jstech.core.text.TextCodecs;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what a trace the studio window {@code window} began has seen happen since its last word, in
 * the order it happened.
 */
public record IsmsTracePayload(int window, List<TraceEvent> events) implements CustomPacketPayload {

    public static final int MAX_EVENTS = 64;
    private static final int MAX_NAME = 48;
    private static final int MAX_DETAIL = 8;

    public static final CustomPacketPayload.Type<IsmsTracePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "isms_trace"));

    public static final StreamCodec<RegistryFriendlyByteBuf, IsmsTracePayload> STREAM_CODEC =
            StreamCodec.of((buf, trace) -> trace.write(buf), IsmsTracePayload::read);

    public IsmsTracePayload {
        events = List.copyOf(events.subList(0, Math.min(MAX_EVENTS, events.size())));
    }

    @Override
    public CustomPacketPayload.Type<IsmsTracePayload> type() {
        return TYPE;
    }

    private static String clip(final String text) {
        return TextBounds.clip(text, MAX_NAME);
    }

    private void write(final RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(window);
        buf.writeVarInt(events.size());
        for (final TraceEvent event : events) {
            buf.writeVarInt(event.kind().id());
            TextCodecs.STREAM_CODEC.encode(buf, event.text());
            ByteBufCodecs.stringUtf8(MAX_NAME).encode(buf, clip(event.requester()));
            ByteBufCodecs.stringUtf8(MAX_NAME).encode(buf, clip(event.computer()));
            buf.writeVarLong(event.items() + 1L);
            buf.writeVarLong(event.duration() + 1L);
            buf.writeVarLong(event.tick());
            final List<Text> detail = event.detail().subList(0, Math.min(MAX_DETAIL, event.detail().size()));
            buf.writeVarInt(detail.size());
            for (final Text line : detail) {
                TextCodecs.STREAM_CODEC.encode(buf, line);
            }
        }
    }

    private static IsmsTracePayload read(final RegistryFriendlyByteBuf buf) {
        final int window = buf.readVarInt();
        final int count = Math.min(buf.readVarInt(), MAX_EVENTS);
        final List<TraceEvent> events = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final TraceEventClass kind = TraceEventClass.byId(buf.readVarInt());
            final Text text = TextCodecs.STREAM_CODEC.decode(buf);
            final String requester = ByteBufCodecs.stringUtf8(MAX_NAME).decode(buf);
            final String computer = ByteBufCodecs.stringUtf8(MAX_NAME).decode(buf);
            final long items = buf.readVarLong() - 1L;
            final long duration = buf.readVarLong() - 1L;
            final long tick = buf.readVarLong();
            final int lines = Math.min(buf.readVarInt(), MAX_DETAIL);
            final List<Text> detail = new ArrayList<>(lines);
            for (int l = 0; l < lines; l++) {
                detail.add(TextCodecs.STREAM_CODEC.decode(buf));
            }
            if (kind != null) {
                events.add(new TraceEvent(kind, text, requester, computer, items, duration, tick, detail));
            }
        }
        return new IsmsTracePayload(window, events);
    }
}
