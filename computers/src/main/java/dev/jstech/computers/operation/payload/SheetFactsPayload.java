/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server to client: what the network holds of the things a sheet asked about.
 *
 * <p>The names come back beside the counts so the sheet can match them up without trusting the order, and
 * a thing the network has none of comes back as nothing rather than being left out, which is the
 * difference between a cell saying zero and a cell saying it does not know.
 */
public record SheetFactsPayload(List<String> items, List<Long> counts,
                                long free, long servers) implements CustomPacketPayload {

    public SheetFactsPayload {
        items = List.copyOf(items);
        counts = List.copyOf(counts);
    }

    public static final CustomPacketPayload.Type<SheetFactsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "sheet_facts"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SheetFactsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(128).apply(
                            ByteBufCodecs.list(RequestSheetFactsPayload.MAX_ITEMS)),
                    SheetFactsPayload::items,
                    ByteBufCodecs.VAR_LONG.apply(
                            ByteBufCodecs.list(RequestSheetFactsPayload.MAX_ITEMS)),
                    SheetFactsPayload::counts,
                    ByteBufCodecs.VAR_LONG, SheetFactsPayload::free,
                    ByteBufCodecs.VAR_LONG, SheetFactsPayload::servers,
                    SheetFactsPayload::new);

    @Override
    public CustomPacketPayload.Type<SheetFactsPayload> type() {
        return TYPE;
    }
}
