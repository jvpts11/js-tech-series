/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.core.text.TextBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: what the Nextgre Planner Studio window {@code window} on the computer at {@code hostPos} asks of
 * its network's NextgreIQL: where things stand, a statement's plan explained, or explained and run, a rule of the
 * planner switched, the statistics gathered afresh, or a plan of the history opened.
 *
 * @param action what is asked, one of the constants here
 * @param arg    what it is asked of: the statement, the rule's id, the plan's number; empty where nothing is
 */
public record NextgreActionPayload(BlockPos hostPos, int window, int action, String arg)
        implements CustomPacketPayload {

    /** Where things stand; {@code arg} is the number of the plan the window shows, to bring it up to date. */
    public static final int REFRESH = 0;
    /** The plan of the statement in {@code arg}, not run. */
    public static final int EXPLAIN = 1;
    /** The plan of the statement in {@code arg}, run and measured. */
    public static final int EXPLAIN_ANALYZE = 2;
    /** The rule {@code arg} switched the other way. */
    public static final int TOGGLE_RULE = 3;
    /** The statistics gathered afresh. */
    public static final int ANALYZE = 4;
    /** The plan numbered {@code arg}, out of the history. */
    public static final int OPEN = 5;

    /** The longest statement a studio sends, with room for the words the studio puts before it. */
    public static final int MAX_ARG = RunIqlPayload.MAX_LEN + 32;

    public static final CustomPacketPayload.Type<NextgreActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "nextgre_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NextgreActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, NextgreActionPayload::hostPos,
                    ByteBufCodecs.VAR_INT, NextgreActionPayload::window,
                    ByteBufCodecs.VAR_INT, NextgreActionPayload::action,
                    ByteBufCodecs.stringUtf8(MAX_ARG), NextgreActionPayload::arg,
                    NextgreActionPayload::new);

    public NextgreActionPayload {
        arg = TextBounds.clip(arg, MAX_ARG);
    }

    @Override
    public CustomPacketPayload.Type<NextgreActionPayload> type() {
        return TYPE;
    }
}
