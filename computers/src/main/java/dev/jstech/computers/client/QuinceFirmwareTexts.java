/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the firmware of Quince's machines says on the glass (computers/docs/design/quince.md): Open Firmware's page,
 * the Sad Quince's words and the start-up choosers'.
 *
 * <p>Kept apart from the screen that draws it, since the language generator loads every class that declares sentences,
 * on a server as well, where screens do not exist.
 */
@TextHolder
public final class QuinceFirmwareTexts {

    /** Open Firmware's banner, its machine named: the boot ROM's version and the day it was built. */
    static final TextKey OF_ROM = TextKey.of("jsc.quince.of.rom",
            "Quince %s 3.2.4f1 BootROM built on 09/12/2000 at 14:25:12");
    static final TextKey OF_COPYRIGHT = TextKey.of("jsc.quince.of.copyright",
            "Copyright 1994-2000 Quince Computer, Inc.");
    static final TextKey OF_RIGHTS = TextKey.of("jsc.quince.of.rights", "All Rights Reserved");
    static final TextKey OF_WELCOME = TextKey.of("jsc.quince.of.welcome",
            "Welcome to Open Firmware, the system time and date is: 11/04/2000 12:00:00");
    static final TextKey OF_TO_BOOT = TextKey.of("jsc.quince.of.to_boot",
            "To continue booting, type \"quince-boot\" and press return.");
    static final TextKey OF_TO_SHUT_DOWN = TextKey.of("jsc.quince.of.to_shut_down",
            "To shut down, type \"shut-down\" and press return.");
    /** What Open Firmware says after a word it ran, and after one it does not know. */
    static final TextKey OF_OK = TextKey.of("jsc.quince.of.ok", " ok");
    static final TextKey OF_UNKNOWN = TextKey.of("jsc.quince.of.unknown", "%s, unknown word");
    /** Open Firmware's word along the foot of the Sad Quince. */
    static final TextKey OF_FAULT = TextKey.of("jsc.quince.of.fault",
            "Open Firmware: hardware fault on the logic board");
    /** The words of the later firmwares' Sad Quince: EFI's and the Quince chip's. */
    static final TextKey SAD_EFI = TextKey.of("jsc.quince.sad.efi", "Your Quince has a hardware problem.");
    static final TextKey SAD_CHIP = TextKey.of("jsc.quince.sad.chip", "Your Quince needs to be revived.");
    /** The Startup Options: the line over the volumes, the gear's name and the button under the one picked. */
    static final TextKey LOADING_OPTIONS = TextKey.of("jsc.quince.options.loading", "Loading startup options...");
    static final TextKey OPTIONS = TextKey.of("jsc.quince.options.options", "Options");
    static final TextKey CONTINUE = TextKey.of("jsc.quince.options.continue", "Continue");

    private QuinceFirmwareTexts() {
    }

    static String of(final TextKey key) {
        return GameText.resolve(key.text());
    }
}
