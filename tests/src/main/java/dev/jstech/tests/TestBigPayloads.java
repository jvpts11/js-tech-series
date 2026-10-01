/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.network.transfer.BigPayload;
import dev.jstech.core.network.transfer.BigPayloads;
import java.util.Random;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;

/**
 * Large values the tests send: bytes to a player's game and bytes to the server, each side keeping the last it got.
 */
public final class TestBigPayloads {

    /** Bytes sent to a player's game. */
    public static final BigPayload<byte[]> TO_CLIENT = BigPayloads.declare(
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "bytes_to_client"), ByteBufCodecs.BYTE_ARRAY.cast());
    /** Bytes sent to the server. */
    public static final BigPayload<byte[]> TO_SERVER = BigPayloads.declare(
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "bytes_to_server"), ByteBufCodecs.BYTE_ARRAY.cast());

    private static volatile byte[] lastOnClient = new byte[0];
    private static volatile byte[] lastOnServer = new byte[0];

    private TestBigPayloads() {
    }

    /** Declares the kinds and keeps what each side gets. */
    public static void declare() {
        TO_CLIENT.onReceive((bytes, context) -> lastOnClient = bytes);
        TO_SERVER.onReceive((bytes, context) -> lastOnServer = bytes);
    }

    /** The last bytes the player's game got. */
    public static byte[] lastOnClient() {
        return lastOnClient;
    }

    /** The last bytes the server got. */
    public static byte[] lastOnServer() {
        return lastOnServer;
    }

    /** {@code length} bytes that do not pack small, the same for the same seed. */
    public static byte[] noise(final int length, final long seed) {
        final byte[] bytes = new byte[length];
        new Random(seed).nextBytes(bytes);
        return bytes;
    }
}
