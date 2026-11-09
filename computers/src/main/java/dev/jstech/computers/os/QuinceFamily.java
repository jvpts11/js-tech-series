/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

/**
 * Quince's systems as a family (computers/docs/design/quince.md): where each sits in its order, and which of them are
 * the classic ones. The classic systems, the Quince System and Quince OS 9, run on the cooperative quince_classic kernel
 * with no command line at all; Quince OS X, its Server and quinceOS run on wallace, preemptive and POSIX.
 *
 * <p>Pure: the place every rule that tells the two kinds apart reads it from, so they cannot come to disagree.
 */
public final class QuinceFamily {

    /** The Quince System's place in the family. */
    public static final int QUINCE_SYSTEM = 1;
    /** Quince OS 9's. */
    public static final int OS_9 = 2;
    /** Quince OS X's, and its Server's beside it. */
    public static final int OS_X = 3;
    /** quinceOS's. */
    public static final int QUINCE_OS = 4;

    private QuinceFamily() {
    }

    /** Whether a Quince system at that place in the family is one of the classic ones. */
    public static boolean classic(final int familyRank) {
        return familyRank <= OS_9;
    }
}
