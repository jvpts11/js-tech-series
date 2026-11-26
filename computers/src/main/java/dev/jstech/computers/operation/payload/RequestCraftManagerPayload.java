/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.os.fs.FsPaths;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: request the Crafting Manager state for a Crafting Computer: the {@code .craft} files and folders
 * in one folder of a disk it reads, and the recipes each of its places keeps. The server replies with a
 * {@link CraftManagerStatePayload} that lists that folder and carries {@code view} back.
 *
 * @param hostPos the position of the Crafting Computer
 * @param source  the disk to list: {@link CraftManagerStatePayload#SYSTEM_DISK}, a medium's {@code media:} key, or
 *                {@code ""} for the one the computer shows first
 * @param folder  the folder on it, {@code ""} for its top
 * @param view    the asker's count of the places it has gone to, carried back so a reply about one it has left is
 *                not shown
 */
public record RequestCraftManagerPayload(BlockPos hostPos, String source, String folder, int view)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestCraftManagerPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath("jsc", "request_craft_manager"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestCraftManagerPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestCraftManagerPayload::hostPos,
                    ByteBufCodecs.stringUtf8(64), RequestCraftManagerPayload::source,
                    ByteBufCodecs.stringUtf8(FsPaths.MAX_PATH_LENGTH), RequestCraftManagerPayload::folder,
                    ByteBufCodecs.VAR_INT, RequestCraftManagerPayload::view,
                    RequestCraftManagerPayload::new);

    /** Asks for what the computer shows first, as a view that does not walk its disks does. */
    public static RequestCraftManagerPayload first(final BlockPos hostPos) {
        return new RequestCraftManagerPayload(hostPos, "", "", 0);
    }

    @Override
    public CustomPacketPayload.Type<RequestCraftManagerPayload> type() {
        return TYPE;
    }
}
