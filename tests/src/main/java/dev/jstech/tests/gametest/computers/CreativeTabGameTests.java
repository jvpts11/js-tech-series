/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.IEraChassisBlock;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * J's Computers shows its things in six creative tabs: one for what every era shares, then one for each era from the
 * Vintage to the Advanced, side by side in that order. A thing is in one tab only, and a thing that carries an era,
 * a part by its specification or a machine by its chassis, is in the tab of that era.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CreativeTabGameTests {

    private static final String ARENA = "empty";
    /** The tabs, left to right: the shared one, then one for each era. */
    private static final List<String> TABS = List.of("computing", "computing_vintage", "computing_legacy",
            "computing_transition", "computing_standard", "computing_advanced");

    private CreativeTabGameTests() {
    }

    @GameTest(template = ARENA)
    public static void tabs_standSideBySideInTheirOrder(final GameTestHelper helper) {
        final List<String> found = new ArrayList<>();
        for (final CreativeModeTab tab : CreativeModeTabRegistry.getSortedCreativeModeTabs()) {
            final ResourceLocation name = CreativeModeTabRegistry.getName(tab);
            if (name != null && name.getNamespace().equals(JsComputers.MODID)) {
                found.add(name.getPath());
            }
        }
        helper.assertTrue(found.equals(TABS), "the tabs stand as " + TABS + "; got " + found);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void tabs_neverShowTheSameThingTwice(final GameTestHelper helper) {
        final Map<Item, List<Shown>> byItem = new HashMap<>();
        for (final Map.Entry<String, List<ItemStack>> tab : contents(helper.getLevel()).entrySet()) {
            for (final ItemStack stack : tab.getValue()) {
                byItem.computeIfAbsent(stack.getItem(), item -> new ArrayList<>()).add(new Shown(tab.getKey(), stack));
            }
        }
        final List<String> twice = new ArrayList<>();
        for (final List<Shown> same : byItem.values()) {
            for (int i = 0; i < same.size(); i++) {
                for (int j = i + 1; j < same.size(); j++) {
                    if (ItemStack.isSameItemSameComponents(same.get(i).stack(), same.get(j).stack())) {
                        twice.add(same.get(i).stack().getItem() + " in " + same.get(i).tab() + " and "
                                + same.get(j).tab());
                    }
                }
            }
        }
        helper.assertTrue(twice.isEmpty(), "a thing shows in one place only; twice: " + twice);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void tabs_showAThingThatCarriesAnEraInTheTabOfThatEra(final GameTestHelper helper) {
        final List<String> wrong = new ArrayList<>();
        int checked = 0;
        for (final Map.Entry<String, List<ItemStack>> tab : contents(helper.getLevel()).entrySet()) {
            for (final ItemStack stack : tab.getValue()) {
                final Era era = eraOf(stack.getItem());
                if (era == null) {
                    continue;
                }
                checked++;
                if (!tab.getKey().equals("computing_" + era.path())) {
                    wrong.add(stack.getItem() + " of the " + era.serializedName() + " era in " + tab.getKey());
                }
            }
        }
        helper.assertTrue(checked > 100, "the parts and machines were found in the tabs; checked " + checked);
        helper.assertTrue(wrong.isEmpty(), "a thing of an era is in that era's tab; wrong: " + wrong);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void tabs_holdTheInstallersOfTheirEra(final GameTestHelper helper) {
        final Map<String, List<ItemStack>> tabs = contents(helper.getLevel());
        final ItemStack frames7 = installer(tabs.get("computing_transition"), "frames_7");
        final ItemStack frames95 = installer(tabs.get("computing_legacy"), "frames_95");
        helper.assertTrue(frames7 != null, "Frames 7 installs from the Transition, so its installer is in that tab");
        helper.assertTrue(frames95 != null, "Frames 95 installs from the Legacy, so its installer is in that tab");
        helper.succeed();
    }

    /* What each tab of the mod shows, by the tab's path. */
    private static Map<String, List<ItemStack>> contents(final ServerLevel level) {
        final CreativeModeTab.ItemDisplayParameters parameters = new CreativeModeTab.ItemDisplayParameters(
                level.enabledFeatures(), true, level.registryAccess());
        final Map<String, List<ItemStack>> contents = new HashMap<>();
        for (final CreativeModeTab tab : CreativeModeTabRegistry.getSortedCreativeModeTabs()) {
            final ResourceLocation name = CreativeModeTabRegistry.getName(tab);
            if (name != null && name.getNamespace().equals(JsComputers.MODID)) {
                tab.buildContents(parameters);
                contents.put(name.getPath(), List.copyOf(tab.getDisplayItems()));
            }
        }
        return contents;
    }

    /* The installer in that list whose payload is the system or program of that path, or null. */
    @Nullable
    private static ItemStack installer(final List<ItemStack> stacks, final String path) {
        for (final ItemStack stack : stacks) {
            final ResourceLocation payload = MediaItem.payload(stack);
            if (payload != null && payload.getPath().equals(path)) {
                return stack;
            }
        }
        return null;
    }

    /* The era a thing carries: a part's by its entry, a machine's by its chassis; null for the rest. */
    @Nullable
    private static Era eraOf(final Item item) {
        final HardwareRegistry.Part part = HardwareRegistry.part(item);
        if (part != null) {
            return part.era();
        }
        return item instanceof BlockItem block && block.getBlock() instanceof IEraChassisBlock chassis
                ? chassis.chassisEra() : null;
    }

    /** A stack and the tab it shows in. */
    private record Shown(String tab, ItemStack stack) {
    }
}
