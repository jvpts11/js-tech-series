/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.computers.program.iql.IqlRedstoneStatement;
import org.junit.jupiter.api.Test;

class RedstoneScriptTest {

    @Test
    void iql_writesInAndOutWithItsStrength() {
        assertEquals("SET REDSTONE 'Gate' IN", RedstoneScript.iql("Gate", false, 9));
        assertEquals("SET REDSTONE 'Gate' OUT 15", RedstoneScript.iql("Gate", true, 15));
    }

    @Test
    void iql_doublesAQuoteInTheName() {
        assertEquals("SET REDSTONE 'Joe''s gate' OUT 3", RedstoneScript.iql("Joe's gate", true, 3));
    }

    @Test
    void iql_isReadBackAsTheSameSetting() {
        final IqlRedstoneStatement read = IqlRedstoneStatement.parse(RedstoneScript.iql("Joe's gate", true, 7));
        assertEquals(new IqlRedstoneStatement("Joe's gate", true, 7), read);
    }

    @Test
    void sigma_writesTheCallAndEscapesTheName() {
        assertEquals("redstone(\"Gate\").In();", RedstoneScript.sigma("Gate", false, 0));
        assertEquals("redstone(\"Gate\").Out(15);", RedstoneScript.sigma("Gate", true, 15));
        assertEquals("redstone(\"say \\\"hi\\\" \\\\ there\").In();",
                RedstoneScript.sigma("say \"hi\" \\ there", false, 0));
    }
}
