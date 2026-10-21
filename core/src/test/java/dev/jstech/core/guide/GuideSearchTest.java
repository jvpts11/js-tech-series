/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GuideSearchTest {

    private static final GuideStyle STYLE = new GuideStyle("", Map.of(), new GuideStyle.Pages(true, 166, 201, 10, 1),
            GuideStyle.Decor.binder(true), "", "", GuideStyle.Folios.CHAPTER_PAGE, "",
            new GuideStyle.Cover(GuideStyle.CoverKind.BINDER, true, false), GuideStyle.HoldBar.PLAIN);
    private static final GuideManual MANUAL = new GuideManual("test:manual", "manual.title", List.of(), "", "",
            "test:style", List.of(GuideManual.EVERY_CHAPTER), List.of(), 0, "");
    /** The two recipes of the press, in words. */
    private static final IRecipeWords PRESSING = (type, output) -> type.equals("test:pressing") ? List.of(
            new IRecipeWords.RecipeWords(List.of("Block of Iron", "Charcoal"), List.of("Iron Ingot", "Coal",
                    "Gold Nugget"), "Test Press", "minecraft:iron_block"),
            new IRecipeWords.RecipeWords(List.of("Block of Gold"), List.of("Gold Ingot", "Redstone"), "Test Press",
                    "minecraft:gold_block")) : List.of();

    @Test
    void find_listsTheIndexFirstThenEachEntryFoundInItsWordsWithItsLine() {
        final GuideBook book = book();

        final List<GuideSearch.Hit> hits = GuideSearch.find(book, "DIR");

        assertEquals(1, hits.size(), () -> "" + hits);
        final GuideSearch.Hit hit = hits.getFirst();
        assertEquals("alpha:prompt", hit.target());
        assertEquals("dir", hit.line(), "the line the word was found on");
        assertEquals(book.pageOf("alpha:prompt").orElseThrow(), hit.page());
        assertEquals(book.pages().get(hit.page()).folio(), hit.folio());
    }

    @Test
    void find_listsAnEntryFoundByItsTitleOnlyOnce() {
        final List<GuideSearch.Hit> hits = GuideSearch.find(book(), "prompt");

        assertEquals(List.of("alpha:prompt"), hits.stream().map(GuideSearch.Hit::target).toList());
        assertTrue(hits.getFirst().line().isEmpty(), "found in the index, it has no line of its own");
    }

    @Test
    void find_withNothingTypedListsTheWholeIndex() {
        final GuideBook book = book();

        assertEquals(book.index().size(), GuideSearch.find(book, "  ").size());
    }

    @Test
    void find_listsARecipeMakingWhatIsTypedWithWhatWorksIt() {
        final GuideBook book = pressBook();

        final List<GuideSearch.Hit> hits = GuideSearch.find(book, "block of IRON", PRESSING);

        assertEquals(1, hits.size(), () -> "" + hits);
        final GuideSearch.Hit hit = hits.getFirst();
        assertEquals(GuideSearch.Found.RECIPE, hit.found());
        assertEquals("Block of Iron, Test Press", hit.line());
        assertEquals("minecraft:iron_block", hit.icon(), "the item it makes is drawn beside it");
        assertEquals("alpha:press", hit.target());
        assertEquals(book.pageOf("alpha:press").orElseThrow(), hit.page(), "it opens the page the recipe is on");
    }

    @Test
    void find_listsAUseAsWhatTheRecipeMakes() {
        final List<GuideSearch.Hit> hits = GuideSearch.find(pressBook(), "redstone", PRESSING);

        assertEquals(1, hits.size(), () -> "" + hits);
        assertEquals(GuideSearch.Found.USE, hits.getFirst().found());
        assertEquals("Block of Gold, Test Press", hits.getFirst().line());
    }

    @Test
    void find_listsPagesThenRecipesThenUses() {
        final List<GuideSearch.Hit> hits = GuideSearch.find(pressBook(), "gold", PRESSING);

        assertEquals(List.of(GuideSearch.Found.PAGE, GuideSearch.Found.RECIPE, GuideSearch.Found.USE),
                hits.stream().map(GuideSearch.Hit::found).toList(), () -> "" + hits);
        assertEquals(1, GuideSearch.count(hits, GuideSearch.Found.RECIPE));
        assertEquals(1, GuideSearch.count(hits, GuideSearch.Found.USE));
    }

    @Test
    void find_aRecipeThatMakesWhatIsTypedIsNotAlsoAUse() {
        // The first recipe makes charcoal and takes coal: "coal" finds it once, as what makes charcoal.
        final List<GuideSearch.Hit> hits = GuideSearch.find(pressBook(), "coal", PRESSING);

        assertEquals(1, hits.size(), () -> "" + hits);
        assertEquals(GuideSearch.Found.RECIPE, hits.getFirst().found());
        assertEquals("Charcoal, Test Press", hits.getFirst().line());
    }

    @Test
    void find_withoutTheRecipesInWordsFindsNoRecipe() {
        assertTrue(GuideSearch.find(pressBook(), "redstone").isEmpty());
    }

    /** A manual of two entries: one about files, one about the prompt with a table of its commands. */
    private static GuideBook book() {
        final GuideEntry files = new GuideEntry("alpha:files", "alpha:section", 0, "files.title", "", List.of(),
                List.of(new GuideBlock.Paragraph("Files hold what you write.")));
        final GuideEntry prompt = new GuideEntry("alpha:prompt", "alpha:section", 1, "prompt.title", "", List.of(),
                List.of(new GuideBlock.Paragraph("Type a command."), new GuideBlock.Table("caption",
                        List.of("Command", "What it does"), List.of(new GuideBlock.TableRow(List.of(
                                new GuideBlock.GuideValue.Literal("dir"),
                                new GuideBlock.GuideValue.Words("Lists a folder.")))))));
        return GuideLayout.lay(MANUAL, STYLE, new GuideContents(List.of(new GuideContents.Chapter(
                new GuideChapter("alpha", 0, "alpha.title", "", ""), List.of(new GuideContents.Section(
                        new GuideSection("alpha:section", 0, "section.title", ""), List.of(files, prompt)))))),
                new Words(0));
    }

    /** A manual of one entry, about a press whose two recipes its page shows. */
    private static GuideBook pressBook() {
        final GuideEntry press = new GuideEntry("alpha:press", "alpha:section", 0, "press.title", "", List.of(),
                List.of(new GuideBlock.Paragraph("Gold goes in last."), new GuideBlock.Recipes("test:pressing", "")));
        return GuideLayout.lay(MANUAL, STYLE, new GuideContents(List.of(new GuideContents.Chapter(
                new GuideChapter("alpha", 0, "alpha.title", "", ""), List.of(new GuideContents.Section(
                        new GuideSection("alpha:section", 0, "section.title", ""), List.of(press)))))),
                new Words(2));
    }

    /** Words that are their own keys, six pixels to a letter, with that many recipes of every type. */
    private record Words(int recipes) implements IGuideText {

        @Override
        public String text(final String key, final Object... args) {
            return key;
        }

        @Override
        public int width(final String text, final TextSize size) {
            return text.length() * 6;
        }

        @Override
        public int recipeCount(final String type, final String output) {
            return this.recipes;
        }

        @Override
        public String amount(final long value, final String unit) {
            return value + " " + unit;
        }
    }
}
