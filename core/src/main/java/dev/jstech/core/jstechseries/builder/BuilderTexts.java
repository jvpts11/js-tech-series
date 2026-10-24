/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.builder;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The words of the Multiblock Builder: its screen, its tabs and keys, and what it tells as it builds. */
@TextHolder
public final class BuilderTexts {

    public static final TextKey TITLE = TextKey.of("jscore.builder.title", "MULTIBLOCK BUILDER");
    public static final TextKey AIMED = TextKey.of("jscore.builder.aimed", "AIMED");
    public static final TextKey HEADER = TextKey.of("jscore.builder.header", "%s x %s x %s, FACING %s");
    public static final TextKey NORTH = TextKey.of("jscore.builder.north", "NORTH");
    public static final TextKey EAST = TextKey.of("jscore.builder.east", "EAST");
    public static final TextKey SOUTH = TextKey.of("jscore.builder.south", "SOUTH");
    public static final TextKey WEST = TextKey.of("jscore.builder.west", "WEST");
    public static final TextKey TO_PLACE = TextKey.of("jscore.builder.to_place", "TO PLACE");
    public static final TextKey COUNTS = TextKey.of("jscore.builder.counts", "%s blocks, %s already there");
    public static final TextKey NEEDS = TextKey.of("jscore.builder.needs", "%s");
    public static final TextKey HAVE = TextKey.of("jscore.builder.have", "you have %s");
    public static final TextKey HAVE_SHORT = TextKey.of("jscore.builder.have_short", "have %s");
    public static final TextKey ONE_PLACE_EITHER = TextKey.of("jscore.builder.one_place_either",
            "1 PLACE TAKES EITHER");
    public static final TextKey PLACES_EITHER = TextKey.of("jscore.builder.places_either", "%s PLACES TAKE EITHER");
    public static final TextKey SOURCE = TextKey.of("jscore.builder.source", "SOURCE");
    public static final TextKey INVENTORY = TextKey.of("jscore.builder.inventory", "Your inventory");
    public static final TextKey CREATIVE = TextKey.of("jscore.builder.creative", "Creative: nothing is taken");
    public static final TextKey READY = TextKey.of("jscore.builder.ready", "READY");
    public static final TextKey READY_LINE = TextKey.of("jscore.builder.ready_line", "OK builds it, block by block");
    public static final TextKey LACKING = TextKey.of("jscore.builder.lacking", "LACKING");
    public static final TextKey LACKING_LINE = TextKey.of("jscore.builder.lacking_line", "%s more %s");
    public static final TextKey BUILDING = TextKey.of("jscore.builder.building", "BUILDING");
    public static final TextKey BUILDING_LINE = TextKey.of("jscore.builder.building_line", "%s of %s placed");
    public static final TextKey HELD = TextKey.of("jscore.builder.held", "HELD");
    public static final TextKey HELD_LINE = TextKey.of("jscore.builder.held_line", "HOLD carries on");
    public static final TextKey STOPPED = TextKey.of("jscore.builder.stopped", "STOPPED");
    public static final TextKey BUILT = TextKey.of("jscore.builder.built", "BUILT");
    public static final TextKey BUILT_LINE = TextKey.of("jscore.builder.built_line", "Nothing is missing");
    public static final TextKey NOT_AIMED = TextKey.of("jscore.builder.not_aimed",
            "Use it on a multiblock's terminal");
    public static final TextKey UNKNOWN = TextKey.of("jscore.builder.unknown",
            "This terminal forms no multiblock it knows");
    public static final TextKey CHOOSE = TextKey.of("jscore.builder.choose", "CHOOSE WHAT GOES WHERE");
    public static final TextKey NO_CHOICES = TextKey.of("jscore.builder.no_choices",
            "Every place takes one block only");
    public static final TextKey CHOICE_PLACES = TextKey.of("jscore.builder.choice_places", "%s places:");
    public static final TextKey CHOICE_ONE_PLACE = TextKey.of("jscore.builder.choice_one_place", "1 place:");
    public static final TextKey PICKED = TextKey.of("jscore.builder.picked", "< %s >");
    public static final TextKey SOURCES = TextKey.of("jscore.builder.sources", "TAKE THE BLOCKS FROM");
    public static final TextKey TAB_BUILD = TextKey.of("jscore.builder.tab_build", "BUILD");
    public static final TextKey TAB_CHOICES = TextKey.of("jscore.builder.tab_choices", "CHOICES");
    public static final TextKey TAB_SOURCE = TextKey.of("jscore.builder.tab_source", "SOURCE");
    public static final TextKey TAB_BACK = TextKey.of("jscore.builder.tab_back", "BACK");
    public static final TextKey OK = TextKey.of("jscore.builder.ok", "OK");
    public static final TextKey HOLD = TextKey.of("jscore.builder.hold", "HOLD");
    public static final TextKey SHOW = TextKey.of("jscore.builder.show", "SHOW");
    public static final TextKey LIGHT = TextKey.of("jscore.builder.light", "LIGHT");
    public static final TextKey POWER = TextKey.of("jscore.builder.power", "POWER");
    public static final TextKey PROTECTED = TextKey.of("jscore.builder.protected",
            "Stopped: a place is protected from you");
    public static final TextKey TOO_FAR = TextKey.of("jscore.builder.too_far", "Stopped: you walked too far away");
    public static final TextKey RAN_OUT = TextKey.of("jscore.builder.ran_out", "Stopped: you ran out of %s");
    public static final TextKey BLOCKED = TextKey.of("jscore.builder.blocked",
            "Stopped: something stands where a block goes");
    public static final TextKey WRONG_LEFT = TextKey.of("jscore.builder.wrong_left",
            "Done, but other blocks stand in its places: break them");
    public static final TextKey NOT_ALLOWED = TextKey.of("jscore.builder.not_allowed",
            "You may not build multiblocks here");

    private BuilderTexts() {
    }
}
