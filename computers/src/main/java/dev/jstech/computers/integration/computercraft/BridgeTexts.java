/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.computercraft;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the Gateway's ComputerCraft side says, kept apart from the classes that name ComputerCraft's own: the language
 * generator loads every class that declares sentences, and it runs where ComputerCraft may not be installed.
 */
@TextHolder
final class BridgeTexts {

    static final TextKey NOT_ON_WIRE = TextKey.of("jsc.gateway.bridge.not_on_wire",
            "there is no %s on this Gateway's wire");
    static final TextKey CALL_FAILED = TextKey.of("jsc.gateway.bridge.call_failed", "%s failed");

    private BridgeTexts() {
    }
}
