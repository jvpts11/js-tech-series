/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GuideSiteTest {

    private static final GuideManual MANUAL = new GuideManual("test:manual", "manual.title", List.of(), "", "",
            "test:style", List.of(GuideManual.EVERY_CHAPTER), List.of(), 0, "");
    /** The press's one recipe, in words. */
    private static final IRecipeWords PRESSING = (type, output) -> type.equals("test:pressing")
            ? List.of(new IRecipeWords.RecipeWords(List.of("Block of Iron"), List.of("Iron Ingot", "Coal"),
                    "Test Press", "minecraft:iron_block"))
            : List.of();

    @Test
    void files_holdTheFrontPageTheStyleTheSearchAndAPageAnEntry() {
        final Map<String, String> files = site();

        assertEquals(List.of(GuideSite.FRONT, GuideSite.STYLE, GuideSite.SEARCH, "entries/alpha/press.html",
                "entries/alpha/tips/belt.html"), List.copyOf(files.keySet()));
    }

    @Test
    void files_frontPageListsTheContentsWithLinksToEachEntry() {
        final String front = site().get(GuideSite.FRONT);

        assertTrue(front.contains("<a href=\"entries/alpha/press.html\"><span class=\"number\">1.1.1</span> "
                + "press.title</a>"), front);
        assertTrue(front.contains("<li id=\"alpha-machines\">"), "a section can be linked to on the front page");
        assertTrue(front.contains("<script src=\"search.js\"></script>"), "the front page searches");
    }

    @Test
    void files_entryPageWritesItsWordsSafelyAndLeadsBackAndOn() {
        final String press = site().get("entries/alpha/press.html");

        assertTrue(press.contains("<p>Feed it &lt;coal&gt; &amp; iron.</p>"), "what an entry says is escaped");
        assertTrue(press.contains("href=\"../../style.css\""), "a page two folders in reads the style from the top");
        assertTrue(press.contains("<a class=\"home\" href=\"../../index.html\">"), "and leads back to the front");
        assertTrue(press.contains("<a rel=\"next\" href=\"../../entries/alpha/tips/belt.html\">"),
                "and on to the next entry");
        assertFalse(press.contains("rel=\"prev\""), "the first entry has none before it");
    }

    @Test
    void files_entryPageWritesItsRecipesInWords() {
        final String press = site().get("entries/alpha/press.html");

        assertTrue(press.contains("<li>jscore.guide.site.recipe_by[Block of Iron,Iron Ingot, Coal,Test Press]</li>"),
                press);
    }

    @Test
    void files_linkToAnotherEntryGoesToItsPageFromWhereverItIs() {
        final String belt = site().get("entries/alpha/tips/belt.html");

        assertTrue(belt.contains("<a href=\"../../../entries/alpha/press.html\">"), belt);
    }

    @Test
    void files_searchHoldsEveryEntrysWordsAndItsRecipes() {
        final String search = site().get(GuideSite.SEARCH);

        assertTrue(search.contains("u: \"entries\\/alpha\\/press.html\""), "each entry's page, its slashes escaped");
        assertTrue(search.contains("block of iron iron ingot coal test press"), "the recipe's words, in lower case");
        assertTrue(search.contains("const FOUND = \"jscore.guide.found\";"), "the count's words");
    }

    private static Map<String, String> site() {
        final GuideEntry press = new GuideEntry("alpha:press", "alpha:machines", 0, "press.title", "", List.of(),
                List.of(new GuideBlock.Paragraph("Feed it <coal> & iron."),
                        new GuideBlock.Recipes("test:pressing", "")));
        final GuideEntry belt = new GuideEntry("alpha:tips/belt", "alpha:machines", 1, "belt.title", "", List.of(),
                List.of(new GuideBlock.SeeAlso(List.of("alpha:press"))));
        final ManualReader reader = new ManualReader(MANUAL, new GuideContents(List.of(new GuideContents.Chapter(
                new GuideChapter("alpha", 0, "alpha.title", "", ""), List.of(new GuideContents.Section(
                        new GuideSection("alpha:machines", 0, "machines.title", ""), List.of(press, belt)))))),
                new KeyText());
        return GuideSite.files(reader, new KeyText(), PRESSING, "en-us");
    }

    /** Words that are their own keys, with any arguments after them in brackets. */
    private record KeyText() implements IGuideText {

        @Override
        public String text(final String key, final Object... args) {
            if (args.length == 0) {
                return key;
            }
            final List<String> written = new ArrayList<>();
            for (final Object arg : args) {
                written.add(String.valueOf(arg));
            }
            return key + "[" + String.join(",", written) + "]";
        }

        @Override
        public int width(final String text, final TextSize size) {
            return text.length() * 6;
        }

        @Override
        public int recipeCount(final String type, final String output) {
            return 1;
        }

        @Override
        public String amount(final long value, final String unit) {
            return value + " " + unit;
        }
    }
}
