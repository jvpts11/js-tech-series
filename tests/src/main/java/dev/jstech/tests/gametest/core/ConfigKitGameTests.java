/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.config.ConfigApply;
import dev.jstech.core.config.ConfigCommands;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigPreset;
import dev.jstech.core.config.ConfigPresets;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.ConfigSync;
import dev.jstech.core.config.format.ConfigFormatException;
import dev.jstech.core.config.format.ConfigFormats;
import dev.jstech.core.team.CorePermissions;
import dev.jstech.core.text.TextKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestSettings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpListEntry;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The settings kit beyond one setting: when a change takes hold and what waits for a restart, a mod told of each
 * change, presets shipped and saved, settings written out as text and taken back, a world starting from the modpack's
 * copy of its settings, {@code /jstech config}, and who may change a world's settings from their own game.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ConfigKitGameTests {

    private static final String ARENA = "empty";
    private static final ConfigKey<Integer> RADIUS = ConfigKey.whole("radius", 8).range(1, 64)
            .appliesOn(ConfigApply.RESTART);
    private static final ConfigKey<Boolean> GLOW = ConfigKey.flag("glow", true).appliesOn(ConfigApply.RELOAD);
    private static final ConfigKey<String> NAME = ConfigKey.text("name", "plain");
    private static final TextKey GENTLE = TextKey.of("jstests.config.gentle", "Gentle");
    private static final ConfigPreset GENTLE_PRESET = ConfigPreset.of("gentle", GENTLE).with(RADIUS, 2)
            .with(GLOW, false);
    /** The preset a test saves and throws away again. */
    private static final String SAVED = "gametest preset";

    private ConfigKitGameTests() {
    }

    @GameTest(template = ARENA)
    public static void restartDue_countsOnlyWhatTakesHoldOnARestart(final GameTestHelper helper) {
        final ConfigFile file = file("restart-test");
        file.read(Map.of());
        file.set(GLOW, false);
        helper.assertTrue(file.restartDue().isEmpty(), "a change taken on reload waits for no restart");
        file.set(RADIUS, 16);
        helper.assertTrue(file.restartDue().equals(Set.of("radius")), "one taken on restart does");
        file.reset();
        helper.assertTrue(file.restartDue().isEmpty(), "and the file read afresh waits for nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void watchers_areToldOfEachChangeOnce(final GameTestHelper helper) {
        final ConfigFile file = file("watch-test");
        final List<Integer> radii = new ArrayList<>();
        final AtomicInteger any = new AtomicInteger();
        file.whenChanged(RADIUS, radii::add);
        file.onAnyChange(key -> any.incrementAndGet());
        file.set(RADIUS, 12);
        file.set(RADIUS, 12);
        file.set(NAME, "fancy");
        helper.assertTrue(radii.equals(List.of(12)), "the watcher of one setting hears its change once: " + radii);
        helper.assertTrue(any.get() == 2, "the watcher of every change hears both: " + any.get());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void presets_shippedAndSaved_areFoundAndPicked(final GameTestHelper helper) throws IOException {
        final ConfigFile file = file("preset-test");
        helper.assertTrue(ConfigPresets.find(file, "gentle").map(values -> values.get("radius")).orElse(0)
                .equals(2), "the mod's preset is found by its name");
        ConfigPresets.save(file, SAVED, Map.of("name", "saved"));
        try {
            helper.assertTrue(ConfigPresets.saved(file).contains(SAVED), "a saved preset is listed");
            helper.assertTrue(ConfigPresets.find(file, SAVED).map(values -> values.get("name")).orElse("")
                    .equals("saved"), "and read back");
        } finally {
            ConfigPresets.delete(file, SAVED);
        }
        helper.assertFalse(ConfigPresets.saved(file).contains(SAVED), "thrown away, it is gone");
        helper.assertFalse(ConfigPresets.validName("../escape"), "a name cannot climb out of its folder");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void exportAndImport_carryTheSettingsAsText(final GameTestHelper helper)
            throws ConfigFormatException {
        final ConfigFile file = file("text-test");
        file.set(NAME, "shared");
        final String exported = ConfigPresets.export(file, file.toPlain());
        helper.assertTrue(exported.contains("shared"), "the text holds the values");
        final Map<String, Object> back = ConfigPresets.importText(file, exported);
        helper.assertTrue("shared".equals(back.get("name")), "and reads back as the file's own format");
        final Map<String, Object> json = ConfigPresets.importText(file, "{\"radius\": 3}");
        helper.assertTrue(Integer.valueOf(3).equals(json.get("radius")), "JSON pasted into a YAML file is understood");
        boolean refused = false;
        try {
            ConfigPresets.importText(file, "{ this is not settings");
        } catch (final ConfigFormatException expected) {
            refused = true;
        }
        helper.assertTrue(refused, "and what no format reads is refused");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void worldFile_startsAsTheModpacksCopy(final GameTestHelper helper) throws IOException {
        final ConfigFile file = ConfigFile.builder("jstests-modpack", ConfigSide.SERVER, ConfigFormats.JSON)
                .version(1).key(RADIUS).key(NAME).build();
        final Path modpack = FMLPaths.GAMEDIR.get().resolve(FMLConfig.defaultConfigPath())
                .resolve(file.fileName());
        final Path world = Files.createTempDirectory("jstests-world");
        Files.createDirectories(modpack.getParent());
        Files.writeString(modpack, "{\"config_version\": 1, \"radius\": 40, \"name\": \"packed\"}",
                StandardCharsets.UTF_8);
        try {
            ConfigFiles.loadWorldFile(file, world);
            helper.assertTrue(file.get(RADIUS) == 40 && file.get(NAME).equals("packed"),
                    "a world without the file starts with the modpack's values");
            helper.assertTrue(Files.isRegularFile(world.resolve(file.fileName())), "and keeps its own copy");
        } finally {
            Files.deleteIfExists(modpack);
            Files.deleteIfExists(world.resolve(file.fileName()));
            Files.deleteIfExists(world);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void command_getsSetsAndResetsAWorldsSetting(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
        final ConfigFile file = TestSettings.JSON;
        try {
            server.getCommands().performPrefixedCommand(source, "jstech config set jstests-json note \"set here\"");
            helper.assertTrue(file.get(TestSettings.NOTE).equals("set here"), "set changes the world's setting");
            server.getCommands().performPrefixedCommand(source, "jstech config set jstests-json speed 5000");
            helper.assertTrue(file.get(TestSettings.SPEED) == 1024, "held to its range: " + file.get(TestSettings
                    .SPEED));
            server.getCommands().performPrefixedCommand(source, "jstech config set jstests-json speed fast");
            helper.assertTrue(file.get(TestSettings.SPEED) == 1024, "a value it cannot take changes nothing");
            server.getCommands().performPrefixedCommand(source, "jstech config reset jstests-json speed");
            helper.assertTrue(file.get(TestSettings.SPEED) == 512, "reset puts the default back");
        } finally {
            file.set(TestSettings.NOTE, TestSettings.NOTE.defaultValue());
            file.set(TestSettings.SPEED, TestSettings.SPEED.defaultValue());
        }
        helper.assertTrue(ConfigCommands.readValue("[\"a\", \"b\"]").equals(List.of("a", "b")),
                "a list is written as JSON");
        helper.assertTrue(ConfigCommands.readValue("two words").equals("two words"), "and words as they are");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void command_picksAPresetForTheWorld(final GameTestHelper helper) throws IOException {
        final MinecraftServer server = helper.getLevel().getServer();
        final CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
        final ConfigFile file = TestSettings.JSON;
        ConfigPresets.save(file, SAVED, Map.of("note", "from a preset"));
        try {
            server.getCommands().performPrefixedCommand(source, "jstech config preset jstests-json " + SAVED);
            helper.assertTrue(file.get(TestSettings.NOTE).equals("from a preset"), "the preset's value is set");
            helper.assertTrue(file.get(TestSettings.SPEED) == 512, "and what it does not name is left as it was");
        } finally {
            ConfigPresets.delete(file, SAVED);
            file.set(TestSettings.NOTE, TestSettings.NOTE.defaultValue());
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void mayEdit_onlyAnOperatorChangesAWorldFromAfar(final GameTestHelper helper) {
        final ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        final MinecraftServer server = helper.getLevel().getServer();
        helper.assertFalse(ConfigSync.mayEdit(player), "a player who is no operator may not");
        // The test server makes its operators at level 0, so the entry says the level the game's operators have.
        server.getPlayerList().getOps().add(new ServerOpListEntry(player.getGameProfile(), CorePermissions.OPERATORS,
                false));
        try {
            helper.assertTrue(ConfigSync.mayEdit(player), "an operator may");
        } finally {
            server.getPlayerList().getOps().remove(player.getGameProfile());
        }
        helper.succeed();
    }

    /* A player's file with a setting taken at once, one on reload, one on restart, and a preset, kept nowhere. */
    private static ConfigFile file(final String name) {
        final ConfigFile file = ConfigFile.builder(name, ConfigSide.CLIENT, ConfigFormats.YAML).version(1)
                .key(RADIUS).key(GLOW).key(NAME).preset(GENTLE_PRESET).build();
        file.reset();
        return file;
    }
}
