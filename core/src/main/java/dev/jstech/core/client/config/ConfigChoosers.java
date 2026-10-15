/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.config;

import dev.jstech.core.config.ConfigDraft;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.util.DeclarationError;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The choosers settings are picked on, by the id a setting names them with. Registered from client setup. */
public final class ConfigChoosers {

    private static final Map<String, IConfigChooser> BY_ID = new ConcurrentHashMap<>();

    private ConfigChoosers() {
    }

    /**
     * Registers {@code chooser} as {@code id}.
     *
     * @throws DeclarationError when a chooser of that id is registered already
     */
    public static void register(final String id, final IConfigChooser chooser) {
        if (BY_ID.putIfAbsent(id, chooser) != null) {
            throw DeclarationError.of(null, "the settings chooser " + id, "is registered twice",
                    "register it once, from client setup");
        }
    }

    /** The chooser registered as {@code id}, or none. */
    public static Optional<IConfigChooser> get(final String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    /** {@code value} of {@code key} as its chooser shows it, or as it is written when it has none. */
    public static Component describe(final ConfigKey<?> key, final Object value) {
        return key.chooser().flatMap(ConfigChoosers::get).map(chooser -> chooser.describe(key, value))
                .orElseGet(() -> Component.literal(String.valueOf(value)));
    }

    /** The chooser's screen for {@code key}, or {@code parent} again when its chooser is not registered. */
    public static Screen open(final ConfigKey<?> key, final Screen parent, final ConfigDraft draft) {
        return key.chooser().flatMap(ConfigChoosers::get).map(chooser -> chooser.open(parent, draft, key))
                .orElse(parent);
    }
}
