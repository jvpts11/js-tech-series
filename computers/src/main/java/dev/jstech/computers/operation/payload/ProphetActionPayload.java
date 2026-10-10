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
 * Client to server: what the Prophet Reactive Console window {@code window} on the computer at {@code hostPos} asks of
 * its network's Prophet YourIQL: where its states stand, a statement applied or only checked, a state or a watch let
 * go, or its settings changed.
 *
 * @param action what is asked, one of the constants here
 * @param arg    what it is asked of: the statement, the item, the settings; empty where nothing is
 */
public record ProphetActionPayload(BlockPos hostPos, int window, int action, String arg)
        implements CustomPacketPayload {

    /** Where the states stand. */
    public static final int REFRESH = 0;
    /** The statement in {@code arg}, applied. */
    public static final int APPLY = 1;
    /** The statement in {@code arg}, read without applying it. */
    public static final int CHECK = 2;
    /** The state of the item {@code arg} let go, or the watch {@code WATCH n}. */
    public static final int FORGET = 3;
    /** The settings, written {@code interval;batch;reacting}. */
    public static final int SETTINGS = 4;

    /** The longest statement a console sends. */
    public static final int MAX_ARG = RunIqlPayload.MAX_LEN;

    public static final CustomPacketPayload.Type<ProphetActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "prophet_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProphetActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ProphetActionPayload::hostPos,
                    ByteBufCodecs.VAR_INT, ProphetActionPayload::window,
                    ByteBufCodecs.VAR_INT, ProphetActionPayload::action,
                    ByteBufCodecs.stringUtf8(MAX_ARG), ProphetActionPayload::arg,
                    ProphetActionPayload::new);

    public ProphetActionPayload {
        arg = TextBounds.clip(arg, MAX_ARG);
    }

    @Override
    public CustomPacketPayload.Type<ProphetActionPayload> type() {
        return TYPE;
    }
}
