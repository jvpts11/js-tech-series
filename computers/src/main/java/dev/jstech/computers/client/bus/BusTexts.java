/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.bus;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the buses' windows say, kept apart from the screens so the language generator can read it on a server too,
 * where screens do not exist.
 */
@TextHolder
final class BusTexts {

    static final TextKey EXPORT_TITLE = TextKey.of("jsc.bus.screen.export_title", "EXPORT BUS");
    static final TextKey IMPORT_TITLE = TextKey.of("jsc.bus.screen.import_title", "IMPORT BUS");
    static final TextKey ROUTER_TITLE = TextKey.of("jsc.bus.screen.router_title", "CRAFTING INPUT ROUTER");
    static final TextKey RECEIVING_TITLE = TextKey.of("jsc.bus.screen.receiving_title", "CRAFTING RECEIVING BUS");
    static final TextKey EXTERNAL_TITLE = TextKey.of("jsc.bus.screen.external_title", "EXTERNAL STORAGE BUS");
    static final TextKey HOLDS = TextKey.of("jsc.bus.screen.holds", "HOLDS");
    static final TextKey HOLDS_VALUE = TextKey.of("jsc.bus.screen.holds_value", "%s slots, %s used");
    static final TextKey HOLDS_NOTE = TextKey.of("jsc.bus.screen.holds_note", "shown to the network as its storage");
    static final TextKey NOTHING_FACED = TextKey.of("jsc.bus.screen.nothing_faced", "no inventory before it");
    static final TextKey SEES = TextKey.of("jsc.bus.screen.sees", "SEES");
    static final TextKey ACCESS = TextKey.of("jsc.bus.screen.access", "ACCESS");
    static final TextKey READ_WRITE = TextKey.of("jsc.bus.screen.read_write", "READ AND WRITE");
    static final TextKey READ_ONLY = TextKey.of("jsc.bus.screen.read_only", "READ ONLY");
    static final TextKey WRITE_ONLY = TextKey.of("jsc.bus.screen.write_only", "WRITE ONLY");
    static final TextKey INTRO_EXTERNAL = TextKey.of("jsc.bus.screen.intro_external",
            "The network sees and uses all of it. 10x slower than its own storage.");
    static final TextKey EXTERNAL_NOTE = TextKey.of("jsc.bus.screen.external_note",
            "10x slower than the network's own storage.");
    static final TextKey FILL_FIRST = TextKey.of("jsc.bus.screen.fill_first", "fill this before lower ones");
    static final TextKey NOTHING_TO_SET = TextKey.of("jsc.bus.screen.nothing_to_set",
            "Nothing to set: the network sees and uses all of the inventory.");
    static final TextKey RECEIVING_SOFTWARE = TextKey.of("jsc.bus.screen.receiving_software",
            "A Receiving Bus is set in CONFIGURE only: it credits what its interfaces fed, and software sets those.");
    static final TextKey CELL_HINT = TextKey.of("jsc.bus.screen.cell_hint",
            "Click with an item to list it here; with nothing in hand, to clear it");
    static final TextKey NAME_FIELD = TextKey.of("jsc.bus.screen.name_field", "name");
    static final TextKey LINKED = TextKey.of("jsc.bus.screen.linked", "LINKED");
    static final TextKey OFFLINE = TextKey.of("jsc.bus.screen.offline", "OFFLINE");
    static final TextKey INVENTORY = TextKey.of("jsc.bus.screen.inventory", "INVENTORY");

    static final TextKey TAB_CONFIGURE = TextKey.of("jsc.bus.screen.tab_configure", "CONFIGURE");
    static final TextKey TAB_ACTIVITY = TextKey.of("jsc.bus.screen.tab_activity", "ACTIVITY");
    static final TextKey TAB_SOFTWARE = TextKey.of("jsc.bus.screen.tab_software", "SOFTWARE");

    static final TextKey NAME = TextKey.of("jsc.bus.screen.name", "NAME");
    static final TextKey INTRO_IMPORT = TextKey.of("jsc.bus.screen.intro_import",
            "Takes whatever it finds first, one kind at a time. No filter.");
    static final TextKey INTRO_EXPORT = TextKey.of("jsc.bus.screen.intro_export",
            "Sends the one kind set here, a move at a time. No filter.");
    static final TextKey NOW = TextKey.of("jsc.bus.screen.now", "NOW");
    static final TextKey SENDS = TextKey.of("jsc.bus.screen.sends", "SENDS");
    static final TextKey MOVED = TextKey.of("jsc.bus.screen.moved", "%s moved");
    static final TextKey NOTHING_YET = TextKey.of("jsc.bus.screen.nothing_yet", "nothing yet");
    static final TextKey FILTER = TextKey.of("jsc.bus.screen.filter", "FILTER");
    static final TextKey ITEMS_LABEL = TextKey.of("jsc.bus.screen.items_label",
            "FILTER, A KEEP AND A MAX FOR EACH ITEM");
    static final TextKey ADD_ITEM = TextKey.of("jsc.bus.screen.add_item", "+ item");
    static final TextKey ADD_ITEM_HINT = TextKey.of("jsc.bus.screen.add_item_hint",
            "Click with an item to list it");
    static final TextKey ONLY_THESE = TextKey.of("jsc.bus.screen.only_these", "ONLY THESE");
    static final TextKey ALL_BUT = TextKey.of("jsc.bus.screen.all_but", "ALL BUT THESE");
    static final TextKey KEEP = TextKey.of("jsc.bus.screen.min", "KEEP");
    static final TextKey MAX = TextKey.of("jsc.bus.screen.max", "MAX");
    static final TextKey ANY = TextKey.of("jsc.bus.screen.any", "any");
    static final TextKey KEEP_NOTE_IMPORT = TextKey.of("jsc.bus.screen.keep_note_import", "left in the chest");
    static final TextKey KEEP_NOTE_EXPORT = TextKey.of("jsc.bus.screen.keep_note_export", "kept in the chest");
    static final TextKey MAX_NOTE = TextKey.of("jsc.bus.screen.max_note", "a time");
    static final TextKey STEP_HINT = TextKey.of("jsc.bus.screen.step_hint", "Shift: by 16");
    static final TextKey MODE = TextKey.of("jsc.bus.screen.mode", "MODE");
    static final TextKey CONTINUOUS = TextKey.of("jsc.bus.screen.continuous", "CONTINUOUS");
    static final TextKey ON_DEMAND = TextKey.of("jsc.bus.screen.on_demand", "ON DEMAND");
    static final TextKey ON_DEMAND_HINT = TextKey.of("jsc.bus.screen.on_demand_hint",
            "Moves only while its cable has a redstone signal");
    static final TextKey POWER = TextKey.of("jsc.bus.screen.power", "POWER");
    static final TextKey ON = TextKey.of("jsc.bus.screen.on", "ON");
    static final TextKey OFF = TextKey.of("jsc.bus.screen.off", "OFF");
    static final TextKey PRIORITY = TextKey.of("jsc.bus.screen.priority", "PRIORITY");
    static final TextKey PRIORITY_HINT = TextKey.of("jsc.bus.screen.priority_hint",
            "When buses want the same thing, the higher goes first");
    static final TextKey CONDITIONS = TextKey.of("jsc.bus.screen.conditions", "CONDITIONS");
    static final TextKey COND_STOCK = TextKey.of("jsc.bus.screen.cond_stock",
            "only while the network holds under %s %s");
    static final TextKey COND_HOURS = TextKey.of("jsc.bus.screen.cond_hours", "only between %s and %s");
    static final TextKey COND_AFTER = TextKey.of("jsc.bus.screen.cond_after", "after the bus %s finishes");
    static final TextKey ADD_CONDITION = TextKey.of("jsc.bus.screen.add_condition", "+ condition");
    static final TextKey KIND_STOCK = TextKey.of("jsc.bus.screen.kind_stock", "STOCK");
    static final TextKey KIND_HOURS = TextKey.of("jsc.bus.screen.kind_hours", "HOURS");
    static final TextKey KIND_AFTER = TextKey.of("jsc.bus.screen.kind_after", "AFTER");
    static final TextKey ADD = TextKey.of("jsc.bus.screen.add", "ADD");
    static final TextKey CANCEL = TextKey.of("jsc.bus.screen.cancel", "CANCEL");
    static final TextKey FROM = TextKey.of("jsc.bus.screen.from", "FROM");
    static final TextKey TO = TextKey.of("jsc.bus.screen.to", "TO");
    static final TextKey STOCK_FIELD = TextKey.of("jsc.bus.screen.stock_field", "item, or #tag");
    static final TextKey STOCK_HINT = TextKey.of("jsc.bus.screen.stock_hint",
            "An item's id, or a tag after #; click with an item to write its id. Then how few the network holds.");
    static final TextKey AFTER_FIELD = TextKey.of("jsc.bus.screen.after_field", "the other bus's name");
    static final TextKey REMOVE_HINT = TextKey.of("jsc.bus.screen.remove_hint", "Take it off");
    static final TextKey TAGS = TextKey.of("jsc.bus.screen.tags", "TAGS");
    static final TextKey ADD_TAG = TextKey.of("jsc.bus.screen.add_tag", "+ tag");
    static final TextKey TAG_FIELD = TextKey.of("jsc.bus.screen.tag_field", "c:ores");
    static final TextKey MATCH = TextKey.of("jsc.bus.screen.match", "MATCH");
    static final TextKey EXACT = TextKey.of("jsc.bus.screen.exact", "EXACT");
    static final TextKey FUZZY = TextKey.of("jsc.bus.screen.fuzzy", "FUZZY");
    static final TextKey FUZZY_NOTE = TextKey.of("jsc.bus.screen.fuzzy_note",
            "Fuzzy: an item matches whatever its damage and its components.");
    static final TextKey SPEED = TextKey.of("jsc.bus.screen.speed", "SPEED");
    static final TextKey SPEED_VALUE = TextKey.of("jsc.bus.screen.speed_value", "%s it/t");
    static final TextKey CABLE_CARRIES = TextKey.of("jsc.bus.screen.cable_carries", "the cable carries %s");
    static final TextKey NO_CABLE = TextKey.of("jsc.bus.screen.no_cable", "on no network");
    static final TextKey TIE = TextKey.of("jsc.bus.screen.tie", "TIE TO INTERFACE...");
    static final TextKey CREDITED = TextKey.of("jsc.bus.screen.credited", "to %s");
    static final TextKey UNEXPECTED_TO = TextKey.of("jsc.bus.screen.unexpected_to", "to the network: unexpected");
    static final TextKey LATE_TO = TextKey.of("jsc.bus.screen.late_to", "where its settled job's outputs go");
    static final TextKey STATUS_CREDITED = TextKey.of("jsc.bus.screen.status_credited", "CREDITED");
    static final TextKey STATUS_UNEXPECTED = TextKey.of("jsc.bus.screen.status_unexpected", "UNEXPECTED");
    static final TextKey STATUS_LATE = TextKey.of("jsc.bus.screen.status_late", "LATE");
    static final TextKey CREDIT_NOTE = TextKey.of("jsc.bus.screen.credit_note",
            "Credit is capped by what each job fed.");
    static final TextKey SET_BY = TextKey.of("jsc.bus.screen.set_by",
            "Set by the program %s. Changing it here takes it back by hand.");

    static final TextKey ACTIVITY_NOTE = TextKey.of("jsc.bus.screen.activity_note",
            "Each move is an Operation: the terminal and the Network Manager list the same log.");
    static final TextKey NO_ACTIVITY = TextKey.of("jsc.bus.screen.no_activity", "Nothing moved yet.");
    static final TextKey AMOUNT = TextKey.of("jsc.bus.screen.amount", "%s x%s");
    static final TextKey INTO_NETWORK = TextKey.of("jsc.bus.screen.into_network", "into the network");
    static final TextKey INTO_CHEST = TextKey.of("jsc.bus.screen.into_chest", "into the chest");
    static final TextKey CHEST_KEEPS = TextKey.of("jsc.bus.screen.chest_keeps", "the chest keeps %s");
    static final TextKey NETWORK_FULL = TextKey.of("jsc.bus.screen.network_full", "the network is full");
    static final TextKey CHEST_FULL = TextKey.of("jsc.bus.screen.chest_full", "the chest is full");
    static final TextKey CONDITION_HOLDS = TextKey.of("jsc.bus.screen.condition_holds", "a condition holds it");
    static final TextKey STATUS_COMPLETED = TextKey.of("jsc.bus.screen.status_completed", "COMPLETED");
    static final TextKey STATUS_PARTIAL = TextKey.of("jsc.bus.screen.status_partial", "COMPLETED_PARTIAL");
    static final TextKey STATUS_WAITING = TextKey.of("jsc.bus.screen.status_waiting", "WAITING");
    static final TextKey STATUS_LOCKED = TextKey.of("jsc.bus.screen.status_locked", "RESOURCE_LOCKED");

    static final TextKey ADDRESS = TextKey.of("jsc.bus.screen.address", "ADDRESS");
    static final TextKey IQL = TextKey.of("jsc.bus.screen.iql", "IQL");
    static final TextKey SIGMA = TextKey.of("jsc.bus.screen.sigma", "SIGMA");
    static final TextKey SET_BY_PROGRAMS = TextKey.of("jsc.bus.screen.set_by_programs", "SET BY PROGRAMS");
    static final TextKey NOTHING_SET = TextKey.of("jsc.bus.screen.nothing_set",
            "Nothing yet: what a program sets is listed here.");
    static final TextKey PROGRAM_SET = TextKey.of("jsc.bus.screen.program_set", "%s: %s");
    static final TextKey SOFTWARE_NOTE = TextKey.of("jsc.bus.screen.software_note",
            "Everything here can also be set in CONFIGURE, and what a program set is marked there.");
    static final TextKey NO_NAME = TextKey.of("jsc.bus.screen.no_name",
            "Give the bus a name in CONFIGURE so software can find it.");
    static final TextKey SETTING_POWER = TextKey.of("jsc.bus.screen.setting_power", "on or off");
    static final TextKey SETTING_MODE = TextKey.of("jsc.bus.screen.setting_mode", "the mode");
    static final TextKey SETTING_FILTER = TextKey.of("jsc.bus.screen.setting_filter", "the filter");
    static final TextKey SETTING_KEEP = TextKey.of("jsc.bus.screen.setting_keep", "the keep");
    static final TextKey SETTING_MAX = TextKey.of("jsc.bus.screen.setting_max", "the max");
    static final TextKey SETTING_PRIORITY = TextKey.of("jsc.bus.screen.setting_priority", "the priority");
    static final TextKey SETTING_CONDITIONS = TextKey.of("jsc.bus.screen.setting_conditions", "the conditions");
    static final TextKey SETTING_MATCH = TextKey.of("jsc.bus.screen.setting_match", "the match");
    static final TextKey SETTING_ACCESS = TextKey.of("jsc.bus.screen.setting_access", "the access");

    private BusTexts() {
    }
}
