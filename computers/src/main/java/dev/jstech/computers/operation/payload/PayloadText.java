/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

/**
 * Text a message carries, held to what the message can carry.
 *
 * <p>A message that caps a piece of text refuses to be written with a longer one, which on the sending side is
 * a crash rather than a shorter message. So whatever makes such a message cuts its text to the cap first, and a
 * client that sends more than the cap is read only up to it.
 */
public final class PayloadText {

    private PayloadText() {
    }

    /** The text cut to at most that many characters, and empty for none. */
    public static String clip(final String text, final int most) {
        if (text == null) {
            return "";
        }
        return text.length() <= most ? text : text.substring(0, most);
    }
}
