/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the studio's Access page says. */
@TextHolder
public final class AccessTexts {

    public static final TextKey HEAD = TextKey.of("gui.jscore.studio.access.head",
            "Who may do what at the place you look at");
    public static final TextKey LOOK_AGAIN = TextKey.of("gui.jscore.studio.access.look_again", "Look again");
    public static final TextKey QUESTION = TextKey.of("gui.jscore.studio.access.question",
            "The same question every mod asks");
    public static final TextKey ANSWERED = TextKey.of("gui.jscore.studio.access.answered", "Who answered");
    public static final TextKey YOU = TextKey.of("gui.jscore.studio.access.you", "You");
    public static final TextKey YOUR_TEAM = TextKey.of("gui.jscore.studio.access.your_team", "Your team");
    /** A player's machines, by the player's name. */
    public static final TextKey MACHINES = TextKey.of("gui.jscore.studio.access.machines", "%s's machines");
    /** The player whose machines are weighed. */
    public static final TextKey WEIGHED_AS = TextKey.of("gui.jscore.studio.access.weighed_as",
            "%1$s's machines are weighed as %1$s, online or not.");
    public static final TextKey PICK = TextKey.of("gui.jscore.studio.access.pick", "Pick who to weigh:");
    public static final TextKey NOBODY = TextKey.of("gui.jscore.studio.access.nobody",
            "Name a player to weigh them and their machines.");
    public static final TextKey BREAK = TextKey.of("gui.jscore.studio.access.break", "Break");
    public static final TextKey PLACE = TextKey.of("gui.jscore.studio.access.place", "Place");
    public static final TextKey USE = TextKey.of("gui.jscore.studio.access.use", "Use");
    public static final TextKey EXPLODE = TextKey.of("gui.jscore.studio.access.explode", "Explode");
    public static final TextKey BURN = TextKey.of("gui.jscore.studio.access.burn", "Burn");
    public static final TextKey FLOW = TextKey.of("gui.jscore.studio.access.flow", "Flow into");
    public static final TextKey MOVE = TextKey.of("gui.jscore.studio.access.move", "Move a structure into");
    public static final TextKey SHOOT = TextKey.of("gui.jscore.studio.access.shoot", "Shoot");
    /** Who a protection lets break the place. */
    public static final TextKey ALLOWS = TextKey.of("gui.jscore.studio.access.allows", "lets %s break here");
    public static final TextKey STOPS = TextKey.of("gui.jscore.studio.access.stops", "stops %s breaking here");
    public static final TextKey SAYS_NOTHING = TextKey.of("gui.jscore.studio.access.says_nothing",
            "says nothing of here");
    public static final TextKey CLAIM = TextKey.of("gui.jscore.studio.access.claim", "The claim");
    /** Who claimed the place. */
    public static final TextKey CLAIMED_BY = TextKey.of("gui.jscore.studio.access.claimed_by", "claimed by %s");
    public static final TextKey NO_CLAIM = TextKey.of("gui.jscore.studio.access.no_claim", "in no claim");
    public static final TextKey PVP = TextKey.of("gui.jscore.studio.access.pvp", "The server's PvP rule");
    /** Who may hurt the player there. */
    public static final TextKey PVP_HURTS = TextKey.of("gui.jscore.studio.access.pvp_hurts",
            "on: %s may hurt you here");
    public static final TextKey PVP_SPARES = TextKey.of("gui.jscore.studio.access.pvp_spares",
            "on: %s may not hurt you here");
    public static final TextKey PVP_ON = TextKey.of("gui.jscore.studio.access.pvp_on", "on");
    public static final TextKey PVP_OFF = TextKey.of("gui.jscore.studio.access.pvp_off", "off: nobody hurts anybody");
    public static final TextKey NONE_REGISTERED = TextKey.of("gui.jscore.studio.access.none_registered",
            "No protection is installed: the game's own rules answer");

    private AccessTexts() {
    }
}
