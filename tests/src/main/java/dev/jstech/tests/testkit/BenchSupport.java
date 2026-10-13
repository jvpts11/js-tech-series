/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;

/** What every benchmark test measures and guards with, kept once beside {@link BenchReport}. */
public final class BenchSupport {

    private BenchSupport() {
    }

    /** Turns any crash in a measurement step into a test failure instead of a server crash. */
    public static void guarded(final Runnable step) {
        try {
            step.run();
        } catch (final GameTestAssertException e) {
            throw e;
        } catch (final RuntimeException e) {
            throw new GameTestAssertException("benchmark step crashed: " + e);
        }
    }

    /**
     * The server's mean tick over the ticks the game averages over (the last hundred). A measurement must therefore
     * be taken at least that many ticks after any one-off heavy tick, or that tick's whole cost is divided into the
     * average and reported as if the server were doing it every tick.
     */
    public static double averageTickMs(final MinecraftServer server) {
        return server.getAverageTickTimeNanos() / 1_000_000.0;
    }

    /** How many bytes {@code tag} takes when written out, counted without keeping them. */
    public static long nbtBytes(final CompoundTag tag) {
        final long[] count = new long[1];
        final OutputStream counter = new OutputStream() {
            @Override
            public void write(final int b) {
                count[0]++;
            }

            @Override
            public void write(final byte[] b, final int off, final int len) {
                count[0] += len;
            }
        };
        try {
            NbtIo.write(tag, new DataOutputStream(counter));
        } catch (final IOException e) {
            throw new GameTestAssertException("could not measure the save: " + e);
        }
        return count[0];
    }
}
