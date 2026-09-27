/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

class MediaPictureTest {

    private static final byte[] FRONT = {1, 2, 3, 4, 5};
    private static final byte[] BACK = {9, 9, 9};

    @Test
    void find_readsTheFrontCoverOfAnOggVorbisFile() {
        final byte[] ogg = ogg(List.of(picture(4, BACK), "TITLE=Pain", picture(3, FRONT)));
        assertArrayEquals(FRONT, MediaPicture.find("ogg", ogg).orElseThrow(),
                "the front cover is taken over a picture of another kind");
    }

    @Test
    void find_takesTheOnlyPictureWhenNoneIsTheFront() {
        final byte[] ogg = ogg(List.of(picture(4, BACK)));
        assertArrayEquals(BACK, MediaPicture.find("ogg", ogg).orElseThrow());
    }

    @Test
    void find_isEmptyForAnOggWithNoPicture() {
        assertTrue(MediaPicture.find("ogg", ogg(List.of("TITLE=Pain"))).isEmpty());
    }

    @Test
    void find_readsThePictureAttachedToAWaveFilesTag() {
        final byte[] wav = wav(id3(apic(3, FRONT)));
        assertArrayEquals(FRONT, MediaPicture.find("wav", wav).orElseThrow());
    }

    @Test
    void find_isEmptyForSomethingThatIsNoRecording() {
        assertTrue(MediaPicture.find("wav", new byte[] {1, 2, 3}).isEmpty());
        assertTrue(MediaPicture.find("ogg", new byte[40]).isEmpty());
        assertTrue(MediaPicture.find("mp3", new byte[40]).isEmpty());
    }

    private static String picture(final int type, final byte[] image) {
        final ByteArrayOutputStream block = new ByteArrayOutputStream();
        be32(block, type);
        final byte[] mime = "image/png".getBytes(StandardCharsets.US_ASCII);
        be32(block, mime.length);
        block.writeBytes(mime);
        be32(block, 0);
        for (int i = 0; i < 4; i++) {
            be32(block, 0);
        }
        be32(block, image.length);
        block.writeBytes(image);
        return "METADATA_BLOCK_PICTURE=" + Base64.getEncoder().encodeToString(block.toByteArray());
    }

    /* An Ogg file of one page holding a Vorbis identification packet and a comment packet with those comments. */
    private static byte[] ogg(final List<String> comments) {
        final byte[] identification = new byte[30];
        identification[0] = 1;
        System.arraycopy("vorbis".getBytes(StandardCharsets.US_ASCII), 0, identification, 1, 6);
        final ByteArrayOutputStream packet = new ByteArrayOutputStream();
        packet.write(3);
        packet.writeBytes("vorbis".getBytes(StandardCharsets.US_ASCII));
        le32(packet, 0);
        le32(packet, comments.size());
        for (final String comment : comments) {
            final byte[] bytes = comment.getBytes(StandardCharsets.UTF_8);
            le32(packet, bytes.length);
            packet.writeBytes(bytes);
        }
        final byte[] second = packet.toByteArray();
        final ByteArrayOutputStream lacing = new ByteArrayOutputStream();
        lacing.write(identification.length);
        int left = second.length;
        while (left >= 255) {
            lacing.write(255);
            left -= 255;
        }
        lacing.write(left);
        final ByteArrayOutputStream page = new ByteArrayOutputStream();
        page.writeBytes("OggS".getBytes(StandardCharsets.US_ASCII));
        page.write(0);
        page.write(2);
        page.writeBytes(new byte[8]);
        le32(page, 1234);
        le32(page, 0);
        le32(page, 0);
        page.write(lacing.size());
        page.writeBytes(lacing.toByteArray());
        page.writeBytes(identification);
        page.writeBytes(second);
        return page.toByteArray();
    }

    private static byte[] apic(final int type, final byte[] image) {
        final ByteArrayOutputStream frame = new ByteArrayOutputStream();
        frame.write(0);
        frame.writeBytes("image/png".getBytes(StandardCharsets.US_ASCII));
        frame.write(0);
        frame.write(type);
        frame.writeBytes("cover".getBytes(StandardCharsets.US_ASCII));
        frame.write(0);
        frame.writeBytes(image);
        final byte[] body = frame.toByteArray();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes("APIC".getBytes(StandardCharsets.US_ASCII));
        be32(out, body.length);
        out.write(0);
        out.write(0);
        out.writeBytes(body);
        return out.toByteArray();
    }

    /* An ID3 tag of version 2.3 holding that frame. */
    private static byte[] id3(final byte[] frame) {
        final ByteArrayOutputStream tag = new ByteArrayOutputStream();
        tag.writeBytes("ID3".getBytes(StandardCharsets.US_ASCII));
        tag.write(3);
        tag.write(0);
        tag.write(0);
        final int size = frame.length;
        tag.write(size >> 21 & 0x7F);
        tag.write(size >> 14 & 0x7F);
        tag.write(size >> 7 & 0x7F);
        tag.write(size & 0x7F);
        tag.writeBytes(frame);
        return tag.toByteArray();
    }

    /* A Wave file of no samples with that ID3 tag in its own chunk. */
    private static byte[] wav(final byte[] tag) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes("RIFF".getBytes(StandardCharsets.US_ASCII));
        le32(out, 4 + 8 + tag.length);
        out.writeBytes("WAVE".getBytes(StandardCharsets.US_ASCII));
        out.writeBytes("id3 ".getBytes(StandardCharsets.US_ASCII));
        le32(out, tag.length);
        out.writeBytes(tag);
        return out.toByteArray();
    }

    private static void le32(final ByteArrayOutputStream out, final long value) {
        out.write((int) (value & 0xFF));
        out.write((int) (value >> 8 & 0xFF));
        out.write((int) (value >> 16 & 0xFF));
        out.write((int) (value >> 24 & 0xFF));
    }

    private static void be32(final ByteArrayOutputStream out, final long value) {
        out.write((int) (value >> 24 & 0xFF));
        out.write((int) (value >> 16 & 0xFF));
        out.write((int) (value >> 8 & 0xFF));
        out.write((int) (value & 0xFF));
    }
}
