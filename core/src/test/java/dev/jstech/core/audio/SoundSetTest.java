/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SoundSetTest {

    private static final SoundSet BOOT = new SoundSet(List.of(
            new SoundSet.Rule(Map.of(SoundContext.DEVICE, "jsc:pc_speaker"), "jsc:computer/boot_beep"),
            new SoundSet.Rule(Map.of(SoundContext.ERA, "vintage", SoundContext.FAMILY, "dos"),
                    "jsc:computer/boot_dos"),
            new SoundSet.Rule(Map.of(SoundContext.ERA, "vintage"), "jsc:computer/boot_vintage"),
            new SoundSet.Rule(Map.of(), "jsc:computer/boot")));

    @Test
    void pick_takesTheFirstRuleThatHolds() {
        final SoundContext speaker = new SoundContext(Map.of(SoundContext.DEVICE, "jsc:pc_speaker",
                SoundContext.ERA, "vintage", SoundContext.FAMILY, "dos"));
        assertEquals("jsc:computer/boot_beep", BOOT.pick(speaker));
    }

    @Test
    void pick_needsEveryConditionOfARule() {
        assertEquals("jsc:computer/boot_dos", BOOT.pick(new SoundContext(Map.of(SoundContext.ERA, "vintage",
                SoundContext.FAMILY, "dos"))));
        assertEquals("jsc:computer/boot_vintage", BOOT.pick(new SoundContext(Map.of(SoundContext.ERA, "vintage",
                SoundContext.FAMILY, "unix"))));
    }

    @Test
    void pick_fallsBackToTheRuleWithNoCondition() {
        assertEquals("jsc:computer/boot", BOOT.pick(SoundContext.EMPTY));
        assertEquals("jsc:computer/boot", BOOT.pick(new SoundContext(Map.of(SoundContext.ERA, "standard"))));
    }

    @Test
    void pick_isNothingWhenNoRuleHolds() {
        assertNull(SoundSet.SILENT.pick(SoundContext.EMPTY));
        final SoundSet onlySpeaker = new SoundSet(List.of(new SoundSet.Rule(Map.of(SoundContext.DEVICE, "x:y"),
                "x:beep")));
        assertNull(onlySpeaker.pick(SoundContext.EMPTY));
        assertEquals("x:hum", SoundSet.always("x:hum").pick(SoundContext.EMPTY));
    }

    @Test
    void rule_namesASound() {
        assertThrows(IllegalArgumentException.class, () -> new SoundSet.Rule(Map.of(), ""));
    }
}
