/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The words of a multiblock shown in a recipe viewer: its layers, what may stand where, its materials. */
@TextHolder
public final class MultiblockViewTexts {

    public static final TextKey CATEGORY = TextKey.of("jscore.multiblock_view.category", "Multiblocks");
    public static final TextKey LAYER = TextKey.of("jscore.multiblock_view.layer", "Layer");
    public static final TextKey ALL = TextKey.of("jscore.multiblock_view.all", "All");
    public static final TextKey PREVIOUS = TextKey.of("jscore.multiblock_view.previous", "<");
    public static final TextKey NEXT = TextKey.of("jscore.multiblock_view.next", ">");
    public static final TextKey NONE = TextKey.of("jscore.multiblock_view.none", "-");
    public static final TextKey DRAG = TextKey.of("jscore.multiblock_view.drag", "drag to turn");
    public static final TextKey CLICK = TextKey.of("jscore.multiblock_view.click", "click to turn");
    public static final TextKey ONE_PLACE = TextKey.of("jscore.multiblock_view.one_place", "1 place,");
    public static final TextKey PLACES = TextKey.of("jscore.multiblock_view.places", "%s places,");
    public static final TextKey EITHER = TextKey.of("jscore.multiblock_view.either", "either:");
    public static final TextKey TURNS = TextKey.of("jscore.multiblock_view.turns", "Turns,");
    public static final TextKey TURNS_AND = TextKey.of("jscore.multiblock_view.turns_and", "Turns and");
    public static final TextKey NO_MIRROR = TextKey.of("jscore.multiblock_view.no_mirror", "no mirror");
    public static final TextKey MIRRORS = TextKey.of("jscore.multiblock_view.mirrors", "mirrors");
    public static final TextKey MIRRORS_AND = TextKey.of("jscore.multiblock_view.mirrors_and", "mirrors,");
    public static final TextKey LIES_DOWN = TextKey.of("jscore.multiblock_view.lies_down", "lies down");
    public static final TextKey MATERIALS = TextKey.of("jscore.multiblock_view.materials", "Materials");
    public static final TextKey BLOCKS = TextKey.of("jscore.multiblock_view.blocks", "%s blocks");
    public static final TextKey SIZE = TextKey.of("jscore.multiblock_view.size", "%s x %s x %s");
    public static final TextKey CONTROLLER = TextKey.of("jscore.multiblock_view.controller", "The controller");
    public static final TextKey SHOW = TextKey.of("jscore.multiblock_view.show", "Show in world");
    public static final TextKey HIDE = TextKey.of("jscore.multiblock_view.hide", "Hide in world");
    public static final TextKey PAGE = TextKey.of("jscore.multiblock_view.page", "Open the page");
    public static final TextKey NO_PAGE = TextKey.of("jscore.multiblock_view.no_page",
            "No manual has a page for it");
    public static final TextKey SHOW_HOW = TextKey.of("jscore.multiblock_view.show_how",
            "Shows its ghosts in front of you until it is built or you walk away");

    private MultiblockViewTexts() {
    }
}
