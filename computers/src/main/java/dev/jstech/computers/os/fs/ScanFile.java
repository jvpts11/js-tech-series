/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.OptionalInt;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jetbrains.annotations.Nullable;

/**
 * What a scanner writes for an enchanted book ({@code .ench}) and a map ({@code .map}), and the seal that keeps either
 * from being made by hand.
 *
 * <p>An enchanted book prints back with every enchantment at the level it was scanned with, and a map prints back as
 * the map it was (computers/docs/design/peripherals.md). A file anyone could write would print any book or any map, so
 * the world seals what its scanners write: the last line is a keyed hash of everything above it, under a key only the
 * world holds. A copy carries its seal and stays good; a file whose words were changed, or one written from nothing,
 * does not, and the disk refuses it under these kinds.
 *
 * <p>An enchanted book's file names its enchantments in words for whoever opens it, then each again as the printer reads
 * it; a map's names the map, then its picture as Paint holds one, so Paint shows the map.
 *
 * <p>This class is pure and carries no Minecraft dependency: the key comes from whoever calls it.
 */
public final class ScanFile {

    /** The line that ends a sealed file: the seal of everything above it. */
    public static final String SEAL = "#seal ";
    /** The line, after the words, that names an enchantment and its level as the printer reads them. */
    public static final String ENCHANTMENT = "#ench ";
    /** The first line of a map's file, which names the map. */
    public static final String MAP = "#map ";
    private static final String ALGORITHM = "HmacSHA256";
    private static final char NEWLINE = '\n';
    private static final HexFormat HEX = HexFormat.of();

    private ScanFile() {
    }

    /** {@code body} with its seal under {@code key} as its last line. */
    public static String seal(final String body, final byte[] key) {
        final String text = body.endsWith(String.valueOf(NEWLINE)) ? body : body + NEWLINE;
        return text + SEAL + HEX.formatHex(mac(text, key)) + NEWLINE;
    }

    /** What a sealed file holds above its seal, or null when its seal is missing or is not {@code key}'s. */
    @Nullable
    public static String open(@Nullable final String content, final byte[] key) {
        if (content == null) {
            return null;
        }
        final int at = content.lastIndexOf(NEWLINE + SEAL);
        if (at < 0) {
            return null;
        }
        final String body = content.substring(0, at + 1);
        final byte[] seal;
        try {
            seal = HEX.parseHex(content.substring(at + 1 + SEAL.length()).strip());
        } catch (final IllegalArgumentException notHex) {
            return null;
        }
        return MessageDigest.isEqual(seal, mac(body, key)) ? body : null;
    }

    /** What {@code content} holds above its seal line, whether or not the seal is good; for showing, not for printing. */
    public static String body(final String content) {
        final int at = content.lastIndexOf(NEWLINE + SEAL);
        return at < 0 ? content : content.substring(0, at + 1);
    }

    /** An enchanted book's body: each enchantment in words, a blank line, then each as the printer reads it. */
    public static String enchantments(final List<Enchanted> enchantments) {
        final StringBuilder words = new StringBuilder();
        final StringBuilder lines = new StringBuilder();
        for (final Enchanted enchanted : enchantments) {
            words.append(enchanted.words()).append(NEWLINE);
            lines.append(ENCHANTMENT).append(enchanted.id()).append(' ').append(enchanted.level()).append(NEWLINE);
        }
        return words.append(NEWLINE).append(lines).toString();
    }

    /** The enchantments an enchanted book's body names, in order, as the printer reads them. */
    public static List<Enchanted> enchantmentsOf(final String body) {
        final List<Enchanted> out = new ArrayList<>();
        for (final String line : body.split("\n")) {
            if (!line.startsWith(ENCHANTMENT)) {
                continue;
            }
            final String[] words = line.substring(ENCHANTMENT.length()).strip().split(" ");
            if (words.length != 2) {
                continue;
            }
            try {
                out.add(new Enchanted("", words[0], Integer.parseInt(words[1])));
            } catch (final NumberFormatException notALevel) {
                // A line that names no level is not one the scanner wrote; the seal already says the rest is.
            }
        }
        return List.copyOf(out);
    }

    /** A map's body: the map it is a copy of, then its picture as Paint holds one. */
    public static String map(final int id, final String picture) {
        return MAP + id + NEWLINE + picture;
    }

    /** The map a map's body names, or empty when it names none. */
    public static OptionalInt mapOf(final String body) {
        if (!body.startsWith(MAP)) {
            return OptionalInt.empty();
        }
        final int end = body.indexOf(NEWLINE);
        try {
            return OptionalInt.of(Integer.parseInt(body.substring(MAP.length(), end < 0 ? body.length() : end).strip()));
        } catch (final NumberFormatException notAMap) {
            return OptionalInt.empty();
        }
    }

    /** The picture inside a map's file, as Paint reads one, or the whole text when it is not a map's. */
    public static String pictureOf(final String content) {
        final String body = body(content);
        if (!body.startsWith(MAP)) {
            return body;
        }
        final int start = body.indexOf(NEWLINE);
        return start < 0 ? "" : body.substring(start + 1).stripTrailing();
    }

    private static byte[] mac(final String text, final byte[] key) {
        try {
            final Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            return mac.doFinal(text.getBytes(StandardCharsets.UTF_8));
        } catch (final GeneralSecurityException missing) {
            // Every Java runtime ships HmacSHA256; one without it cannot run the game either.
            throw new IllegalStateException(missing);
        }
    }

    /**
     * An enchantment as an enchanted book's file names it.
     *
     * @param words how the file shows it, such as {@code Sharpness V}; empty when read back for the printer
     * @param id    the enchantment's id, such as {@code minecraft:sharpness}
     * @param level its level
     */
    public record Enchanted(String words, String id, int level) {
    }
}
