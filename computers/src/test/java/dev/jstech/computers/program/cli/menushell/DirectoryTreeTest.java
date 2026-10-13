/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.menushell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class DirectoryTreeTest {

    private static final List<String> FOLDERS = List.of("C:\\DOS", "C:\\DOS\\TOOLS", "C:\\NETMGR", "C:\\SIGMA");

    @Test
    void rows_showTheRootOpenWithItsFoldersJoinedByLines() {
        final List<DirectoryTree.Row> rows = new DirectoryTree("C:\\", FOLDERS).rows();
        assertEquals(4, rows.size());
        assertEquals("C:\\", rows.get(0).name());
        assertEquals("├─", rows.get(1).lead());
        assertEquals("DOS", rows.get(1).name());
        assertTrue(rows.get(1).hasChildren());
        assertFalse(rows.get(1).open());
        assertEquals("└─", rows.get(3).lead());
        assertEquals("SIGMA", rows.get(3).name());
    }

    @Test
    void expand_showsAFoldersFoldersUnderIt() {
        final DirectoryTree tree = new DirectoryTree("C:\\", FOLDERS);
        tree.expand("C:\\DOS");
        final List<DirectoryTree.Row> rows = tree.rows();
        assertEquals("TOOLS", rows.get(2).name());
        assertEquals("│ └─", rows.get(2).lead());
        assertEquals(2, rows.get(2).depth());
    }

    @Test
    void collapse_putsAwayEverythingUnderTheFolder() {
        final DirectoryTree tree = new DirectoryTree("C:\\", FOLDERS);
        tree.expandAll();
        assertEquals(5, tree.rows().size());
        tree.collapse("C:\\");
        assertEquals(1, tree.rows().size());
    }

    @Test
    void reveal_opensTheWayDownToAFolder() {
        final DirectoryTree tree = new DirectoryTree("C:\\", FOLDERS);
        tree.collapse("C:\\");
        tree.reveal("C:\\DOS\\TOOLS");
        assertTrue(tree.isOpen("C:\\"));
        assertTrue(tree.isOpen("c:\\dos"));
        assertEquals(5, tree.rows().size());
    }
}
