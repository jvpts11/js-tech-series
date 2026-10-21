/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.Nullable;

/**
 * Every call made to an MCP server, and every pairing: who, what, when, and what became of it, the newest first, a few
 * hundred kept. A call that acts is written as it begins, so the log shows it acting, and finished when it ends.
 */
public final class McpCallLog {

    private final Deque<Entry> entries = new ArrayDeque<>();
    private final AtomicLong next = new AtomicLong();
    private final int kept;

    /** How long a call's arguments are written, at most. */
    private static final int SUMMARY = 80;

    public McpCallLog(final int kept) {
        this.kept = kept;
    }

    /** Writes down what {@code client} did, with {@code what} of it, ended as {@code answer}; its number. */
    public synchronized long add(final String client, final String tool, final String what, final TextKey answer,
                                 @Nullable final Text why, final long now) {
        final Entry entry = new Entry(this.next.incrementAndGet(), now, client, tool, cut(what), answer, why);
        this.entries.addFirst(entry);
        while (this.entries.size() > this.kept) {
            this.entries.removeLast();
        }
        return entry.number();
    }

    /** Writes down how the call numbered {@code number} ended. */
    public synchronized void finish(final long number, final TextKey answer, @Nullable final Text why) {
        final List<Entry> all = new ArrayList<>(this.entries);
        this.entries.clear();
        for (final Entry entry : all) {
            this.entries.addLast(entry.number() == number ? new Entry(entry.number(), entry.millis(), entry.client(),
                    entry.tool(), entry.what(), answer, why) : entry);
        }
    }

    /** What was written, the newest first. */
    public synchronized List<Entry> entries() {
        return List.copyOf(this.entries);
    }

    /** The arguments of a call, written short: each value after its name. */
    public static String summary(final JsonObject args) {
        final StringBuilder out = new StringBuilder();
        args.entrySet().forEach(entry -> {
            if (!out.isEmpty()) {
                out.append(", ");
            }
            final JsonElement value = entry.getValue();
            out.append(value.isJsonPrimitive() ? value.getAsString() : value.toString());
        });
        return cut(out.toString());
    }

    private static String cut(final String what) {
        return what.length() <= SUMMARY ? what : what.substring(0, SUMMARY - 3) + "...";
    }

    /**
     * One call or pairing written down.
     *
     * @param number its number, the first one
     * @param millis when, in milliseconds since the epoch
     * @param client who made it
     * @param tool   the tool it called, or what it was
     * @param what   its arguments, written short
     * @param answer what became of it
     * @param why    why it was refused or failed, or none
     */
    public record Entry(long number, long millis, String client, String tool, String what, TextKey answer,
                        @Nullable Text why) {
    }
}
