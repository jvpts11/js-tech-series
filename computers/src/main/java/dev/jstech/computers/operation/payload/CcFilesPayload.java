/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.JsComputers;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * What a ComputerCraft computer said its folder holds, on its way to the player who asked.
 *
 * <p>It arrives when that computer gets round to answering, which is why it is its own thing rather than
 * a reply: the explorer asked some ticks ago and has been showing that it is waiting since.
 *
 * @param computer the number of the computer over there
 * @param path     the folder that was asked about
 * @param names    what it holds, a folder ending in {@code /}
 * @param message  why there is nothing, when nothing came back
 */
public record CcFilesPayload(int computer, String path, List<String> names, String message)
        implements CustomPacketPayload {

    /** The most a listing carries; a folder with more than this is not something an explorer shows. */
    public static final int NAMES_MAX = 512;
    private static final int NAME_MAX = 128;
    private static final int MESSAGE_MAX = 160;

    public static final CustomPacketPayload.Type<CcFilesPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "cc_files"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CcFilesPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CcFilesPayload::computer,
                    ByteBufCodecs.stringUtf8(RequestCcFilesPayload.PATH_MAX), CcFilesPayload::path,
                    ByteBufCodecs.stringUtf8(NAME_MAX).apply(ByteBufCodecs.list(NAMES_MAX)), CcFilesPayload::names,
                    ByteBufCodecs.stringUtf8(MESSAGE_MAX), CcFilesPayload::message,
                    CcFilesPayload::new);

    public CcFilesPayload {
        path = cut(path, RequestCcFilesPayload.PATH_MAX);
        message = cut(message, MESSAGE_MAX);
        final List<String> kept = new java.util.ArrayList<>();
        for (final String name : names == null ? List.<String>of() : names) {
            if (kept.size() >= NAMES_MAX) {
                break;
            }
            kept.add(cut(name, NAME_MAX));
        }
        names = List.copyOf(kept);
    }

    private static String cut(final String text, final int most) {
        if (text == null) {
            return "";
        }
        return text.length() > most ? text.substring(0, most) : text;
    }

    @Override
    public CustomPacketPayload.Type<CcFilesPayload> type() {
        return TYPE;
    }
}
