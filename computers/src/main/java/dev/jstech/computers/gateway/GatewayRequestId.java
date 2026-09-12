/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

import java.util.UUID;

/**
 * The name a question put across a Gateway is known by, and answered by.
 *
 * <p>It is a whole name rather than a number that counts up, because a machine can have more than one
 * Gateway and each of them would start counting at one: two questions called "1" would be told apart by
 * nothing, and an answer could reach the program that asked the other one. A name that is unique
 * wherever it is made cannot do that, and it leaves no room for a wrapped counter to collide either.
 *
 * <p>It crosses to the other side as text, because text is what a computer over there can hold and hand
 * back unchanged.
 */
public record GatewayRequestId(UUID value) {

    public GatewayRequestId {
        if (value == null) {
            throw new IllegalArgumentException("a question must have a name");
        }
    }

    /** A name no other question has. */
    public static GatewayRequestId made() {
        return new GatewayRequestId(UUID.randomUUID());
    }

    /** The name as the other side holds it, or null when what came back is not one of ours. */
    public static GatewayRequestId of(final String text) {
        if (text == null || text.length() != 36) {
            return null;
        }
        try {
            return new GatewayRequestId(UUID.fromString(text));
        } catch (final IllegalArgumentException notOne) {
            return null;
        }
    }

    /** The name as the other side holds it. */
    public String text() {
        return this.value.toString();
    }
}
