/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FileOpenersTest {

    private static final List<String> NOTHING = List.of();
    private static final List<String> EVERYTHING =
            List.of("virtual_studio_code", "virtual_studio", "exposure", "nms", "crafting_manager");

    @Test
    void defaultFor_opensASourceFileInACodeEditorWhenThereIsOne() {
        assertEquals("virtual_studio_code", FileOpeners.defaultFor("progs/a.sgs", EVERYTHING));
        assertEquals("virtual_studio_code", FileOpeners.defaultFor("progs/a.asm", EVERYTHING));
    }

    @Test
    void defaultFor_runsACompiledProgramWhenTheRuntimeIsThere() {
        // Opening a compiled program means running it; reading its listing is what "Open with" is for.
        assertEquals(FileOpeners.RUNTIME, FileOpeners.defaultFor("progs/a.asm", List.of("sigma", "exposure")));
        assertEquals("exposure", FileOpeners.defaultFor("progs/a.asm", List.of("exposure")));
    }

    @Test
    void defaultFor_fallsBackToThePlainEditorOnAMachineWithNoCodeEditor() {
        assertEquals(FileOpeners.EDITOR, FileOpeners.defaultFor("progs/a.sgs", NOTHING));
    }

    @Test
    void defaultFor_opensPlainTextInTheEditor() {
        assertEquals(FileOpeners.EDITOR, FileOpeners.defaultFor("notes.txt", EVERYTHING));
    }

    @Test
    void defaultFor_opensAQueryInTheStudioWhenItIsInstalled() {
        assertEquals("nms", FileOpeners.defaultFor("q.iql", EVERYTHING));
        assertEquals(FileOpeners.EDITOR, FileOpeners.defaultFor("q.iql", NOTHING));
    }

    @Test
    void defaultFor_saysNothingOpensAPatternWithoutItsProgram() {
        assertEquals("crafting_manager", FileOpeners.defaultFor("iron.craft", EVERYTHING));
        assertEquals("", FileOpeners.defaultFor("iron.craft", NOTHING));
    }

    @Test
    void defaultFor_saysNothingOpensAKindNobodyClaims() {
        assertEquals("", FileOpeners.defaultFor("noextension", EVERYTHING));
        assertEquals("", FileOpeners.defaultFor("trailing.", EVERYTHING));
        assertEquals("", FileOpeners.defaultFor(null, EVERYTHING));
    }

    @Test
    void defaultFor_refusesToOpenWhatCannotBeEdited() {
        /*
         * A .dat is a read-only view of what a drive is holding and an .exe is a program: handing either
         * to the text editor would offer to edit something the filesystem itself refuses to write.
         */
        for (final String path : List.of("items.dat", "setup.exe", "thing.bin", "disc.inf")) {
            assertEquals("", FileOpeners.defaultFor(path, EVERYTHING), path + " must open in nothing");
        }
    }

    @Test
    void available_ordersTheCodeEditorsBestFirst() {
        assertEquals(List.of("virtual_studio_code", "virtual_studio", "exposure", FileOpeners.EDITOR),
                FileOpeners.available("a.sgs", EVERYTHING));
    }

    /** The smaller language is a program like the other, so its sources and its projects open where theirs do. */
    @Test
    void available_opensTheSmallerLanguageWhereTheFullOneOpens() {
        assertEquals(FileType.SG, FileType.of("sg"));
        assertEquals(FileType.SGPROJ, FileType.of("SGPROJ"));
        assertEquals(FileOpeners.available("a.sgs", EVERYTHING), FileOpeners.available("a.sg", EVERYTHING));
        assertEquals(FileOpeners.available("a.sgsproj", EVERYTHING), FileOpeners.available("a.sgproj", EVERYTHING));
        assertEquals("virtual_studio", FileOpeners.defaultFor("Old/Old.sgproj", EVERYTHING));
        assertTrue(FileOpeners.creatable().contains(FileType.SG), "and one can be started from nothing as well");
    }

    @Test
    void available_keepsOnlyWhatTheMachineHas() {
        assertEquals(List.of("exposure", FileOpeners.EDITOR),
                FileOpeners.available("a.sgs", List.of("exposure")));
    }

    @Test
    void available_alwaysOffersThePlainEditorForSomethingReadable() {
        for (final String path : List.of("a.sgs", "a.asm", "a.txt", "a.iql", "a.cfg", "a.csv", "a.cmd", "a.log")) {
            assertTrue(FileOpeners.available(path, NOTHING).contains(FileOpeners.EDITOR),
                    path + " should still be readable on a bare machine");
        }
    }

    @Test
    void available_readsTheExtensionWhateverItsCase() {
        assertEquals(FileOpeners.available("A.SGS", EVERYTHING), FileOpeners.available("a.sgs", EVERYTHING));
    }

    @Test
    void creatable_offersOnlyTheKindsWorthMakingEmpty() {
        final List<FileType> kinds = FileOpeners.creatable();
        assertTrue(kinds.contains(FileType.TXT), "a text file is the plain case");
        assertTrue(kinds.contains(FileType.SGS), "a program has to be startable from nothing");
        assertFalse(kinds.contains(FileType.CRAFT), "a pattern is written by the encoder, not by hand");
        assertFalse(kinds.contains(FileType.ASM), "assembly is what the compiler produces");
        assertFalse(kinds.contains(FileType.DAT), "a projection of stored items is not a file to create");
    }

    @Test
    void creatable_offersOnlyKindsAPlayerCanThenEdit() {
        for (final FileType type : FileOpeners.creatable()) {
            assertTrue(type.userEditable(), type + " is offered for creation but cannot be edited");
        }
    }

    @Test
    void available_offersEveryProgramThatOpensAnyFileForAKindNobodyClaims() {
        assertEquals(List.of(FileOpeners.EDITOR, "virtual_studio_code", "virtual_studio", "exposure"),
                FileOpeners.available("thing.fk", EVERYTHING).subList(0, 4));
        assertEquals(List.of(FileOpeners.EDITOR), FileOpeners.available("thing.fk", NOTHING));
    }

    @Test
    void defaultFor_leavesAKindNobodyClaimsToThePlayer() {
        assertEquals("", FileOpeners.defaultFor("thing.fk", EVERYTHING));
        assertTrue(FileOpeners.isUnknownKind("thing.fk"));
        assertFalse(FileOpeners.isUnknownKind("notes.txt"));
    }

    @Test
    void defaultFor_opensAFileInTheProgramChosenForItsExtension() {
        assertEquals("virtual_studio",
                FileOpeners.defaultFor("thing.fk", EVERYTHING, Map.of("fk", "virtual_studio")));
        assertEquals(FileOpeners.EDITOR, FileOpeners.defaultFor("THING.FK", EVERYTHING, Map.of("fk", "jsc:editor")));
        assertEquals("virtual_studio_code",
                FileOpeners.defaultFor("notes.txt", EVERYTHING, Map.of("txt", "virtual_studio_code")),
                "a kind the machines know can be given another program too");
    }

    @Test
    void defaultFor_setsAsideAChoiceTheMachineCannotOpenTheFileWith() {
        assertEquals("", FileOpeners.defaultFor("thing.fk", NOTHING, Map.of("fk", "virtual_studio")),
                "a program that is not on the machine any more leaves the player to choose again");
        assertEquals("", FileOpeners.defaultFor("items.dat", EVERYTHING, Map.of("dat", FileOpeners.EDITOR)),
                "and a read-only view is never handed to an editor");
    }

    @Test
    void choices_addTheProgramsThatOpenAnyFileToAKindThatIsText() {
        assertEquals(List.of(FileOpeners.EDITOR, "virtual_studio_code", "virtual_studio", "exposure"),
                FileOpeners.choices("notes.txt", EVERYTHING).subList(0, 4));
        assertEquals(List.of(), FileOpeners.choices("items.dat", EVERYTHING));
        assertEquals(List.of("crafting_manager"), FileOpeners.choices("iron.craft", EVERYTHING));
    }

    @Test
    void registerAnyFileOpener_addsAProgramOnce() {
        FileOpeners.registerAnyFileOpener("test_viewer");
        FileOpeners.registerAnyFileOpener("test_viewer");
        assertEquals(1, FileOpeners.forPath("thing.fk").stream().filter("test_viewer"::equals).count());
        assertTrue(FileOpeners.available("thing.fk", List.of("test_viewer")).contains("test_viewer"));
    }

    @Test
    void extensionOf_readsTheFileNameOnly() {
        assertEquals("fk", FileOpeners.extensionOf("my.folder/Thing.FK"));
        assertEquals("", FileOpeners.extensionOf("my.folder/notes"));
        assertEquals("", FileOpeners.extensionOf("trailing."));
        assertEquals("", FileOpeners.extensionOf(null));
    }
}
