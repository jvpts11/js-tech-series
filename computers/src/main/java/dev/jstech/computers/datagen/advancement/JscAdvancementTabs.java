/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen.advancement;

import java.util.List;
import java.util.function.BiConsumer;

/** The mod's advancement tabs, in the order the advancements screen lists them. */
public final class JscAdvancementTabs {

    private JscAdvancementTabs() {
    }

    /** A fresh set of the tabs, written down again: each provider reads its own. */
    public static List<AdvancementTab> all() {
        return List.of(new HardwareAdvancements(), new SystemAdvancements(), new NetworkAdvancements(),
                new SigmaAdvancements());
    }

    /** Every title and description of every tab, for the language file. */
    public static void translations(final BiConsumer<String, String> add) {
        for (final AdvancementTab tab : all()) {
            tab.translations(add);
        }
    }
}
