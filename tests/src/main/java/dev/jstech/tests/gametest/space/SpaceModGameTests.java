/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.JsCore;
import dev.jstech.space.JsSpace;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.language.IModInfo;

/**
 * J's Space as the game loads it: beside the Core, at the Core's own version, as the series ships in lockstep, and
 * requiring the Core and no other mod of the series, so it is played without them.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SpaceModGameTests {

    private static final String ARENA = "empty";
    /* Every mod of the series has an id that starts so. */
    private static final String SERIES = "js";

    private SpaceModGameTests() {
    }

    @GameTest(template = ARENA)
    public static void mod_loadsBesideTheCoreAtItsVersion(final GameTestHelper helper) {
        final IModInfo space = info(JsSpace.MODID);
        final IModInfo core = info(JsCore.MODID);
        helper.assertTrue(space.getVersion().equals(core.getVersion()), "J's Space is at the Core's version: "
                + space.getVersion() + " beside " + core.getVersion());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void mod_requiresTheCoreAndNoOtherModOfTheSeries(final GameTestHelper helper) {
        final List<String> required = info(JsSpace.MODID).getDependencies().stream()
                .filter(dependency -> dependency.getType() == IModInfo.DependencyType.REQUIRED)
                .map(IModInfo.ModVersion::getModId)
                .filter(id -> id.startsWith(SERIES))
                .toList();
        helper.assertTrue(required.equals(List.of(JsCore.MODID)), "of the series it requires the Core alone: "
                + required);
        helper.succeed();
    }

    private static IModInfo info(final String modId) {
        return ModList.get().getModContainerById(modId).map(ModContainer::getModInfo)
                .orElseThrow(() -> new GameTestAssertException(modId + " is loaded"));
    }
}
