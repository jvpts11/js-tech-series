/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.core.audio.media.IMediaUploadHandler;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaInfo;
import dev.jstech.core.audio.media.MediaUploads;
import dev.jstech.core.text.Text;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Recordings for the tests: a WAV of silence of any length, made on the spot, and a taker for the recordings a test
 * brings to the server, which writes down what it was handed and refuses whatever says so.
 */
public final class TestMedia {

    /** What the tests' recordings are brought for. */
    public static final String PURPOSE = "jstests:echo";
    /** A context the taker refuses, so a test can see a refusal come back. */
    public static final String REFUSE = "refuse";
    public static final int RATE = 8000;

    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final List<Received> RECEIVED = new ArrayList<>();

    private TestMedia() {
    }

    /** One recording the taker was handed. */
    public record Received(String context, String name, MediaId media, MediaInfo info) {
    }

    /** Says the tests take the recordings brought for {@link #PURPOSE}; once, however often it is asked. */
    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            MediaUploads.handle(PURPOSE, new IMediaUploadHandler() {
                @Nullable
                @Override
                public Text refuse(final ServerPlayer player, final String context, final String name,
                                   final MediaId media) {
                    return REFUSE.equals(context) ? Text.literal("refused by the test") : null;
                }

                @Override
                public Text received(final ServerPlayer player, final String context, final String name,
                                     final MediaId media, final MediaInfo info) {
                    synchronized (RECEIVED) {
                        RECEIVED.add(new Received(context, name, media, info));
                    }
                    return Text.EMPTY;
                }
            });
        }
    }

    /** What the taker was handed, oldest first. */
    public static List<Received> received() {
        synchronized (RECEIVED) {
            return List.copyOf(RECEIVED);
        }
    }

    /** A mono 16-bit WAV of that many milliseconds of silence, which {@code salt} makes a recording of its own. */
    public static byte[] wav(final int millis, final String salt) {
        final byte[] tag = salt.getBytes(StandardCharsets.US_ASCII);
        final ByteArrayOutputStream info = new ByteArrayOutputStream();
        info.writeBytes("INFOINAM".getBytes(StandardCharsets.US_ASCII));
        le32(info, tag.length + 1);
        info.writeBytes(tag);
        info.write(0);
        if (((tag.length + 1) & 1) == 1) {
            info.write(0);
        }
        final int data = RATE * millis / 1000 * 2;
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes("RIFF".getBytes(StandardCharsets.US_ASCII));
        le32(out, 4 + 24 + 8 + info.size() + 8 + data);
        out.writeBytes("WAVEfmt ".getBytes(StandardCharsets.US_ASCII));
        le32(out, 16);
        le16(out, 1);
        le16(out, 1);
        le32(out, RATE);
        le32(out, RATE * 2);
        le16(out, 2);
        le16(out, 16);
        out.writeBytes("LIST".getBytes(StandardCharsets.US_ASCII));
        le32(out, info.size());
        out.writeBytes(info.toByteArray());
        out.writeBytes("data".getBytes(StandardCharsets.US_ASCII));
        le32(out, data);
        out.writeBytes(new byte[data]);
        return out.toByteArray();
    }

    private static void le16(final ByteArrayOutputStream out, final int value) {
        out.write(value & 0xFF);
        out.write(value >> 8 & 0xFF);
    }

    private static void le32(final ByteArrayOutputStream out, final int value) {
        le16(out, value & 0xFFFF);
        le16(out, value >>> 16);
    }
}
