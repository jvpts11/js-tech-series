/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The words of the Network Interactor's Update window, kept apart from it so a server can read them too. */
@TextHolder
final class UpdatePopupTexts {

    static final TextKey OPEN = TextKey.of("jsc.update_window.open", "Update...");
    static final TextKey TITLE = TextKey.of("jsc.update_window.title", "Update: %s");
    static final TextKey TAB_ENCHANT = TextKey.of("jsc.update_window.tab_enchant", "Enchant");
    static final TextKey TAB_SMELT = TextKey.of("jsc.update_window.tab_smelt", "Smelt");
    static final TextKey TAB_REPAIR = TextKey.of("jsc.update_window.tab_repair", "Repair");
    static final TextKey NOT_TAKEN = TextKey.of("jsc.update_window.not_taken", "the item does not take it");
    static final TextKey LOADING = TextKey.of("jsc.update_window.loading", "Reading the network...");
    static final TextKey HELD = TextKey.of("jsc.update_window.held", "%s in the network, on %s");
    static final TextKey HELD_NOWHERE = TextKey.of("jsc.update_window.held_nowhere", "none in the network");
    static final TextKey CARD_ENCHANT = TextKey.of("jsc.update_window.card_enchant", "%s on %s, a full table");
    static final TextKey CARD_SMELT = TextKey.of("jsc.update_window.card_smelt", "%s on %s, %sx a furnace");
    static final TextKey CARD_REPAIR = TextKey.of("jsc.update_window.card_repair", "%s on %s, never wears");
    static final TextKey PAY = TextKey.of("jsc.update_window.pay", "Costs %s of your %s levels, no lapis");
    /** How worn the most worn of the item is, the one a repair takes: its damage and its durability. */
    static final TextKey WORN = TextKey.of("jsc.update_window.worn", "worn: %s of %s used");
    static final TextKey WITH = TextKey.of("jsc.update_window.with", "Repair with: %s, from the network (%s of %s)");
    static final TextKey WITH_NOTHING = TextKey.of("jsc.update_window.with_nothing",
            "Nothing in the network mends it: only the name changes");
    static final TextKey NAME = TextKey.of("jsc.update_window.name", "Name:");
    static final TextKey COST = TextKey.of("jsc.update_window.cost", "Cost: %s (an anvil would take %s)");
    static final TextKey NO_CHANGE = TextKey.of("jsc.update_window.no_change", "Nothing to change, or too expensive");
    static final TextKey YOU_HAVE = TextKey.of("jsc.update_window.you_have", "You have %s");
    static final TextKey SMELT = TextKey.of("jsc.update_window.smelt", "Smelt");
    static final TextKey MAX = TextKey.of("jsc.update_window.max", "Max");
    static final TextKey BACK = TextKey.of("jsc.update_window.back", "%s %s, back to %s");
    static final TextKey BACK_ANYWHERE = TextKey.of("jsc.update_window.back_anywhere",
            "%s %s, back to the network");
    static final TextKey WAIT_WORKSHOP = TextKey.of("jsc.update_window.wait_workshop",
            "The card's furnace is busy with a Workshop job (%s of %s): this one waits its turn.");
    static final TextKey WAIT_NETWORK = TextKey.of("jsc.update_window.wait_network",
            "The card's furnace is busy with another update: this one waits its turn.");
    static final TextKey FREE = TextKey.of("jsc.update_window.free",
            "The card's furnace is free: this one starts at once.");
    static final TextKey UPDATE = TextKey.of("jsc.update_window.update", "Update");
    static final TextKey CANCEL = TextKey.of("jsc.update_window.cancel", "Cancel");

    private UpdatePopupTexts() {
    }
}
