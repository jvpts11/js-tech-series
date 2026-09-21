/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: what is in the trash of the computer at {@code hostPos}, in the order it went in. */
public record TrashListingPayload(BlockPos hostPos, List<WireEntry> entries) implements CustomPacketPayload {

    /** The most things one listing carries; a trash holding more lists the ones that went in first. */
    public static final int MAX_ENTRIES = 256;

    /** The longest old place a listing carries, which is the longest path any file payload carries. */
    public static final int MAX_ORIGINAL = 160;

    public static final CustomPacketPayload.Type<TrashListingPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "trash_listing"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrashListingPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, TrashListingPayload::hostPos,
                    WireEntry.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ENTRIES)), TrashListingPayload::entries,
                    TrashListingPayload::new);

    /**
     * One thing in the trash as a window shows it.
     *
     * @param stored    the name it is kept under, which is how a window names it back
     * @param original  where it was, as a path under the root; cut to what a listing carries, since it is only shown
     * @param directory whether it is a folder
     * @param weight    the room it takes
     */
    public record WireEntry(String stored, String original, boolean directory, long weight) {

        public static final StreamCodec<RegistryFriendlyByteBuf, WireEntry> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(64), WireEntry::stored,
                        ByteBufCodecs.stringUtf8(MAX_ORIGINAL), WireEntry::original,
                        ByteBufCodecs.BOOL, WireEntry::directory,
                        ByteBufCodecs.VAR_LONG, WireEntry::weight,
                        WireEntry::new);

        /* Cut here rather than refused on the wire: a limit on a string there drops the connection. */
        public WireEntry {
            original = original.length() <= MAX_ORIGINAL ? original : original.substring(0, MAX_ORIGINAL);
        }
    }

    public TrashListingPayload {
        entries = List.copyOf(entries.size() <= MAX_ENTRIES ? entries : entries.subList(0, MAX_ENTRIES));
    }

    @Override
    public CustomPacketPayload.Type<TrashListingPayload> type() {
        return TYPE;
    }
}
