/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Just as much of HTTP/1.1 as the MCP server speaks: a request read from a connection, its line, its headers and a body
 * of a length given or sent in chunks, each within a limit, and a response written back. Anything else is refused with
 * the status that says why, before a byte of a body is kept.
 */
public final class McpHttp {

    /** The most a request's line and headers may take, and its body. */
    public static final int MOST_HEAD = 16 * 1024;
    public static final int MOST_BODY = 4 * 1024 * 1024;

    private static final int MOST_HEADERS = 64;
    private static final byte CR = '\r';
    private static final byte LF = '\n';
    private static final String CHUNKED = "chunked";

    private McpHttp() {
    }

    /**
     * The next request on {@code in}, or none when the connection closed before one began.
     *
     * @throws Bad when what came is no request this server takes
     */
    public static Optional<Request> read(final InputStream in) throws IOException {
        final String line = line(in, MOST_HEAD, true);
        if (line == null) {
            return Optional.empty();
        }
        final String[] parts = line.split(" ");
        if (parts.length != 3 || !parts[2].startsWith("HTTP/1.")) {
            throw new Bad(400, "not an HTTP/1.1 request line");
        }
        final Map<String, String> headers = new LinkedHashMap<>();
        int used = line.length();
        while (true) {
            final String header = line(in, MOST_HEAD - used, false);
            if (header == null) {
                throw new Bad(400, "the headers end before their blank line");
            }
            if (header.isEmpty()) {
                break;
            }
            used += header.length() + 2;
            final int colon = header.indexOf(':');
            if (colon <= 0 || headers.size() >= MOST_HEADERS) {
                throw new Bad(400, "a header that cannot be read");
            }
            final String name = header.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            final String value = header.substring(colon + 1).trim();
            // A header sent twice is joined, as HTTP joins lists; Host and the length must come once.
            if (headers.containsKey(name) && ("host".equals(name) || "content-length".equals(name))) {
                throw new Bad(400, "the header " + name + " is sent twice");
            }
            headers.merge(name, value, (was, more) -> was + ", " + more);
        }
        final byte[] body;
        if (CHUNKED.equalsIgnoreCase(headers.getOrDefault("transfer-encoding", ""))) {
            body = chunks(in);
        } else if (headers.containsKey("transfer-encoding")) {
            throw new Bad(501, "only chunked bodies are read");
        } else {
            body = sized(in, headers.get("content-length"));
        }
        return Optional.of(new Request(parts[0], parts[1], Map.copyOf(headers), body));
    }

    /** Writes {@code response} to {@code out}, its length given, keeping the connection open or not. */
    public static void write(final OutputStream out, final Response response, final boolean close)
            throws IOException {
        final StringBuilder head = new StringBuilder("HTTP/1.1 ").append(response.status()).append(' ')
                .append(reason(response.status())).append("\r\n");
        response.headers().forEach((name, value) -> head.append(name).append(": ").append(value).append("\r\n"));
        head.append("Content-Length: ").append(response.body().length).append("\r\n");
        head.append("Connection: ").append(close ? "close" : "keep-alive").append("\r\n\r\n");
        out.write(head.toString().getBytes(StandardCharsets.US_ASCII));
        out.write(response.body());
        out.flush();
    }

    /* A line up to its CRLF, of at most {@code most} bytes; none when the stream ends before any byte of it. */
    private static String line(final InputStream in, final int most, final boolean first) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int previous = -1;
        while (true) {
            final int b = in.read();
            if (b < 0) {
                if (bytes.size() == 0 && first) {
                    return null;
                }
                throw new Bad(400, "the connection closed in the middle of a line");
            }
            if (b == LF && previous == CR) {
                final byte[] read = bytes.toByteArray();
                return new String(read, 0, read.length - 1, StandardCharsets.ISO_8859_1);
            }
            bytes.write(b);
            previous = b;
            if (bytes.size() > most) {
                throw new Bad(431, "the request's head is too long");
            }
        }
    }

    private static byte[] sized(final InputStream in, final String length) throws IOException {
        if (length == null) {
            return new byte[0];
        }
        final long size;
        try {
            size = Long.parseLong(length.trim());
        } catch (final NumberFormatException wrong) {
            throw new Bad(400, "a length that is no number");
        }
        if (size < 0) {
            throw new Bad(400, "a length below nothing");
        }
        if (size > MOST_BODY) {
            throw new Bad(413, "the body is longer than " + MOST_BODY + " bytes");
        }
        final byte[] body = in.readNBytes((int) size);
        if (body.length != size) {
            throw new Bad(400, "the body ends before its length");
        }
        return body;
    }

    private static byte[] chunks(final InputStream in) throws IOException {
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        while (true) {
            final String sizeLine = line(in, 64, false);
            final int semicolon = sizeLine.indexOf(';');
            final int size;
            try {
                size = Integer.parseInt((semicolon < 0 ? sizeLine : sizeLine.substring(0, semicolon)).trim(), 16);
            } catch (final NumberFormatException wrong) {
                throw new Bad(400, "a chunk's size that is no number");
            }
            if (size < 0 || body.size() + (long) size > MOST_BODY) {
                throw new Bad(413, "the body is longer than " + MOST_BODY + " bytes");
            }
            if (size == 0) {
                // The trailer, if any, up to its blank line.
                String trailer = line(in, MOST_HEAD, false);
                while (!trailer.isEmpty()) {
                    trailer = line(in, MOST_HEAD, false);
                }
                return body.toByteArray();
            }
            final byte[] chunk = in.readNBytes(size);
            if (chunk.length != size || !line(in, 2, false).isEmpty()) {
                throw new Bad(400, "a chunk ends before its size");
            }
            body.write(chunk);
        }
    }

    private static String reason(final int status) {
        return switch (status) {
            case 200 -> "OK";
            case 202 -> "Accepted";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 413 -> "Content Too Large";
            case 415 -> "Unsupported Media Type";
            case 429 -> "Too Many Requests";
            case 431 -> "Request Header Fields Too Large";
            case 501 -> "Not Implemented";
            case 503 -> "Service Unavailable";
            default -> "Error";
        };
    }

    /**
     * A request.
     *
     * @param method  its method, as sent
     * @param target  what it asks for, its path and query
     * @param headers its headers, each name in lower case
     * @param body    its body, none when it sent none
     */
    public record Request(String method, String target, Map<String, String> headers, byte[] body) {

        /** The header {@code name}, given in any case, or none. */
        public Optional<String> header(final String name) {
            return Optional.ofNullable(this.headers.get(name.toLowerCase(Locale.ROOT)));
        }

        /** Its path, its query left out. */
        public String path() {
            final int query = this.target.indexOf('?');
            return query < 0 ? this.target : this.target.substring(0, query);
        }
    }

    /**
     * A response.
     *
     * @param status  its status
     * @param headers its headers, the length and the connection's left to {@link #write}
     * @param body    its body
     */
    public record Response(int status, Map<String, String> headers, byte[] body) {

        /** A response of {@code status} with no body. */
        public static Response empty(final int status) {
            return new Response(status, Map.of(), new byte[0]);
        }

        /** A response of {@code status} with {@code json} as its body. */
        public static Response json(final int status, final String json) {
            return json(status, json, Map.of());
        }

        /** A response of {@code status} with {@code json} as its body and the headers {@code more}. */
        public static Response json(final int status, final String json, final Map<String, String> more) {
            final Map<String, String> headers = new LinkedHashMap<>(more);
            headers.put("Content-Type", "application/json; charset=utf-8");
            return new Response(status, Map.copyOf(headers), json.getBytes(StandardCharsets.UTF_8));
        }

        /** A plain response of {@code status} saying {@code why}. */
        public static Response text(final int status, final String why) {
            return new Response(status, Map.of("Content-Type", "text/plain; charset=utf-8"),
                    why.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** What was sent is no request this server takes: the status that says so, and why. */
    public static final class Bad extends IOException {

        private final int status;

        public Bad(final int status, final String why) {
            super(why);
            this.status = status;
        }

        public int status() {
            return this.status;
        }
    }
}
