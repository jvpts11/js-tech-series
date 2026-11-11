/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import java.util.random.RandomGenerator;

/**
 * A Teracoin address and the key that spends from it. An address is written as a bitcoin's of the bech32 kind is:
 * {@code trc1q} and thirty letters of the bech32 alphabet, which leaves out the letters easily taken for others. The
 * key is longer and is never shown; whoever holds a wallet file holding it may spend what the address has. Pure.
 */
public final class TeracoinAddress {

    /** What every address starts with. */
    public static final String PREFIX = "trc1q";
    /** The letters of an address, its prefix included. */
    public static final int LENGTH = PREFIX.length() + 30;
    /** The letters of a key. */
    public static final int KEY_LENGTH = 52;

    /* The bech32 alphabet: no 1, b, i or o. */
    private static final String ALPHABET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";

    private TeracoinAddress() {
    }

    /** A new address, drawn at random. */
    public static String create(final RandomGenerator random) {
        return PREFIX + letters(random, LENGTH - PREFIX.length());
    }

    /** A new key, drawn at random. */
    public static String key(final RandomGenerator random) {
        return letters(random, KEY_LENGTH);
    }

    /** Whether {@code text} is written as an address is, whether or not the chain knows it. */
    public static boolean valid(final String text) {
        if (text.length() != LENGTH || !text.startsWith(PREFIX)) {
            return false;
        }
        for (int i = PREFIX.length(); i < text.length(); i++) {
            if (ALPHABET.indexOf(text.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }

    /** Whether {@code text} is written as a key is. */
    public static boolean validKey(final String text) {
        if (text.length() != KEY_LENGTH) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (ALPHABET.indexOf(text.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }

    /** {@code address} shortened as a list shows it, its first eight letters and its last four: trc1q8f3...7h4x. */
    public static String shortForm(final String address) {
        return address.length() <= 15 ? address
                : address.substring(0, 8) + "..." + address.substring(address.length() - 4);
    }

    private static String letters(final RandomGenerator random, final int count) {
        final StringBuilder out = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            out.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return out.toString();
    }
}
