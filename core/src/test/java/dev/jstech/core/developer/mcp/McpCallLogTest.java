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

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

class McpCallLogTest {

    @Test
    void add_keepsTheNewestFirstAndOnlyAsManyAsKept() {
        final McpCallLog log = new McpCallLog(2);
        log.add("a", "one", "", McpWords.DONE, null, 1);
        log.add("a", "two", "", McpWords.DONE, null, 2);
        log.add("a", "three", "", McpWords.DONE, null, 3);
        assertEquals(2, log.entries().size());
        assertEquals("three", log.entries().getFirst().tool());
    }

    @Test
    void finish_writesHowACallThatActsEnded() {
        final McpCallLog log = new McpCallLog(4);
        final long number = log.add("a", "files_write", "x", McpWords.ACTING, null, 1);
        log.add("a", "block_read", "", McpWords.DONE, null, 2);
        log.finish(number, McpWords.FAILED, McpWords.NO_TOOL.with("x"));
        final McpCallLog.Entry finished = log.entries().get(1);
        assertEquals(McpWords.FAILED, finished.answer());
        assertEquals("files_write", finished.tool());
    }

    @Test
    void summary_writesTheValuesShort() {
        final JsonObject args = new JsonObject();
        args.addProperty("path", "mod:a/" + "b".repeat(200));
        args.addProperty("rolls", 10);
        final String summary = McpCallLog.summary(args);
        assertTrue(summary.length() <= 80, summary);
        assertTrue(summary.endsWith("..."), summary);
    }
}
