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

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class McpToolsPageTest {

    private static final McpTool READS = tool("block_read", "Reads a block.", McpSchema.object()
            .place("place", "Where the block is.", true).whole("depth", "How deep to read.", 1, 8, false).build(),
            McpPermission.READ);
    private static final McpTool ACTS = tool("othermod_paint", "Paints <b>a</b> block & more.", McpSchema.object()
            .oneOf("colour", "The colour.", List.of("red", "blue"), true).build(), McpPermission.ACT);
    private static final McpTool BARE = tool("wait_ticks", "Waits.", McpSchema.object().build(),
            McpPermission.READ);

    @Test
    void html_listsEachSidesToolsWithAnAnchorEach() {
        final String html = page();

        assertTrue(html.contains("id=\"server-block_read\""));
        assertTrue(html.contains("id=\"game-othermod_paint\""));
        assertTrue(html.contains("href=\"#game-wait_ticks\""));
        assertTrue(html.contains("(1 tool)") && html.contains("(2 tools)"));
    }

    @Test
    void html_saysWhoDeclaredEachWhetherItActsAndWhetherItIsOff() {
        final String html = page();

        assertTrue(html.contains("othermod &middot; <span class=\"act\">acts</span> &middot; <span class=\"off\">"));
        assertTrue(html.contains("jscore &middot; <span class=\"read\">reads</span></p>"));
    }

    @Test
    void html_escapesWhatTheModsWrote() {
        final String html = page();

        assertTrue(html.contains("Paints &lt;b&gt;a&lt;/b&gt; block &amp; more."));
        assertFalse(html.contains("<b>a</b>"));
    }

    @Test
    void html_givesEachArgumentItsTypeBoundsAndWhetherItMustBeGiven() {
        final String html = page();

        assertTrue(html.contains("<td><code>depth</code></td><td>integer, 1 to 8</td><td>if wanted</td>"));
        assertTrue(html.contains("<td><code>colour</code></td><td>string, one of red, blue</td><td>always</td>"));
        assertTrue(html.contains("It takes no arguments."));
    }

    @Test
    void arguments_readsTheSchemaAsTheToolWroteIt() {
        final List<McpToolsPage.Argument> arguments = McpToolsPage.arguments(READS.inputSchema());

        assertEquals(List.of(new McpToolsPage.Argument("place", "object", "with x, y, z", true,
                "Where the block is."), new McpToolsPage.Argument("depth", "integer", "1 to 8", false,
                "How deep to read.")), arguments);
    }

    @Test
    void html_saysASideWithNoToolsHasNone() {
        final String html = McpToolsPage.html(Map.of(McpSide.SERVER, List.of()), declared -> true);

        assertTrue(html.contains("None declared in this game."));
        assertFalse(html.contains("id=\"game\""));
    }

    private static String page() {
        return McpToolsPage.html(Map.of(McpSide.SERVER, List.of(new McpTools.Declared("jscore", READS)),
                McpSide.GAME, List.of(new McpTools.Declared("othermod", ACTS), new McpTools.Declared("jscore", BARE))),
                declared -> declared.modid().equals("jscore"));
    }

    private static McpTool tool(final String name, final String about, final JsonObject schema,
                                final McpPermission needs) {
        return new McpTool(name, about, schema, needs, call -> CompletableFuture.completedFuture(McpResult.text("")));
    }
}
