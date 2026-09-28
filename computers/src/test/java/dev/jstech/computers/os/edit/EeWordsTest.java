/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.text.Text;
import java.util.List;
import org.junit.jupiter.api.Test;

class EeWordsTest {

    /** Rows a small terminal glass still gives an editor's page, well under a full-size console's own 24. */
    private static final int SMALLEST_CONSOLE_ROWS = 15;

    @Test
    void opened_quotesTheNameAndCountsTheFile() {
        assertEquals("\"notes.txt\" 3 lines, 72 characters", EeWords.opened("notes.txt", 3, 72).english());
    }

    @Test
    void newFile_quotesTheNameAndSaysItIsNew() {
        assertEquals("\"notes.txt\" [New file]", EeWords.newFile("notes.txt").english());
    }

    @Test
    void asking_namesTheQuestionAndWhatWasTyped() {
        assertEquals("Search: hel", EeWords.asking(EeWords.SEARCH_PROMPT_LABEL, "hel").english());
        assertEquals("ASCII number: 65", EeWords.asking(EeWords.ASCII_PROMPT_LABEL, "65").english());
        assertEquals("Command: wq", EeWords.asking(EeWords.COMMAND_PROMPT_LABEL, "wq").english());
    }

    @Test
    void unknownCommand_namesWhatWasTyped() {
        assertTrue(EeWords.unknownCommand("zz").english().contains("zz"));
    }

    @Test
    void notFound_namesWhatWasLookedFor() {
        assertTrue(EeWords.notFound("clock").english().contains("clock"));
    }

    @Test
    void cannotRead_namesTheFileThatWasNotThere() {
        assertTrue(EeWords.cannotRead("missing.txt").english().contains("missing.txt"));
    }

    @Test
    void position_namesTheLineTheColumnAndHowFarFromTheTop() {
        assertEquals("=====line 3 col 27 lines from top 3 ", EeWords.position(3, 27, 3).english());
    }

    @Test
    void keys_areFiveRowsOfFiveColumnsEach() {
        assertEquals(5, EeWords.KEYS.size());
        for (final List<TtyLook.Key> row : EeWords.KEYS) {
            assertEquals(5, row.size(), "every row keeps the same columns so they all line up");
        }
    }

    @Test
    void menuItems_areTheRealOnesInLetterOrder() {
        assertEquals(6, EeWords.MENU_ITEMS.size());
        assertEquals("leave editor", EeWords.MENU_ITEMS.get(0).english());
        assertEquals("settings", EeWords.MENU_ITEMS.get(4).english());
        assertEquals("search", EeWords.MENU_ITEMS.get(5).english());
    }

    @Test
    void leaveItems_areTheTwoAnswersToLeaving() {
        assertEquals(2, EeWords.LEAVE_ITEMS.size());
        assertEquals("save changes", EeWords.LEAVE_ITEMS.get(0).english());
        assertEquals("no save", EeWords.LEAVE_ITEMS.get(1).english());
    }

    @Test
    void fileOpItems_areTheTwoEeCanReallyDoInEesOwnOrder() {
        assertEquals(2, EeWords.FILE_OP_ITEMS.size());
        assertEquals("read a file", EeWords.FILE_OP_ITEMS.get(0).english());
        assertEquals("save file", EeWords.FILE_OP_ITEMS.get(1).english());
    }

    @Test
    void settingsItems_areTheOneEeSettingThisEditorCanHonour() {
        assertEquals(2, EeWords.SETTINGS_ITEMS.size());
        assertEquals("info window: on", EeWords.SETTINGS_ITEMS.get(0).english());
        assertEquals("info window: off", EeWords.SETTINGS_ITEMS.get(1).english());
    }

    @Test
    void helpPage_namesEveryKeyTheTopRowsListTwoToARow() {
        // The title, a blank line, and the 22 chorded keys the five rows on top list (their two blanks and
        // the exit tip carry no chord of their own, so a pair would name nothing for them) packed two to a
        // row: eleven rows rather than twenty-two, which is what keeps the whole page on a small glass.
        assertEquals(13, EeWords.HELP_PAGE.size());
        final String joined = EeWords.HELP_PAGE.stream().map(Text::english).reduce("", (a, b) -> a + "\n" + b);
        assertTrue(joined.contains("^[") && joined.contains("(escape) menu"), joined);
        assertTrue(joined.contains("^c") && joined.contains("command"), joined);
    }

    @Test
    void helpPage_fitsTheSmallestConsoleWithRoomToSpare() {
        assertTrue(EeWords.HELP_PAGE.size() <= SMALLEST_CONSOLE_ROWS,
                "a page this long can run past a small glass, hiding rows the wheel alone would have to reach");
    }

    /** How to leave the page is said on the row under it instead, which never scrolls out of view. */
    @Test
    void helpContinue_isSaidOnTheRowThatNeverScrolls() {
        assertTrue(EeWords.HELP_CONTINUE.text().english().contains("press any key to continue"));
    }
}
