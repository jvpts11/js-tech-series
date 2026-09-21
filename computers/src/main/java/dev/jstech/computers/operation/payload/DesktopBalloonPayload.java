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
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the machine has something to say from the corner of its own desktop.
 *
 * <p>A notice rather than a question. The desktop stays usable under it, it goes away by itself, and where it
 * carries the name of a program a click on it opens that program. It is how one of the editions greeted its
 * owner on a first start, instead of putting a window in front of them before they had touched anything.
 *
 * @param opens the program a click on it opens, or empty when clicking it only puts the notice away
 */
public record DesktopBalloonPayload(BlockPos hostPos, String title, String body, String opens)
        implements CustomPacketPayload {

    /** The longest any part of a notice may be; past that it is a document and not a notice. */
    public static final int MAX_TEXT = 128;

    public static final CustomPacketPayload.Type<DesktopBalloonPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "desktop_balloon"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DesktopBalloonPayload> STREAM_CODEC =
            StreamCodec.of(DesktopBalloonPayload::encode, DesktopBalloonPayload::decode);

    @Override
    public CustomPacketPayload.Type<DesktopBalloonPayload> type() {
        return TYPE;
    }

    private static void encode(final RegistryFriendlyByteBuf buf, final DesktopBalloonPayload p) {
        buf.writeBlockPos(p.hostPos());
        buf.writeUtf(clip(p.title()), MAX_TEXT);
        buf.writeUtf(clip(p.body()), MAX_TEXT);
        buf.writeUtf(clip(p.opens()), MAX_TEXT);
    }

    private static DesktopBalloonPayload decode(final RegistryFriendlyByteBuf buf) {
        return new DesktopBalloonPayload(buf.readBlockPos(), buf.readUtf(MAX_TEXT), buf.readUtf(MAX_TEXT),
                buf.readUtf(MAX_TEXT));
    }

    /* Cut rather than refused: a notice about a machine with a very long name should still be said. */
    private static String clip(final String text) {
        return text.length() <= MAX_TEXT ? text : text.substring(0, MAX_TEXT);
    }
}
