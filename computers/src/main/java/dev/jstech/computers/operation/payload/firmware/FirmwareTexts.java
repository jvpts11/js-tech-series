/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.firmware;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the firmware says about the machine it starts, kept apart from the handlers that open its screens: the
 * language generator loads every class that declares sentences, on a server as well, where screens do not exist.
 */
@TextHolder
final class FirmwareTexts {

    static final TextKey NO_SYSTEM = TextKey.of("jsc.firmware.no_system", "no system");
    static final TextKey EMPTY_DRIVE = TextKey.of("jsc.firmware.empty_drive", "empty");
    static final TextKey INSTALLER = TextKey.of("jsc.firmware.installer", "%s installer");
    static final TextKey LIVE = TextKey.of("jsc.firmware.live", "%s (live)");
    static final TextKey ERA_OR_NEWER = TextKey.of("jsc.firmware.era_or_newer", "%s era or newer");
    static final TextKey UNKNOWN_SYSTEM = TextKey.of("jsc.firmware.unknown_system",
            "The system on the medium is not known to this machine.");
    static final TextKey NEEDS_ERA = TextKey.of("jsc.firmware.needs_era",
            "%s needs %s era hardware or newer; this machine is %s era.");
    static final TextKey BY_HAND = TextKey.of("jsc.firmware.by_hand",
            "%s is put on the disk by hand from its own shell: boot the medium instead.");
    static final TextKey NO_DISK = TextKey.of("jsc.firmware.no_disk", "No disk is installed to put %s on.");
    static final TextKey NO_ROOM = TextKey.of("jsc.firmware.no_room",
            "The target disk has no room for %s (%s MB needed).");
    static final TextKey NO_MEDIUM = TextKey.of("jsc.firmware.no_medium",
            "No installation medium is in a drive linked to this machine.");

    private FirmwareTexts() {
    }
}
