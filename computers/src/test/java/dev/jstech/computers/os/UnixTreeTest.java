/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class UnixTreeTest {

    @Test
    void of_givesSystemVItsOwnTreeAndTheOthersTheCommonOne() {
        assertSame(UnixTree.SYSTEM_V, UnixTree.of(Platform.UNIX));
        assertSame(UnixTree.HOME_AND_MEDIA, UnixTree.of(Platform.LINUX));
        assertSame(UnixTree.HOME_AND_MEDIA, UnixTree.of(Platform.FREEBSD));
    }

    @Test
    void homePath_isUnderUsrOnSystemVAndUnderHomeOnTheOthers() {
        assertEquals("usr/player", UnixTree.SYSTEM_V.homePath());
        assertEquals("home/player", UnixTree.HOME_AND_MEDIA.homePath());
        assertEquals("usr/player/Desktop", UnixTree.SYSTEM_V.desktopPath());
    }

    @Test
    void directories_holdTheHomeTheDesktopAndTheMountPointOfTheirOwnTree() {
        for (final UnixTree tree : List.of(UnixTree.SYSTEM_V, UnixTree.HOME_AND_MEDIA)) {
            assertTrue(tree.directories().contains(tree.homePath()), tree.toString());
            assertTrue(tree.directories().contains(tree.desktopPath()), tree.toString());
            assertTrue(tree.directories().contains(tree.mounts()), tree.toString());
        }
    }

    @Test
    void directories_listEveryParentBeforeItsChildren() {
        for (final UnixTree tree : List.of(UnixTree.SYSTEM_V, UnixTree.HOME_AND_MEDIA)) {
            final List<String> dirs = tree.directories();
            for (int i = 0; i < dirs.size(); i++) {
                final int slash = dirs.get(i).lastIndexOf('/');
                if (slash > 0) {
                    final int parent = dirs.indexOf(dirs.get(i).substring(0, slash));
                    assertTrue(parent >= 0 && parent < i, dirs.get(i) + " comes before its parent");
                }
            }
        }
    }

    @Test
    void systemV_hasNeitherAHomeNorAMediaFolderAtItsRoot() {
        assertFalse(UnixTree.SYSTEM_V.directories().contains("home"));
        assertFalse(UnixTree.SYSTEM_V.directories().contains("media"));
    }
}
