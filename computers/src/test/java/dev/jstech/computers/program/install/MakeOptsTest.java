/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** How many jobs the build options ask for, read the forgiving way a shell reads the file. */
class MakeOptsTest {

    @Test
    void jobs_aFileThatIsNotThereOrSaysNothing_isOne() {
        assertEquals(1, MakeOpts.jobs(null));
        assertEquals(1, MakeOpts.jobs(""));
        assertEquals(1, MakeOpts.jobs("COMMON_FLAGS=\"-O2 -pipe\"\nCFLAGS=\"${COMMON_FLAGS}\""));
    }

    @Test
    void jobs_readsTheNumberAfterTheJ() {
        assertEquals(4, MakeOpts.jobs("MAKEOPTS=\"-j4\""));
        assertEquals(8, MakeOpts.jobs("COMMON_FLAGS=\"-O2\"\nMAKEOPTS=\"-j8 -l8\"\n"));
        assertEquals(6, MakeOpts.jobs("MAKEOPTS=\"--load-average=5 -j 6\""));
        assertEquals(2, MakeOpts.jobs("  MAKEOPTS = -j2"));
    }

    @Test
    void jobs_theLastLineThatSetsThemIsTheOneThatCounts() {
        assertEquals(16, MakeOpts.jobs("MAKEOPTS=\"-j2\"\nMAKEOPTS=\"-j16\""),
                "a line added at the end of the file with a redirect is what the file now says");
    }

    @Test
    void jobs_aLineCommentedOutSetsNothing() {
        assertEquals(1, MakeOpts.jobs("# MAKEOPTS=\"-j12\""));
        assertEquals(3, MakeOpts.jobs("#MAKEOPTS=\"-j12\"\nMAKEOPTS=\"-j3\""));
    }

    @Test
    void jobs_nothingIsNeverFewerThanOne() {
        assertEquals(1, MakeOpts.jobs("MAKEOPTS=\"-j0\""));
    }
}
