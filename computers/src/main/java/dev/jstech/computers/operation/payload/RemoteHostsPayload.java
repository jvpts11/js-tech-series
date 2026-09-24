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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server to client: the machines Remote Control can take over from this computer, every other
 * computer on the network, with what it runs and whether it is up.
 */
public record RemoteHostsPayload(List<Entry> hosts) implements CustomPacketPayload {

    /**
     * One remotely reachable machine: where it is, what kind of machine it is, read in the player's language, and
     * what it is running.
     */
    public record Entry(long pos, String hostname, Text type, String os, boolean running) {
    }

    public static final int MAX_HOSTS = 64;

    public static final CustomPacketPayload.Type<RemoteHostsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "remote_hosts"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Entry> ENTRY_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG, Entry::pos,
                    ByteBufCodecs.STRING_UTF8, Entry::hostname,
                    TextCodecs.STREAM_CODEC, Entry::type,
                    ByteBufCodecs.STRING_UTF8, Entry::os,
                    ByteBufCodecs.BOOL, Entry::running,
                    Entry::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoteHostsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ENTRY_CODEC.apply(ByteBufCodecs.list(MAX_HOSTS)), RemoteHostsPayload::hosts,
                    RemoteHostsPayload::new);

    /* Copied on the way in, so what the client is handed cannot change under it after it arrives. */
    public RemoteHostsPayload {
        hosts = List.copyOf(hosts);
    }

    @Override
    public CustomPacketPayload.Type<RemoteHostsPayload> type() {
        return TYPE;
    }
}
