/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.tests.TestBigPayloads;
import java.util.Arrays;

/**
 * A large value sent in pieces over a real connection: from the server to the player's game, a share of its pieces
 * each tick, and from the player's game back to the server, each arriving whole.
 */
public final class BigPayloadClientTests {

    private static final int SIZE = 400_000;

    private BigPayloadClientTests() {
    }

    @ClientTest(timeoutTicks = 200)
    public static void bigPayload_crossesTheConnectionBothWays(final ClientTestContext ctx) {
        final byte[] down = TestBigPayloads.noise(SIZE, 11L);
        final byte[] up = TestBigPayloads.noise(SIZE, 13L);
        ctx.thenServer(0, level -> TestBigPayloads.TO_CLIENT.sendToPlayer(
                        level.getServer().getPlayerList().getPlayers().getFirst(), down))
                .thenWaitUntil(() -> Arrays.equals(TestBigPayloads.lastOnClient(), down), 80,
                        "four hundred thousand bytes to reach the player's game whole")
                .then(0, () -> TestBigPayloads.TO_SERVER.sendToServer(ctx.mc().level.registryAccess(), up))
                .thenWaitUntilServer(level -> Arrays.equals(TestBigPayloads.lastOnServer(), up), 80,
                        "and four hundred thousand to reach the server whole",
                        level -> "the server has " + TestBigPayloads.lastOnServer().length + " bytes");
    }
}
