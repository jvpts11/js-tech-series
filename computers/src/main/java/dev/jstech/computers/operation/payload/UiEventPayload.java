/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * What a player did to a widget of a Cannon program's window: which window, which widget, and what it
 * was (a button pressed, a line typed, a box ticked, a row picked, a place on a canvas, a window closed).
 *
 * @param said   what the change was, for what carries words: a line typed
 * @param number what the change was, for what carries numbers: the row picked, or where on a canvas
 */
public record UiEventPayload(BlockPos hostPos, int program, long window, long widget, String kind, String said,
                             int number, int second) implements CustomPacketPayload {

    /** What a player's hands can do to a widget; nothing else is taken. */
    public static final Set<String> KINDS = Set.of("click", "text", "submit", "toggle", "select", "close");

    /** The longest line a player can put in a program's box. */
    public static final int MAX_TEXT = 256;

    public static final CustomPacketPayload.Type<UiEventPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "ui_event"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UiEventPayload> STREAM_CODEC =
            StreamCodec.of(UiEventPayload::write, UiEventPayload::read);

    /** What the event carries, as the runtime hands it to the program. */
    public List<Object> values() {
        return switch (this.kind) {
            case "text", "submit" -> List.of(this.said);
            case "toggle" -> List.of(this.number != 0);
            case "select" -> List.of(this.number);
            case "click" -> this.number == 0 && this.second == 0 ? List.of() : List.of(this.number, this.second);
            default -> List.of();
        };
    }

    private static void write(final RegistryFriendlyByteBuf buf, final UiEventPayload payload) {
        BlockPos.STREAM_CODEC.encode(buf, payload.hostPos());
        ByteBufCodecs.VAR_INT.encode(buf, payload.program());
        ByteBufCodecs.VAR_LONG.encode(buf, payload.window());
        ByteBufCodecs.VAR_LONG.encode(buf, payload.widget());
        ByteBufCodecs.stringUtf8(16).encode(buf, payload.kind());
        ByteBufCodecs.stringUtf8(MAX_TEXT).encode(buf, payload.said());
        ByteBufCodecs.VAR_INT.encode(buf, payload.number());
        ByteBufCodecs.VAR_INT.encode(buf, payload.second());
    }

    private static UiEventPayload read(final RegistryFriendlyByteBuf buf) {
        return new UiEventPayload(BlockPos.STREAM_CODEC.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf),
                ByteBufCodecs.stringUtf8(16).decode(buf), ByteBufCodecs.stringUtf8(MAX_TEXT).decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf));
    }

    @Override
    public CustomPacketPayload.Type<UiEventPayload> type() {
        return TYPE;
    }
}
