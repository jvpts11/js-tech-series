/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.modpack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReferenceSiteTest {

    private static final ReferenceSite.Section DATA = new ReferenceSite.Section("data", "Kinds of data",
            "What the registries read.", List.of(new ReferenceSite.Entry("jscore:deposits",
            "data/<namespace>/jstech/deposits/<name>.json", "{\"vein\": \"<big>\"}")));
    private static final ReferenceSite.Section IDS = new ReferenceSite.Section("ids", "Ids", "Every id.",
            List.of(new ReferenceSite.Entry("mypack minecraft:item", "", "mypack:token")));

    @Test
    void files_holdTheFrontPageTheStyleAndAPageASection() {
        final Map<String, String> files = ReferenceSite.files(List.of(DATA, IDS));

        assertEquals(List.of(ReferenceSite.FRONT, ReferenceSite.STYLE, "sections/data.html", "sections/ids.html"),
                List.copyOf(files.keySet()));
    }

    @Test
    void front_leadsToEverySectionWithHowManyItLists() {
        final String front = ReferenceSite.files(List.of(DATA, IDS)).get(ReferenceSite.FRONT);

        assertTrue(front.contains("<a href=\"sections/data.html\">Kinds of data</a> (1)"), front);
        assertTrue(front.contains("<a href=\"sections/ids.html\">Ids</a> (1)"), front);
    }

    @Test
    void page_showsEachEntryWithWhereItGoesAndItsExample() {
        final String page = ReferenceSite.files(List.of(DATA)).get("sections/data.html");

        assertTrue(page.contains("<h2 id=\"jscore:deposits\">jscore:deposits</h2>"), page);
        assertTrue(page.contains("<code>data/&lt;namespace&gt;/jstech/deposits/&lt;name&gt;.json</code>"), page);
        assertTrue(page.contains("<pre><code>{&quot;vein&quot;: &quot;&lt;big&gt;&quot;}</code></pre>"), page);
        assertTrue(page.contains("href=\"../style.css\""), "a section reads the style beside the front page");
    }

    @Test
    void page_leavesOutWhereAndExampleWhenAnEntryHasNone() {
        final String page = ReferenceSite.files(List.of(new ReferenceSite.Section("empty", "Empty", "Nothing.",
                List.of(new ReferenceSite.Entry("mypack:thing", "", ""))))).get("sections/empty.html");

        assertFalse(page.contains("<pre>"), page);
        assertFalse(page.contains("<p><code>"), page);
    }
}
