/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The words inside the portables' apps (computers/docs/design/wireless.md), the network app's in the Interactor's. */
@TextHolder
public final class AppTexts {

    // The network app.
    public static final TextKey STOCK = TextKey.of("jsc.mobile.app.stock", "Stock");
    public static final TextKey DEVICE = TextKey.of("jsc.mobile.app.device", "Device");
    public static final TextKey CRAFT = TextKey.of("jsc.mobile.app.craft", "Craft");
    public static final TextKey SEARCH_TYPES = TextKey.of("jsc.mobile.app.search_types", "Search %s types");
    public static final TextKey SEARCH = TextKey.of("jsc.mobile.app.search", "Search");
    public static final TextKey THIS_DEVICE_HOLDS = TextKey.of("jsc.mobile.app.this_device_holds",
            "This device: %s of %s");
    public static final TextKey IN_THE_NETWORK = TextKey.of("jsc.mobile.app.in_the_network", "%s in the network");
    public static final TextKey BELOW = TextKey.of("jsc.mobile.app.below", "below %s");
    public static final TextKey REQUEST = TextKey.of("jsc.mobile.app.request", "Request");
    public static final TextKey REQUEST_N = TextKey.of("jsc.mobile.app.request_n", "Request %s");
    public static final TextKey INTO_THIS_DEVICE = TextKey.of("jsc.mobile.app.into_this_device", "into this device");
    public static final TextKey FREE_OF = TextKey.of("jsc.mobile.app.free_of", "%s of %s free");
    public static final TextKey CRAFT_MORE = TextKey.of("jsc.mobile.app.craft_more", "Craft more");
    public static final TextKey TAKE_IT_IN_DEVICE = TextKey.of("jsc.mobile.app.take_it_in_device",
            "Take it in Device");
    public static final TextKey THIS_DEVICE = TextKey.of("jsc.mobile.app.this_device", "This device");
    public static final TextKey USED_OF = TextKey.of("jsc.mobile.app.used_of", "%s of %s");
    public static final TextKey TAKE = TextKey.of("jsc.mobile.app.take", "Take");
    public static final TextKey RETURN = TextKey.of("jsc.mobile.app.return", "Return");
    public static final TextKey DEPOSIT_DOTS = TextKey.of("jsc.mobile.app.deposit_dots", "Deposit...");
    public static final TextKey TAKE_HINT = TextKey.of("jsc.mobile.app.take_hint", "Take: to your inventory");
    public static final TextKey NOTHING_ON_DEVICE = TextKey.of("jsc.mobile.app.nothing_on_device",
            "Nothing on this device yet");
    public static final TextKey DEPOSIT = TextKey.of("jsc.mobile.app.deposit", "Deposit");
    public static final TextKey YOUR_INVENTORY = TextKey.of("jsc.mobile.app.your_inventory", "YOUR INVENTORY");
    public static final TextKey GIVE = TextKey.of("jsc.mobile.app.give", "Give");
    public static final TextKey DEPOSIT_ALL = TextKey.of("jsc.mobile.app.deposit_all", "Deposit all");
    public static final TextKey INTO_THE_NETWORK = TextKey.of("jsc.mobile.app.into_the_network",
            "into the network, MEDIUM");
    public static final TextKey NAME_COLUMN = TextKey.of("jsc.mobile.app.name_column", "Name");
    public static final TextKey IN_NETWORK_COLUMN = TextKey.of("jsc.mobile.app.in_network_column", "In network");
    public static final TextKey MENU = TextKey.of("jsc.mobile.app.menu", "Menu");
    public static final TextKey REQUEST_DOTS = TextKey.of("jsc.mobile.app.request_dots", "Request...");
    public static final TextKey ON_DEVICE_DOTS = TextKey.of("jsc.mobile.app.on_device_dots", "On device...");
    public static final TextKey CRAFT_DOTS = TextKey.of("jsc.mobile.app.craft_dots", "Craft...");
    public static final TextKey A_PATTERN = TextKey.of("jsc.mobile.app.a_pattern", "a pattern in the network");
    public static final TextKey A_PIPELINE = TextKey.of("jsc.mobile.app.a_pipeline",
            "a pipeline of machines in the network");
    public static final TextKey IS_LOW = TextKey.of("jsc.mobile.app.is_low", "%s is low: %s left");
    public static final TextKey HOW_MANY = TextKey.of("jsc.mobile.app.how_many", "How many");
    public static final TextKey CRAFT_N = TextKey.of("jsc.mobile.app.craft_n", "Craft %s");
    public static final TextKey PLAN_IT = TextKey.of("jsc.mobile.app.plan_it", "Plan it");
    public static final TextKey RUNNING_ON = TextKey.of("jsc.mobile.app.running_on", "RUNNING ON %s");
    public static final TextKey NOTHING_RUNNING = TextKey.of("jsc.mobile.app.nothing_running", "Nothing running");
    public static final TextKey NO_CRAFTS = TextKey.of("jsc.mobile.app.no_crafts", "The network crafts nothing yet");
    public static final TextKey ALL_THERE = TextKey.of("jsc.mobile.app.all_there", "Everything it needs is there");
    public static final TextKey SHORT_OF = TextKey.of("jsc.mobile.app.short_of", "It can make %s of them");
    public static final TextKey OUT_OF_REACH = TextKey.of("jsc.mobile.app.out_of_reach",
            "No access point in reach");
    public static final TextKey OFFLINE = TextKey.of("jsc.mobile.app.offline", "The network's Mainframe is not on");
    public static final TextKey EMPTY_STOCK = TextKey.of("jsc.mobile.app.empty_stock", "Nothing found");
    public static final TextKey TIMES = TextKey.of("jsc.mobile.app.times", "x%s");
    public static final TextKey NAMED_TIMES = TextKey.of("jsc.mobile.app.named_times", "%s x%s");

    // The alerts.
    public static final TextKey SEND_TO_THIS_DEVICE = TextKey.of("jsc.mobile.app.send_to_this_device",
            "SEND TO THIS DEVICE");
    public static final TextKey KIND_ITEMS = TextKey.of("jsc.mobile.app.kind_items", "Items below a level");
    public static final TextKey KIND_OPERATIONS = TextKey.of("jsc.mobile.app.kind_operations", "Failed operations");
    public static final TextKey KIND_POWER = TextKey.of("jsc.mobile.app.kind_power", "Degraded supply");
    public static final TextKey KIND_UPS = TextKey.of("jsc.mobile.app.kind_ups", "UPS on battery");
    public static final TextKey KIND_CONFLICT = TextKey.of("jsc.mobile.app.kind_conflict", "Network conflicts");
    public static final TextKey WATCHED_ITEMS = TextKey.of("jsc.mobile.app.watched_items", "WATCHED ITEMS");
    public static final TextKey WATCH_AN_ITEM = TextKey.of("jsc.mobile.app.watch_an_item", "+ Watch an item");
    public static final TextKey UNDER = TextKey.of("jsc.mobile.app.under", "< %s");
    public static final TextKey TELL_ME_BELOW = TextKey.of("jsc.mobile.app.tell_me_below", "Tell me below");
    public static final TextKey STOP_WATCHING = TextKey.of("jsc.mobile.app.stop_watching", "Stop watching");
    public static final TextKey CHOOSE_AN_ITEM = TextKey.of("jsc.mobile.app.choose_an_item",
            "Choose an item to watch");
    public static final TextKey WATCHES_FULL = TextKey.of("jsc.mobile.app.watches_full",
            "It watches as many items as it can");

    // The Messenger.
    public static final TextKey ROOMS = TextKey.of("jsc.mobile.app.rooms", "ROOMS");
    public static final TextKey PEOPLE = TextKey.of("jsc.mobile.app.people", "PEOPLE HERE");
    public static final TextKey LOBBY = TextKey.of("jsc.mobile.app.lobby", "Lobby");
    public static final TextKey ON_NETWORK = TextKey.of("jsc.mobile.app.on_network", "on %s");
    public static final TextKey MESSAGE = TextKey.of("jsc.mobile.app.message", "Message");
    public static final TextKey NO_MESSENGER = TextKey.of("jsc.mobile.app.no_messenger",
            "No server on this network runs the Messenger");
    public static final TextKey NUDGED = TextKey.of("jsc.mobile.app.nudged", "%s sent a nudge");
    /* Who said it, then what they said, in a room of many. */
    public static final TextKey SAID_BY = TextKey.of("jsc.mobile.app.said_by", "%s: %s");

    // The camera and the gallery.
    public static final TextKey PHOTO = TextKey.of("jsc.mobile.app.photo", "PHOTO");
    public static final TextKey RESOLUTION = TextKey.of("jsc.mobile.app.resolution", "%s x %s");
    public static final TextKey SENDING = TextKey.of("jsc.mobile.app.sending", "Keeping it: %s%%");
    public static final TextKey PICTURES = TextKey.of("jsc.mobile.app.pictures", "%s pictures");
    public static final TextKey ONE_PICTURE = TextKey.of("jsc.mobile.app.one_picture", "1 picture");
    public static final TextKey NO_PICTURES = TextKey.of("jsc.mobile.app.no_pictures", "No pictures yet");
    public static final TextKey SEND_TO_A_COMPUTER = TextKey.of("jsc.mobile.app.send_to_a_computer",
            "SEND TO A COMPUTER");
    public static final TextKey TAKEN_ON = TextKey.of("jsc.mobile.app.taken_on", "%s x %s · taken Day %s");
    public static final TextKey DELETE = TextKey.of("jsc.mobile.app.delete", "Delete");
    public static final TextKey NO_COMPUTERS = TextKey.of("jsc.mobile.app.no_computers",
            "No personal computer on this network");

    // The maps and the prospecting.
    public static final TextKey YOU = TextKey.of("jsc.mobile.app.you", "You");
    public static final TextKey MAINFRAME = TextKey.of("jsc.mobile.app.mainframe", "Mainframe");
    public static final TextKey YOU_ARE_HERE = TextKey.of("jsc.mobile.app.you_are_here", "You are here");
    public static final TextKey IN_REACH_OF = TextKey.of("jsc.mobile.app.in_reach_of", "In reach of %s");
    public static final TextKey ITEMS_PER_TICK = TextKey.of("jsc.mobile.app.items_per_tick", "%s items per tick");
    public static final TextKey METRES = TextKey.of("jsc.mobile.app.metres", "%s m");
    public static final TextKey STRONG = TextKey.of("jsc.mobile.app.strong", "strong");
    public static final TextKey FAIR = TextKey.of("jsc.mobile.app.fair", "fair");
    public static final TextKey FAINT = TextKey.of("jsc.mobile.app.faint", "faint");
    public static final TextKey HOW_AND_WHERE = TextKey.of("jsc.mobile.app.how_and_where", "%s, %s");
    public static final TextKey NEVER_PLACES = TextKey.of("jsc.mobile.app.never_places", "Directions, never places.");
    public static final TextKey NO_VEINS = TextKey.of("jsc.mobile.app.no_veins", "No veins near");
    public static final TextKey NORTH = TextKey.of("jsc.mobile.app.north", "N");
    public static final TextKey NORTH_EAST = TextKey.of("jsc.mobile.app.north_east", "NE");
    public static final TextKey EAST = TextKey.of("jsc.mobile.app.east", "E");
    public static final TextKey SOUTH_EAST = TextKey.of("jsc.mobile.app.south_east", "SE");
    public static final TextKey SOUTH = TextKey.of("jsc.mobile.app.south", "S");
    public static final TextKey SOUTH_WEST = TextKey.of("jsc.mobile.app.south_west", "SW");
    public static final TextKey WEST = TextKey.of("jsc.mobile.app.west", "W");
    public static final TextKey NORTH_WEST = TextKey.of("jsc.mobile.app.north_west", "NW");

    // The Core's HUD.
    public static final TextKey FROM_YOUR = TextKey.of("jsc.mobile.app.from_your", "from your %s");

    private AppTexts() {
    }
}
