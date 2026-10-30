/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.hangar;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;

/** What the Rocket Design screen writes on its plates, its buttons and its spec sheet. */
@TextHolder
public final class RocketDesignTexts {

    public static final TextKey HANGAR = TextKey.of("jsspace.design.screen.hangar", "Rocket Hangar %s");
    public static final TextKey PARTS_BIN = TextKey.of("jsspace.design.screen.parts_bin", "Parts Bin");
    public static final TextKey STAGES = TextKey.of("jsspace.design.screen.stages", "Stages");
    public static final TextKey COMMAND = TextKey.of("jsspace.design.screen.command", "Command");
    public static final TextKey TANKS = TextKey.of("jsspace.design.screen.tanks", "Tanks");
    public static final TextKey ENGINES = TextKey.of("jsspace.design.screen.engines", "Engines");
    public static final TextKey RINGS = TextKey.of("jsspace.design.screen.rings", "Rings");
    public static final TextKey LANDERS = TextKey.of("jsspace.design.screen.landers", "Landers");
    public static final TextKey RECOVERY = TextKey.of("jsspace.design.screen.recovery", "Recovery");
    public static final TextKey DIAMETER = TextKey.of("jsspace.design.screen.diameter", "D%s");
    public static final TextKey PLACE = TextKey.of("jsspace.design.screen.place", "Place");
    public static final TextKey OFFSET = TextKey.of("jsspace.design.screen.offset", "Offset");
    public static final TextKey ROTATE = TextKey.of("jsspace.design.screen.rotate", "Rotate");
    public static final TextKey ROOT = TextKey.of("jsspace.design.screen.root", "Root");
    public static final TextKey MIRROR = TextKey.of("jsspace.design.screen.mirror", "Mirror x%s");
    public static final TextKey SNAP = TextKey.of("jsspace.design.screen.snap", "Snap");
    public static final TextKey CRANE_READY = TextKey.of("jsspace.design.screen.crane_ready", "Crane ready");
    public static final TextKey COUNTS = TextKey.of("jsspace.design.screen.counts", "%s stages %s blk");
    public static final TextKey COUNT_ONE = TextKey.of("jsspace.design.screen.count_one", "1 stage %s blk");
    public static final TextKey UNNAMED = TextKey.of("jsspace.design.screen.unnamed", "Rocket %s");
    public static final TextKey CONFIRM = TextKey.of("jsspace.design.screen.confirm", "Confirm and build");
    public static final TextKey SAVE = TextKey.of("jsspace.design.screen.save", "Save");
    public static final TextKey PLANS = TextKey.of("jsspace.design.screen.plans", "Plans");
    public static final TextKey CLEAR = TextKey.of("jsspace.design.screen.clear", "Clear");
    public static final TextKey CONFIRM_KEY = TextKey.of("jsspace.design.screen.confirm_key", "E");
    public static final TextKey SAVE_KEY = TextKey.of("jsspace.design.screen.save_key", "S");
    public static final TextKey PLANS_KEY = TextKey.of("jsspace.design.screen.plans_key", "L");
    public static final TextKey CLEAR_KEY = TextKey.of("jsspace.design.screen.clear_key", "X");
    public static final TextKey HINT_PLACE = TextKey.of("jsspace.design.screen.hint_place", "Drag to a node");
    public static final TextKey HINT_OFFSET = TextKey.of("jsspace.design.screen.hint_offset", "Drag a tank taller");
    public static final TextKey HINT_ROTATE = TextKey.of("jsspace.design.screen.hint_rotate", "Click a part to turn");
    public static final TextKey HINT_ROOT = TextKey.of("jsspace.design.screen.hint_root", "Click the root part");
    public static final TextKey HINT_DROP = TextKey.of("jsspace.design.screen.hint_drop", "Drop on a node");
    public static final TextKey SNAPS_HERE = TextKey.of("jsspace.design.screen.snaps_here", "Snaps here");
    public static final TextKey ROOT_TAG = TextKey.of("jsspace.design.screen.root_tag", "Root");
    public static final TextKey SAVED = TextKey.of("jsspace.design.screen.saved", "Saved");
    public static final TextKey NAME_FIRST = TextKey.of("jsspace.design.screen.name_first", "Name it to save");
    public static final TextKey SENT = TextKey.of("jsspace.design.screen.sent", "Sent to the bay");
    public static final TextKey NOT_SENT = TextKey.of("jsspace.design.screen.not_sent", "Not ready to build");
    public static final TextKey CLEARED = TextKey.of("jsspace.design.screen.cleared", "Cleared");
    public static final TextKey ONE_SIZE = TextKey.of("jsspace.design.screen.one_size", "This part has one size");
    public static final TextKey FULL = TextKey.of("jsspace.design.screen.full", "The hangar keeps %s plans");
    public static final TextKey LOADED = TextKey.of("jsspace.design.screen.loaded", "Loaded");
    public static final TextKey SPEC_SHEET = TextKey.of("jsspace.design.screen.spec_sheet", "Spec sheet %s");
    public static final TextKey TIER_MASS = TextKey.of("jsspace.design.screen.tier_mass", "Tier T%s  mass %s t");
    public static final TextKey THRUST_VACUUM = TextKey.of("jsspace.design.screen.thrust_vacuum", "Thrust vac");
    public static final TextKey THRUST_SEA = TextKey.of("jsspace.design.screen.thrust_sea", "Thrust SL");
    public static final TextKey ISP_VACUUM = TextKey.of("jsspace.design.screen.isp_vacuum", "Isp vac");
    public static final TextKey ISP_SEA = TextKey.of("jsspace.design.screen.isp_sea", "Isp SL");
    public static final TextKey MASS = TextKey.of("jsspace.design.screen.mass", "Mass");
    public static final TextKey PROPELLANT = TextKey.of("jsspace.design.screen.propellant", "Propellant");
    public static final TextKey SEATS = TextKey.of("jsspace.design.screen.seats", "Seats");
    public static final TextKey HEIGHT = TextKey.of("jsspace.design.screen.height", "Height");
    public static final TextKey KILONEWTONS = TextKey.of("jsspace.design.screen.kilonewtons", "%s kN");
    public static final TextKey SECONDS = TextKey.of("jsspace.design.screen.seconds", "%s s");
    public static final TextKey TONNES = TextKey.of("jsspace.design.screen.tonnes", "%s t");
    public static final TextKey BLOCKS = TextKey.of("jsspace.design.screen.blocks", "%s blk");
    public static final TextKey PER_LAYER = TextKey.of("jsspace.design.screen.per_layer", "%s t a layer");
    public static final TextKey CARD_LINE = TextKey.of("jsspace.design.screen.card_line", "%s D%s");
    public static final TextKey NOTE_VACUUM = TextKey.of("jsspace.design.screen.note_vacuum",
            "Big nozzle: strong up high, weak low down.");
    public static final TextKey NOTE_SEA = TextKey.of("jsspace.design.screen.note_sea",
            "Small nozzle: good in the air off the pad.");
    public static final TextKey NOTE_STRETCH = TextKey.of("jsspace.design.screen.note_stretch",
            "Built as tall as you like: Offset stretches it.");
    public static final TextKey NOTE_STAGE = TextKey.of("jsspace.design.screen.note_stage",
            "Ends a stage: what is under it drops.");
    public static final TextKey NOTE_CREW = TextKey.of("jsspace.design.screen.note_crew",
            "Carries the crew; a capsule brings them home.");
    public static final TextKey NOTE_LEGS = TextKey.of("jsspace.design.screen.note_legs",
            "Lets a stage with an engine come back and land.");
    public static final TextKey STAGE = TextKey.of("jsspace.design.screen.stage", "S%s");
    public static final TextKey NO_ENGINE_YET = TextKey.of("jsspace.design.screen.no_engine_yet", "No engine yet");
    public static final TextKey NO_THRUST = TextKey.of("jsspace.design.screen.no_thrust", "No thrust");
    public static final TextKey PAYLOAD = TextKey.of("jsspace.design.screen.payload", "Payload");
    public static final TextKey RATIO = TextKey.of("jsspace.design.screen.ratio", "TWR %s");
    public static final TextKey SPEED = TextKey.of("jsspace.design.screen.speed", "%s m/s");
    public static final TextKey UNIT_SPEED = TextKey.of("jsspace.design.screen.unit_speed", "m/s");
    public static final TextKey DELTA_V = TextKey.of("jsspace.design.screen.delta_v", "Delta-v");
    public static final TextKey LEAVES = TextKey.of("jsspace.design.screen.leaves", "TWR %s: it leaves");
    public static final TextKey STAYS = TextKey.of("jsspace.design.screen.stays", "TWR %s: it stays down");
    public static final TextKey NO_ENGINES = TextKey.of("jsspace.design.screen.no_engines", "No engine: it stays down");
    public static final TextKey DESTINATIONS = TextKey.of("jsspace.design.screen.destinations", "Destinations");
    public static final TextKey SHORT = TextKey.of("jsspace.design.screen.short", "Short %s m/s");
    public static final TextKey SPARE = TextKey.of("jsspace.design.screen.spare", "%s m/s to spare");
    public static final TextKey NO_ROUTES = TextKey.of("jsspace.design.screen.no_routes", "No routes from this world");
    public static final TextKey CHECKS = TextKey.of("jsspace.design.screen.checks", "Checks");
    public static final TextKey MORE = TextKey.of("jsspace.design.screen.more", "And %s more");
    public static final TextKey READY = TextKey.of("jsspace.design.screen.ready", "Ready");
    public static final TextKey NOT_READY = TextKey.of("jsspace.design.screen.not_ready", "Not ready");
    public static final TextKey THE_VIEW = TextKey.of("jsspace.design.screen.the_view", "The view");
    public static final TextKey LEFT_BUTTON = TextKey.of("jsspace.design.screen.left_button", "LMB");
    public static final TextKey RIGHT_BUTTON = TextKey.of("jsspace.design.screen.right_button", "RMB");
    public static final TextKey WHEEL = TextKey.of("jsspace.design.screen.wheel", "WHL");
    public static final TextKey MIDDLE_BUTTON = TextKey.of("jsspace.design.screen.middle_button", "MMB");
    public static final TextKey DRAG_PART = TextKey.of("jsspace.design.screen.drag_part", "Drag a part to a node");
    public static final TextKey TURN_VIEW = TextKey.of("jsspace.design.screen.turn_view", "Turn the view");
    public static final TextKey ZOOM = TextKey.of("jsspace.design.screen.zoom", "Zoom");
    public static final TextKey MOVE_VIEW = TextKey.of("jsspace.design.screen.move_view", "Move the view");
    public static final TextKey LOAD = TextKey.of("jsspace.design.screen.load", "Load");
    public static final TextKey DELETE = TextKey.of("jsspace.design.screen.delete", "Delete");
    public static final TextKey IN_THE_BAY = TextKey.of("jsspace.design.screen.in_the_bay", "In the bay");
    public static final TextKey NO_PLANS = TextKey.of("jsspace.design.screen.no_plans", "No plans saved yet");
    /** The view's keys and what each does, in the order the plate lists them. */
    public static final List<TextKey> VIEW_KEYS = List.of(LEFT_BUTTON, RIGHT_BUTTON, WHEEL, MIDDLE_BUTTON);
    public static final List<TextKey> VIEW_DOES = List.of(DRAG_PART, TURN_VIEW, ZOOM, MOVE_VIEW);

    private RocketDesignTexts() {
    }
}
