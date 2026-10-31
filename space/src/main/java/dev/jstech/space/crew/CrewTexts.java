/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.crew;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What a starship tells a player of who may do what aboard, and what its console's crew page shows. */
@TextHolder
public final class CrewTexts {

    public static final TextKey NOT_ABOARD = TextKey.of("jsspace.crew.not_aboard",
            "This ship is %s's: you are not of its crew");
    public static final TextKey PILOTS_ONLY = TextKey.of("jsspace.crew.pilots_only",
            "Only this ship's pilots may do that");
    public static final TextKey CREW_ONLY = TextKey.of("jsspace.crew.crew_only", "Only this ship's crew works it");
    public static final TextKey OWNER_ONLY = TextKey.of("jsspace.crew.owner_only",
            "Only this ship's owner sets its roles");
    public static final TextKey UNKNOWN_PLAYER = TextKey.of("jsspace.crew.unknown_player",
            "No player of that name has played here");
    /* The console's crew page. */
    public static final TextKey MAKER = TextKey.of("jsspace.ship_console.maker", "SCC-1  SHIP CONSOLE");
    public static final TextKey PAGE_CREW = TextKey.of("jsspace.ship_console.page.crew", "CREW");
    public static final TextKey PREVIOUS = TextKey.of("jsspace.ship_console.previous", "PREV");
    public static final TextKey NEXT = TextKey.of("jsspace.ship_console.next", "NEXT");
    public static final TextKey SHIP = TextKey.of("jsspace.ship_console.ship", "SHIP");
    public static final TextKey SEATS = TextKey.of("jsspace.ship_console.seats", "SEATS %s  BUNKS %s");
    public static final TextKey OWNER = TextKey.of("jsspace.ship_console.owner", "OWNER");
    public static final TextKey TEAM = TextKey.of("jsspace.ship_console.team", "TEAM");
    public static final TextKey OWNERS_TEAM = TextKey.of("jsspace.ship_console.owners_team", "OWNER'S TEAM");
    public static final TextKey OTHERS = TextKey.of("jsspace.ship_console.others", "OTHERS");
    public static final TextKey ANYONE_ELSE = TextKey.of("jsspace.ship_console.anyone_else", "ANYONE ELSE");
    public static final TextKey NAMED = TextKey.of("jsspace.ship_console.named", "NAMED");
    public static final TextKey PAGE = TextKey.of("jsspace.ship_console.page", "PAGE %s/%s");
    public static final TextKey FLIES = TextKey.of("jsspace.ship_console.flies", "FLIES");
    public static final TextKey WORKS = TextKey.of("jsspace.ship_console.works", "WORKS ABOARD");
    public static final TextKey RIDES = TextKey.of("jsspace.ship_console.rides", "RIDES");
    public static final TextKey ADD = TextKey.of("jsspace.ship_console.add", "ADD");
    public static final TextKey TYPE_NAME = TextKey.of("jsspace.ship_console.type_name", "TYPE A NAME, ADD");
    public static final TextKey YOU = TextKey.of("jsspace.ship_console.you", "YOU: %s");
    public static final TextKey OWNER_SETS = TextKey.of("jsspace.ship_console.owner_sets",
            "ONLY THE OWNER SETS ROLES");
    public static final TextKey TITLE = TextKey.of("jsspace.ship_console.title", "Ship Console");

    private CrewTexts() {
    }
}
