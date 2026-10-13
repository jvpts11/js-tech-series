/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.computercraft;

import dan200.computercraft.api.ComputerCraftAPI;

/** The installed CC: Tweaked's version, read only once the integration knows the mod is there. */
final class ComputerCraftVersion {

    private ComputerCraftVersion() {
    }

    static String installed() {
        return ComputerCraftAPI.getInstalledVersion();
    }
}
