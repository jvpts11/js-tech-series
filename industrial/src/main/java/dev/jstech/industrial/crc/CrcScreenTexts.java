/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.crc;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the Computational Research Centre's screen says: its tabs, the words engraved on its face, its chart and its
 * card, and its Group tab's table and rule. The face shows its words in capitals, as engraved, whatever the language.
 */
@TextHolder
public final class CrcScreenTexts {

    /** Its own tabs' names. */
    public static final TextKey TAB_PROCESSORS = TextKey.of("jsindustrial.crc.screen.tab.processors", "Processors");
    public static final TextKey TAB_RESEARCH = TextKey.of("jsindustrial.crc.screen.tab.research", "Research");
    public static final TextKey TAB_GROUP = TextKey.of("jsindustrial.crc.screen.tab.group", "Group");
    /** The Processors tab. */
    public static final TextKey SECTION = TextKey.of("jsindustrial.crc.screen.section", "Section %s");
    public static final TextKey DATA = TextKey.of("jsindustrial.crc.screen.data", "Data");
    public static final TextKey HOT = TextKey.of("jsindustrial.crc.screen.hot", "Hot");
    public static final TextKey COOLANT = TextKey.of("jsindustrial.crc.screen.coolant", "Coolant");
    public static final TextKey HEAT_PORT = TextKey.of("jsindustrial.crc.screen.heat_port", "Heat port");
    public static final TextKey RESEARCH_RATE = TextKey.of("jsindustrial.crc.screen.research_rate", "Research");
    public static final TextKey DRAW = TextKey.of("jsindustrial.crc.screen.draw", "Draw");
    public static final TextKey SHED = TextKey.of("jsindustrial.crc.screen.shed", "Shed");
    public static final TextKey PER_SECOND = TextKey.of("jsindustrial.crc.screen.per_second", "/s");
    public static final TextKey KILOWATTS = TextKey.of("jsindustrial.crc.screen.kilowatts", "kW");
    /** The Processors tab's tooltips. */
    public static final TextKey TIP_TEMPERATURE = TextKey.of("jsindustrial.crc.screen.tip.temperature",
            "%s °C: a part burns past 100 °C");
    public static final TextKey TIP_RESEARCH = TextKey.of("jsindustrial.crc.screen.tip.research",
            "The research it made its last second, in points a second");
    public static final TextKey TIP_DRAW = TextKey.of("jsindustrial.crc.screen.tip.draw",
            "The power its processors draw now: all of it becomes heat");
    public static final TextKey TIP_SHED = TextKey.of("jsindustrial.crc.screen.tip.shed",
            "The heat its cooling took away its last second; red while it sheds less than it makes");
    public static final TextKey TIP_COOLANT = TextKey.of("jsindustrial.crc.screen.tip.coolant",
            "Lit while the coolant's water passes through its Liquid Coolers");
    public static final TextKey TIP_HEAT_PORT = TextKey.of("jsindustrial.crc.screen.tip.heat_port",
            "Lit while its Heat Pipe Coolers give their heat out through the Heat Port");
    /** The Research tab. */
    public static final TextKey DISCOVERIES = TextKey.of("jsindustrial.crc.screen.discoveries", "Discoveries");
    public static final TextKey KNOWLEDGE_OF = TextKey.of("jsindustrial.crc.screen.knowledge_of", "Knowledge of %s");
    public static final TextKey NOBODY = TextKey.of("jsindustrial.crc.screen.nobody", "Owned by nobody");
    public static final TextKey CHART = TextKey.of("jsindustrial.crc.screen.chart",
            "Discoveries by the year they were made");
    public static final TextKey NO_FIELD = TextKey.of("jsindustrial.crc.screen.no_field", "Other");
    public static final TextKey NO_DISCOVERIES = TextKey.of("jsindustrial.crc.screen.no_discoveries",
            "No discoveries are there to research");
    public static final TextKey MORE_LANES = TextKey.of("jsindustrial.crc.screen.more_lanes",
            "Fields %s to %s of %s: turn the wheel for the others");
    public static final TextKey KNOWN = TextKey.of("jsindustrial.crc.screen.known", "known");
    public static final TextKey NOW_RUNNING = TextKey.of("jsindustrial.crc.screen.now_running", "now");
    public static final TextKey STARTED = TextKey.of("jsindustrial.crc.screen.started", "started");
    public static final TextKey OPEN = TextKey.of("jsindustrial.crc.screen.open", "open");
    public static final TextKey NOT_YET = TextKey.of("jsindustrial.crc.screen.not_yet", "not yet");
    public static final TextKey NAMED_SHARE = TextKey.of("jsindustrial.crc.screen.named_share", "%s %s%%");
    public static final TextKey CHOSEN = TextKey.of("jsindustrial.crc.screen.chosen", "Chosen");
    public static final TextKey NOTHING_CHOSEN = TextKey.of("jsindustrial.crc.screen.nothing_chosen",
            "Pick a discovery on the chart");
    public static final TextKey NOT_MADE_YET = TextKey.of("jsindustrial.crc.screen.not_made_yet",
            "Not made yet in the real world");
    public static final TextKey UNLOCKS = TextKey.of("jsindustrial.crc.screen.unlocks", "Unlocks %s");
    public static final TextKey UNLOCKS_MORE = TextKey.of("jsindustrial.crc.screen.unlocks_more",
            "Unlocks %s and %s more");
    public static final TextKey NEEDS = TextKey.of("jsindustrial.crc.screen.needs", "Needs");
    public static final TextKey NEEDS_NOTHING = TextKey.of("jsindustrial.crc.screen.needs_nothing",
            "Nothing before it");
    public static final TextKey MORE = TextKey.of("jsindustrial.crc.screen.more", "and %s more");
    public static final TextKey DATA_OF = TextKey.of("jsindustrial.crc.screen.data_of", "of %s");
    public static final TextKey ENOUGH_TO = TextKey.of("jsindustrial.crc.screen.enough_to",
            "Enough to %s%%. %s more:");
    public static final TextKey ENOUGH = TextKey.of("jsindustrial.crc.screen.enough", "Enough data for all of it");
    public static final TextKey NO_DATA_NEEDED = TextKey.of("jsindustrial.crc.screen.no_data_needed",
            "It needs no data");
    public static final TextKey COMPUTING = TextKey.of("jsindustrial.crc.screen.computing", "Computing");
    public static final TextKey PERCENT_OF = TextKey.of("jsindustrial.crc.screen.percent_of", "%% of %s");
    public static final TextKey AT_PACE = TextKey.of("jsindustrial.crc.screen.at_pace", "At %s a second: %s");
    public static final TextKey NO_PACE = TextKey.of("jsindustrial.crc.screen.no_pace", "Nothing computes it now");
    public static final TextKey MADE_ALREADY = TextKey.of("jsindustrial.crc.screen.made_already",
            "Its team has made it");
    public static final TextKey QUEUE = TextKey.of("jsindustrial.crc.screen.queue", "Queue");
    public static final TextKey RESEARCH = TextKey.of("jsindustrial.crc.screen.research", "Research");
    public static final TextKey NOW = TextKey.of("jsindustrial.crc.screen.now", "Now");
    public static final TextKey NOTHING_NOW = TextKey.of("jsindustrial.crc.screen.nothing_now", "Nothing");
    public static final TextKey NAMED_YEAR = TextKey.of("jsindustrial.crc.screen.named_year", "%s, %s");
    public static final TextKey QUEUE_LINE = TextKey.of("jsindustrial.crc.screen.queue_line", "%s  %s");
    public static final TextKey QUEUE_EMPTY = TextKey.of("jsindustrial.crc.screen.queue_empty", "Empty");
    public static final TextKey GROUP_PACE = TextKey.of("jsindustrial.crc.screen.group_pace",
            "Group of %s CRCs: %s a second, ends in %s");
    public static final TextKey ALONE_PACE = TextKey.of("jsindustrial.crc.screen.alone_pace",
            "This CRC alone: %s a second, ends in %s");
    public static final TextKey WAITING_FOR_DATA = TextKey.of("jsindustrial.crc.screen.waiting_for_data",
            "Waiting for data");
    public static final TextKey WAITING_FOR_NEEDS = TextKey.of("jsindustrial.crc.screen.waiting_for_needs",
            "Waiting: its team does not know what it needs");
    public static final TextKey NOT_THERE = TextKey.of("jsindustrial.crc.screen.not_there",
            "Not there: something it asks for is not installed");
    public static final TextKey NOT_COMPUTING = TextKey.of("jsindustrial.crc.screen.not_computing",
            "Not computing");
    public static final TextKey SPENT_EVERY = TextKey.of("jsindustrial.crc.screen.spent_every",
            "%s: 1 spent every %s%%, %s held");
    public static final TextKey TIP_NODE = TextKey.of("jsindustrial.crc.screen.tip.node", "%s: %s");
    public static final TextKey TIP_QUEUE = TextKey.of("jsindustrial.crc.screen.tip.queue",
            "Puts it at the end of the group's queue");
    public static final TextKey TIP_RESEARCH_NOW = TextKey.of("jsindustrial.crc.screen.tip.research_now",
            "Researches it now, for the whole group");
    /** A time to come. */
    public static final TextKey SECONDS = TextKey.of("jsindustrial.crc.screen.seconds", "%s s");
    public static final TextKey MINUTES = TextKey.of("jsindustrial.crc.screen.minutes", "%s min");
    public static final TextKey HOURS = TextKey.of("jsindustrial.crc.screen.hours", "%s h %s min");
    public static final TextKey DAYS = TextKey.of("jsindustrial.crc.screen.days", "%s d %s h");
    /** The Group tab. */
    public static final TextKey RESEARCH_GROUP = TextKey.of("jsindustrial.crc.screen.research_group",
            "Research group");
    public static final TextKey CABLE = TextKey.of("jsindustrial.crc.screen.cable", "%s cable");
    public static final TextKey UNDYED = TextKey.of("jsindustrial.crc.screen.undyed", "Undyed cable");
    public static final TextKey NO_CABLE = TextKey.of("jsindustrial.crc.screen.no_cable", "No cable");
    public static final TextKey THIS_ONE = TextKey.of("jsindustrial.crc.screen.this_one", "this one");
    public static final TextKey OTHER_TEAM = TextKey.of("jsindustrial.crc.screen.other_team", "other team");
    public static final TextKey PLACE = TextKey.of("jsindustrial.crc.screen.place", "Place");
    public static final TextKey TEAM = TextKey.of("jsindustrial.crc.screen.team", "Team");
    public static final TextKey COMPUTES = TextKey.of("jsindustrial.crc.screen.computes", "Computes");
    public static final TextKey COUNTED = TextKey.of("jsindustrial.crc.screen.counted", "Counted");
    public static final TextKey GIVES = TextKey.of("jsindustrial.crc.screen.gives", "Gives");
    public static final TextKey STATE = TextKey.of("jsindustrial.crc.screen.state", "State");
    public static final TextKey WHOLE = TextKey.of("jsindustrial.crc.screen.whole", "whole");
    public static final TextKey PERCENT = TextKey.of("jsindustrial.crc.screen.percent", "%s%%");
    public static final TextKey RATE = TextKey.of("jsindustrial.crc.screen.rate", "%s/s");
    public static final TextKey PLACE_AT = TextKey.of("jsindustrial.crc.screen.place_at", "%s %s %s");
    public static final TextKey TEAM_SUM = TextKey.of("jsindustrial.crc.screen.team_sum",
            "%s = %s/s on %s, %s%%");
    public static final TextKey TEAM_ON = TextKey.of("jsindustrial.crc.screen.team_on", "%s/s on %s, %s%%");
    public static final TextKey TEAM_IDLE = TextKey.of("jsindustrial.crc.screen.team_idle", "%s/s, nothing chosen");
    public static final TextKey MORE_ROWS = TextKey.of("jsindustrial.crc.screen.more_rows",
            "CRCs %s to %s of %s: turn the wheel for the others");
    public static final TextKey RULE_ONE = TextKey.of("jsindustrial.crc.screen.rule.one",
            "One discovery for the whole group; any CRC may choose it.");
    public static final TextKey RULE_TWO = TextKey.of("jsindustrial.crc.screen.rule.two",
            "Each team counts its own CRCs: the strongest whole, each");
    public static final TextKey RULE_THREE = TextKey.of("jsindustrial.crc.screen.rule.three",
            "other at %s%%, and keeps its own progress.");
    public static final TextKey RULE_FOUR = TextKey.of("jsindustrial.crc.screen.rule.four",
            "Cutting the cable loses nothing: the progress is the team's.");
    public static final TextKey RULE_FIVE = TextKey.of("jsindustrial.crc.screen.rule.five",
            "Dye the cable to keep two groups apart.");

    private CrcScreenTexts() {
    }
}
