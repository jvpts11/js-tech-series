/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the Preinstaller's console says on its screen while it works (computers/docs/design/survival.md): the job at
 * its stations, the firmware's bar and the system's, and that it is done.
 */
@TextHolder
public final class PreinstallerTexts {

    public static final TextKey COMPUTER = TextKey.of("jsc.preinstaller.screen.computer", "COMPUTER");
    public static final TextKey MOTHERBOARD = TextKey.of("jsc.preinstaller.screen.motherboard", "MOTHERBOARD");
    public static final TextKey DISK = TextKey.of("jsc.preinstaller.screen.disk", "DISK");
    public static final TextKey FIRMWARE = TextKey.of("jsc.preinstaller.screen.firmware", "FIRMWARE");
    public static final TextKey SYSTEM = TextKey.of("jsc.preinstaller.screen.system", "SYSTEM");
    public static final TextKey DONE = TextKey.of("jsc.preinstaller.screen.done", "DONE");

    private PreinstallerTexts() {
    }
}
