/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.printer;

import dev.jstech.computers.printer.PrintedDocument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: a program of the computer at {@code hostPos} prints a document. The server lays the text out on
 * pages itself, so what the printer turns out never depends on what a client says the pages are.
 *
 * @param hostPos     the computer
 * @param printer     the printer's packed position, or -1 for the computer's first
 * @param title       the document's title
 * @param program     the program printing it
 * @param text        its text, empty for a picture
 * @param picture     a picture as Paint saves it, or empty
 * @param pictureName the picture's file name, or empty
 * @param copies      how many copies
 * @param landscape   whether its pages lie on their side
 * @param fromPage    the first page to print, from 1; 0 prints them all
 * @param toPage      the last page to print, from 1; 0 runs to the end
 */
public record PrintPayload(BlockPos hostPos, long printer, String title, String program, String text, String picture,
                           String pictureName, int copies, boolean landscape, int fromPage, int toPage)
        implements CustomPacketPayload {

    /** The most text one document carries, as much as a file the editors save. */
    public static final int MAX_TEXT = 32_768;

    public static final CustomPacketPayload.Type<PrintPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "print"));

    // Written by hand: composite() tops out at six pairs, and a print job carries eleven fields.
    public static final StreamCodec<RegistryFriendlyByteBuf, PrintPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                BlockPos.STREAM_CODEC.encode(buf, p.hostPos());
                buf.writeLong(p.printer());
                buf.writeUtf(p.title(), PrintedDocument.MAX_NAME);
                buf.writeUtf(p.program(), PrintedDocument.MAX_NAME);
                buf.writeUtf(p.text(), MAX_TEXT);
                buf.writeUtf(p.picture(), PrintedDocument.MAX_PICTURE);
                buf.writeUtf(p.pictureName(), PrintedDocument.MAX_NAME);
                buf.writeVarInt(p.copies());
                buf.writeBoolean(p.landscape());
                buf.writeVarInt(p.fromPage());
                buf.writeVarInt(p.toPage());
            },
            buf -> new PrintPayload(BlockPos.STREAM_CODEC.decode(buf), buf.readLong(),
                    buf.readUtf(PrintedDocument.MAX_NAME), buf.readUtf(PrintedDocument.MAX_NAME), buf.readUtf(MAX_TEXT),
                    buf.readUtf(PrintedDocument.MAX_PICTURE), buf.readUtf(PrintedDocument.MAX_NAME), buf.readVarInt(),
                    buf.readBoolean(), buf.readVarInt(), buf.readVarInt()));

    /** A text document from {@code program}, every page, one copy, upright, to the computer's first printer. */
    public static PrintPayload text(final BlockPos host, final String title, final String program, final String text) {
        return new PrintPayload(host, -1L, title, program, text, "", "", 1, false, 0, 0);
    }

    @Override
    public CustomPacketPayload.Type<PrintPayload> type() {
        return TYPE;
    }
}
