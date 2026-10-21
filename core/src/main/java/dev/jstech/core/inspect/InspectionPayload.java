/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.inspect;

import dev.jstech.core.JsCore;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the place a player watches, as the inspection reads it now, and what the server sent that player
 * in its last tick; empty once they stop watching.
 *
 * @param pos   the place
 * @param lines what the systems say of it, line by line
 * @param sent  what the server sent the player in its last tick, line by line
 */
public record InspectionPayload(BlockPos pos, List<Line> lines, List<Line> sent) implements CustomPacketPayload {

    /** The most lines a payload carries of each. */
    public static final int MOST_LINES = 200;
    /** The longest a value is carried. */
    public static final int LONGEST_VALUE = 256;

    public static final CustomPacketPayload.Type<InspectionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "inspection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InspectionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, InspectionPayload::pos,
            Line.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_LINES)), InspectionPayload::lines,
            Line.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_LINES)), InspectionPayload::sent,
            InspectionPayload::new);

    public InspectionPayload {
        lines = List.copyOf(lines.subList(0, Math.min(MOST_LINES, lines.size())));
        sent = List.copyOf(sent.subList(0, Math.min(MOST_LINES, sent.size())));
    }

    /** That the player watches nothing now. */
    public static InspectionPayload stopped() {
        return new InspectionPayload(BlockPos.ZERO, List.of(), List.of());
    }

    @Override
    public CustomPacketPayload.Type<InspectionPayload> type() {
        return TYPE;
    }

    /**
     * One line as it travels.
     *
     * @param depth how far under another line it is
     * @param name  what it names
     * @param value its value
     */
    public record Line(int depth, Component name, String value) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Line> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Line::depth,
                ComponentSerialization.TRUSTED_STREAM_CODEC, Line::name,
                ByteBufCodecs.stringUtf8(LONGEST_VALUE), Line::value,
                Line::new);

        public Line {
            value = value.length() > LONGEST_VALUE ? value.substring(0, LONGEST_VALUE) : value;
        }
    }
}
