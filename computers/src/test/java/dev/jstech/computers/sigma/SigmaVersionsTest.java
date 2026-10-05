/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * What a version of the language holds a build to: what came after it is refused where it was written, with the
 * version it needs, and everything else compiles as the newest compiles it.
 */
class SigmaVersionsTest {

    private static final String SHARP_SOUND = """
            using System.IO.*; using System.Sound.*; namespace Tests;
            class Beeper { static void Main() { Sound.Beep(880, 200); } }
            """;
    private static final String SHARP_SPEAKER_TYPE = """
            using System.IO.*; using System.Sound.*; namespace Tests;
            class Holder { static void Main() { Speaker kept = null; Console.PrintLine("" + (kept == null)); } }
            """;
    private static final String SUBSET_SOUND = """
            using Standard.*; namespace Tests;
            class Beeper { static void Main() { Sound.Beep(880, 200); } }
            """;
    private static final String OWN_SOUND = """
            using System.IO.*; namespace Tests;
            class Sound { public static void Beep(int pitch, int length) { Console.PrintLine("beep"); } }
            class Beeper { static void Main() { Sound.Beep(880, 200); } }
            """;

    @Test
    void held_isTheNewestWhenNoneIsAskedFor() {
        assertEquals(SigmaVersions.NEWEST, SigmaVersions.held(0));
        assertEquals(SigmaVersions.NEWEST, SigmaVersions.held(-3));
    }

    @Test
    void held_staysWithinTheVersionsThereAre() {
        assertEquals(SigmaVersions.FIRST, SigmaVersions.held(SigmaVersions.FIRST));
        assertEquals(SigmaVersions.NEWEST, SigmaVersions.held(SigmaVersions.NEWEST + 5));
    }

    @Test
    void known_isFromTheFirstToTheNewest() {
        assertTrue(SigmaVersions.known(SigmaVersions.FIRST));
        assertTrue(SigmaVersions.known(SigmaVersions.NEWEST));
        assertFalse(SigmaVersions.known(0));
        assertFalse(SigmaVersions.known(SigmaVersions.NEWEST + 1));
    }

    @Test
    void sinceType_givesTheSecondVersionsWidgetsTheSecondVersion() {
        for (final String widget : List.of("TextArea", "NumberBox", "Slider", "RadioGroup", "ComboBox", "TabView",
                "GroupBox", "ScrollView", "Table", "TreeView", "MenuBar", "ContextMenu", "StatusBar", "Image", "Chart",
                "LogView", "OpenFileDialog", "SaveFileDialog", "ItemSlot", "ItemPicker", "OperationView",
                "GenericComponent", "ComponentAction")) {
            assertEquals(2, SigmaVersions.sinceType(LanguageLevel.SIGMA_SHARP, widget), widget);
        }
        assertEquals(SigmaVersions.FIRST, SigmaVersions.sinceType(LanguageLevel.SIGMA_SHARP, "ListBox"));
    }

    @Test
    void sinceType_givesSoundAndSpeakerTheSecondVersion() {
        for (final LanguageLevel level : LanguageLevel.values()) {
            assertEquals(2, SigmaVersions.sinceType(level, "Sound"));
            assertEquals(2, SigmaVersions.sinceType(level, "Speaker"));
            assertEquals(SigmaVersions.FIRST, SigmaVersions.sinceType(level, "Console"));
        }
    }

    @Test
    void sinceType_givesRandomTheSecondVersionOnlyInTheSmallerLanguage() {
        assertEquals(2, SigmaVersions.sinceType(LanguageLevel.SIGMA, "Random"));
        assertEquals(SigmaVersions.FIRST, SigmaVersions.sinceType(LanguageLevel.SIGMA_SHARP, "Random"));
    }

    @Test
    void checkProgram_atTheFirstVersion_refusesSoundWithTheVersionItNeeds() {
        final SigmaSemantics.Result checked = check("Beeper.sgs", SHARP_SOUND, LanguageLevel.SIGMA_SHARP, 1);
        final List<String> lines = checked.lines();
        assertTrue(lines.stream().anyMatch(line -> line.contains("error S3057: 'Sound' needs Σ# 2; this project is "
                + "Σ# 1")), () -> String.join("\n", lines));
    }

    @Test
    void checkProgram_atTheFirstVersion_refusesSpeakerWrittenAsAType() {
        final SigmaSemantics.Result checked = check("Holder.sgs", SHARP_SPEAKER_TYPE, LanguageLevel.SIGMA_SHARP, 1);
        assertTrue(checked.diagnostics().stream().anyMatch(d -> "S3057".equals(d.code())),
                () -> String.join("\n", checked.lines()));
    }

    @Test
    void checkProgram_inTheSubset_namesItsOwnLanguageAndVersion() {
        final SigmaSemantics.Result checked = check("Beeper.sg", SUBSET_SOUND, LanguageLevel.SIGMA, 1);
        assertTrue(checked.lines().stream().anyMatch(line -> line.contains("'Sound' needs Σ 2; this project is Σ 1")),
                () -> String.join("\n", checked.lines()));
    }

    @Test
    void checkProgram_atTheSecondVersion_takesSound() {
        final SigmaSemantics.Result checked = check("Beeper.sgs", SHARP_SOUND, LanguageLevel.SIGMA_SHARP, 2);
        assertTrue(checked.ok(), () -> String.join("\n", checked.lines()));
    }

    @Test
    void checkProgram_atTheFirstVersion_leavesAProgramsOwnSoundAlone() {
        final SigmaSemantics.Result checked = check("Beeper.sgs", OWN_SOUND, LanguageLevel.SIGMA_SHARP, 1);
        assertTrue(checked.ok(), () -> String.join("\n", checked.lines()));
    }

    @Test
    void compile_atTheFirstVersion_writesNothing() {
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Beeper.sgs", SHARP_SOUND)),
                "jsc:x86", LanguageLevel.SIGMA_SHARP, 1);
        assertFalse(built.ok());
    }

    private static SigmaSemantics.Result check(final String file, final String source, final LanguageLevel level,
                                               final int version) {
        return SigmaSemantics.checkProgram(List.of(new SourceFile(file, source)), level, version);
    }
}
