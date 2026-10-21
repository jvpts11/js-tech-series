/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class PairedClientsTest {

    @Test
    void pair_givesEachClientAKeyOfItsOwn() {
        final PairedClients clients = new PairedClients();
        final PairedClients.PairedClient first = clients.pair("a", "1", McpPermission.READ, null, 0);
        final PairedClients.PairedClient second = clients.pair("b", "1", McpPermission.ACT, null, 0);
        assertNotEquals(first.key(), second.key());
        assertTrue(first.key().matches("[0-9a-f]{64}"), first.key());
        assertEquals(Optional.of(second), clients.find(second.key()));
    }

    @Test
    void find_knowsNoKeyItDidNotGive() {
        final PairedClients clients = new PairedClients();
        final PairedClients.PairedClient client = clients.pair("a", "1", McpPermission.READ, null, 0);
        assertEquals(Optional.empty(), clients.find(client.key().substring(1) + "0"));
        assertEquals(Optional.empty(), clients.find(""));
        assertEquals(Optional.empty(), clients.find(null));
    }

    @Test
    void revoke_takesTheKeyBackAtOnce() {
        final PairedClients clients = new PairedClients();
        final PairedClients.PairedClient client = clients.pair("a", "1", McpPermission.READ, null, 0);
        clients.pair("b", "1", McpPermission.READ, null, 0);
        assertTrue(clients.revoke(client));
        assertEquals(Optional.empty(), clients.find(client.key()));
        assertEquals(1, clients.revokeAll());
        assertTrue(clients.all().isEmpty());
    }

    @Test
    void toString_neverWritesTheKey() {
        final PairedClients.PairedClient client = new PairedClients().pair("Claude Code", "2.1", McpPermission.ACT,
                null, 0);
        assertEquals("Claude Code 2.1", client.toString());
        assertFalse(client.toString().contains(client.key()));
    }

    @Test
    void allows_letsAnActingClientReadToo() {
        assertTrue(McpPermission.ACT.allows(McpPermission.READ));
        assertTrue(McpPermission.ACT.allows(McpPermission.ACT));
        assertTrue(McpPermission.READ.allows(McpPermission.READ));
        assertFalse(McpPermission.READ.allows(McpPermission.ACT));
    }
}
