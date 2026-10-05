/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.jstech.computers.registry.ComputingContent;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.guide.CoreGuide;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideCodecs;
import dev.jstech.core.guide.GuideEntry;
import dev.jstech.core.guide.GuideGifts;
import dev.jstech.core.guide.GuideTexts;
import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The series' own manuals as their mods declare them: the Technical Reference handed to a player once, as they first
 * join a world; every item of the series the page of some entry; every entry of the series following the five parts
 * in their order; and every entry, the drawings' views and plans included, read back from its file the same.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SeriesManualsGameTests {

    private static final String ARENA = "empty";
    /** Fewer entries than this, and a chapter of the series was lost on its way to the manuals. */
    private static final int FEWEST_ENTRIES = 50;
    /** The mods of the series whose every item a manual explains. */
    private static final List<String> SERIES = List.of("jscore", "jsc", "jsindustrial");
    private static final List<String> SPINE = List.of(GuideTexts.WHAT_IT_IS.key(), GuideTexts.WHAT_IT_IS_FOR.key(),
            GuideTexts.HOW_TO_GET_IT.key(), GuideTexts.HOW_TO_USE_IT.key(), GuideTexts.WHAT_CAN_GO_WRONG.key());

    private SeriesManualsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void technicalReference_isHandedToAPlayerOnceAsTheyFirstJoin(final GameTestHelper helper) {
        final UUID id = UUID.randomUUID();
        final FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, "manual-reader"));
        player.getInventory().clearContent();
        helper.assertFalse(GuideGifts.wasGiven(player, CoreGuide.TECHNICAL_REFERENCE),
                "a player new to the world has not been given the manual");
        GuideGifts.welcome(player);
        helper.assertTrue(count(player) == 1, "the first join hands them the Technical Reference");
        helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "and leaves their hotbar as it was");
        helper.assertTrue(GuideGifts.wasGiven(player, CoreGuide.TECHNICAL_REFERENCE), "the world keeps that it did");
        player.getInventory().clearContent();
        GuideGifts.welcome(player);
        helper.assertTrue(count(player) == 0, "a player who put theirs away is not handed another");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void seriesManuals_explainEveryItemOfTheSeries(final GameTestHelper helper) {
        final Set<String> covered = new HashSet<>();
        for (final ModGuide guide : guides()) {
            for (final GuideEntry entry : guide.declaredEntries()) {
                covered.addAll(entry.items());
            }
        }
        final List<String> missing = new ArrayList<>();
        for (final ResourceLocation item : BuiltInRegistries.ITEM.keySet()) {
            if (SERIES.contains(item.getNamespace()) && !covered.contains(item.toString())) {
                missing.add(item.toString());
            }
        }
        helper.assertTrue(missing.isEmpty(), "every item of the series is the page of an entry; none for " + missing);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void seriesEntries_followTheFivePartsInTheirOrder(final GameTestHelper helper) {
        final List<String> wrong = new ArrayList<>();
        for (final ModGuide guide : guides()) {
            for (final GuideEntry entry : guide.declaredEntries()) {
                final List<String> spine = entry.blocks().stream()
                        .filter(part -> part instanceof GuideBlock.Heading heading && SPINE.contains(heading.key()))
                        .map(part -> ((GuideBlock.Heading) part).key()).toList();
                if (!spine.equals(SPINE)) {
                    wrong.add(entry.id() + " " + spine);
                }
            }
        }
        helper.assertTrue(wrong.isEmpty(), "every entry says what it is, what for, how to get it, how to use it and"
                + " what can go wrong, in that order; not " + wrong);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void seriesDeclarations_readBackFromTheirFilesTheSame(final GameTestHelper helper) {
        int entries = 0;
        for (final ModGuide guide : guides()) {
            for (final GuideEntry entry : guide.declaredEntries()) {
                helper.assertTrue(roundTrips(GuideCodecs.ENTRY, entry), entry.id() + " reads back the same");
                entries++;
            }
            guide.declaredManuals().forEach(manual -> helper.assertTrue(roundTrips(GuideCodecs.MANUAL, manual),
                    manual.id() + " reads back the same"));
            guide.declaredStyles().forEach((path, style) -> helper.assertTrue(roundTrips(GuideCodecs.STYLE, style),
                    guide.namespace() + ":" + path + " reads back the same"));
        }
        helper.assertTrue(entries > FEWEST_ENTRIES, "the series' chapters hold their entries; found " + entries);
        helper.succeed();
    }

    /** What the series writes in the manuals: the Core's and the series' own, the Computers' and the Industrial's. */
    private static List<ModGuide> guides() {
        final List<ModGuide> guides = new ArrayList<>();
        for (final ModContent content : List.of(CoreItems.CONTENT, ComputingContent.CONTENT,
                IndustrialModule.CONTENT)) {
            guides.addAll(content.declaredGuides());
        }
        return guides;
    }

    private static int count(final FakePlayer player) {
        int found = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            final ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(CoreGuide.MANUAL.get())) {
                found += stack.getCount();
            }
        }
        return found;
    }

    private static <T> boolean roundTrips(final Codec<T> codec, final T value) {
        final JsonElement written = codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow();
        return value.equals(codec.parse(JsonOps.INSTANCE, written).getOrThrow());
    }
}
