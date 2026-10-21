/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.light.DynamicLightHandOff;
import dev.jstech.core.recipe.RecipeChoices;
import dev.jstech.core.stepaside.StepAside;
import dev.jstech.core.unify.Unification;
import dev.jstech.tests.JsTests;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Stepping aside for other mods: every system of presentation and comfort the modpacks' page names declares the mods
 * that do its job and has a switch to keep it on; with none of those mods installed, none steps aside; and the light
 * the Core hands LambDynamicLights is read from the mods' own files, the game's blaze powder among it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class StepAsideGameTests {

    private static final String ARENA = "empty";
    private static final int BLAZE_POWDER = 8;

    private StepAsideGameTests() {
    }

    @GameTest(template = ARENA)
    public static void systems_declareTheModsThatDoTheirJob(final GameTestHelper helper) {
        final Map<String, List<String>> wanted = Map.of(
                "sound_physics", List.of("sound_physics_remastered", "dsurround"),
                "ambience", List.of("dsurround", "ambientsounds"),
                "footsteps", List.of("presencefootsteps"),
                "dynamic_light", List.of("lambdynlights", "sodiumdynamiclights"),
                "coloured_light", List.of("shimmer"),
                "connected_textures", List.of("continuity", "ctm"),
                "unification", List.of("almostunified"),
                "recipe_choice", List.of("polymorph"));
        // The world's systems are made with their kits.
        helper.assertTrue(Unification.STEP_ASIDE != null && RecipeChoices.STEP_ASIDE != null, "the kits are loaded");
        final Map<String, StepAside> systems = new HashMap<>();
        StepAside.all().forEach(aside -> systems.put(aside.system(), aside));
        wanted.forEach((system, mods) -> {
            final StepAside aside = systems.get(system);
            helper.assertTrue(aside != null, "the system " + system + " steps aside; there are " + systems.keySet());
            helper.assertTrue(aside.mods().equals(mods), system + " names " + mods + "; it names " + aside.mods());
            helper.assertTrue(aside.key() != null, system + " has its switch");
            helper.assertTrue(aside.installed().isEmpty() && !aside.steps(),
                    system + " stays on with none of its mods installed");
        });
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void dynamicLight_handsTheItemsLightFromTheModsFiles(final GameTestHelper helper) {
        final Map<String, Integer> handed = DynamicLightHandOff.handed();
        helper.assertTrue(Integer.valueOf(BLAZE_POWDER).equals(handed.get("minecraft:blaze_powder")),
                "the blaze powder is handed with the light the Core's file gives it; handed " + handed);
        helper.succeed();
    }
}
