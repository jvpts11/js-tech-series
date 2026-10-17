/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the body's screen and the map of hazards say. */
@TextHolder
public final class HazardScreenTexts {

    public static final TextKey BODY_TITLE = TextKey.of("jscore.body.title", "Your body");
    public static final TextKey BODY_DOSIMETRY = TextKey.of("jscore.body.dosimetry", "BODY  DOSIMETRY");
    public static final TextKey PROTECTION = TextKey.of("jscore.body.protection", "PROTECTION");
    public static final TextKey NOTHING_WORN = TextKey.of("jscore.body.nothing_worn", "Nothing worn");
    public static final TextKey DOSE_TAKEN = TextKey.of("jscore.body.dose_taken", "DOSE TAKEN");
    public static final TextKey RATE_HERE = TextKey.of("jscore.body.rate_here", "RATE HERE %s");
    public static final TextKey WASH = TextKey.of("jscore.body.wash", "WASH");
    public static final TextKey OUTSIDE = TextKey.of("jscore.body.outside", "OUTSIDE %s");
    public static final TextKey ON_YOU = TextKey.of("jscore.body.on_you", "ON YOU  %s");
    /** The dose under its meter: the dose with its unit, then its band. */
    public static final TextKey DOSE_AND_BAND = TextKey.of("jscore.body.dose_and_band", "%s  %s");
    public static final TextKey LUNGS = TextKey.of("jscore.body.lungs", "LUNGS");
    /** The lungs' harm in all, then each dust's share of it. */
    public static final TextKey LUNG_HARM = TextKey.of("jscore.body.lung_harm", "%s%%: %s");
    /** One dust's share of the lungs' harm: its name, then its percent. */
    public static final TextKey LUNG_SHARE = TextKey.of("jscore.body.lung_share", "%s %s");
    public static final TextKey CLEAR_LUNGS = TextKey.of("jscore.body.clear_lungs", "CLEAR");
    public static final TextKey DISSONANCE = TextKey.of("jscore.body.dissonance", "DISSONANCE");
    public static final TextKey NORMAL_LAWS = TextKey.of("jscore.body.normal_laws", "NORMAL LAWS");
    public static final TextKey OTHER_NONE = TextKey.of("jscore.body.other_none", "OTHER HAZARDS: NONE");
    public static final TextKey OTHER = TextKey.of("jscore.body.other", "OTHER HAZARDS: %s");
    /** One other hazard on the body: its name, then its band or how much has built up. */
    public static final TextKey OTHER_ONE = TextKey.of("jscore.body.other_one", "%s (%s)");

    public static final TextKey MAP_TITLE = TextKey.of("jscore.hazard_map.title", "Hazards: %s");
    public static final TextKey MAP_SOURCES = TextKey.of("jscore.hazard_map.sources",
            "From your instruments' readings and the network's sensors");
    public static final TextKey MAP_OVER = TextKey.of("jscore.hazard_map.over", "over %s");
    public static final TextKey MAP_BETWEEN = TextKey.of("jscore.hazard_map.between", "%s to %s");
    public static final TextKey MAP_NORMAL = TextKey.of("jscore.hazard_map.normal", "normal");
    public static final TextKey MAP_NEVER_READ = TextKey.of("jscore.hazard_map.never_read", "never read");
    public static final TextKey MAP_CLICK = TextKey.of("jscore.hazard_map.click",
            "Click a chunk for its reading and its age");
    /** A chunk's reading: the rate with its unit, then how long ago it was read. */
    public static final TextKey MAP_READING = TextKey.of("jscore.hazard_map.reading", "%s, read %s ago");
    public static final TextKey MAP_NOT_READ = TextKey.of("jscore.hazard_map.not_read", "Never read");
    /** How long ago a chunk was read, in the largest unit that fits. */
    public static final TextKey AGE_SECONDS = TextKey.of("jscore.hazard_map.age.seconds", "%s s");
    public static final TextKey AGE_MINUTES = TextKey.of("jscore.hazard_map.age.minutes", "%s min");
    public static final TextKey AGE_HOURS = TextKey.of("jscore.hazard_map.age.hours", "%s h");
    public static final TextKey AGE_DAYS = TextKey.of("jscore.hazard_map.age.days", "%s days");
    public static final TextKey MAP_NOTHING = TextKey.of("jscore.hazard_map.nothing",
            "No hazard is mapped: an instrument carried reads one onto the map");

    private HazardScreenTexts() {
    }
}
