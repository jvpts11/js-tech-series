/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.os.OsDef;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What a system stops a program with that read or wrote outside its memory, by the kernel it runs: the Unix line on
 * Linux, the BSDs, UNIX and the Quince that stands on one, the illegal operation of the Frames that run on DOS, and the
 * bomb of the classic Quince systems (computers/docs/design/sigma.md). Any other system says the plain words.
 */
@TextHolder
final class CrashWords {

    private static final TextKey SEGMENTATION_FAULT = TextKey.of("jsc.machine.crash.segmentation_fault",
            "Segmentation fault (core dumped)");
    private static final TextKey ILLEGAL_OPERATION = TextKey.of("jsc.machine.crash.illegal_operation",
            "This program has performed an illegal operation and will be shut down.");
    private static final TextKey SYSTEM_ERROR = TextKey.of("jsc.machine.crash.system_error",
            "Sorry, a system error occurred.");

    private CrashWords() {
    }

    /** The words of the system installed, or {@code plain} for none or one with no words of its own. */
    static Text of(final OsDef system, final Text plain) {
        if (system == null) {
            return plain;
        }
        return switch (system.kernelId().getPath()) {
            case "linux", "freebsd", "openbsd", "unix", "wallace" -> SEGMENTATION_FAULT.text();
            case "win9x" -> ILLEGAL_OPERATION.text();
            case "quince_classic" -> SYSTEM_ERROR.text();
            default -> plain;
        };
    }
}
