/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.JsComputers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A player asking what a ComputerCraft computer holds in one of its folders.
 *
 * <p>The answer cannot come back with the asking, because it is another mod's computer that has to be
 * asked and it answers when it gets to it. So this only starts the asking; what comes back arrives on
 * its own as a {@link CcFilesPayload}, and the explorer shows that it is waiting in the meantime.
 *
 * @param hostPos  the machine whose desktop the player has open, which is what gives them the right to ask
 * @param computer the number of the computer over there
 * @param path     the folder there, as that computer writes it
 */
public record RequestCcFilesPayload(BlockPos hostPos, int computer, String path) implements CustomPacketPayload {

    /** The longest path this carries; a folder name over there is not a place for a payload's worth of text. */
    public static final int PATH_MAX = 256;

    public static final CustomPacketPayload.Type<RequestCcFilesPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "request_cc_files"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestCcFilesPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestCcFilesPayload::hostPos,
                    ByteBufCodecs.VAR_INT, RequestCcFilesPayload::computer,
                    ByteBufCodecs.stringUtf8(PATH_MAX), RequestCcFilesPayload::path,
                    RequestCcFilesPayload::new);

    public RequestCcFilesPayload {
        path = path == null ? "" : path.length() > PATH_MAX ? path.substring(0, PATH_MAX) : path;
    }

    @Override
    public CustomPacketPayload.Type<RequestCcFilesPayload> type() {
        return TYPE;
    }
}
