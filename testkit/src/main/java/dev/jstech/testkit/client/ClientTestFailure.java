/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.client;

/** A failed assertion or timeout inside a client test; the runner records it and moves on to the next test. */
public final class ClientTestFailure extends RuntimeException {

    public ClientTestFailure(final String message) {
        super(message);
    }

    public ClientTestFailure(final String message, final Throwable cause) {
        super(message, cause);
    }
}
