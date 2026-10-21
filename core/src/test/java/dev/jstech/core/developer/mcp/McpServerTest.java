/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.text.Text;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class McpServerTest {

    private McpServer server;

    private static final String INITIALIZE = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":"
            + "{\"protocolVersion\":\"2025-06-18\",\"clientInfo\":{\"name\":\"socket\",\"version\":\"1\"}}}";

    @BeforeEach
    void setUp() throws IOException {
        this.server = McpServer.start(new McpProtocol(new Host(), System::currentTimeMillis, IMcpLogger.quiet()),
                0);
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
    }

    @Test
    void start_listensOnThisComputerOnly() throws IOException {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), this.server.port())) {
            assertTrue(socket.getInetAddress().isLoopbackAddress());
        }
    }

    @Test
    void aClient_pairsAndCallsOverTheWire() throws IOException {
        final String paired = this.exchange("POST /mcp HTTP/1.1\r\nHost: 127.0.0.1:" + this.server.port()
                + "\r\nContent-Type: application/json\r\nAccept: application/json, text/event-stream\r\n"
                + "Connection: close\r\nContent-Length: " + INITIALIZE.length() + "\r\n\r\n" + INITIALIZE);
        assertTrue(paired.startsWith("HTTP/1.1 200"), paired);
        final String key = paired.lines().filter(line -> line.startsWith(McpProtocol.SESSION + ":")).findFirst()
                .orElseThrow().substring(McpProtocol.SESSION.length() + 1).strip();
        final String ping = "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"ping\"}";
        final String answered = this.exchange("POST /mcp HTTP/1.1\r\nHost: localhost:" + this.server.port()
                + "\r\nContent-Type: application/json\r\nMcp-Session-Id: " + key + "\r\nConnection: close\r\n"
                + "Content-Length: " + ping.length() + "\r\n\r\n" + ping);
        assertTrue(answered.startsWith("HTTP/1.1 200") && answered.contains("\"result\":{}"), answered);
    }

    @Test
    void aWebPage_isRefusedBeforeItIsRead() throws IOException {
        final String refused = this.exchange("POST /mcp HTTP/1.1\r\nHost: 127.0.0.1:" + this.server.port()
                + "\r\nOrigin: https://example.com\r\nContent-Type: application/json\r\nContent-Length: "
                + INITIALIZE.length() + "\r\n\r\n" + INITIALIZE);
        assertTrue(refused.startsWith("HTTP/1.1 403"), refused);
    }

    @Test
    void aRenamedHost_isRefused() throws IOException {
        final String refused = this.exchange("POST /mcp HTTP/1.1\r\nHost: attacker.example:" + this.server.port()
                + "\r\nContent-Type: application/json\r\nContent-Length: 2\r\n\r\n{}");
        assertTrue(refused.startsWith("HTTP/1.1 403"), refused);
    }

    @Test
    void aStream_isNotOffered() throws IOException {
        final String answered = this.exchange("GET /mcp HTTP/1.1\r\nHost: 127.0.0.1:" + this.server.port()
                + "\r\nAccept: text/event-stream\r\nConnection: close\r\n\r\n");
        assertTrue(answered.startsWith("HTTP/1.1 405"), answered);
    }

    @Test
    void stop_givesThePortBack() throws IOException {
        final int port = this.server.port();
        this.server.stop();
        assertEquals(false, this.server.running());
        final McpServer again = McpServer.start(new McpProtocol(new Host(), System::currentTimeMillis,
                IMcpLogger.quiet()), port);
        try {
            assertEquals(port, again.port());
        } finally {
            again.stop();
        }
    }

    /* Sends {@code raw} and reads every byte the server answers until it closes the connection. */
    private String exchange(final String raw) throws IOException {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), this.server.port())) {
            socket.setSoTimeout(5_000);
            final OutputStream out = socket.getOutputStream();
            out.write(raw.getBytes(StandardCharsets.UTF_8));
            out.flush();
            final InputStream in = socket.getInputStream();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** A game that lets every client in to read. */
    private static final class Host implements IMcpHost {

        @Override
        public McpSide side() {
            return McpSide.GAME;
        }

        @Override
        public String version() {
            return "0.0.0";
        }

        @Override
        public Optional<Text> closed() {
            return Optional.empty();
        }

        @Override
        public CompletableFuture<Pairing> pair(final String name, final String version) {
            return CompletableFuture.completedFuture(Pairing.allowed(McpPermission.READ, null));
        }

        @Override
        public void began(final McpCall call) {
            // Nothing to show.
        }

        @Override
        public void ended(final McpCall call) {
            // Nothing to show.
        }
    }
}
