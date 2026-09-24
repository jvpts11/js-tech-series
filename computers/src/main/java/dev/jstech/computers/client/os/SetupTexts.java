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
 * What a program's Setup window says while it installs or removes a program, and when it has done. Program names,
 * makers, sizes and where the program came from are data. Kept apart from the window so the language generator can
 * read it on a server too, where windows do not exist.
 */
@TextHolder
final class SetupTexts {

    static final TextKey SETUP = TextKey.of("jsc.setup.setup", "Setup");
    static final TextKey CANCEL = TextKey.of("jsc.setup.cancel", "Cancel");
    static final TextKey CLOSE = TextKey.of("jsc.setup.close", "Close");

    // The headline.
    static final TextKey INSTALLING = TextKey.of("jsc.setup.installing", "Installing %s");
    static final TextKey REMOVING = TextKey.of("jsc.setup.removing", "Removing %s");
    static final TextKey IS_INSTALLED = TextKey.of("jsc.setup.is_installed", "%s is installed.");
    static final TextKey WAS_REMOVED = TextKey.of("jsc.setup.was_removed", "%s was removed.");
    static final TextKey CANNOT_INSTALL = TextKey.of("jsc.setup.cannot_install", "Setup cannot install %s.");
    static final TextKey CANNOT_REMOVE = TextKey.of("jsc.setup.cannot_remove", "Setup cannot remove %s.");
    static final TextKey CANCELLED = TextKey.of("jsc.setup.cancelled", "Setup was cancelled.");

    // The line under it.
    static final TextKey COPYING = TextKey.of("jsc.setup.copying", "Copying files to your computer.");
    static final TextKey REMOVING_FROM = TextKey.of("jsc.setup.removing_from", "Removing %s from your computer.");
    static final TextKey IN_START_MENU = TextKey.of("jsc.setup.in_start_menu", "You will find it in the Start menu.");
    static final TextKey FILES_GONE = TextKey.of("jsc.setup.files_gone", "Its files are gone from the disk.");
    static final TextKey NOTHING_INSTALLED = TextKey.of("jsc.setup.nothing_installed", "Nothing was installed.");
    static final TextKey NOTHING_REMOVED = TextKey.of("jsc.setup.nothing_removed", "Nothing was removed.");

    // The details.
    static final TextKey PHASE = TextKey.of("jsc.setup.phase", "%s...");
    static final TextKey PERCENT = TextKey.of("jsc.setup.percent", "%s%%");
    static final TextKey SIZE = TextKey.of("jsc.setup.size", "%s MB");
    static final TextKey FROM = TextKey.of("jsc.setup.from", "from %s");

    private SetupTexts() {
    }
}
