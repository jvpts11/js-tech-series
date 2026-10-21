/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.inspect;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the inspection of a place says, in chat, on the debug screen and in the studio. */
@TextHolder
public final class InspectTexts {

    public static final TextKey FAILED = TextKey.of("jscore.inspect.failed", "Could not be read");
    /* Where, and in which dimension. */
    public static final TextKey HEADER = TextKey.of("jscore.inspect.header", "What is at %s in %s:");
    public static final TextKey NOTHING = TextKey.of("jscore.inspect.nothing", "Nothing to say of it");
    public static final TextKey NO_TARGET = TextKey.of("jscore.inspect.no_target",
            "Look at a block, or name a place");
    /* Where, and in which dimension. */
    public static final TextKey WATCHING = TextKey.of("jscore.inspect.watching",
            "Watching %s in %s; the debug screen (F3) shows it as it changes");
    public static final TextKey STOPPED = TextKey.of("jscore.inspect.stopped", "No longer watching");
    public static final TextKey PANEL = TextKey.of("jscore.inspect.panel", "Inspected");
    public static final TextKey PAYLOADS_PANEL = TextKey.of("jscore.inspect.payloads_panel", "Sent this tick");
    public static final TextKey BLOCK = TextKey.of("jscore.inspect.block", "Block");
    public static final TextKey STATE = TextKey.of("jscore.inspect.state", "State");
    public static final TextKey BLOCK_ENTITY = TextKey.of("jscore.inspect.block_entity", "Block entity");
    public static final TextKey OWNER = TextKey.of("jscore.inspect.owner", "Owner");
    public static final TextKey FIELDS = TextKey.of("jscore.inspect.fields", "Fields");
    /* The face, or all of them. */
    public static final TextKey OFFERS = TextKey.of("jscore.inspect.offers", "Offers on %s");
    public static final TextKey ALL_FACES = TextKey.of("jscore.inspect.all_faces", "every face");
    public static final TextKey ITEMS = TextKey.of("jscore.inspect.items", "Items");
    public static final TextKey ENERGY = TextKey.of("jscore.inspect.energy", "Energy");
    public static final TextKey FLUIDS = TextKey.of("jscore.inspect.fluids", "Fluids");
    public static final TextKey HEAT = TextKey.of("jscore.inspect.heat", "Heat");
    public static final TextKey SHAFT = TextKey.of("jscore.inspect.shaft", "Shaft");
    public static final TextKey NETWORK = TextKey.of("jscore.inspect.network", "Data network");
    public static final TextKey NODE = TextKey.of("jscore.inspect.node", "Node");
    public static final TextKey MEMBERS = TextKey.of("jscore.inspect.members", "Members");
    public static final TextKey OFF_NETWORK = TextKey.of("jscore.inspect.off_network", "On no network");
    /* The kind of grid. */
    public static final TextKey GRID = TextKey.of("jscore.inspect.grid", "Grid %s");
    public static final TextKey LANE = TextKey.of("jscore.inspect.lane", "Lane");
    public static final TextKey CONNECTED = TextKey.of("jscore.inspect.connected", "Places joined");
    public static final TextKey ROOM = TextKey.of("jscore.inspect.room", "Sealed room");
    public static final TextKey VOLUME = TextKey.of("jscore.inspect.volume", "Volume");
    public static final TextKey PRESSURE = TextKey.of("jscore.inspect.pressure", "Pressure");
    public static final TextKey LEAKS = TextKey.of("jscore.inspect.leaks", "Leaks");
    public static final TextKey MULTIBLOCK = TextKey.of("jscore.inspect.multiblock", "Part of a multiblock");
    public static final TextKey CONTROLLER = TextKey.of("jscore.inspect.controller", "Controller");
    /* Which tick, how many packets and how many bytes. */
    public static final TextKey PAYLOADS_TICK = TextKey.of("jscore.inspect.payloads_tick",
            "Tick %s: %s packets, %s bytes");
    /* The kind of packet, how many, and how many bytes. */
    public static final TextKey PAYLOADS_KIND = TextKey.of("jscore.inspect.payloads_kind", "%s: %s, %s bytes");
    public static final TextKey PAYLOADS_BUSIEST = TextKey.of("jscore.inspect.payloads_busiest",
            "The busiest of the last ticks:");
    public static final TextKey PAYLOADS_NONE = TextKey.of("jscore.inspect.payloads_none",
            "Nothing was sent in the last ticks, or nothing is counted");

    private InspectTexts() {
    }
}
