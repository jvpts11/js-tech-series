/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.text.Text;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SafeCommandsTest {

    @Test
    void refusal_letsThroughWhatSetsUpATest() {
        assertEquals(Optional.empty(), SafeCommands.refusal("/time set day"));
        assertEquals(Optional.empty(), SafeCommands.refusal("give @s minecraft:stone 64"));
        assertEquals(Optional.empty(), SafeCommands.refusal("minecraft:setblock 0 64 0 minecraft:chest"));
    }

    @Test
    void refusal_neverLetsThroughOperatorRightsBansOrAStop() {
        for (final String command : new String[] {"op jvpts11", "/deop x", "ban x", "kick x", "stop",
            "whitelist off", "minecraft:op x"}) {
            assertTrue(english(SafeCommands.refusal(command)).contains("never"), command);
        }
    }

    @Test
    void refusal_neverLetsThroughACommandThatRunsAnother() {
        assertTrue(english(SafeCommands.refusal("execute as @a run op @s")).contains("never"));
        assertTrue(english(SafeCommands.refusal("function mypack:everything")).contains("never"));
    }

    @Test
    void refusal_refusesWhatIsNotListedAndWhatIsNoCommand() {
        assertTrue(english(SafeCommands.refusal("somemod:command")).contains("never"));
        assertTrue(english(SafeCommands.refusal("unknowncommand")).contains("not among"));
        assertTrue(SafeCommands.refusal("").isPresent());
        assertTrue(SafeCommands.refusal("time set day\nstop").isPresent());
    }

    private static String english(final Optional<Text> refusal) {
        return refusal.orElseThrow().english();
    }
}
