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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class McpGuardTest {

    private static final int PORT = 25599;

    @Test
    void check_letsThroughAJsonPostFromHere() {
        assertEquals(Optional.empty(), McpGuard.check(post(Map.of()), PORT, true));
    }

    @Test
    void check_refusesAnotherComputer() {
        assertEquals(403, McpGuard.check(post(Map.of()), PORT, false).orElseThrow().status());
    }

    @Test
    void check_refusesAPageThatRenamedItselfAsThisComputer() {
        final Map<String, String> headers = new HashMap<>();
        headers.put("host", "evil.example:" + PORT);
        assertEquals(403, McpGuard.check(post(headers), PORT, true).orElseThrow().status());
    }

    @Test
    void check_refusesEveryRequestAWebPageSends() {
        final Map<String, String> headers = new HashMap<>();
        headers.put("origin", "https://example.com");
        assertEquals(403, McpGuard.check(post(headers), PORT, true).orElseThrow().status());
        headers.put("origin", "null");
        assertEquals(403, McpGuard.check(post(headers), PORT, true).orElseThrow().status());
    }

    @Test
    void check_refusesWhatIsNotJson() {
        final Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "text/plain");
        assertEquals(415, McpGuard.check(post(headers), PORT, true).orElseThrow().status());
    }

    @Test
    void check_refusesAnyOtherPath() {
        final McpHttp.Request request = new McpHttp.Request("POST", "/other", Map.of("host", "127.0.0.1:" + PORT,
                "content-type", "application/json"), new byte[0]);
        assertEquals(404, McpGuard.check(request, PORT, true).orElseThrow().status());
    }

    @Test
    void hostIsHere_takesTheNamesOfThisComputerAtItsPort() {
        assertTrue(McpGuard.hostIsHere("127.0.0.1:" + PORT, PORT));
        assertTrue(McpGuard.hostIsHere("LOCALHOST:" + PORT, PORT));
        assertTrue(McpGuard.hostIsHere("[::1]:" + PORT, PORT));
        assertFalse(McpGuard.hostIsHere("127.0.0.1:" + (PORT + 1), PORT));
        assertFalse(McpGuard.hostIsHere("127.0.0.1", PORT));
        assertFalse(McpGuard.hostIsHere("127.0.0.1.evil.example:" + PORT, PORT));
    }

    private static McpHttp.Request post(final Map<String, String> over) {
        final Map<String, String> headers = new HashMap<>();
        headers.put("host", "127.0.0.1:" + PORT);
        headers.put("content-type", "application/json; charset=utf-8");
        headers.putAll(over);
        return new McpHttp.Request("POST", "/mcp", Map.copyOf(headers), new byte[0]);
    }
}
