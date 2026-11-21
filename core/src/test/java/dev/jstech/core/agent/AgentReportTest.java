/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AgentReportTest {

    @Test
    void values_keepTheOrderTheyWereToldIn() {
        final AgentReport report = new AgentReport().words("mode", "orbit").number("speed_mps", 12.5)
                .whole("throttle_percent", 80).flag("boosting", false);
        assertEquals(List.of("mode", "speed_mps", "throttle_percent", "boosting"),
                List.copyOf(report.values().keySet()));
        assertEquals(12.5, report.values().get("speed_mps"));
        assertEquals(80L, report.values().get("throttle_percent"));
    }

    @Test
    void part_isTheSamePartEachTimeItIsAskedFor() {
        final AgentReport report = new AgentReport();
        final AgentReport pilot = report.part("autopilot");
        pilot.words("mode", "land");
        assertSame(pilot, report.part("autopilot"));
        assertEquals("land", report.part("autopilot").values().get("mode"));
    }

    @Test
    void isEmpty_whileItsPartsTellNothing() {
        final AgentReport report = new AgentReport();
        report.part("ship").part("pulse");
        assertTrue(report.isEmpty(), "parts that tell nothing tell nothing");
        report.part("ship").flag("stalled", true);
        assertFalse(report.isEmpty());
    }

    @Test
    void merge_takesInValuesAndPartsByName() {
        final AgentReport mine = new AgentReport().words("name", "JS-02");
        mine.part("pulse").flag("jumping", false);
        final AgentReport newer = new AgentReport().words("name", "JS-03");
        newer.part("pulse").whole("spool_percent", 40);
        mine.merge(newer);
        assertEquals("JS-03", mine.values().get("name"), "told again, a name takes the newer value");
        assertEquals(List.of("jumping", "spool_percent"), List.copyOf(mine.part("pulse").values().keySet()));
    }

    @Test
    void words_refuseABlankName() {
        assertThrows(IllegalArgumentException.class, () -> new AgentReport().words(" ", "orbit"));
    }
}
