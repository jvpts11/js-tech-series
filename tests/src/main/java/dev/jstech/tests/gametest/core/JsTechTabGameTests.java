/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.content.ModContent;
import dev.jstech.core.guide.CoreGuide;
import dev.jstech.core.jstechseries.JsTechTab;
import dev.jstech.core.jstechseries.SharedUses;
import dev.jstech.core.material.MaterialItems;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The J's Tech creative tab: the series' one tab of shared content, in its three sections, showing only what the
 * installed mods use, and leaving what it shows out of the other tabs of the mods that use it, so nothing shows twice.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class JsTechTabGameTests {

    private static final String ARENA = "empty";

    private JsTechTabGameTests() {
    }

    @GameTest(template = ARENA)
    public static void tab_isJsTechInThreeSections(final GameTestHelper helper) {
        final CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(JsTechTab.TAB.id());
        helper.assertTrue(tab != null, "the tab is registered as " + JsTechTab.TAB.id());
        helper.assertTrue(tab.getDisplayName().getContents() instanceof TranslatableContents title
                && title.getKey().equals("itemGroup.jscore.jstech") && "J's Tech".equals(title.getFallback()),
                "and called J's Tech");
        helper.assertTrue(JsTechTab.TAB.sections().equals(List.of(JsTechTab.MATERIALS,
                JsTechTab.FLUIDS_AND_COMPONENTS, JsTechTab.BLOCKS_AND_TOOLS)), "in three sections");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void tab_showsOnlyWhatTheInstalledModsUse(final GameTestHelper helper) {
        final List<Item> shown = contents(helper.getLevel(), JsTechTab.TAB.id());
        helper.assertTrue(shown.contains(CoreGuide.MANUAL.get()), "the series' manual, which every player has");
        for (final var material : MaterialItems.all()) {
            helper.assertTrue(shown.contains(material.get()) == SharedUses.used(material.get()),
                    material.getId() + " is shown only while a mod says it uses it");
        }
        helper.assertTrue(shown.equals(JsTechTab.shown()), "the tab shows what it says it shows: " + shown);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void tab_leavesWhatItShowsOutOfTheTabsOfTheModsThatUseIt(final GameTestHelper helper) {
        helper.assertTrue(JsTechTab.leftOutOf(ResourceLocation.fromNamespaceAndPath("jscore", "elsewhere"))
                .contains(CoreGuide.MANUAL.get()), "another tab of a mod that uses the manual leaves it out");
        helper.assertTrue(JsTechTab.leftOutOf(JsTechTab.TAB.id()).isEmpty(), "the tab itself keeps everything");
        helper.assertTrue(JsTechTab.leftOutOf(ResourceLocation.fromNamespaceAndPath("jstests_nobody", "tab"))
                .isEmpty(), "a mod that uses nothing keeps its tab as it is");
        final Set<Item> inJsTech = new HashSet<>(contents(helper.getLevel(), JsTechTab.TAB.id()));
        final List<String> twice = new ArrayList<>();
        final Set<String> series = new HashSet<>();
        ModContent.all().forEach(content -> series.add(content.modid()));
        for (final var entry : BuiltInRegistries.CREATIVE_MODE_TAB.entrySet()) {
            final ResourceLocation id = entry.getKey().location();
            if (!id.equals(JsTechTab.TAB.id()) && series.contains(id.getNamespace())
                    && SharedUses.users().contains(id.getNamespace())) {
                for (final Item item : contents(helper.getLevel(), id)) {
                    if (inJsTech.contains(item)) {
                        twice.add(id + " " + BuiltInRegistries.ITEM.getKey(item));
                    }
                }
            }
        }
        helper.assertTrue(twice.isEmpty(), "nothing of J's Tech shows again in the tab of a mod that uses it: "
                + twice);
        helper.succeed();
    }

    private static List<Item> contents(final ServerLevel level, final ResourceLocation id) {
        final CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(id);
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), true,
                level.registryAccess()));
        return tab.getDisplayItems().stream().map(ItemStack::getItem).toList();
    }
}
