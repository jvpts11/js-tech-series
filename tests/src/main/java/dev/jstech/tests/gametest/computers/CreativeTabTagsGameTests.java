/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.registry.ComputingContent.Shelf;
import dev.jstech.computers.registry.ComputingContent;
import dev.jstech.core.content.ContentTab;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * J's Computers' item tags hold what its creative tabs hold: everything of an era in {@code #jsc:era/<era>}, and
 * everything on a shelf, in any tab, in that shelf's tag. A modpack's recipe or chest that asks for "any Vintage
 * part" reads these, so an item missing from its tag would silently never be asked for.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CreativeTabTagsGameTests {

    private static final String ARENA = "empty";

    private CreativeTabTagsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void tabItems_areInTheTagsOfTheirShelfAndEra(final GameTestHelper helper) {
        final List<String> wrong = new ArrayList<>();
        int checked = 0;
        for (final Shelf shelf : Shelf.values()) {
            final TagKey<Item> shelfTag = tag(shelf.tag());
            for (final Item item : ComputingContent.shelf(shelf, null).items()) {
                checked++;
                expect(wrong, item, shelfTag, true);
                for (final HardwareEra era : erasWithTabs()) {
                    expect(wrong, item, tag(ComputingContent.eraTag(era)), false);
                }
            }
            for (final HardwareEra era : erasWithTabs()) {
                for (final Item item : ComputingContent.shelf(shelf, era).items()) {
                    checked++;
                    expect(wrong, item, shelfTag, true);
                    for (final HardwareEra other : erasWithTabs()) {
                        expect(wrong, item, tag(ComputingContent.eraTag(other)), other == era);
                    }
                }
            }
        }
        helper.assertTrue(checked > 300, "the tabs hold J's Computers' items; read " + checked);
        helper.assertTrue(wrong.isEmpty(), wrong.size() + " items out of step with their tags: "
                + String.join("; ", wrong.subList(0, Math.min(10, wrong.size()))));
        helper.succeed();
    }

    /** One item named by hand, so the test does not only agree with itself: the 8800 GT is a Transition part. */
    @GameTest(template = ARENA)
    public static void aTransitionGraphicsCard_isATransitionComponent(final GameTestHelper helper) {
        final Item card = HardwareItems.GPU_VERTEX_8800_GT.get();
        helper.assertTrue(card.builtInRegistryHolder().is(tag("era/transition")), "the card is in #jsc:era/transition");
        helper.assertTrue(card.builtInRegistryHolder().is(tag("components")), "and in #jsc:components");
        helper.assertFalse(card.builtInRegistryHolder().is(tag("era/standard")), "and in no other era's tag");
        helper.succeed();
    }

    private static List<HardwareEra> erasWithTabs() {
        final List<HardwareEra> eras = new ArrayList<>();
        for (final HardwareEra era : HardwareEra.values()) {
            final ContentTab tab = ComputingContent.tab(era);
            if (tab != null) {
                eras.add(era);
            }
        }
        return eras;
    }

    private static void expect(final List<String> wrong, final Item item, final TagKey<Item> tag,
                               final boolean inside) {
        if (item.builtInRegistryHolder().is(tag) != inside) {
            wrong.add(BuiltInRegistries.ITEM.getKey(item) + (inside ? " missing from #" : " wrongly in #")
                    + tag.location());
        }
    }

    private static TagKey<Item> tag(final String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path));
    }
}
