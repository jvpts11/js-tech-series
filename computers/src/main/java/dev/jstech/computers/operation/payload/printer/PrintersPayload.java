/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.printer;

import dev.jstech.computers.printer.PrintedDocument;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the printers the computer at {@code hostPos} can print on, as its Print dialog lists them.
 *
 * @param hostPos  the computer
 * @param machine  the computer's host name, which the dialog says the printers are on
 * @param printers its printers, in the order the server finds them, the first being the default
 */
public record PrintersPayload(BlockPos hostPos, String machine, List<Row> printers) implements CustomPacketPayload {

    /** The most printers one computer lists. */
    public static final int MAX_PRINTERS = 16;

    public static final CustomPacketPayload.Type<PrintersPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "printers"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PrintersPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PrintersPayload::hostPos,
            ByteBufCodecs.stringUtf8(PrintedDocument.MAX_NAME), PrintersPayload::machine,
            Row.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_PRINTERS)), PrintersPayload::printers,
            PrintersPayload::new);

    public PrintersPayload {
        printers = List.copyOf(printers);
    }

    @Override
    public CustomPacketPayload.Type<PrintersPayload> type() {
        return TYPE;
    }

    /**
     * One printer.
     *
     * @param pos    its packed position
     * @param model  its {@code PrinterModel} name
     * @param status what it is doing
     * @param port   the port it is on
     * @param paper  the sheets in its tray
     */
    public record Row(long pos, String model, Text status, String port, int paper) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Row> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, Row::pos,
                ByteBufCodecs.stringUtf8(48), Row::model,
                TextCodecs.STREAM_CODEC, Row::status,
                ByteBufCodecs.stringUtf8(16), Row::port,
                ByteBufCodecs.VAR_INT, Row::paper,
                Row::new);
    }
}
