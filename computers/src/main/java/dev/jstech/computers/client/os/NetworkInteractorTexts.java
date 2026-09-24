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

/**
 * What the Network Interactor's window says, kept apart from the window so the language generator can read it on a
 * server too, where windows do not exist.
 */
@TextHolder
final class NetworkInteractorTexts {

    // The tabs and the toolbar.
    static final TextKey STATUS_TAB = TextKey.of("jsc.interactor.status_tab", "Status");
    static final TextKey LOCAL_TAB = TextKey.of("jsc.interactor.local_tab", "Local");
    static final TextKey NETWORK_TAB = TextKey.of("jsc.interactor.network_tab", "Network");
    static final TextKey CRAFTING_TAB = TextKey.of("jsc.interactor.crafting_tab", "Crafting");
    static final TextKey OPERATIONS_TAB = TextKey.of("jsc.interactor.operations_tab", "Operations");
    static final TextKey FAVOURITES_TAB = TextKey.of("jsc.interactor.favourites_tab", "%s Favourites");
    static final TextKey SORT_A_Z = TextKey.of("jsc.interactor.sort_a_z", "A-Z");
    static final TextKey SORT_MOST = TextKey.of("jsc.interactor.sort_most", "MOST");
    static final TextKey SORT_LEAST = TextKey.of("jsc.interactor.sort_least", "LEAST");
    static final TextKey MOD = TextKey.of("jsc.interactor.mod", "Mod");
    static final TextKey CATEGORY = TextKey.of("jsc.interactor.category", "Category");
    static final TextKey ANY_MOD = TextKey.of("jsc.interactor.any_mod", "Any mod");
    static final TextKey ANY_CATEGORY = TextKey.of("jsc.interactor.any_category", "Any category");
    static final TextKey HINT = TextKey.of("jsc.interactor.hint",
            "Arrows move  ·  Enter request  ·  C craft  ·  F favourite  ·  / search");
    static final TextKey MINECRAFT = TextKey.of("jsc.interactor.minecraft", "Minecraft");

    // What an empty well says.
    static final TextKey NO_CRAFTS_MATCH =
            TextKey.of("jsc.interactor.no_crafts_match", "No crafts match the search and filters.");
    static final TextKey NO_PATTERNS = TextKey.of("jsc.interactor.no_patterns",
            "No patterns on the network. Load .craft files on a Crafting Computer.");
    static final TextKey NOTHING_STARRED = TextKey.of("jsc.interactor.nothing_starred",
            "Nothing starred yet. Press F on an item, or its star in the details.");
    static final TextKey NO_FAVOURITES_MATCH =
            TextKey.of("jsc.interactor.no_favourites_match", "No favourites match the search and filters.");
    static final TextKey NOTHING_MATCHES =
            TextKey.of("jsc.interactor.nothing_matches", "Nothing matches the search and filters.");
    static final TextKey NOTHING_LOCAL = TextKey.of("jsc.interactor.nothing_local", "Nothing on this computer's disks.");
    static final TextKey NOTHING_ON_NETWORK = TextKey.of("jsc.interactor.nothing_on_network", "Nothing on the network.");

    // The caption over the well.
    static final TextKey LOCAL_STORAGE = TextKey.of("jsc.interactor.local_storage", "LOCAL STORAGE");
    static final TextKey CRAFTABLE_CAPTION = TextKey.of("jsc.interactor.craftable_caption", "CRAFTABLE");
    static final TextKey FAVOURITES_CAPTION = TextKey.of("jsc.interactor.favourites_caption", "FAVOURITES");
    static final TextKey OPERATIONS_CAPTION = TextKey.of("jsc.interactor.operations_caption", "OPERATIONS");
    static final TextKey NETWORK_STORAGE = TextKey.of("jsc.interactor.network_storage", "NETWORK STORAGE");
    static final TextKey TYPES = TextKey.of("jsc.interactor.types", "%s types");
    static final TextKey ONE_RECIPE = TextKey.of("jsc.interactor.one_recipe", "%s recipe");
    static final TextKey RECIPES = TextKey.of("jsc.interactor.recipes", "%s recipes");
    static final TextKey STARRED = TextKey.of("jsc.interactor.starred", "%s starred");
    static final TextKey TYPES_AND_SIZE = TextKey.of("jsc.interactor.types_and_size", "%s types · %s");

    // The status bar and the network's card.
    static final TextKey MAINFRAME_ONLINE = TextKey.of("jsc.interactor.mainframe_online", "● Mainframe online");
    static final TextKey MAINFRAME_OFFLINE = TextKey.of("jsc.interactor.mainframe_offline", "○ Mainframe offline");
    static final TextKey ONE_SERVER = TextKey.of("jsc.interactor.one_server", "%s server");
    static final TextKey SERVERS_COUNT = TextKey.of("jsc.interactor.servers_count", "%s servers");
    static final TextKey USED_OF = TextKey.of("jsc.interactor.used_of", "%s / %s");
    static final TextKey STORED = TextKey.of("jsc.interactor.stored", "%s stored");
    static final TextKey THIS_NETWORK = TextKey.of("jsc.interactor.this_network", "This network");
    static final TextKey STORAGE = TextKey.of("jsc.interactor.storage", "STORAGE");
    static final TextKey USED = TextKey.of("jsc.interactor.used", "used");
    static final TextKey HELD = TextKey.of("jsc.interactor.held", "held");
    static final TextKey OF = TextKey.of("jsc.interactor.of", "%s of %s");
    static final TextKey OF_ITEMS = TextKey.of("jsc.interactor.of_items", "%s of %s items");
    static final TextKey ITEMS = TextKey.of("jsc.interactor.items", "%s items");
    static final TextKey ON_THE_NETWORK = TextKey.of("jsc.interactor.on_the_network", "ON THE NETWORK");
    static final TextKey ITEM_TYPES = TextKey.of("jsc.interactor.item_types", "Item types");
    static final TextKey SERVERS = TextKey.of("jsc.interactor.servers", "Servers");
    static final TextKey CRAFTABLE = TextKey.of("jsc.interactor.craftable", "Craftable");
    static final TextKey FAVOURITES = TextKey.of("jsc.interactor.favourites", "Favourites");
    static final TextKey RUNNING = TextKey.of("jsc.interactor.running", "RUNNING");
    static final TextKey OPERATIONS = TextKey.of("jsc.interactor.operations", "Operations");
    static final TextKey LIVE = TextKey.of("jsc.interactor.live", "%s live");
    static final TextKey SELECT_AN_ITEM =
            TextKey.of("jsc.interactor.select_an_item", "Select an item to see its details.");
    static final TextKey INVENTORY = TextKey.of("jsc.interactor.inventory", "INVENTORY");

    // The details panel.
    static final TextKey MADE_BY = TextKey.of("jsc.interactor.made_by", "MADE BY");
    static final TextKey USED_IN = TextKey.of("jsc.interactor.used_in", "USED IN");
    static final TextKey REQUEST = TextKey.of("jsc.interactor.request", "Request");
    static final TextKey CRAFT = TextKey.of("jsc.interactor.craft", "Craft");
    static final TextKey ID = TextKey.of("jsc.interactor.id", "ID");
    static final TextKey KIND = TextKey.of("jsc.interactor.kind", "KIND");
    static final TextKey ITEM = TextKey.of("jsc.interactor.item", "Item");
    static final TextKey FLUID = TextKey.of("jsc.interactor.fluid", "Fluid");
    static final TextKey CHEMICAL = TextKey.of("jsc.interactor.chemical", "Chemical");
    static final TextKey ROOM = TextKey.of("jsc.interactor.room", "ROOM");
    static final TextKey DURABILITY = TextKey.of("jsc.interactor.durability", "DURABILITY");
    static final TextKey TAGS = TextKey.of("jsc.interactor.tags", "TAGS");
    static final TextKey COMPONENTS = TextKey.of("jsc.interactor.components", "COMPONENTS");
    static final TextKey NONE = TextKey.of("jsc.interactor.none", "(none)");
    static final TextKey ITEMS_SELECTED = TextKey.of("jsc.interactor.items_selected", "%s items selected");
    static final TextKey SELECTED = TextKey.of("jsc.interactor.selected", "SELECTED");
    static final TextKey TOGETHER = TextKey.of("jsc.interactor.together", "TOGETHER");
    static final TextKey ITEMS_AND_ROOM = TextKey.of("jsc.interactor.items_and_room", "%s items · %s");
    static final TextKey MORE = TextKey.of("jsc.interactor.more", "+%s more");
    static final TextKey NOT_IN_STOCK = TextKey.of("jsc.interactor.not_in_stock", "not in stock");
    static final TextKey NOT_IN_STOCK_CRAFTABLE =
            TextKey.of("jsc.interactor.not_in_stock_craftable", "not in stock · craftable");
    static final TextKey IN_THE_NETWORK = TextKey.of("jsc.interactor.in_the_network", "%s in the network");
    static final TextKey IN_THE_NETWORK_ON = TextKey.of("jsc.interactor.in_the_network_on", "%s in the network · %s");
    /* An exact amount with its unit, the number already grouped: "1,248 items". */
    static final TextKey ONE_ITEM_EXACT = TextKey.of("jsc.interactor.one_item_exact", "%s item");
    static final TextKey ITEMS_EXACT = TextKey.of("jsc.interactor.items_exact", "%s items");
    static final TextKey MILLIBUCKETS = TextKey.of("jsc.interactor.millibuckets", "%s mB");

    // The request dialog.
    static final TextKey THIS_COMPUTER = TextKey.of("jsc.interactor.this_computer", "This computer");
    static final TextKey REQUEST_ITEMS = TextKey.of("jsc.interactor.request_items", "Request %s items");
    static final TextKey AVAILABLE = TextKey.of("jsc.interactor.available", "%s available");
    static final TextKey ADVANCED = TextKey.of("jsc.interactor.advanced", "Adv");
    static final TextKey QUANTITY = TextKey.of("jsc.interactor.quantity", "x%s");
    static final TextKey QUANTITY_EACH = TextKey.of("jsc.interactor.quantity_each", "x%s each");
    static final TextKey MAX = TextKey.of("jsc.interactor.max", "Max");
    static final TextKey PRIORITY = TextKey.of("jsc.interactor.priority", "PRIORITY");
    static final TextKey PULL_FROM = TextKey.of("jsc.interactor.pull_from", "PULL FROM (servers)");
    static final TextKey ALL_SOURCES = TextKey.of("jsc.interactor.all_sources", "all sources");
    static final TextKey SEND_TO = TextKey.of("jsc.interactor.send_to", "SEND TO");
    static final TextKey SEND = TextKey.of("jsc.interactor.send", "Send");
    static final TextKey TO_INVENTORY = TextKey.of("jsc.interactor.to_inventory", "To Inventory");
    static final TextKey TO_NETWORK = TextKey.of("jsc.interactor.to_network", "To Network");

    // The craft dialog.
    static final TextKey CRAFT_ITEM = TextKey.of("jsc.interactor.craft_item", "Craft %s");
    static final TextKey KIND_AND_MACHINES = TextKey.of("jsc.interactor.kind_and_machines", "%s · %s");
    static final TextKey ONE_STAGE = TextKey.of("jsc.interactor.one_stage", "%s stage");
    static final TextKey STAGES = TextKey.of("jsc.interactor.stages", "%s stages");
    static final TextKey ONE_STAGE_TIMED = TextKey.of("jsc.interactor.one_stage_timed", "%s stage · ~%s s");
    static final TextKey STAGES_TIMED = TextKey.of("jsc.interactor.stages_timed", "%s stages · ~%s s");
    static final TextKey INPUT_IN_STOCK = TextKey.of("jsc.interactor.input_in_stock", "%s %s (%s in stock)");
    static final TextKey PRIORITY_BUTTON = TextKey.of("jsc.interactor.priority_button", "Prio: %s");
    static final TextKey PARTIAL = TextKey.of("jsc.interactor.partial", "Partial");
    static final TextKey CLOSE = TextKey.of("jsc.interactor.close", "Close");
    static final TextKey RECIPE_CHOICES = TextKey.of("jsc.interactor.recipe_choices", "RECIPE  ·  %s patterns make this");
    static final TextKey RECIPE_CHOICES_MORE = TextKey.of("jsc.interactor.recipe_choices_more",
            "RECIPE  ·  %s patterns make this (Left/Right for more)");
    static final TextKey PLAN_WITH = TextKey.of("jsc.interactor.plan_with", "PLAN - with %s");
    static final TextKey PLAN_RAW = TextKey.of("jsc.interactor.plan_raw", "PLAN - raw ingredients");
    static final TextKey PLANNING = TextKey.of("jsc.interactor.planning", "planning...");
    static final TextKey ESTIMATE = TextKey.of("jsc.interactor.estimate", "EST ~%ss");
    static final TextKey NO_ESTIMATE = TextKey.of("jsc.interactor.no_estimate", "EST --");
    static final TextKey ESTIMATE_STAGES = TextKey.of("jsc.interactor.estimate_stages", "%s · %s stages");
    static final TextKey MAX_FEASIBLE = TextKey.of("jsc.interactor.max_feasible", "max %s");

    // The Operations tab: where an Operation has got to, short for a row and long for the detail.
    static final TextKey SHORT_DONE = TextKey.of("jsc.interactor.ops.short_done", "done");
    static final TextKey SHORT_PARTIAL = TextKey.of("jsc.interactor.ops.short_partial", "part");
    static final TextKey SHORT_FAILED = TextKey.of("jsc.interactor.ops.short_failed", "fail");
    static final TextKey SHORT_RUNNING = TextKey.of("jsc.interactor.ops.short_running", "run");
    static final TextKey SHORT_WAITING = TextKey.of("jsc.interactor.ops.short_waiting", "wait");
    static final TextKey SHORT_LOCKED = TextKey.of("jsc.interactor.ops.short_locked", "lock");
    static final TextKey SHORT_PENDING = TextKey.of("jsc.interactor.ops.short_pending", "pend");
    static final TextKey SHORT_DROPPED = TextKey.of("jsc.interactor.ops.short_dropped", "drop");
    static final TextKey COMPLETED = TextKey.of("jsc.interactor.ops.completed", "Completed");
    static final TextKey COMPLETED_PARTIAL = TextKey.of("jsc.interactor.ops.completed_partial", "Completed (partial)");
    static final TextKey FAILED = TextKey.of("jsc.interactor.ops.failed", "Failed");
    static final TextKey PROCESSING = TextKey.of("jsc.interactor.ops.processing", "Processing");
    static final TextKey WAITING = TextKey.of("jsc.interactor.ops.waiting", "Waiting");
    static final TextKey RESOURCE_LOCKED = TextKey.of("jsc.interactor.ops.resource_locked", "Resource locked");
    static final TextKey PENDING = TextKey.of("jsc.interactor.ops.pending", "Pending");
    static final TextKey DISCARDED = TextKey.of("jsc.interactor.ops.discarded", "Discarded");
    static final TextKey SUB_READING = TextKey.of("jsc.interactor.ops.sub_reading", "reading");
    static final TextKey SUB_STREAMING = TextKey.of("jsc.interactor.ops.sub_streaming", "streaming");
    static final TextKey SUB_DONE = TextKey.of("jsc.interactor.ops.sub_done", "done");
    static final TextKey SUB_QUEUED = TextKey.of("jsc.interactor.ops.sub_queued", "queued");
    static final TextKey OPS_CAPTION = TextKey.of("jsc.interactor.ops.caption", "%s live · %s recent");
    static final TextKey SUPERCOMPUTER_SLOTS =
            TextKey.of("jsc.interactor.ops.supercomputer_slots", "Supercomputer: %s / %s parallel crafts");
    static final TextKey NO_OPERATIONS = TextKey.of("jsc.interactor.ops.no_operations", "No operations on the network.");
    static final TextKey MOVED = TextKey.of("jsc.interactor.ops.moved", "moved %s / %s");
    static final TextKey SUBOPERATIONS = TextKey.of("jsc.interactor.ops.suboperations", "SUBOPERATIONS");
    /* One machine's share: the machine, how far it has got, and in what state. */
    static final TextKey SUB_LINE = TextKey.of("jsc.interactor.ops.sub_line", "%s: %s/%s %s");
    static final TextKey SOURCES = TextKey.of("jsc.interactor.ops.sources", "SOURCES");

    private NetworkInteractorTexts() {
    }
}
