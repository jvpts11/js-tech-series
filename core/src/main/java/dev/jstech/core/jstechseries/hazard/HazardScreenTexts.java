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
    /* The words the body's screen's file shows as they are. */
    public static final TextKey SUBTITLE = TextKey.of("jscore.body.subtitle",
            "What builds up in you, and what you wear against it");
    public static final TextKey WEATHER = TextKey.of("jscore.body.weather", "AGAINST THE WEATHER");
    public static final TextKey LETS_OUT = TextKey.of("jscore.body.lets_out", "Lets heat out");
    public static final TextKey WIND = TextKey.of("jscore.body.wind", "Wind");
    public static final TextKey WET = TextKey.of("jscore.body.wet", "Wet");
    public static final TextKey STOPS = TextKey.of("jscore.body.stops", "WHAT IT STOPS");
    public static final TextKey WARMER = TextKey.of("jscore.body.warmer", "The less it lets out, the warmer you stay");
    public static final TextKey TEMPERATURE = TextKey.of("jscore.body.temperature", "Temperature");
    public static final TextKey RADIATION = TextKey.of("jscore.body.radiation", "Radiation");
    public static final TextKey LUNGS = TextKey.of("jscore.body.lungs", "Lungs");
    public static final TextKey DISSONANCE = TextKey.of("jscore.body.dissonance", "Dissonance");
    public static final TextKey WASH = TextKey.of("jscore.body.wash", "Contaminated: wash in a shower");
    /* What the body's screen's code writes from the readings. */
    public static final TextKey NOTHING_WORN = TextKey.of("jscore.body.nothing_worn", "Nothing worn");
    /** How much heat what is worn lets out, its figure. */
    public static final TextKey LETS_OUT_VALUE = TextKey.of("jscore.body.lets_out_value", "%s W/m²K");
    public static final TextKey BARE = TextKey.of("jscore.body.bare", "bare skin");
    public static final TextKey KEPT_OUT = TextKey.of("jscore.body.kept_out", "kept out");
    public static final TextKey GETS_IN = TextKey.of("jscore.body.gets_in", "gets in");
    /** The share of the body whose clothes keep the wind out, as a percent. */
    public static final TextKey PARTLY_KEPT_OUT = TextKey.of("jscore.body.partly_kept_out", "%s%% kept out");
    public static final TextKey DRY = TextKey.of("jscore.body.dry", "dry");
    public static final TextKey SOAKED = TextKey.of("jscore.body.soaked", "wet");
    public static final TextKey NOTHING_STOPPED = TextKey.of("jscore.body.nothing_stopped", "Nothing");
    /** A share as a percent. */
    public static final TextKey SHARE = TextKey.of("jscore.body.share", "%s%%");
    public static final TextKey NORMAL = TextKey.of("jscore.body.normal", "normal");
    public static final TextKey NONE = TextKey.of("jscore.body.none", "none");
    /** The core's temperature, then which way it goes. */
    public static final TextKey CORE_LINE = TextKey.of("jscore.body.core_line", "Your core: %s, %s");
    public static final TextKey FALLING = TextKey.of("jscore.body.falling", "falling %s a minute");
    public static final TextKey RISING = TextKey.of("jscore.body.rising", "rising %s a minute");
    public static final TextKey STEADY = TextKey.of("jscore.body.steady", "steady");
    /** A mark on the core's scale, in degrees. */
    public static final TextKey DEGREE_MARK = TextKey.of("jscore.body.degree_mark", "%s°");
    /** The place's air, its wind and its sun, what the body makes, then what it loses. */
    public static final TextKey PLACE_LOSES = TextKey.of("jscore.body.place_loses",
            "Here %s, %s, %s. You make %s and lose %s.");
    /** As above, when the place gives the body more than it takes. */
    public static final TextKey PLACE_GIVES = TextKey.of("jscore.body.place_gives",
            "Here %s, %s, %s. You make %s and take in %s.");
    public static final TextKey A_WIND = TextKey.of("jscore.body.a_wind", "a wind of %s m/s");
    public static final TextKey NO_WIND = TextKey.of("jscore.body.no_wind", "no wind");
    public static final TextKey SUN = TextKey.of("jscore.body.sun", "sun of %s W/m²");
    public static final TextKey NO_SUN = TextKey.of("jscore.body.no_sun", "no sun");
    public static final TextKey WATTS = TextKey.of("jscore.body.watts", "%s W");
    /** How far the body's bands have moved toward the cold it is used to. */
    public static final TextKey USED_TO_COLD = TextKey.of("jscore.body.used_to_cold",
            "Used to the cold: your bands sit %s lower");
    public static final TextKey USED_TO_HEAT = TextKey.of("jscore.body.used_to_heat",
            "Used to the heat: your bands sit %s higher");
    public static final TextKey DOSE_LINE = TextKey.of("jscore.body.dose_line", "Dose built up: %s");
    /** The rate here with its unit, what comes from outside, then what comes from what is on the body. */
    public static final TextKey RATE_LINE = TextKey.of("jscore.body.rate_line",
            "Here %s: %s from outside, %s from what is on you.");
    public static final TextKey LUNG_LINE = TextKey.of("jscore.body.lung_line", "Damage %s");
    /** The lungs' harm in all, then each dust's share of it. */
    public static final TextKey LUNG_SHARES = TextKey.of("jscore.body.lung_shares", "Damage %s: %s");
    /** One dust's share of the lungs' harm: its name, then its percent. */
    public static final TextKey LUNG_SHARE = TextKey.of("jscore.body.lung_share", "%s %s");
    public static final TextKey CLEAR = TextKey.of("jscore.body.clear", "clear");
    public static final TextKey NORMAL_LAWS = TextKey.of("jscore.body.normal_laws",
            "The laws here are the normal ones");
    public static final TextKey BUILT_UP = TextKey.of("jscore.body.built_up", "Built up: %s");
    public static final TextKey OTHER_NONE = TextKey.of("jscore.body.other_none", "No other hazard on you");
    public static final TextKey OTHER = TextKey.of("jscore.body.other", "Also on you: %s");
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
