/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import java.util.List;
import org.junit.jupiter.api.Test;

class SystemOsSubsetTest {

    @Test
    void standard_reachesTheCallsOfAPlayersSystem() {
        final SigmaCompiler.Result built = built("Screen.WriteLine(\"up\"); int free = Memory.FreeMb; "
                + "Boot.Progress(50); bool sound = Drivers.Has(\"sound\"); Services.Start(\"watchman\");");
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
    }

    @Test
    void standard_leavesWhatHandsOverAListToTheFullLanguage() {
        final SigmaCompiler.Result built = built("Processes.List();");
        assertFalse(built.ok(), "a list is the full language's");
    }

    private static SigmaCompiler.Result built(final String body) {
        final String source = "using Standard.*; namespace Tests; class Says : Script { "
                + "public override void OnTick() { " + body + " } }";
        return SigmaCompiler.compile(List.of(new SourceFile("Says.sg", source)), "jsc:ia_16", LanguageLevel.SIGMA);
    }
}
