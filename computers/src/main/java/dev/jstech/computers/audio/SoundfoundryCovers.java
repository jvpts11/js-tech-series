/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.computers.audio.catalog.SoundfoundryCatalog;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaPicture;
import dev.jstech.core.audio.media.MediaStore;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.jetbrains.annotations.Nullable;

/**
 * The covers the Standard Soundfoundry shows: an album's {@code cover.png} beside its songs in the catalogue, else the
 * picture a recording carries of itself; with neither, the window makes one of the album's colours and initials.
 *
 * <p>A cover travels as a small square PNG, made once on the server from whatever picture it was: a PNG or a JPEG of
 * any size. A cover is named by a key a page hands the window with each album and song: {@code album:<id>} for an
 * album's own, {@code media:<recording>} for the one a recording carries. The covers made are kept, the most recently
 * asked for, so a page of the same songs asked for again reads nothing.
 */
public final class SoundfoundryCovers {

    /** The side of a cover as it travels, in pixels: the largest a page draws one. */
    public static final int SIZE = 96;
    /** The most bytes a cover travels as. */
    public static final int MAX_BYTES = 64 * 1024;
    /** What a cover is when there is none. */
    public static final byte[] NONE = new byte[0];

    private static final String ALBUM = "album:";
    private static final String MEDIA = "media:";
    /* A recording read further than this for its picture is not worth it: its songs come first. */
    private static final int MOST_READ = 64 * 1024 * 1024;
    /* The largest picture a cover is made from, in pixels: far past any album art, far short of a decoded bomb. */
    private static final long MOST_PIXELS = 4096L * 4096L;
    private static final int KEPT = 256;
    private static final Map<String, byte[]> MADE = new LinkedHashMap<>(KEPT, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<String, byte[]> eldest) {
            return size() > KEPT;
        }
    };
    /*
     * Covers being made, by key, kept under the lock of MADE: a key asked for again while it is being made waits for
     * the same making rather than reading its recording a second time.
     */
    private static final Map<String, CompletableFuture<byte[]>> MAKING = new HashMap<>();
    /*
     * Covers are made one at a time on a thread of their own, with a short line of requests waiting: making one can
     * read a long recording, and a client asking for covers without end must not take the game's own workers or its
     * memory. A request past the line comes back with no cover.
     */
    private static final int MOST_WAITING = 64;
    private static final ThreadPoolExecutor MAKER = maker();

    private SoundfoundryCovers() {
    }

    /** The key of the cover a catalogue album has of its own. */
    public static String albumKey(final String album) {
        return ALBUM + album;
    }

    /** The key of the cover a recording carries. */
    public static String mediaKey(final MediaId media) {
        return MEDIA + media.hash() + "." + media.format() + "." + media.bytes();
    }

    /**
     * The key of a catalogue song's cover: its album's own when the album has one, else the one the recording
     * carries.
     */
    public static String catalogKey(final String album, final MediaId media) {
        return SoundfoundryCatalog.current().cover(album) != null ? albumKey(album) : mediaKey(media);
    }

    /**
     * The cover a key names, as a small PNG, or {@link #NONE}. It may read a recording, so it is asked off the game's
     * thread.
     */
    public static byte[] cover(final String key) {
        synchronized (MADE) {
            final byte[] made = MADE.get(key);
            if (made != null) {
                return made;
            }
        }
        final byte[] made = make(key);
        synchronized (MADE) {
            MADE.put(key, made);
        }
        return made;
    }

    /**
     * The cover a key names, made on the covers' own thread, or {@link #NONE} at once when too many are already
     * waiting to be made.
     */
    public static CompletableFuture<byte[]> coverLater(final String key) {
        synchronized (MADE) {
            final byte[] made = MADE.get(key);
            if (made != null) {
                return CompletableFuture.completedFuture(made);
            }
            final CompletableFuture<byte[]> making = MAKING.get(key);
            if (making != null) {
                return making;
            }
            final CompletableFuture<byte[]> future = new CompletableFuture<>();
            try {
                MAKER.execute(() -> {
                    byte[] cover = NONE;
                    try {
                        cover = cover(key);
                    } finally {
                        synchronized (MADE) {
                            MAKING.remove(key);
                        }
                        future.complete(cover);
                    }
                });
            } catch (final RejectedExecutionException full) {
                return CompletableFuture.completedFuture(NONE);
            }
            MAKING.put(key, future);
            return future;
        }
    }

    /** Forgets the covers made, the catalogue having been read again. */
    public static void forget() {
        synchronized (MADE) {
            MADE.clear();
        }
    }

    /**
     * A picture made into a cover: cut square about its middle and brought down to {@link #SIZE} pixels a side, as a
     * PNG; {@link #NONE} for bytes that are no picture this can read.
     */
    public static byte[] scaled(final byte[] picture) {
        try {
            final BufferedImage read = decoded(picture);
            if (read == null || read.getWidth() <= 0 || read.getHeight() <= 0) {
                return NONE;
            }
            final int side = Math.min(read.getWidth(), read.getHeight());
            final int left = (read.getWidth() - side) / 2;
            final int top = (read.getHeight() - side) / 2;
            final int out = Math.min(SIZE, side);
            final BufferedImage small = new BufferedImage(out, out, BufferedImage.TYPE_INT_ARGB);
            // Each pixel is the average of the square of the picture it stands for, so a large cover keeps its look.
            for (int y = 0; y < out; y++) {
                for (int x = 0; x < out; x++) {
                    small.setRGB(x, y, average(read, left + x * side / out, top + y * side / out,
                            left + (x + 1) * side / out, top + (y + 1) * side / out));
                }
            }
            final ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(small, "png", png);
            return png.size() <= MAX_BYTES ? png.toByteArray() : NONE;
        } catch (final IOException | RuntimeException unreadable) {
            return NONE;
        }
    }

    private static byte[] make(final String key) {
        if (key.startsWith(ALBUM)) {
            final byte[] cover = SoundfoundryCatalog.current().cover(key.substring(ALBUM.length()));
            return cover == null ? NONE : cover;
        }
        final MediaId media = key.startsWith(MEDIA) ? mediaOf(key.substring(MEDIA.length())) : null;
        final MediaStore store = MediaStore.current().orElse(null);
        if (media == null || store == null || !store.has(media) || media.bytes() > MOST_READ) {
            return NONE;
        }
        try (InputStream in = store.open(media)) {
            return MediaPicture.find(media.format(), in.readNBytes(MOST_READ)).map(SoundfoundryCovers::scaled)
                    .orElse(NONE);
        } catch (final IOException unreadable) {
            return NONE;
        }
    }

    @Nullable
    private static MediaId mediaOf(final String written) {
        final String[] parts = written.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new MediaId(parts[0], parts[1], Long.parseLong(parts[2]));
        } catch (final IllegalArgumentException malformed) {
            return null;
        }
    }

    private static int average(final BufferedImage image, final int x0, final int y0, final int x1, final int y1) {
        long a = 0;
        long r = 0;
        long g = 0;
        long b = 0;
        int n = 0;
        for (int y = y0; y < Math.max(y0 + 1, y1); y++) {
            for (int x = x0; x < Math.max(x0 + 1, x1); x++) {
                final int argb = image.getRGB(x, y);
                a += argb >>> 24;
                r += argb >> 16 & 0xFF;
                g += argb >> 8 & 0xFF;
                b += argb & 0xFF;
                n++;
            }
        }
        return (int) (a / n) << 24 | (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
    }

    /*
     * The picture's pixels, or null when it is no picture this reads or says it is larger than a cover is ever made
     * from. The size a file declares is read before a pixel of it is: a few bytes of PNG can declare a picture whose
     * pixels fill gigabytes, and a recording a player brought can carry one.
     */
    @Nullable
    private static BufferedImage decoded(final byte[] picture) throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(picture))) {
            if (stream == null) {
                return null;
            }
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                return null;
            }
            final ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                final long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                return pixels <= 0 || pixels > MOST_PIXELS ? null : reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }

    /* One thread, gone when there is nothing to make, so a server that never shows a cover keeps none. */
    private static ThreadPoolExecutor maker() {
        final ThreadPoolExecutor maker = new ThreadPoolExecutor(1, 1, 30L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(MOST_WAITING), runnable -> {
                    final Thread thread = new Thread(runnable, "Soundfoundry covers");
                    thread.setDaemon(true);
                    return thread;
                });
        maker.allowCoreThreadTimeOut(true);
        return maker;
    }
}
