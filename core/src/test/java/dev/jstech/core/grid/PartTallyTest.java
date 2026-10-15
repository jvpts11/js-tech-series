/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PartTallyTest {

    private static final long OUTPUT = 1L;
    private static final long INPUT = 2L;
    private static final long TANK = 3L;
    private static final long IDLE = 4L;

    @Test
    void moved_countsWhatEachBlockGaveAndTookAndWhatTheyAre() {
        final PartTally tally = new PartTally(5, Optional.of(new LineSurvey.Narrowest(9L, "test:pipe", 100L)),
                List.of(OUTPUT, INPUT, TANK, IDLE));

        assertEquals(60L, tally.moved(OUTPUT, INPUT, 60L), "it hands the amount back");
        tally.moved(OUTPUT, TANK, 30L);
        tally.moved(TANK, INPUT, 10L);
        final LineSurvey survey = tally.survey();
        final Map<Long, LineSurvey.Member> members = byPos(survey);

        assertEquals(100L, survey.moved());
        assertEquals(LineSurvey.UNKNOWN, survey.wanted(), "a routed line cannot tell what was asked of it");
        assertEquals(0L, survey.lost(), "and loses nothing");
        assertEquals(5, survey.cables());
        assertEquals(100L, survey.narrowest().orElseThrow().ceiling());
        assertEquals(LineSurvey.Role.SOURCE, members.get(OUTPUT).role());
        assertEquals(90L, members.get(OUTPUT).gave());
        assertEquals(LineSurvey.Role.CONSUMER, members.get(INPUT).role());
        assertEquals(70L, members.get(INPUT).took());
        assertEquals(LineSurvey.Role.STORE, members.get(TANK).role());
        assertEquals(LineSurvey.Role.IDLE, members.get(IDLE).role());
    }

    @Test
    void reset_forgetsTheLastTick() {
        final PartTally tally = new PartTally(1, Optional.empty(), List.of(OUTPUT, INPUT));
        tally.moved(OUTPUT, INPUT, 5L);

        tally.reset();
        final LineSurvey survey = tally.survey();

        assertEquals(0L, survey.moved());
        assertTrue(survey.members().stream().allMatch(member -> member.role() == LineSurvey.Role.IDLE));
        assertEquals(2, survey.members().size(), "the blocks stay, with nothing done");
    }

    @Test
    void moved_ignoresNothingMoved() {
        final PartTally tally = new PartTally(1, Optional.empty(), List.of(OUTPUT, INPUT));

        tally.moved(OUTPUT, INPUT, 0L);

        assertEquals(0L, tally.survey().moved());
        assertEquals(LineSurvey.Role.IDLE, byPos(tally.survey()).get(OUTPUT).role());
    }

    @Test
    void role_isWhatABlockWasSeenToDo() {
        assertEquals(LineSurvey.Role.STORE, LineSurvey.Role.of(true, true));
        assertEquals(LineSurvey.Role.SOURCE, LineSurvey.Role.of(true, false));
        assertEquals(LineSurvey.Role.CONSUMER, LineSurvey.Role.of(false, true));
        assertEquals(LineSurvey.Role.IDLE, LineSurvey.Role.of(false, false));
    }

    private static Map<Long, LineSurvey.Member> byPos(final LineSurvey survey) {
        return survey.members().stream().collect(Collectors.toMap(LineSurvey.Member::pos, Function.identity()));
    }
}
