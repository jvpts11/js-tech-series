/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import dev.jstech.core.JsCore;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * An operator's answer to a dedicated server's question whether a client may come in.
 *
 * @param id     the question's number
 * @param answer {@link #REFUSE}, {@link #READ} or {@link #ACT}
 */
public record McpPairAnswerPayload(int id, String answer) implements CustomPacketPayload {

    /** The answers, as they are carried. */
    public static final String REFUSE = "refuse";
    public static final String READ = "read";
    public static final String ACT = "act";

    public static final CustomPacketPayload.Type<McpPairAnswerPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "mcp_pair_answer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, McpPairAnswerPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, McpPairAnswerPayload::id,
                    ByteBufCodecs.stringUtf8(8), McpPairAnswerPayload::answer,
                    McpPairAnswerPayload::new);

    /** The answer carried for {@code permission}, or for a refusal when none. */
    public static String of(final Optional<McpPermission> permission) {
        return permission.map(given -> given == McpPermission.ACT ? ACT : READ).orElse(REFUSE);
    }

    /** What the answer lets the client do, or none for a refusal or an answer that is none of them. */
    public Optional<McpPermission> permission() {
        return switch (this.answer) {
            case READ -> Optional.of(McpPermission.READ);
            case ACT -> Optional.of(McpPermission.ACT);
            default -> Optional.empty();
        };
    }

    @Override
    public CustomPacketPayload.Type<McpPairAnswerPayload> type() {
        return TYPE;
    }
}
