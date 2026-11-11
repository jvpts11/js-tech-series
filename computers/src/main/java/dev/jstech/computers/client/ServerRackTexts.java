/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.backup.TapeDrive;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What a server rack's screen says, kept apart from the screen so the language generator can read it on a server
 * too, where screens do not exist.
 */
@TextHolder
final class ServerRackTexts {

    /* The rack units in use out of the cabinet's, then its state. */
    static final TextKey THROTTLED = TextKey.of("jsc.rack.throttled", "%s/%sU  %s°C  THROTTLED %s%%");
    static final TextKey LINKED = TextKey.of("jsc.rack.linked", "%s/%sU  LINKED");
    static final TextKey OFFLINE = TextKey.of("jsc.rack.offline", "%s/%sU  OFFLINE");
    static final TextKey UNIT = TextKey.of("jsc.rack.unit", "%sU");
    static final TextKey INCOMPLETE = TextKey.of("jsc.rack.incomplete", "INCOMPLETE");
    static final TextKey OFF = TextKey.of("jsc.rack.off", "OFF");
    static final TextKey ONLINE = TextKey.of("jsc.rack.online", "ONLINE");
    static final TextKey READY = TextKey.of("jsc.rack.ready", "READY");
    static final TextKey REBUILD = TextKey.of("jsc.rack.rebuild", "REBUILD %s%%");
    static final TextKey POWER = TextKey.of("jsc.rack.power", "PWR");

    // The card of the selected unit: its lines' labels, then what they can say.
    static final TextKey CARD_STATE = TextKey.of("jsc.rack.card_state", "STATE");
    static final TextKey CARD_BOOT = TextKey.of("jsc.rack.card_boot", "BOOT");
    static final TextKey CARD_SYSTEM = TextKey.of("jsc.rack.card_system", "SYSTEM");
    static final TextKey CARD_NETWORK = TextKey.of("jsc.rack.card_network", "NETWORK");
    static final TextKey CARD_RAID = TextKey.of("jsc.rack.card_raid", "RAID");
    static final TextKey CARD_KVM = TextKey.of("jsc.rack.card_kvm", "KVM");
    static final TextKey NO_UNIT = TextKey.of("jsc.rack.no_unit", "no unit");
    /* The disk the machine boots from, counted among its drive bays from 0 as its firmware counts them. */
    static final TextKey BOOT_DISK = TextKey.of("jsc.rack.boot_disk", "Disk %s");
    static final TextKey NONE = TextKey.of("jsc.rack.none", "none");
    static final TextKey NETWORK_LINKED = TextKey.of("jsc.rack.network_linked", "LINKED");
    /* What a linked monitor shows: this unit, another (by its rack unit), or nothing, two machines and no switch. */
    static final TextKey THIS_UNIT = TextKey.of("jsc.rack.this_unit", "this unit");
    static final TextKey SHOWS_UNIT = TextKey.of("jsc.rack.shows_unit", "shows %sU");
    static final TextKey NO_SWITCH = TextKey.of("jsc.rack.no_switch", "no switch");

    // The tooltips.
    static final TextKey BELONGS_IN_SERVER_RACK =
            TextKey.of("jsc.rack.belongs_in_server_rack", "This chassis belongs in a Server Rack");
    static final TextKey BELONGS_IN_SUPERCOMPUTER_RACK =
            TextKey.of("jsc.rack.belongs_in_supercomputer_rack", "This chassis belongs in a Supercomputer Rack");
    static final TextKey BELONGS_IN_AI_RACK =
            TextKey.of("jsc.rack.belongs_in_ai_rack", "This chassis belongs in an AI Rack");
    static final TextKey NEWER_THAN_RACK =
            TextKey.of("jsc.rack.newer_than_rack", "This chassis is of a later era than this rack");
    static final TextKey ARRAY_MEMBER = TextKey.of("jsc.rack.array_member", "Array member slot - %s");
    static final TextKey DRIVE_BAY = TextKey.of("jsc.rack.drive_bay", "Drive bay - hotswap a disk here");
    static final TextKey BLOCKED_NO_UNIT =
            TextKey.of("jsc.rack.blocked_no_unit", "Blocked - no unit in this row cables these slots");
    static final TextKey BLOCKED_BUDGET =
            TextKey.of("jsc.rack.blocked_budget", "Blocked - the chassis does not cable this slot");
    static final TextKey NODE = TextKey.of("jsc.rack.node", "Node %s");
    static final TextKey UNASSIGNED_NODE = TextKey.of("jsc.rack.unassigned_node", "Unassigned node");
    static final TextKey CHASSIS = TextKey.of("jsc.rack.chassis", "%sU - %s drive bays");
    static final TextKey NEEDS_BOARD = TextKey.of("jsc.rack.needs_board", "Incomplete - needs a board + PSU");
    static final TextKey NOT_ON_NETWORK = TextKey.of("jsc.rack.not_on_network", "Rack cable not on a network");
    /* The header while the rack's UPS carries it, or protects nothing. */
    static final TextKey ON_BATTERY_PILL = TextKey.of("jsc.rack.on_battery_pill", "%s/%sU  ON BATTERY");
    static final TextKey BYPASS_PILL = TextKey.of("jsc.rack.bypass_pill", "%s/%sU  BYPASS");
    /* The Rack UPS's card: where the power comes from, its charge, what it holds, the load, how long, when it stops. */
    static final TextKey CARD_CHARGE = TextKey.of("jsc.rack.card_charge", "CHARGE");
    static final TextKey CARD_HOLDS = TextKey.of("jsc.rack.card_holds", "HOLDS");
    static final TextKey CARD_LOAD = TextKey.of("jsc.rack.card_load", "LOAD");
    static final TextKey CARD_LASTS = TextKey.of("jsc.rack.card_lasts", "LASTS");
    static final TextKey CARD_SHUTDOWN = TextKey.of("jsc.rack.card_shutdown", "SHUTDOWN");
    static final TextKey UPS_ON_LINE = TextKey.of("jsc.rack.ups_on_line", "ON LINE");
    static final TextKey UPS_ON_BATTERY = TextKey.of("jsc.rack.ups_on_battery", "ON BATTERY");
    static final TextKey UPS_BYPASS = TextKey.of("jsc.rack.ups_bypass", "BYPASS");
    static final TextKey PERCENT = TextKey.of("jsc.rack.percent", "%s%%");
    static final TextKey MINUTES = TextKey.of("jsc.rack.minutes", "%s min");
    static final TextKey SHUTDOWN_AT = TextKey.of("jsc.rack.shutdown_at", "at 10%%");
    static final TextKey WATTS = TextKey.of("jsc.rack.watts", "%s W");
    static final TextKey WATTS_SHORT = TextKey.of("jsc.rack.watts_short", "%s%sW");
    /* The UPS Battery Expansion's card: what it adds, and the UPS it adds to. */
    static final TextKey CARD_ADDS = TextKey.of("jsc.rack.card_adds", "ADDS");
    /* The cabinet's heat on a cooling unit's card and a server's (computers/docs/design/temperature.md). */
    static final TextKey CARD_BUDGET = TextKey.of("jsc.rack.card_budget", "BUDGET");
    static final TextKey CARD_TEMP = TextKey.of("jsc.rack.card_temp", "TEMP");
    static final TextKey CARD_MACHINES = TextKey.of("jsc.rack.card_machines", "MACHINES");
    static final TextKey ADDS_WATTS = TextKey.of("jsc.rack.adds_watts", "+%s W");
    /* The card of a server running the Mining Pool. */
    static final TextKey CARD_SERVICE = TextKey.of("jsc.rack.card_service", "SERVICE");
    static final TextKey CARD_MINERS = TextKey.of("jsc.rack.card_miners", "MINERS");
    static final TextKey CARD_HASHRATE = TextKey.of("jsc.rack.card_hashrate", "HASHRATE");
    static final TextKey CARD_FEE = TextKey.of("jsc.rack.card_fee", "FEE");
    static final TextKey CARD_BLOCKS = TextKey.of("jsc.rack.card_blocks", "BLOCKS");
    static final TextKey CARD_LAST = TextKey.of("jsc.rack.card_last", "LAST");
    static final TextKey MINING_POOL = TextKey.of("jsc.rack.mining_pool", "Mining Pool");
    static final TextKey MINUTES_AGO = TextKey.of("jsc.rack.minutes_ago", "%s min ago");
    static final TextKey DEGREES = TextKey.of("jsc.rack.degrees", "%s °C");
    static final TextKey DEGREES_AT = TextKey.of("jsc.rack.degrees_at", "%s °C %s%%");
    static final TextKey MACHINES_AT = TextKey.of("jsc.rack.machines_at", "at %s%%");
    static final TextKey THROTTLED_STATE = TextKey.of("jsc.rack.throttled_state", "THROTTLED");
    static final TextKey CARD_UPS = TextKey.of("jsc.rack.card_ups", "UPS");
    /* The PDU's card: the whole rack's draw, each unit's, and who switches the bays. */
    static final TextKey CARD_TOTAL = TextKey.of("jsc.rack.card_total", "TOTAL");
    static final TextKey CARD_POWER = TextKey.of("jsc.rack.card_power", "POWER");
    static final TextKey UNIT_NAMED = TextKey.of("jsc.rack.unit_named", "%sU %s");
    static final TextKey UPS_CHARGED = TextKey.of("jsc.rack.ups_charged", "charged");
    static final TextKey UPS_CHARGING = TextKey.of("jsc.rack.ups_charging", "charging");
    static final TextKey UPS_DRAINING = TextKey.of("jsc.rack.ups_draining", "on battery");
    static final TextKey IQL_JOBS = TextKey.of("jsc.rack.iql_jobs", "IQL, jobs");
    static final TextKey MORE_UNITS = TextKey.of("jsc.rack.more_units", "+%s more");
    /* The Drive Shelf's card: the server it extends, the bays, the drives in its own, and the array they join. */
    static final TextKey CARD_EXTENDS = TextKey.of("jsc.rack.card_extends", "EXTENDS");
    static final TextKey CARD_BAYS = TextKey.of("jsc.rack.card_bays", "BAYS");
    static final TextKey CARD_DRIVES = TextKey.of("jsc.rack.card_drives", "DRIVES");
    static final TextKey CARD_ARRAY = TextKey.of("jsc.rack.card_array", "ARRAY");
    static final TextKey NOTHING = TextKey.of("jsc.rack.nothing", "nothing");
    static final TextKey BAYS_PLUS = TextKey.of("jsc.rack.bays_plus", "%s + %s");
    static final TextKey OF_TOTAL = TextKey.of("jsc.rack.of_total", "%s / %s");
    static final TextKey WITH_UNIT = TextKey.of("jsc.rack.with_unit", "with %sU");
    static final TextKey SHELF_ALONE = TextKey.of("jsc.rack.shelf_alone", "No server above this shelf");
    static final TextKey SHELF_MOUNT = TextKey.of("jsc.rack.shelf_mount", "Mount it right under a server");
    // A Tape Library's card, its drive and its slots.
    static final TextKey CARD_TAPES = TextKey.of("jsc.rack.card_tapes", "TAPES");
    static final TextKey CARD_USED = TextKey.of("jsc.rack.card_used", "USED");
    static final TextKey CARD_BACKUPS = TextKey.of("jsc.rack.card_backups", "BACKUPS");
    static final TextKey CARD_DRIVE = TextKey.of("jsc.rack.card_drive", "DRIVE");
    static final TextKey CARD_ITEMS = TextKey.of("jsc.rack.card_items", "ITEMS");
    static final TextKey TAPE_DRIVE = TextKey.of("jsc.rack.tape_drive", "RD");
    static final TextKey TAPE_SLOT = TextKey.of("jsc.rack.tape_slot", "Takes a tape of the library's era");
    static final TextKey DRIVE_IDLE = TextKey.of("jsc.rack.drive_idle", "idle");
    static final TextKey DRIVE_WINDING = TextKey.of("jsc.rack.drive_winding", "winding");
    static final TextKey DRIVE_READING = TextKey.of("jsc.rack.drive_reading", "reading");
    static final TextKey DRIVE_WRITING = TextKey.of("jsc.rack.drive_writing", "writing");

    private ServerRackTexts() {
    }

    /** What a tape drive is doing, in the card's word for it. */
    static TextKey driveWord(final TapeDrive.State state) {
        return switch (state) {
            case IDLE -> DRIVE_IDLE;
            case WINDING -> DRIVE_WINDING;
            case READING -> DRIVE_READING;
            case WRITING -> DRIVE_WRITING;
        };
    }
}
