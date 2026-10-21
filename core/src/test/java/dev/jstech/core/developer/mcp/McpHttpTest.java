/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class McpHttpTest {

    @Test
    void read_takesARequestWithItsLength() throws IOException {
        final McpHttp.Request request = read("POST /mcp HTTP/1.1\r\nHost: 127.0.0.1:25599\r\n"
                + "Content-Type: application/json\r\nContent-Length: 2\r\n\r\n{}").orElseThrow();
        assertEquals("POST", request.method());
        assertEquals("/mcp", request.path());
        assertEquals(Optional.of("127.0.0.1:25599"), request.header("HOST"));
        assertEquals("{}", new String(request.body(), StandardCharsets.UTF_8));
    }

    @Test
    void read_joinsABodySentInChunks() throws IOException {
        final McpHttp.Request request = read("POST /mcp HTTP/1.1\r\nHost: a\r\nTransfer-Encoding: chunked\r\n\r\n"
                + "3\r\n{\"a\r\n4\r\n\":1}\r\n0\r\n\r\n").orElseThrow();
        assertEquals("{\"a\":1}", new String(request.body(), StandardCharsets.UTF_8));
    }

    @Test
    void read_isNothingWhenTheConnectionClosesFirst() throws IOException {
        assertEquals(Optional.empty(), read(""));
    }

    @Test
    void read_refusesAHeadTooLong() {
        final String header = "X-Long: " + "a".repeat(McpHttp.MOST_HEAD) + "\r\n";
        final McpHttp.Bad bad = assertThrows(McpHttp.Bad.class, () -> read("GET /mcp HTTP/1.1\r\n" + header
                + "\r\n"));
        assertEquals(431, bad.status());
    }

    @Test
    void read_refusesABodyTooLong() {
        final McpHttp.Bad bad = assertThrows(McpHttp.Bad.class, () -> read("POST /mcp HTTP/1.1\r\nHost: a\r\n"
                + "Content-Length: " + (McpHttp.MOST_BODY + 1) + "\r\n\r\n"));
        assertEquals(413, bad.status());
    }

    @Test
    void read_refusesAHostSentTwice() {
        final McpHttp.Bad bad = assertThrows(McpHttp.Bad.class, () -> read("GET /mcp HTTP/1.1\r\nHost: a\r\n"
                + "Host: b\r\n\r\n"));
        assertEquals(400, bad.status());
    }

    @Test
    void read_refusesWhatIsNoRequestLine() {
        assertThrows(McpHttp.Bad.class, () -> read("hello\r\n\r\n"));
    }

    @Test
    void read_refusesABodyShorterThanItsLength() {
        assertThrows(McpHttp.Bad.class, () -> read("POST /mcp HTTP/1.1\r\nContent-Length: 10\r\n\r\nabc"));
    }

    @Test
    void write_givesTheLengthAndWhetherTheConnectionStays() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        McpHttp.write(out, McpHttp.Response.json(200, "{}", Map.of("Mcp-Session-Id", "k")), false);
        final String written = out.toString(StandardCharsets.US_ASCII);
        assertTrue(written.startsWith("HTTP/1.1 200 OK\r\n"), written);
        assertTrue(written.contains("Content-Length: 2\r\n"), written);
        assertTrue(written.contains("Connection: keep-alive\r\n"), written);
        assertTrue(written.contains("Mcp-Session-Id: k\r\n"), written);
        assertTrue(written.endsWith("\r\n\r\n{}"), written);
    }

    private static Optional<McpHttp.Request> read(final String raw) throws IOException {
        return McpHttp.read(new ByteArrayInputStream(raw.getBytes(StandardCharsets.ISO_8859_1)));
    }
}
