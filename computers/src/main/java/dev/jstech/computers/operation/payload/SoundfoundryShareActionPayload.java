/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.program.SoundfoundryState;
import dev.jstech.core.text.TextBounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * What a player did in Soundfoundry's sharing window, sent to the machine it runs on, which answers with how its
 * sharing stands.
 *
 * @param hostPos the machine
 * @param action  one of the constants below
 * @param index   for {@link #LOOK}, 1 when the window shows the songs this machine shares; for {@link #DOWNLOAD}, 1 to
 *                put the song on the playlist once it is kept
 * @param value   for {@link #DOWNLOAD}, where the computer sharing the song stands, or the catalogue's mark
 * @param text    what was searched for, or where the song to download is
 * @param indexes the places in the list of downloads an action is about
 */
public record SoundfoundryShareActionPayload(BlockPos hostPos, int action, int index, long value, String text,
                                             List<Integer> indexes) implements CustomPacketPayload {

    public static final int LOOK = 0;
    public static final int SEARCH = 1;
    public static final int DOWNLOAD = 2;
    public static final int REMOVE = 3;
    public static final int CLEAR = 4;

    public static final CustomPacketPayload.Type<SoundfoundryShareActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                    "soundfoundry_share_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoundfoundryShareActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SoundfoundryShareActionPayload::hostPos,
                    ByteBufCodecs.VAR_INT, SoundfoundryShareActionPayload::action,
                    ByteBufCodecs.VAR_INT, SoundfoundryShareActionPayload::index,
                    ByteBufCodecs.VAR_LONG, SoundfoundryShareActionPayload::value,
                    ByteBufCodecs.stringUtf8(SoundfoundryActionPayload.MAX_PATH), SoundfoundryShareActionPayload::text,
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(SoundfoundryState.MAX_DOWNLOADS)),
                    SoundfoundryShareActionPayload::indexes,
                    SoundfoundryShareActionPayload::new);

    /* Cut to what the wire takes here, since a string past its cap throws as it is sent. */
    public SoundfoundryShareActionPayload {
        text = TextBounds.clip(text, SoundfoundryActionPayload.MAX_PATH);
        indexes = List.copyOf(indexes.subList(0, Math.min(indexes.size(), SoundfoundryState.MAX_DOWNLOADS)));
    }

    /** An action about nothing but itself, or a switch. */
    public static SoundfoundryShareActionPayload of(final BlockPos host, final int action, final int index) {
        return new SoundfoundryShareActionPayload(host, action, index, 0L, "", List.of());
    }

    @Override
    public CustomPacketPayload.Type<SoundfoundryShareActionPayload> type() {
        return TYPE;
    }
}
