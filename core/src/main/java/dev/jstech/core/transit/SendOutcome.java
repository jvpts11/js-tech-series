/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.transit;

/** What came of asking a station to send a capsule at once ({@link CapsuleStationBlockEntity#sendNow}). */
public enum SendOutcome {

    /** The capsule left. */
    SENT,
    /** Not yet: its next capsule is not due, or its mod has not what it spends to send, as compressed air. */
    NOT_YET,
    /** Nothing waits in it to be sent. */
    NOTHING,
    /** No station of that address is on its tubes. */
    NO_STATION
}
