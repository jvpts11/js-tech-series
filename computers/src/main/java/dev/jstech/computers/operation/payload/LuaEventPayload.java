/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A key, a character, a paste or a click on the screen of the Lua program in front of a machine's
 * terminal, as the event ComputerCraft gives a program for it: its name, then what it carries (numbers,
 * text, a yes or no).
 *
 * <p>Only the events a keyboard and a mouse make can be sent this way; anything else a program hears
 * comes from the machine itself.
 */
public record LuaEventPayload(BlockPos hostPos, int session, String name, List<Object> values)
        implements CustomPacketPayload {

    /** The events a player's keyboard and mouse make. */
    public static final Set<String> NAMES = Set.of("key", "key_up", "char", "paste", "mouse_click", "mouse_up",
            "mouse_drag", "mouse_scroll", "terminate");

    /** The longest paste, as ComputerCraft allows it. */
    public static final int MAX_TEXT = 512;

    private static final int MAX_VALUES = 4;
    private static final byte LONG = 0;
    private static final byte TEXT = 1;
    private static final byte FLAG = 2;

    public LuaEventPayload {
        values = List.copyOf(values);
    }

    public static final CustomPacketPayload.Type<LuaEventPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "lua_event"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LuaEventPayload> STREAM_CODEC =
            StreamCodec.of(LuaEventPayload::write, LuaEventPayload::read);

    private static void write(final RegistryFriendlyByteBuf buf, final LuaEventPayload payload) {
        BlockPos.STREAM_CODEC.encode(buf, payload.hostPos());
        ByteBufCodecs.VAR_INT.encode(buf, payload.session());
        ByteBufCodecs.stringUtf8(16).encode(buf, payload.name());
        final int count = Math.min(MAX_VALUES, payload.values().size());
        ByteBufCodecs.VAR_INT.encode(buf, count);
        for (int i = 0; i < count; i++) {
            final Object value = payload.values().get(i);
            if (value instanceof Number number) {
                buf.writeByte(LONG);
                ByteBufCodecs.VAR_LONG.encode(buf, number.longValue());
            } else if (value instanceof Boolean flag) {
                buf.writeByte(FLAG);
                ByteBufCodecs.BOOL.encode(buf, flag);
            } else {
                final String text = String.valueOf(value);
                buf.writeByte(TEXT);
                ByteBufCodecs.stringUtf8(MAX_TEXT).encode(buf, text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text);
            }
        }
    }

    private static LuaEventPayload read(final RegistryFriendlyByteBuf buf) {
        final BlockPos host = BlockPos.STREAM_CODEC.decode(buf);
        final int session = ByteBufCodecs.VAR_INT.decode(buf);
        final String name = ByteBufCodecs.stringUtf8(16).decode(buf);
        final int count = Math.min(MAX_VALUES, ByteBufCodecs.VAR_INT.decode(buf));
        final List<Object> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final byte kind = buf.readByte();
            values.add(switch (kind) {
                case LONG -> ByteBufCodecs.VAR_LONG.decode(buf);
                case FLAG -> ByteBufCodecs.BOOL.decode(buf);
                default -> ByteBufCodecs.stringUtf8(MAX_TEXT).decode(buf);
            });
        }
        return new LuaEventPayload(host, session, name, values);
    }

    @Override
    public CustomPacketPayload.Type<LuaEventPayload> type() {
        return TYPE;
    }
}
