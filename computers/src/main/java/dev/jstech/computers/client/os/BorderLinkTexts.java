/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.block.DataLinkNames;
import dev.jstech.computers.operation.payload.wan.BorderLinkStatePayload;
import dev.jstech.computers.wan.GatewayLinks;
import dev.jstech.computers.wan.WanMode;
import dev.jstech.computers.wan.WanRight;
import dev.jstech.core.jstechseries.network.DataLink;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * The words of Nouvell BorderLink: its window on the Frames desktops and its operating space on MC-NET, which say the
 * same things of the same ports.
 */
@TextHolder
public final class BorderLinkTexts {

    /* The window. */
    public static final TextKey HEADER = TextKey.of("jsc.borderlink.header", "%s · %s ports · %s %s");
    public static final TextKey ASKING = TextKey.of("jsc.borderlink.asking", "Asking the gateway ...");
    public static final TextKey COLUMN_PORT = TextKey.of("jsc.borderlink.column.port", "#");
    public static final TextKey COLUMN_LINE = TextKey.of("jsc.borderlink.column.line", "LINE");
    public static final TextKey COLUMN_FAR = TextKey.of("jsc.borderlink.column.far", "FAR END");
    public static final TextKey COLUMN_MODE = TextKey.of("jsc.borderlink.column.mode", "MODE");
    public static final TextKey COLUMN_STATE = TextKey.of("jsc.borderlink.column.state", "STATE");
    public static final TextKey NO_CARD = TextKey.of("jsc.borderlink.no_card", "no card");
    public static final TextKey UP = TextKey.of("jsc.borderlink.up", "UP");
    public static final TextKey DOWN = TextKey.of("jsc.borderlink.down", "DOWN");
    public static final TextKey PORT_FAR = TextKey.of("jsc.borderlink.port_far", "Port %s: %s");
    public static final TextKey PORT_NO_CARD = TextKey.of("jsc.borderlink.port_no_card", "Port %s: no card");
    public static final TextKey PORT_NOTHING = TextKey.of("jsc.borderlink.port_nothing",
            "Port %s: nothing at the far end");
    public static final TextKey INFO = TextKey.of("jsc.borderlink.info", "%s, %s it/t · %s");
    public static final TextKey FAR_NETWORK = TextKey.of("jsc.borderlink.far_network", "network %s");
    public static final TextKey THIS_NETWORK = TextKey.of("jsc.borderlink.this_network", "part of this network");
    public static final TextKey LINE_IS_DOWN = TextKey.of("jsc.borderlink.line_is_down", "line down");
    public static final TextKey PUT_A_CARD = TextKey.of("jsc.borderlink.put_a_card",
            "Put a WAN Interface Card in this port.");
    public static final TextKey WHAT_MAY = TextKey.of("jsc.borderlink.what_may", "What %s may do here:");
    public static final TextKey THE_FAR_SIDE = TextKey.of("jsc.borderlink.the_far_side", "the far side");
    public static final TextKey SEE = TextKey.of("jsc.borderlink.see", "See our items");
    public static final TextKey TAKE = TextKey.of("jsc.borderlink.take", "Take items");
    public static final TextKey SEND = TextKey.of("jsc.borderlink.send", "Send items");
    public static final TextKey CRAFT = TextKey.of("jsc.borderlink.craft", "Craft here");
    public static final TextKey RELAY = TextKey.of("jsc.borderlink.relay", "Relay for other networks");
    public static final TextKey PORT_ON = TextKey.of("jsc.borderlink.port_on", "Port on");
    public static final TextKey OK = TextKey.of("jsc.borderlink.ok", "OK");
    public static final TextKey CANCEL = TextKey.of("jsc.borderlink.cancel", "Cancel");
    public static final TextKey APPLY = TextKey.of("jsc.borderlink.apply", "Apply");
    public static final TextKey LINES_UP = TextKey.of("jsc.borderlink.lines_up", "%s of %s lines up");

    /* The operating space on MC-NET. */
    public static final TextKey PORTS = TextKey.of("jsc.borderlink.space.ports", "PORTS");
    public static final TextKey SLOTS_CARDS = TextKey.of("jsc.borderlink.space.slots_cards", "%s SLOTS, %s CARDS");
    public static final TextKey SLOTS_ONE_CARD = TextKey.of("jsc.borderlink.space.slots_one_card",
            "%s SLOTS, 1 CARD");
    public static final TextKey COLUMN_PORT_WORD = TextKey.of("jsc.borderlink.space.column.port", "PORT");
    public static final TextKey PORT_HEADING = TextKey.of("jsc.borderlink.space.port", "PORT %s");
    public static final TextKey NOTHING_THERE = TextKey.of("jsc.borderlink.space.nothing_there",
            "nothing at the far end");
    public static final TextKey WHAT_MAY_HEADING = TextKey.of("jsc.borderlink.space.what_may",
            "WHAT %s MAY DO HERE");
    public static final TextKey THE_FAR_SIDE_HEADING = TextKey.of("jsc.borderlink.space.the_far_side",
            "THE FAR SIDE");
    public static final TextKey HINT = TextKey.of("jsc.borderlink.space.hint", "At the prompt: borderlink port %s %s");
    public static final TextKey LOG = TextKey.of("jsc.borderlink.space.log", "LOG");
    public static final TextKey LOG_WHOSE = TextKey.of("jsc.borderlink.space.log_whose", "%s'S OWN LINES");
    public static final TextKey NOTHING_LOGGED = TextKey.of("jsc.borderlink.space.nothing_logged",
            "Nothing logged yet: a line coming up or going down is.");
    public static final TextKey DAY = TextKey.of("jsc.borderlink.space.day", "Day %s");

    private BorderLinkTexts() {
    }

    /** The line in a port, by the name the mod gives every data link; its own name when it names none. */
    public static String lineName(final BorderLinkStatePayload.Port port) {
        final DataLink link = port.link();
        return link == null ? port.line() : GameText.resolve(DataLinkNames.of(link));
    }

    /** What the far end of a port's line is to this network: another network, a place of this one, or down. */
    public static Text where(final BorderLinkStatePayload.Port port) {
        if (!port.up()) {
            return LINE_IS_DOWN.text();
        }
        return port.wanMode() == WanMode.FEDERATION ? FAR_NETWORK.with(port.network()) : THIS_NETWORK.text();
    }

    /** What a port's switch for {@code right} is called. */
    public static TextKey rightLabel(final WanRight right) {
        return switch (right) {
            case SEE -> SEE;
            case TAKE -> TAKE;
            case SEND -> SEND;
            case CRAFT -> CRAFT;
        };
    }

    /** The name of a port's mode, empty for none. */
    public static String modeName(final BorderLinkStatePayload.Port port) {
        final WanMode mode = port.wanMode();
        return mode == WanMode.NONE ? "" : GameText.resolve(GatewayLinks.modeName(mode));
    }
}
