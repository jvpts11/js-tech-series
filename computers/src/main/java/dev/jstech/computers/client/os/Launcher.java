/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.os.WindowKeys;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;

/**
 * One thing on the desktop that can be started.
 *
 * <p>{@code key} is what it is known by: the key its window goes by ({@link WindowKeys}), or {@code run:} and the
 * listing for a player's own program. {@code label} is only what it reads as on this desktop. {@code programId} is its
 * identity and the icon it wears. {@code factory} makes its window, and is null for a player's own program, which has
 * no window of its own: {@code runs} is the listing it starts at, and like any console program it gets a terminal
 * and prints into it, which is the same thing that happens when one is opened in the file explorer.
 */
record Launcher(String key, String label, ResourceLocation programId, Supplier<IDesktopApp> factory, String runs) {

    Launcher(final String key, final String label, final ResourceLocation programId,
             final Supplier<IDesktopApp> factory) {
        this(key, label, programId, factory, "");
    }
}
