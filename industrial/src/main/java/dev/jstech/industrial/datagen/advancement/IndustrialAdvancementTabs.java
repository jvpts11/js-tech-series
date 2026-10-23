/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.datagen.advancement;

import dev.jstech.core.datagen.advancement.AdvancementTab;
import java.util.List;
import java.util.function.BiConsumer;

/** The mod's advancement tabs, in the order the advancements screen lists them: the phases of its industry. */
public final class IndustrialAdvancementTabs {

    private IndustrialAdvancementTabs() {
    }

    /** A fresh set of the tabs, written down again: each provider reads its own. */
    public static List<AdvancementTab> all() {
        return List.of(new FoundationAdvancements(), new IndustrialisationAdvancements(), new DiscoveryAdvancements(),
                new TranscendenceAdvancements());
    }

    /** Every title and description of every tab, for the language file. */
    public static void translations(final BiConsumer<String, String> add) {
        for (final AdvancementTab tab : all()) {
            tab.translations(add);
        }
    }
}
