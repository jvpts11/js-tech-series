/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.core.network.transfer.BigPayloads;
import dev.jstech.core.network.transfer.BigPiecePayload;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestBigPayloads;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A large value sent in pieces: written, packed and cut into more than one piece, and put back together into the same
 * value; pieces out of order, or a piece that says the sending is another size, refused.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class BigPayloadGameTests {

    private static final String ARENA = "empty";
    private static final int NOISE = 200_000;

    private BigPayloadGameTests() {
    }

    @GameTest(template = ARENA)
    public static void bigPayload_comesBackWholeFromItsPieces(final GameTestHelper helper) throws IOException {
        final RegistryAccess registries = helper.getLevel().registryAccess();
        final byte[] noise = TestBigPayloads.noise(NOISE, 7L);

        final List<BigPiecePayload> pieces = TestBigPayloads.TO_CLIENT.pieces(noise, registries);

        helper.assertTrue(pieces.size() > 1, "two hundred thousand bytes go in more than one piece: " + pieces.size());
        helper.assertTrue(Arrays.equals(BigPayloads.assemble(TestBigPayloads.TO_CLIENT, pieces, registries), noise),
                "and come back the same");

        final byte[] repeats = new byte[NOISE * 10];
        final List<BigPiecePayload> packed = TestBigPayloads.TO_CLIENT.pieces(repeats, registries);
        helper.assertTrue(packed.size() == 1, "two million bytes of nothing pack into one piece: " + packed.size());
        helper.assertTrue(Arrays.equals(BigPayloads.assemble(TestBigPayloads.TO_CLIENT, packed, registries),
                repeats), "and unpack the same");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void bigPayload_refusesPiecesOutOfOrderOrOfAnotherSize(final GameTestHelper helper) {
        final RegistryAccess registries = helper.getLevel().registryAccess();
        final List<BigPiecePayload> pieces = TestBigPayloads.TO_CLIENT.pieces(TestBigPayloads.noise(NOISE, 9L),
                registries);

        final List<BigPiecePayload> swapped = new ArrayList<>(pieces);
        swapped.set(0, pieces.get(1));
        swapped.set(1, pieces.get(0));
        helper.assertTrue(refused(swapped, registries), "pieces out of order are refused");

        final List<BigPiecePayload> resized = new ArrayList<>(pieces);
        final BigPiecePayload last = pieces.getLast();
        resized.set(resized.size() - 1, new BigPiecePayload(last.kind(), last.transfer(), last.index(),
                last.count() + 1, last.data()));
        helper.assertTrue(refused(resized, registries), "a piece that says the sending is another size is refused");

        helper.assertTrue(refused(pieces.subList(0, pieces.size() - 1), registries),
                "a sending that lacks its last piece is refused");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void bigPayload_refusesAtTheSenderWhatPacksPastTheLimit(final GameTestHelper helper) {
        final RegistryAccess registries = helper.getLevel().registryAccess();
        final byte[] tooBig = TestBigPayloads.noise(BigPayloads.MOST_PACKED + 1024 * 1024, 5L);
        boolean refusedAtSender = false;
        try {
            TestBigPayloads.TO_CLIENT.pieces(tooBig, registries);
        } catch (final IllegalArgumentException e) {
            refusedAtSender = true;
        }
        helper.assertTrue(refusedAtSender, "bytes that do not pack under the limit are refused where they are sent");
        helper.succeed();
    }

    private static boolean refused(final List<BigPiecePayload> pieces, final RegistryAccess registries) {
        try {
            BigPayloads.assemble(TestBigPayloads.TO_CLIENT, pieces, registries);
            return false;
        } catch (final IOException e) {
            return true;
        }
    }
}
