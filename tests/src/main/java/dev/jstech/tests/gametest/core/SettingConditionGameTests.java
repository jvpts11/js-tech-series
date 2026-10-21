/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.IConfigValues;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestSettings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The condition {@code jscore:setting} on the test mod's recipes ({@code data/jstests/recipe/condition}), each on a
 * setting of the test mod's world file at its default, and the world's file read from disk, as a world's data loads
 * before the world's settings are read.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SettingConditionGameTests {

    private static final String ARENA = "empty";
    private static final String WORLD_FOLDER = "serverconfig";
    private static final int SPEED_ON_DISK = 99;
    private static final int MOST_SPEED = 1024;

    private SettingConditionGameTests() {
    }

    @GameTest(template = ARENA)
    public static void settingCondition_loadsWhatTheSettingAllows(final GameTestHelper helper) {
        final RecipeManager recipes = helper.getLevel().getServer().getRecipeManager();
        for (final String loaded : List.of("on_switch", "on_number", "on_word", "on_list")) {
            helper.assertTrue(recipes.byKey(id(loaded)).isPresent(), "the recipe " + loaded + " loads");
        }
        for (final String skipped : List.of("off_switch", "off_number", "off_unknown_setting", "off_unknown_file",
                "off_players_file")) {
            helper.assertFalse(recipes.byKey(id(skipped)).isPresent(), "the recipe " + skipped + " does not load");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void forData_isTheFileItselfWhileTheWorldRuns(final GameTestHelper helper) {
        helper.assertTrue(ConfigFiles.forData(TestSettings.YAML) == TestSettings.YAML,
                "a running world's data reads its settings as they are");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void onDisk_readsTheWorldsFileWithoutTakingItIn(final GameTestHelper helper) {
        final int live = TestSettings.YAML.get(TestSettings.SPEED);
        final Path world;
        try {
            world = Files.createTempDirectory("jstests-world");
            final Path folder = Files.createDirectories(world.resolve(WORLD_FOLDER));
            Files.writeString(folder.resolve(TestSettings.YAML.fileName()), "speed: " + SPEED_ON_DISK
                    + "\nboot:\n  show_menu: false\n", StandardCharsets.UTF_8);
            Files.writeString(folder.resolve(TestSettings.TOML.fileName()), "speed = 5000\n",
                    StandardCharsets.UTF_8);
        } catch (final IOException e) {
            helper.fail("a world's folder could not be made: " + e.getMessage());
            return;
        }
        final IConfigValues yaml = ConfigFiles.onDisk(TestSettings.YAML, world);
        helper.assertTrue(yaml.get(TestSettings.SPEED) == SPEED_ON_DISK, "the speed the file on disk holds");
        helper.assertFalse(yaml.get(TestSettings.MENU), "and its switch");
        helper.assertTrue(yaml.get(TestSettings.DIALECT).equals("SIMPLE"), "a setting it lacks is its default");
        helper.assertTrue(ConfigFiles.onDisk(TestSettings.TOML, world).get(TestSettings.SPEED) == MOST_SPEED,
                "a value out of its range is pulled to its end, as reading pulls it");
        helper.assertTrue(TestSettings.YAML.get(TestSettings.SPEED) == live, "the file's own value is untouched");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void onDisk_isTheDefaultsForAWorldWithNoFile(final GameTestHelper helper) {
        final Path world;
        try {
            world = Files.createTempDirectory("jstests-world");
        } catch (final IOException e) {
            helper.fail("a world's folder could not be made: " + e.getMessage());
            return;
        }
        helper.assertTrue(ConfigFiles.onDisk(TestSettings.YAML, world).get(TestSettings.SPEED)
                .equals(TestSettings.SPEED.defaultValue()), "a world with no file of its own starts at the defaults");
        helper.succeed();
    }

    private static ResourceLocation id(final String name) {
        return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "condition/" + name);
    }
}
