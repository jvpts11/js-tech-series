/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.launch;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the Launch Panel's console says, engraved on its face and lit on its screens and annunciators. */
@TextHolder
public final class LaunchTexts {

    public static final TextKey TITLE = TextKey.of("jsspace.launch.title", "LCC-1  LAUNCH CONTROL");
    public static final TextKey PAD = TextKey.of("jsspace.launch.pad", "PAD %s");
    public static final TextKey NO_ROCKET = TextKey.of("jsspace.launch.no_rocket", "NO VEHICLE");
    public static final TextKey COUNTDOWN = TextKey.of("jsspace.launch.countdown", "COUNTDOWN");
    public static final TextKey SEQUENCE = TextKey.of("jsspace.launch.sequence", "SEQUENCE");
    public static final TextKey MASTER = TextKey.of("jsspace.launch.master", "MASTER");
    public static final TextKey HOLD = TextKey.of("jsspace.launch.hold", "HOLD");
    public static final TextKey ABORT = TextKey.of("jsspace.launch.abort", "ABORT");
    public static final TextKey MET = TextKey.of("jsspace.launch.met", "MET");
    public static final TextKey HOLDS = TextKey.of("jsspace.launch.holds", "HOLDS %s");
    public static final TextKey GO_NO_GO = TextKey.of("jsspace.launch.go_no_go", "GO / NO-GO");
    public static final TextKey VEHICLE = TextKey.of("jsspace.launch.vehicle", "VEHICLE");
    public static final TextKey PROPELLANT = TextKey.of("jsspace.launch.propellant", "PROPELLANT");
    public static final TextKey TOWER_ARMS = TextKey.of("jsspace.launch.tower_arms", "TOWER ARMS");
    public static final TextKey DELUGE = TextKey.of("jsspace.launch.deluge", "DELUGE");
    public static final TextKey WEATHER = TextKey.of("jsspace.launch.weather", "WEATHER");
    public static final TextKey CREW = TextKey.of("jsspace.launch.crew", "CREW");
    public static final TextKey RANGE = TextKey.of("jsspace.launch.range", "RANGE");
    public static final TextKey CLAMPS = TextKey.of("jsspace.launch.clamps", "CLAMPS");
    public static final TextKey GO = TextKey.of("jsspace.launch.go", "GO");
    public static final TextKey NO_GO = TextKey.of("jsspace.launch.no_go", "NO-GO");
    public static final TextKey MOVING = TextKey.of("jsspace.launch.moving", "MOVING");
    public static final TextKey CLEAR = TextKey.of("jsspace.launch.clear", "CLEAR");
    public static final TextKey OFF = TextKey.of("jsspace.launch.off", "OFF");
    public static final TextKey TANKS = TextKey.of("jsspace.launch.tanks", "TANKS");
    public static final TextKey OXIDISER = TextKey.of("jsspace.launch.oxidiser", "LOX");
    public static final TextKey FUEL = TextKey.of("jsspace.launch.fuel", "CH4");
    public static final TextKey TOPPING_OFF = TextKey.of("jsspace.launch.topping_off", "TOPPING OFF");
    public static final TextKey LOADED = TextKey.of("jsspace.launch.loaded", "LOADED");
    public static final TextKey AUTO = TextKey.of("jsspace.launch.auto", "AUTO");
    public static final TextKey ARM = TextKey.of("jsspace.launch.arm", "ARM");
    public static final TextKey SAFE = TextKey.of("jsspace.launch.safe", "SAFE");
    public static final TextKey IGNITION = TextKey.of("jsspace.launch.ignition", "IGNITION");
    public static final TextKey LAUNCH = TextKey.of("jsspace.launch.launch", "LAUNCH");
    public static final TextKey HOLD_COUNT = TextKey.of("jsspace.launch.hold_count", "HOLD\nCOUNT");
    public static final TextKey SCRUB = TextKey.of("jsspace.launch.scrub", "SCRUB");
    public static final TextKey RECYCLE = TextKey.of("jsspace.launch.recycle", "RECYCLE\nT-10");
    public static final TextKey COUNT_RUNNING = TextKey.of("jsspace.launch.count_running", "COUNT\nRUNNING");
    public static final TextKey COVER = TextKey.of("jsspace.launch.cover", "COVER LIFTS\nAT T-10 S");
    public static final TextKey ARMS_RETRACT = TextKey.of("jsspace.launch.arms_retract", "TOWER ARMS");
    public static final TextKey ARMS_MOVING = TextKey.of("jsspace.launch.arms_moving", "ARMS MOVING");
    public static final TextKey GUARD_LIFTS = TextKey.of("jsspace.launch.guard_lifts", "GUARD LIFTS");
    public static final TextKey IGNITION_EVENT = TextKey.of("jsspace.launch.ignition_event", "IGNITION");
    public static final TextKey RELEASE = TextKey.of("jsspace.launch.release", "RELEASE");

    private LaunchTexts() {
    }
}
