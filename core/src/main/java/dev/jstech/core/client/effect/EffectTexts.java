/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.effect;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;

/** The words of the effects editor and of the card of a new effect. */
@TextHolder
public final class EffectTexts {

    public static final TextKey EFFECT = TextKey.of("gui.jscore.effects.effect", "Effect: %s");
    public static final TextKey PARTICLES = TextKey.of("gui.jscore.effects.particles",
            "Particles: %s of %s, the budget");
    public static final TextKey COST = TextKey.of("gui.jscore.effects.cost", "Cost: %s ms a frame");
    public static final TextKey WIND = TextKey.of("gui.jscore.effects.wind", "Wind: %s m/s from the %s, the world's");
    public static final TextKey NO_WIND = TextKey.of("gui.jscore.effects.no_wind", "Wind: none");
    public static final TextKey AIR = TextKey.of("gui.jscore.effects.air", "Air: %s kg/m3   Gravity: %s m/s2");
    public static final TextKey EMITTERS = TextKey.of("gui.jscore.effects.emitters", "Emitters");
    public static final TextKey RATE = TextKey.of("gui.jscore.effects.rate", "Rate: %s a second");
    public static final TextKey LIFE = TextKey.of("gui.jscore.effects.life", "Life: %s s");
    public static final TextKey LEAVES_AT = TextKey.of("gui.jscore.effects.leaves_at", "Leaves at: %s m/s");
    public static final TextKey SIZE = TextKey.of("gui.jscore.effects.size", "Size: %s to %s m");
    public static final TextKey WIDTH = TextKey.of("gui.jscore.effects.width", "Width: %s m");
    public static final TextKey LASTS = TextKey.of("gui.jscore.effects.lasts", "Lasts: %s s");
    public static final TextKey REACH = TextKey.of("gui.jscore.effects.reach", "Reach: %s m");
    public static final TextKey STRENGTH = TextKey.of("gui.jscore.effects.strength", "Strength: %s");
    public static final TextKey MOVED_BY = TextKey.of("gui.jscore.effects.moved_by", "Moved by");
    public static final TextKey GRAVITY = TextKey.of("gui.jscore.effects.gravity", "Gravity");
    public static final TextKey THE_AIR = TextKey.of("gui.jscore.effects.the_air", "The air");
    public static final TextKey THE_WIND = TextKey.of("gui.jscore.effects.the_wind", "The wind");
    public static final TextKey SAVE = TextKey.of("gui.jscore.effects.save", "Save");
    public static final TextKey DONE = TextKey.of("gui.jscore.effects.done", "Done");
    public static final TextKey SAVED = TextKey.of("gui.jscore.effects.saved", "Saved to %s");
    public static final TextKey NOT_SAVED = TextKey.of("gui.jscore.effects.not_saved", "Not saved: %s");
    public static final TextKey NO_FOLDER = TextKey.of("gui.jscore.effects.no_folder",
            "the mod %s is not loaded from its folders in this run");
    public static final TextKey NO_EFFECT = TextKey.of("gui.jscore.effects.no_effect",
            "No file holds the effect %s");
    public static final TextKey NEW_EFFECT = TextKey.of("gui.jscore.effects.new", "New effect");
    public static final TextKey AT_THE_BLOCK = TextKey.of("gui.jscore.effects.at_the_block",
            "at the block you look at");
    public static final TextKey BLOCK = TextKey.of("gui.jscore.effects.block", "Block");
    public static final TextKey FROM = TextKey.of("gui.jscore.effects.from", "From: %s");
    public static final TextKey FILE = TextKey.of("gui.jscore.effects.file", "File, in effects/");
    public static final TextKey START_FROM = TextKey.of("gui.jscore.effects.start_from", "Start from");
    public static final TextKey CREATE = TextKey.of("gui.jscore.effects.create", "Create");
    public static final TextKey PREVIEW = TextKey.of("gui.jscore.effects.preview",
            "New effect: a preview of %s, as it starts");
    public static final TextKey HERE = TextKey.of("gui.jscore.effects.here", "here: %s");
    public static final TextKey EXISTS = TextKey.of("gui.jscore.effects.exists", "That file is there already");
    public static final TextKey LOCATOR = TextKey.of("gui.jscore.effects.locator", "its locator %s");
    public static final TextKey TOP = TextKey.of("gui.jscore.effects.face.up", "its top face");
    public static final TextKey BOTTOM = TextKey.of("gui.jscore.effects.face.down", "its bottom face");
    public static final TextKey NORTH_FACE = TextKey.of("gui.jscore.effects.face.north", "its north face");
    public static final TextKey SOUTH_FACE = TextKey.of("gui.jscore.effects.face.south", "its south face");
    public static final TextKey WEST_FACE = TextKey.of("gui.jscore.effects.face.west", "its west face");
    public static final TextKey EAST_FACE = TextKey.of("gui.jscore.effects.face.east", "its east face");
    public static final TextKey THE_TOP = TextKey.of("gui.jscore.effects.the_face.up", "the top face");
    public static final TextKey THE_BOTTOM = TextKey.of("gui.jscore.effects.the_face.down", "the bottom face");
    public static final TextKey THE_NORTH = TextKey.of("gui.jscore.effects.the_face.north", "the north face");
    public static final TextKey THE_SOUTH = TextKey.of("gui.jscore.effects.the_face.south", "the south face");
    public static final TextKey THE_WEST = TextKey.of("gui.jscore.effects.the_face.west", "the west face");
    public static final TextKey THE_EAST = TextKey.of("gui.jscore.effects.the_face.east", "the east face");
    public static final TextKey THE_LOCATOR = TextKey.of("gui.jscore.effects.the_locator", "the locator %s");
    public static final TextKey SMOKE = TextKey.of("gui.jscore.effects.start.smoke", "Smoke");
    public static final TextKey STEAM = TextKey.of("gui.jscore.effects.start.steam", "Steam");
    public static final TextKey SPARKS = TextKey.of("gui.jscore.effects.start.sparks", "Sparks");
    public static final TextKey A_BEAM = TextKey.of("gui.jscore.effects.start.beam", "A beam");
    public static final TextKey A_TRAIL = TextKey.of("gui.jscore.effects.start.trail", "A trail");
    public static final TextKey NOTHING = TextKey.of("gui.jscore.effects.start.nothing", "Nothing");
    public static final TextKey NORTH = TextKey.of("gui.jscore.effects.compass.north", "north");
    public static final TextKey NORTH_EAST = TextKey.of("gui.jscore.effects.compass.north_east", "north-east");
    public static final TextKey EAST = TextKey.of("gui.jscore.effects.compass.east", "east");
    public static final TextKey SOUTH_EAST = TextKey.of("gui.jscore.effects.compass.south_east", "south-east");
    public static final TextKey SOUTH = TextKey.of("gui.jscore.effects.compass.south", "south");
    public static final TextKey SOUTH_WEST = TextKey.of("gui.jscore.effects.compass.south_west", "south-west");
    public static final TextKey WEST = TextKey.of("gui.jscore.effects.compass.west", "west");
    public static final TextKey NORTH_WEST = TextKey.of("gui.jscore.effects.compass.north_west", "north-west");
    /** The compass points, from north round by east. */
    public static final List<TextKey> COMPASS = List.of(NORTH, NORTH_EAST, EAST, SOUTH_EAST, SOUTH, SOUTH_WEST, WEST,
            NORTH_WEST);
    /** The faces, in the order of the game's directions: down, up, north, south, west, east. */
    public static final List<TextKey> FACES = List.of(BOTTOM, TOP, NORTH_FACE, SOUTH_FACE, WEST_FACE, EAST_FACE);
    /** The faces as the place marked in the world, in the same order. */
    public static final List<TextKey> THE_FACES = List.of(THE_BOTTOM, THE_TOP, THE_NORTH, THE_SOUTH, THE_WEST,
            THE_EAST);

    private EffectTexts() {
    }
}
