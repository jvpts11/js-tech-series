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
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: something the player did in the Gateway Manager to one Gateway. Every action is
 * answered with a fresh state for the same selection.
 *
 * @param hostPos    the computer running the manager
 * @param gatewayPos the Gateway acted on, as a long
 * @param action     one of the {@code ACTION_*} constants
 * @param value      the number an action carries (a switch position, a knob index), else 0
 * @param text       the text an action carries (a new name), else empty
 */
public record GatewayManagerActionPayload(BlockPos hostPos, long gatewayPos, int action, int value, String text)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<GatewayManagerActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "gateway_manager_action"));

    public static final int MAX_TEXT = 32;

    public static final int ACTION_REFRESH = 0;
    public static final int ACTION_RENAME = 1;
    public static final int ACTION_IDENTIFY = 2;
    public static final int ACTION_SET_READ = 3;
    public static final int ACTION_SET_OPERATIONS = 4;
    public static final int ACTION_SET_FILES = 5;
    public static final int ACTION_SET_CEILING = 6;
    public static final int ACTION_SET_CAP = 7;
    public static final int ACTION_CLEAR_BUFFER = 8;
    public static final int ACTION_TURN_ON = 9;
    public static final int ACTION_REBOOT = 10;
    public static final int ACTION_SHUTDOWN = 11;
    public static final int ACTION_TEST_EVENT = 12;

    public static final StreamCodec<RegistryFriendlyByteBuf, GatewayManagerActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, GatewayManagerActionPayload::hostPos,
                    ByteBufCodecs.VAR_LONG, GatewayManagerActionPayload::gatewayPos,
                    ByteBufCodecs.VAR_INT, GatewayManagerActionPayload::action,
                    ByteBufCodecs.VAR_INT, GatewayManagerActionPayload::value,
                    ByteBufCodecs.stringUtf8(MAX_TEXT), GatewayManagerActionPayload::text,
                    GatewayManagerActionPayload::new);

    public static GatewayManagerActionPayload of(final BlockPos hostPos, final long gatewayPos, final int action) {
        return new GatewayManagerActionPayload(hostPos, gatewayPos, action, 0, "");
    }

    public static GatewayManagerActionPayload valued(final BlockPos hostPos, final long gatewayPos, final int action,
                                                     final int value) {
        return new GatewayManagerActionPayload(hostPos, gatewayPos, action, value, "");
    }

    @Override
    public CustomPacketPayload.Type<GatewayManagerActionPayload> type() {
        return TYPE;
    }
}
