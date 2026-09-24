/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What a desktop's shell window says on its own account, without asking the machine: where to start, and how its
 * {@code run} command answers. Commands and program names are data.
 */
@TextHolder
final class ShellViewTexts {

    static final TextKey START_HINT = TextKey.of("jsc.shell.start_hint", "Type %s for a list of commands");
    static final TextKey SCROLLED = TextKey.of("jsc.shell.scrolled", "scrolled +%s");
    static final TextKey PROGRAMS = TextKey.of("jsc.shell.programs", "Programs: %s");
    static final TextKey RUN_USAGE = TextKey.of("jsc.shell.run_usage", "Usage: %s <program>");
    static final TextKey OPENING = TextKey.of("jsc.shell.opening", "Opening %s...");
    static final TextKey NO_SUCH_PROGRAM =
            TextKey.of("jsc.shell.no_such_program", "No such program: %s (type '%s' to list them)");

    private ShellViewTexts() {
    }
}
