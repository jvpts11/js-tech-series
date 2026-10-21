/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The MCP server's sockets: bound to this computer's loopback addresses and nothing else, 127.0.0.1 and, where the
 * computer has it, ::1, which a client naming {@code localhost} may reach first, so no other machine can reach it
 * whatever the firewall says; each connection served on a thread of its own, a few at a time; every request checked by
 * {@link McpGuard} before {@link McpProtocol} reads it.
 */
public final class McpServer {

    private final McpProtocol protocol;
    private final List<ServerSocket> sockets;
    private final IMcpLogger logger;
    private final Semaphore room = new Semaphore(CONNECTIONS);
    private final AtomicBoolean running = new AtomicBoolean(true);

    /** How many connections are served at once, and how long one may stay silent, in milliseconds. */
    private static final int CONNECTIONS = 16;
    private static final int SILENCE = 30_000;
    private static final byte[] LOOPBACK_V4 = {127, 0, 0, 1};
    private static final byte[] LOOPBACK_V6 = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};

    private McpServer(final McpProtocol protocol, final List<ServerSocket> sockets) {
        this.protocol = protocol;
        this.sockets = sockets;
        this.logger = protocol.logger();
    }

    /**
     * Starts a server answering with {@code protocol} on this computer's port {@code port}, or any free one for 0.
     *
     * @throws IOException when the port cannot be taken
     */
    public static McpServer start(final McpProtocol protocol, final int port) throws IOException {
        final ServerSocket v4 = new ServerSocket(port, CONNECTIONS, InetAddress.getByAddress(LOOPBACK_V4));
        final List<ServerSocket> sockets = new ArrayList<>(List.of(v4));
        try {
            sockets.add(new ServerSocket(v4.getLocalPort(), CONNECTIONS, InetAddress.getByAddress(LOOPBACK_V6)));
        } catch (final IOException noSixHere) {
            // A computer with no IPv6 loopback is reached at 127.0.0.1 alone.
        }
        final McpServer server = new McpServer(protocol, List.copyOf(sockets));
        for (final ServerSocket socket : server.sockets) {
            final Thread acceptor = new Thread(() -> server.accept(socket), "J's Core MCP server");
            acceptor.setDaemon(true);
            acceptor.start();
        }
        server.logger.info("The MCP server listens at http://127.0.0.1:" + server.port() + McpGuard.PATH);
        return server;
    }

    /** The port it listens on. */
    public int port() {
        return this.sockets.getFirst().getLocalPort();
    }

    /** Whether it still listens. */
    public boolean running() {
        return this.running.get();
    }

    /** The protocol it answers with. */
    public McpProtocol protocol() {
        return this.protocol;
    }

    /** Stops listening, and closes the connections as their requests end. */
    public void stop() {
        if (this.running.compareAndSet(true, false)) {
            for (final ServerSocket socket : this.sockets) {
                try {
                    socket.close();
                } catch (final IOException ignored) {
                    // Closing a socket that is closing already says nothing new.
                }
            }
            this.logger.info("The MCP server stopped");
        }
    }

    private void accept(final ServerSocket socket) {
        while (this.running.get()) {
            final Socket connection;
            try {
                connection = socket.accept();
            } catch (final IOException closed) {
                if (this.running.get()) {
                    this.logger.warn("The MCP server could not take a connection", closed);
                }
                continue;
            }
            if (!this.room.tryAcquire()) {
                // Too many at once: the newest waits for nothing and is closed.
                close(connection);
                continue;
            }
            Thread.ofVirtual().name("J's Core MCP connection").start(() -> {
                try {
                    this.serve(connection);
                } finally {
                    this.room.release();
                    close(connection);
                }
            });
        }
    }

    private void serve(final Socket connection) {
        final boolean fromHere = connection.getInetAddress().isLoopbackAddress();
        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            connection.setSoTimeout(SILENCE);
            while (this.running.get()) {
                final Optional<McpHttp.Request> read;
                try {
                    read = McpHttp.read(in);
                } catch (final McpHttp.Bad bad) {
                    McpHttp.write(out, McpHttp.Response.text(bad.status(), bad.getMessage()), true);
                    return;
                }
                if (read.isEmpty()) {
                    return;
                }
                final McpHttp.Request request = read.get();
                final Optional<McpGuard.Refusal> refusal = McpGuard.check(request, this.port(), fromHere);
                if (refusal.isPresent()) {
                    McpHttp.write(out, McpHttp.Response.text(refusal.get().status(), refusal.get().why()), true);
                    return;
                }
                final boolean close = request.header("connection").map(value -> value.equalsIgnoreCase("close"))
                        .orElse(false);
                McpHttp.write(out, this.protocol.handle(request), close);
                if (close) {
                    return;
                }
            }
        } catch (final SocketTimeoutException | SocketException ended) {
            // A client that went quiet or away; its connection is closed.
        } catch (final IOException failed) {
            this.logger.warn("An MCP connection failed", failed);
        }
    }

    private static void close(final Socket connection) {
        try {
            connection.close();
        } catch (final IOException ignored) {
            // Closed already.
        }
    }
}
