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

/**
 * Server to client: the content of a file the Editor asked to open. {@code exists} is false when the
 * path had no readable user file (so the Editor opens it as a new, empty file with that name).
 *
 * <p>{@code tooLarge} says the file is really there but is past what this packet may carry. It comes back
 * empty then, and whoever asked has to say so rather than show an empty page.
 */
public record FileContentPayload(String path, String content, boolean exists,
                                 boolean tooLarge) implements CustomPacketPayload {

    /** A file that came back whole. */
    public FileContentPayload(final String path, final String content, final boolean exists) {
        this(path, content, exists, false);
    }

    public static final int MAX_CONTENT = 32768;

    /**
     * What a file too big to carry comes back as.
     *
     * <p>The cap above throws when it is handed more than it takes, so a file past it used to take the
     * packet down rather than come back. It comes back empty and saying so instead, and whoever asked can
     * say why rather than showing an empty page that would overwrite the real one if it were saved.
     */
    public static FileContentPayload tooLarge(final String path) {
        return new FileContentPayload(path, "", true, true);
    }

    public static final CustomPacketPayload.Type<FileContentPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "file_content"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FileContentPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(160), FileContentPayload::path,
                    ByteBufCodecs.stringUtf8(MAX_CONTENT), FileContentPayload::content,
                    ByteBufCodecs.BOOL, FileContentPayload::exists,
                    ByteBufCodecs.BOOL, FileContentPayload::tooLarge,
                    FileContentPayload::new);

    @Override
    public CustomPacketPayload.Type<FileContentPayload> type() {
        return TYPE;
    }
}
