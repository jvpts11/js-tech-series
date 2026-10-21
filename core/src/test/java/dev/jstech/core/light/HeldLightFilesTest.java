/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.light;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HeldLightFilesTest {

    @Test
    void levels_readEveryFileALaterOneWinning() {
        final List<String> files = List.of(
                "{\"values\": {\"mymod:flashlight\": {\"level\": 13, \"colour\": \"#FFF4E0\"},"
                        + " \"mymod:lamp\": {\"value\": {\"level\": 9}, \"replace\": true}}}",
                "{not json",
                "{\"values\": {\"mymod:flashlight\": {\"level\": 20}, \"mymod:odd\": \"#FFFFFF\"}}");

        assertEquals(Map.of("mymod:flashlight", 15, "mymod:lamp", 9), HeldLightFiles.levels(files));
    }

    @Test
    void lambFile_isWhatLambDynamicLightsReads() {
        assertEquals(JsonParser.parseString("{\"match\": {\"items\": \"mymod:flashlight\"}, \"luminance\": 13}"),
                JsonParser.parseString(HeldLightFiles.lambFile("mymod:flashlight", 13)));
        assertEquals("mymod_tools_flashlight", HeldLightFiles.fileName("mymod:tools/flashlight"));
    }
}
