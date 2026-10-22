/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * What every request to the MCP server is checked for before anything reads it. It comes from this computer; it names
 * this computer as the host it asks, so a page whose own name was pointed at this computer (DNS rebinding) is
 * refused; it carries no origin, which only a web page's request does, so no site open in a browser reaches the game;
 * and what it sends is JSON, which a page cannot send to another site without asking it first.
 */
public final class McpGuard {

    /** The path the server answers messages at. */
    public static final String PATH = "/mcp";
    /** The path of the page of its tools, which a browser on this computer opens; read only. */
    public static final String TOOLS = "/tools";

    private static final Set<String> HOSTS = Set.of("127.0.0.1", "localhost", "[::1]");
    private static final String JSON = "application/json";

    private McpGuard() {
    }

    /**
     * Why {@code request}, which came from a peer on this computer or not, is refused by the server listening on
     * {@code port}; none when it may be read.
     */
    public static Optional<Refusal> check(final McpHttp.Request request, final int port, final boolean fromHere) {
        if (!fromHere) {
            return refused(403, "only this computer reaches the server");
        }
        final Optional<String> host = request.header("host");
        if (host.isEmpty() || !hostIsHere(host.get(), port)) {
            return refused(403, "the request must name this computer as its host");
        }
        if (request.header("origin").isPresent()) {
            return refused(403, "a request from a web page is refused");
        }
        if (TOOLS.equals(request.path())) {
            return "GET".equals(request.method()) ? Optional.empty() : refused(405, "the page is only read");
        }
        if (!PATH.equals(request.path())) {
            return refused(404, "the server answers " + PATH + " and its page at " + TOOLS + " only");
        }
        if ("POST".equals(request.method())) {
            final String type = request.header("content-type").orElse("").toLowerCase(Locale.ROOT);
            if (!type.equals(JSON) && !type.startsWith(JSON + ";")) {
                return refused(415, "a message is sent as application/json");
            }
        }
        return Optional.empty();
    }

    /** Whether {@code host}, a request's Host header, names this computer at {@code port}. */
    public static boolean hostIsHere(final String host, final int port) {
        final String lower = host.trim().toLowerCase(Locale.ROOT);
        final int colon = lower.lastIndexOf(':');
        if (colon < 0 || lower.endsWith("]")) {
            return false;
        }
        final String name = lower.substring(0, colon);
        final String number = lower.substring(colon + 1);
        return HOSTS.contains(name) && number.equals(String.valueOf(port));
    }

    private static Optional<Refusal> refused(final int status, final String why) {
        return Optional.of(new Refusal(status, why));
    }

    /**
     * A request refused.
     *
     * @param status the HTTP status it is answered with
     * @param why    why, in the protocol's English
     */
    public record Refusal(int status, String why) {
    }
}
