/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.guide.CoreGuide;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.GuideCodecs;
import dev.jstech.core.guide.GuideEntry;
import dev.jstech.core.guide.GuideFacts;
import dev.jstech.core.guide.GuideManual;
import dev.jstech.core.guide.GuideSection;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.GuideTexts;
import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestGuide;
import dev.jstech.tests.TestPress;
import dev.jstech.tests.TestSounds;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What a mod declares for the manuals, as the data generation writes it: every entry, section, chapter, manual and
 * style of the test mod read back from its file the same, a chapter written for one book bound by it alone, items
 * named by their registered ids, every English sentence under a key of the mod's own, the five parts of an entry in
 * their order, and the figures and rows a manual reads from its mod as it runs.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class GuideGameTests {

    private static final String ARENA = "empty";

    private GuideGameTests() {
    }

    @GameTest(template = ARENA)
    public static void declarations_readBackFromTheirFilesTheSame(final GameTestHelper helper) {
        final ModGuide guide = TestSounds.CONTENT.guide();
        for (final GuideEntry entry : guide.declaredEntries()) {
            helper.assertTrue(roundTrips(GuideCodecs.ENTRY, entry), entry.id() + " reads back the same");
        }
        for (final GuideSection section : guide.declaredSections()) {
            helper.assertTrue(roundTrips(GuideCodecs.SECTION, section), section.id() + " reads back the same");
        }
        for (final GuideManual manual : guide.declaredManuals()) {
            helper.assertTrue(roundTrips(GuideCodecs.MANUAL, manual), manual.id() + " reads back the same");
        }
        helper.assertTrue(roundTrips(GuideCodecs.CHAPTER, guide.declaredChapter()), "the chapter reads back");
        for (final GuideChapter chapter : guide.declaredOtherChapters()) {
            helper.assertTrue(roundTrips(GuideCodecs.CHAPTER, chapter), chapter.id() + " reads back the same");
        }
        final GuideStyle binder = CoreItems.CONTENT.guide().declaredStyles().get("binder");
        helper.assertTrue(binder != null && roundTrips(GuideCodecs.STYLE, binder), "the binder style reads back");
        helper.succeed();
    }

    /*
     * A mod writes a chapter for one of its books only: it has its own id, its sections say they sit in it, the
     * series' reference leaves it out, and the book that binds it names it before the mod's main chapter.
     */
    @GameTest(template = ARENA)
    public static void chapters_aBookBindsTheOnesItNamesInItsOrder(final GameTestHelper helper) {
        final ModGuide guide = TestSounds.CONTENT.guide();
        helper.assertTrue(TestGuide.SHARED.id().equals("jstests:shared") && !TestGuide.SHARED.inReference()
                        && guide.declaredChapter().inReference(),
                "the shared chapter has its own id and is left out of the reference; the main one is not");
        final GuideSection notes = guide.declaredSections().stream().filter(s -> s.id().equals("jstests:notes"))
                .findFirst().orElseThrow();
        helper.assertTrue(notes.chapter().equals(TestGuide.SHARED.id()), "the notes sit in the shared chapter");
        final GuideManual second = guide.declaredManuals().stream()
                .filter(m -> m.id().equals(TestGuide.SECOND_BOOK)).findFirst().orElseThrow();
        helper.assertTrue(second.chapters().equals(List.of("jstests:shared", "jstests")) && second.holds(
                TestGuide.SHARED) && !second.holdsEvery(), "the second book binds the shared chapter, then the main");
        final GuideManual reference = CoreItems.CONTENT.guide().declaredManuals().stream()
                .filter(GuideManual::holdsEvery).findFirst().orElseThrow();
        helper.assertTrue(!reference.holds(TestGuide.SHARED) && reference.holds(guide.declaredChapter()),
                "the reference holds the main chapter and leaves the shared one out");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void entry_namesItsItemsByTheirRegisteredIds(final GameTestHelper helper) {
        final GuideEntry press = entry(TestGuide.PRESS);
        final String upgrade = BuiltInRegistries.ITEM.getKey(TestPress.UPGRADE.get()).toString();
        helper.assertTrue(press.items().equals(List.of(upgrade)), "the press's page is its upgrade's: "
                + press.items());
        helper.assertTrue(press.blocks().stream().anyMatch(part -> part instanceof GuideBlock.Figure figure
                && figure.item().equals(upgrade)), "its figure draws the upgrade");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void entry_followsTheFivePartsInTheirOrder(final GameTestHelper helper) {
        final List<String> headings = entry(TestGuide.PRESS).blocks().stream()
                .filter(part -> part instanceof GuideBlock.Heading).map(part -> ((GuideBlock.Heading) part).key())
                .toList();
        helper.assertTrue(headings.equals(List.of(GuideTexts.WHAT_IT_IS.key(), GuideTexts.WHAT_IT_IS_FOR.key(),
                GuideTexts.HOW_TO_GET_IT.key(), GuideTexts.HOW_TO_USE_IT.key(), GuideTexts.WHAT_CAN_GO_WRONG.key())),
                "the five parts come in their order: " + headings);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void declarations_keepEverySentenceUnderAKeyOfTheMods(final GameTestHelper helper) {
        final ModGuide guide = TestSounds.CONTENT.guide();
        helper.assertTrue(guide.translations().keySet().stream().allMatch(key -> key.startsWith(JsTests.MODID
                + ".guide.")), "every key is the test mod's own");
        helper.assertTrue("Test Press".equals(guide.translations().get(JsTests.MODID + ".guide.press.title")),
                "the press's title is kept in English under its key");
        helper.assertTrue(TestGuide.MANUAL_ITEM.get().manual().equals(TestGuide.MANUAL)
                && guide.declaredManuals().getFirst().style().equals(CoreGuide.BINDER),
                "the manual item opens the test manual, drawn in the binder style");
        helper.succeed();
    }

    /*
     * A table read from its mod holds no rows in its file and answers from the game as it runs: a row for every item
     * the test mod registered, in the order of their ids, each naming its item; a value answers the press's own figure.
     */
    @GameTest(template = ARENA)
    public static void facts_answerFromTheGameAsItRuns(final GameTestHelper helper) {
        final GuideBlock.Table items = entry(TestGuide.TABLES).blocks().stream()
                .filter(part -> part instanceof GuideBlock.Table table && table.source().equals(TestGuide.ITEMS))
                .map(part -> (GuideBlock.Table) part).findFirst().orElseThrow();
        helper.assertTrue(items.rows().isEmpty(), "the table's file holds no rows of its own");
        final List<Item> registered = BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> id.getNamespace().equals(JsTests.MODID)).sorted().map(BuiltInRegistries.ITEM::get)
                .toList();
        final List<GuideBlock.TableRow> rows = GuideFacts.rowsOf(TestGuide.ITEMS, items.columns());
        helper.assertTrue(!registered.isEmpty() && rows.size() == registered.size(),
                "a row for each of the test mod's " + registered.size() + " items: " + rows.size());
        for (int i = 0; i < rows.size(); i++) {
            final GuideBlock.GuideValue name = rows.get(i).cells().getFirst();
            helper.assertTrue(name.equals(new GuideBlock.GuideValue.Words(registered.get(i).getDescriptionId())),
                    "row " + i + " names " + registered.get(i) + ": " + name);
        }
        helper.assertTrue(GuideFacts.valueFor(TestGuide.PRESS_ENERGY).orElseThrow()
                        .equals(new GuideBlock.GuideValue.Amount(TestPress.ENERGY_PER_TICK, "FE/t")),
                "the press's figure is the press's own");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void callout_keepsItsKeyApartAcrossTwoViewsOfOneEntry(final GameTestHelper helper) {
        final ModGuide guide = new ModGuide("jstests");
        final ModGuide.SectionRef section = guide.section("balloons").titled("Balloons").register();
        guide.page("two_views", section)
                .views(() -> Items.STONE).callout(1, GuideBlock.View.TOP, 2, 2, "The first legend")
                .views(() -> Items.STONE).callout(1, GuideBlock.View.TOP, 4, 4, "The second legend");
        helper.assertTrue(guide.translations().containsValue("The first legend")
                && guide.translations().containsValue("The second legend"), "both legends keep their own key");
        helper.succeed();
    }

    private static GuideEntry entry(final String id) {
        return TestSounds.CONTENT.guide().declaredEntries().stream().filter(entry -> entry.id().equals(id))
                .findFirst().orElseThrow();
    }

    private static <T> boolean roundTrips(final Codec<T> codec, final T value) {
        final JsonElement written = codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow();
        return value.equals(codec.parse(JsonOps.INSTANCE, written).getOrThrow());
    }
}
