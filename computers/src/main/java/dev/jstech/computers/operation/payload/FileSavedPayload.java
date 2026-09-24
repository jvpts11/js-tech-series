/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what came of a save, or of packing or unpacking an archive: whether it worked, and a short
 * line for the window's status bar, read in the player's language.
 *
 * <p>The line travels as text rather than as capped words: a save names its path, and a path near the longest one
 * allowed made a line past the old cap, which failed to send and left the window waiting.
 */
@TextHolder
public record FileSavedPayload(boolean ok, Text message) implements CustomPacketPayload {

    public static final TextKey SAVED = TextKey.of("jsc.file_saved.saved", "Saved %s");
    public static final TextKey NO_COMPUTER = TextKey.of("jsc.file_saved.no_computer", "No computer");
    public static final TextKey NO_LIVE_MEDIUM =
            TextKey.of("jsc.file_saved.no_live_medium", "No live medium is booted");
    public static final TextKey NO_SHELL = TextKey.of("jsc.file_saved.no_shell", "No shell");
    public static final TextKey NO_MEDIUM = TextKey.of("jsc.file_saved.no_medium", "No medium");
    public static final TextKey NO_SYSTEM_DISK = TextKey.of("jsc.file_saved.no_system_disk", "No system disk");
    public static final TextKey TYPE_READ_ONLY = TextKey.of("jsc.file_saved.type_read_only", ".%s is read-only");
    public static final TextKey INVALID_NAME = TextKey.of("jsc.file_saved.invalid_name", "Invalid file name");
    public static final TextKey NO_ROOM = TextKey.of("jsc.file_saved.no_room", "Not enough free space");
    public static final TextKey READ_ONLY = TextKey.of("jsc.file_saved.read_only", "Read-only");

    public static final CustomPacketPayload.Type<FileSavedPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "file_saved"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FileSavedPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, FileSavedPayload::ok,
                    TextCodecs.STREAM_CODEC, FileSavedPayload::message,
                    FileSavedPayload::new);

    @Override
    public CustomPacketPayload.Type<FileSavedPayload> type() {
        return TYPE;
    }
}
