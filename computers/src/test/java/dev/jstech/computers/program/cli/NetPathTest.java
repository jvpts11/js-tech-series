/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NetPathTest {

    @Test
    void parse_readsTheThreeSpellingsAsOnePath() {
        final NetPath dos = NetPath.parse("\\\\lab\\pub\\notes\\a.txt");
        final NetPath slashes = NetPath.parse("//lab/pub/notes/a.txt");
        final NetPath linux = NetPath.parse("/net/lab/pub/notes/a.txt");
        assertEquals(new NetPath("lab", "pub", "notes/a.txt"), dos);
        assertEquals(dos, slashes);
        assertEquals(dos, linux);
    }

    @Test
    void parse_namesTheNetworkAndAHostOnTheirOwn() {
        final NetPath network = NetPath.parse("\\\\");
        assertNotNull(network);
        assertTrue(network.isNetwork());
        assertTrue(NetPath.parse("/net").isNetwork());
        final NetPath host = NetPath.parse("\\\\lab\\");
        assertTrue(host.isHost());
        assertEquals("lab", host.host());
        assertTrue(NetPath.parse("//lab/pub/").rest().isEmpty(), "a trailing separator names the share's root");
    }

    @Test
    void parse_leavesOrdinaryPathsAlone() {
        assertNull(NetPath.parse("C:\\pub\\a.txt"));
        assertNull(NetPath.parse("/home/player/a.txt"));
        assertNull(NetPath.parse("pub/a.txt"));
        assertNull(NetPath.parse("/network/x"));
        assertFalse(NetPath.looksLike("\\pub"));
    }

    @Test
    void remotePath_joinsTheShareWithWhatLiesBelowIt() {
        final NetPath path = NetPath.parse("//lab/pub/notes/a.txt");
        assertEquals("C:\\pub\\notes\\a.txt", path.remotePath("C:\\pub"));
        assertEquals("C:\\notes\\a.txt", path.remotePath("C:\\"));
        assertEquals("C:\\pub", NetPath.parse("//lab/pub").remotePath("C:\\pub"));
    }

    @Test
    void display_andLinux_writeThePathEachWay() {
        final NetPath path = NetPath.parse("/net/lab/pub/notes/a.txt");
        assertEquals("\\\\lab\\pub\\notes\\a.txt", path.display());
        assertEquals("/net/lab/pub/notes/a.txt", path.linux());
        assertEquals("a.txt", path.name());
        assertEquals("pub", NetPath.parse("//lab/pub").name());
        assertEquals("lab", NetPath.parse("//lab").name());
    }

    @Test
    void parent_climbsToTheShareTheHostAndTheNetwork() {
        NetPath at = NetPath.parse("//lab/pub/notes/a.txt");
        at = at.parent();
        assertEquals("notes", at.rest());
        at = at.parent();
        assertEquals("", at.rest());
        at = at.parent();
        assertTrue(at.isHost());
        at = at.parent();
        assertTrue(at.isNetwork());
        assertTrue(at.parent().isNetwork(), "the network is its own parent");
    }

    @Test
    void onHost_ignoresCase() {
        assertTrue(NetPath.parse("//LAB/pub").onHost("lab"));
        assertEquals("pub", NetPath.key(" PUB "));
    }
}
