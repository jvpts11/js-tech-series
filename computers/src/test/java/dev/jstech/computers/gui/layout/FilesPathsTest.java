/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Which world a path in the explorer belongs to, and what it names there.
 *
 * <p>The explorer shows four worlds in one tree, and telling them apart by hand in the middle of the
 * drawing is how a folder ends up asking the wrong machine for its contents.
 */
class FilesPathsTest {

    @Test
    void ccComputer_readsTheNumberAndNothingElse() {
        assertEquals(0, FilesPaths.ccComputer("cc:"), "the list of computers is not a computer");
        assertEquals(3, FilesPaths.ccComputer("cc:3"));
        assertEquals(3, FilesPaths.ccComputer("cc:3/disk"));
        assertEquals(12, FilesPaths.ccComputer("cc:12/disk/deeper"));
        assertEquals(0, FilesPaths.ccComputer("cc:seven/disk"), "a name that is not a number asks nobody");
        assertEquals(0, FilesPaths.ccComputer("net:desk/pub"), "and another world is not this one");
    }

    @Test
    void ccPath_saysWhatToAskThatComputerFor() {
        assertEquals("/", FilesPaths.ccPath("cc:3"), "a computer on its own means its root");
        assertEquals("/", FilesPaths.ccPath("cc:3/"));
        assertEquals("/disk", FilesPaths.ccPath("cc:3/disk"));
        assertEquals("/disk/deeper", FilesPaths.ccPath("cc:3/disk/deeper"));
    }

    @Test
    void ccOf_andCcPath_areEachOthersOpposite() {
        assertEquals("cc:3/disk/deeper", FilesPaths.ccOf(3, "/disk/deeper"));
        assertEquals("cc:3/disk/deeper", FilesPaths.ccOf(3, "disk/deeper"));
        assertEquals("cc:3", FilesPaths.ccOf(3, "/"));
        assertEquals("cc:", FilesPaths.ccOf(0, "/anything"), "with no computer there is only the list");
        assertEquals("/disk/deeper", FilesPaths.ccPath(FilesPaths.ccOf(3, "/disk/deeper")));
    }

    @Test
    void ccName_showsTheComputerOrTheFolder() {
        assertEquals("Computer 3", FilesPaths.ccName("cc:3"));
        assertEquals("disk", FilesPaths.ccName("cc:3/disk"));
        assertEquals("deeper", FilesPaths.ccName("cc:3/disk/deeper"));
    }

    @Test
    void isCc_tellsTheWorldsApart() {
        assertTrue(FilesPaths.isCc("cc:3/disk"));
        assertFalse(FilesPaths.isCc("net:desk/pub"));
        assertFalse(FilesPaths.isCc("media:2/photos"));
        assertFalse(FilesPaths.isCc("Documents/notes.txt"));
        assertFalse(FilesPaths.isCc(null));
    }

    @Test
    void netDepth_andNetName_stillSayWhatTheyDid() {
        assertEquals(-1, FilesPaths.netDepth("Documents"));
        assertEquals(0, FilesPaths.netDepth("net:desk"));
        assertEquals(1, FilesPaths.netDepth("net:desk/pub"));
        assertEquals(2, FilesPaths.netDepth("net:desk/pub/deeper"));
        assertEquals("desk", FilesPaths.netName("net:desk"));
        assertEquals("pub", FilesPaths.netName("net:desk/pub"));
    }

    @Test
    void parentOf_goesUpWithinTheSameWorld() {
        assertEquals("cc:3/disk", FilesPaths.parentOf("cc:3/disk/deeper"));
        assertEquals("cc:3", FilesPaths.parentOf("cc:3/disk"));
        assertEquals("cc:", FilesPaths.parentOf("cc:3"), "above a computer is the list of computers");
        assertEquals("net:desk", FilesPaths.parentOf("net:desk/pub"));
        assertEquals("Documents", FilesPaths.parentOf("Documents/notes.txt"));
    }
}
