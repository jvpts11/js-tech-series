/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.peripheral;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the players near a peripheral are told over the hotbar when it links to its owner. */
@TextHolder
final class LinkTexts {

    /* The peripheral, the owner, where the owner stands. */
    static final TextKey AGAINST = TextKey.of("jscore.peripheral.linked.against", "%s linked to %s at %s, against it");
    static final TextKey ONE_CABLE = TextKey.of("jscore.peripheral.linked.one_cable",
            "%s linked to %s at %s (1 block of cable)");
    /* The peripheral, the owner, where the owner stands, and over how many cables. */
    static final TextKey CABLES = TextKey.of("jscore.peripheral.linked.cables",
            "%s linked to %s at %s (%s blocks of cable)");

    private LinkTexts() {
    }
}
