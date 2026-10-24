/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.industrial;

import dev.jstech.core.research.DataKind;
import dev.jstech.core.research.DataKinds;
import dev.jstech.core.research.Discoveries;
import dev.jstech.industrial.JsIndustrial;
import dev.jstech.industrial.config.IndustrialServerConfig;
import dev.jstech.industrial.research.IndustrialResearch;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * J's Industrial's research: its four kinds of data, each with its item, whose tooltip says which instruments give
 * it; and its research locking what its discoveries open by the world's setting, on unless a server turns it off.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class IndustrialResearchGameTests {

    private static final String ARENA = "empty";

    private IndustrialResearchGameTests() {
    }

    @GameTest(template = ARENA)
    public static void data_areFourKindsEachWithItsItemSayingWhereItComesFrom(final GameTestHelper helper) {
        final Map<Item, DataKind> items = Map.of(
                IndustrialResearch.SAMPLE_DATA.get(), IndustrialResearch.SAMPLE,
                IndustrialResearch.CRYSTAL_DATA.get(), IndustrialResearch.CRYSTAL,
                IndustrialResearch.SPECTRAL_DATA.get(), IndustrialResearch.SPECTRAL,
                IndustrialResearch.PARTICLE_DATA.get(), IndustrialResearch.PARTICLE);
        items.forEach((item, kind) -> {
            helper.assertTrue(DataKinds.byId(kind.id()).equals(Optional.of(kind)), kind.id() + " is declared");
            helper.assertTrue(DataKinds.of(new ItemStack(item)).equals(Optional.of(kind)), item + " is " + kind.id());
        });
        final List<Component> lines = new ArrayList<>();
        final ItemStack crystal = new ItemStack(IndustrialResearch.CRYSTAL_DATA.get());
        crystal.getItem().appendHoverText(crystal, Item.TooltipContext.EMPTY, lines, TooltipFlag.NORMAL);
        helper.assertTrue(lines.stream().anyMatch(line -> line.getContents() instanceof TranslatableContents said
                        && said.getKey().equals(IndustrialResearch.CRYSTAL_FROM.key())),
                "the crystal data's tooltip says which instruments give it; got " + lines);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void research_locksByTheWorldsSettingOnUnlessTurnedOff(final GameTestHelper helper) {
        helper.assertTrue(IndustrialServerConfig.requireResearch(), "research locks by default");
        helper.assertTrue(Discoveries.required(helper.getLevel().getServer(), JsIndustrial.MODID),
                "and the research of J's Industrial says so");
        helper.succeed();
    }
}
