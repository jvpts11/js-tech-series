/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextBounds;
import dev.jstech.core.text.TextCodecs;
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
 * <p>A notice from the machine's UPS carries its battery too, which each desktop shows in its own way: a line under the
 * words, or a bar.
 *
 * @param title          what the notice is headed with, read in the player's language
 * @param body           what it says, read the same way
 * @param opens          the program a click on it opens, or empty when clicking it only puts the notice away
 * @param batteryPercent how full the UPS's battery is, or {@link #NO_BATTERY} for a notice about anything else
 * @param batteryMinutes how long the battery lasts at the load, in whole minutes
 */
public record DesktopBalloonPayload(BlockPos hostPos, Text title, Text body, String opens, int batteryPercent,
                                    long batteryMinutes) implements CustomPacketPayload {

    /** The longest any part of a notice may be; past that it is a document and not a notice. */
    public static final int MAX_TEXT = 128;
    /** A notice with no battery to show. */
    public static final int NO_BATTERY = -1;

    public static final CustomPacketPayload.Type<DesktopBalloonPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "desktop_balloon"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DesktopBalloonPayload> STREAM_CODEC =
            StreamCodec.of(DesktopBalloonPayload::encode, DesktopBalloonPayload::decode);

    public DesktopBalloonPayload {
        batteryPercent = batteryPercent < 0 ? NO_BATTERY : Math.min(100, batteryPercent);
        batteryMinutes = Math.max(0L, batteryMinutes);
    }

    /** A notice with no battery to show. */
    public DesktopBalloonPayload(final BlockPos hostPos, final Text title, final Text body, final String opens) {
        this(hostPos, title, body, opens, NO_BATTERY, 0L);
    }

    @Override
    public CustomPacketPayload.Type<DesktopBalloonPayload> type() {
        return TYPE;
    }

    private static void encode(final RegistryFriendlyByteBuf buf, final DesktopBalloonPayload p) {
        buf.writeBlockPos(p.hostPos());
        TextCodecs.STREAM_CODEC.encode(buf, p.title());
        TextCodecs.STREAM_CODEC.encode(buf, p.body());
        buf.writeUtf(clip(p.opens()), MAX_TEXT);
        buf.writeVarInt(p.batteryPercent());
        buf.writeVarLong(p.batteryMinutes());
    }

    private static DesktopBalloonPayload decode(final RegistryFriendlyByteBuf buf) {
        return new DesktopBalloonPayload(buf.readBlockPos(), TextCodecs.STREAM_CODEC.decode(buf),
                TextCodecs.STREAM_CODEC.decode(buf), buf.readUtf(MAX_TEXT), buf.readVarInt(), buf.readVarLong());
    }

    /* Cut rather than refused: a notice about a machine with a very long name should still be said. */
    private static String clip(final String text) {
        return TextBounds.clip(text, MAX_TEXT);
    }
}
