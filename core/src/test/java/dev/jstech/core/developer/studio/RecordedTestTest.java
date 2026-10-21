/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecordedTestTest {

    @Test
    void toJson_andFromJson_bringEveryKindOfStepBack() {
        final RecordedTest test = new RecordedTest("jsindustrial", "compressor_starts", List.of(
                new RecordedTest.Use("jsindustrial:compressor"), new RecordedTest.Click("start"),
                new RecordedTest.ClickAt(-20, 14), new RecordedTest.Type("8"), new RecordedTest.Key(257),
                new RecordedTest.Wait(10), new RecordedTest.Picture("running"), new RecordedTest.Close()));
        assertEquals(test, RecordedTest.fromJson(test.toJson()));
    }

    @Test
    void fromJson_readsTheFileAsWrittenByHand() {
        final RecordedTest read = RecordedTest.fromJson(JsonParser.parseString(
                "{\"mod\":\"m\",\"name\":\"t\",\"steps\":[{\"use\":\"m:press\"},{\"click\":\"start\"}]}")
                .getAsJsonObject());
        assertEquals(List.of(new RecordedTest.Use("m:press"), new RecordedTest.Click("start")), read.steps());
    }

    @Test
    void fromJson_refusesAStepItDoesNotKnow() {
        assertThrows(IllegalArgumentException.class, () -> RecordedTest.fromJson(JsonParser.parseString(
                "{\"mod\":\"m\",\"name\":\"t\",\"steps\":[{\"fly\":1}]}").getAsJsonObject()));
    }

    @Test
    void new_refusesANameWithCapitals() {
        assertThrows(IllegalArgumentException.class, () -> new RecordedTest("m", "Press", List.of()));
        assertFalse(RecordedTest.validName("Press"));
        assertTrue(RecordedTest.validName("press_2"));
    }

    @Test
    void wait_isATickAtLeast() {
        assertThrows(IllegalArgumentException.class, () -> new RecordedTest.Wait(0));
    }
}
