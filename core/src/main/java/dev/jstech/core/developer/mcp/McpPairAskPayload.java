/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import dev.jstech.core.JsCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A dedicated server's MCP server asks an operator in the game whether a client on the server's computer may come in.
 *
 * @param id      the question's number, which the answer carries back
 * @param name    the client's name, as it gave it
 * @param version its version
 */
public record McpPairAskPayload(int id, String name, String version) implements CustomPacketPayload {

    /** The longest a client's name and version are carried. */
    public static final int LONGEST = 64;

    public static final CustomPacketPayload.Type<McpPairAskPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "mcp_pair_ask"));

    public static final StreamCodec<RegistryFriendlyByteBuf, McpPairAskPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, McpPairAskPayload::id,
            ByteBufCodecs.stringUtf8(LONGEST), McpPairAskPayload::name,
            ByteBufCodecs.stringUtf8(LONGEST), McpPairAskPayload::version,
            McpPairAskPayload::new);

    public McpPairAskPayload {
        name = name.length() > LONGEST ? name.substring(0, LONGEST) : name;
        version = version.length() > LONGEST ? version.substring(0, LONGEST) : version;
    }

    @Override
    public CustomPacketPayload.Type<McpPairAskPayload> type() {
        return TYPE;
    }
}
