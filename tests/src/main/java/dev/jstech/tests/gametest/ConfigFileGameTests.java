/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.mojang.serialization.Codec;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.core.audio.AudioSettings;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.CoreConfigKeys;
import dev.jstech.core.config.IConfigLogger;
import dev.jstech.core.config.IConfigUpgrade;
import dev.jstech.core.config.format.ConfigFormatException;
import dev.jstech.core.config.format.ConfigFormats;
import dev.jstech.core.config.format.IConfigFormat;
import dev.jstech.core.config.format.NbtConfigFormat;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestSettings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A settings file: how reading one upgrades an older layout, leaves a newer one alone, puts right what it lacks or
 * cannot use, and gives back what was written in every format; and the files of a world, written beside it as it
 * starts, read back, and written again when a setting changes.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ConfigFileGameTests {

    private static final String ARENA = "empty";
    private static final ConfigKey<Boolean> MENU = ConfigKey.flag("boot.show_menu", true)
            .comment("Whether the boot menu shows.");
    private static final ConfigKey<Integer> SPEED = ConfigKey.whole("speed", 512).range(1, 1024);
    private static final ConfigKey<List<String>> WORDS = ConfigKey.of("words", Codec.STRING.listOf(), List.of("a"));

    private ConfigFileGameTests() {
    }

    @GameTest(template = ARENA)
    public static void file_takesEachValueAndPassesItOn(final GameTestHelper helper) {
        final List<Integer> taken = new ArrayList<>();
        final ConfigFile file = ConfigFile.builder("test", ConfigSide.SERVER, ConfigFormats.TOML)
                .key(SPEED, taken::add)
                .logger(IConfigLogger.NOOP)
                .build();
        same(helper, 512, file.get(SPEED), "the value before anything is read");

        final ConfigFile.ReadOutcome outcome = file.read(Map.of(ConfigFile.VERSION_KEY, 1, "speed", 100));
        file.reset();

        same(helper, List.of(100, 512), taken, "what was passed on, then the default again");
        helper.assertTrue(!outcome.rewrite(), "a clean file is left as it is");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void file_upgradesAnOlderLayoutStepByStep(final GameTestHelper helper) {
        final ConfigFile file = ConfigFile.builder("test", ConfigSide.SERVER, ConfigFormats.TOML)
                .version(3)
                .key(SPEED)
                .upgrade(1, IConfigUpgrade.rename("velocity", "rate"))
                .upgrade(2, IConfigUpgrade.rename("rate", "speed"))
                .logger(IConfigLogger.NOOP)
                .build();

        final ConfigFile.ReadOutcome outcome = file.read(Map.of(ConfigFile.VERSION_KEY, 1, "velocity", 64));
        final ConfigFile.ReadOutcome unversioned = file(ConfigFormats.TOML).read(clean(file(ConfigFormats.TOML),
                false));

        same(helper, 64, file.get(SPEED), "the value carried through both steps");
        helper.assertTrue(outcome.upgraded() && outcome.rewrite(), "an upgraded file is written back");
        helper.assertTrue(unversioned.foundVersion() == 0 && unversioned.rewrite(),
                "a file from before versions is the layout before the first, and written back");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void file_readsANewerLayoutButNeverWritesOverIt(final GameTestHelper helper) {
        final ConfigFile file = file(ConfigFormats.TOML);
        final int[] saves = {0};
        file.onSave(() -> saves[0]++);

        final ConfigFile.ReadOutcome outcome = file.read(Map.of(ConfigFile.VERSION_KEY, 9, "speed", 100,
                "something_new", true));
        file.set(SPEED, 200);

        helper.assertTrue(outcome.newer() && !outcome.rewrite(), "a newer file is not to be written back");
        same(helper, 200, file.get(SPEED), "the value set in the game");
        same(helper, 0, saves[0], "the times the file was written");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void file_putsRightWhatItLacksOrCannotUse(final GameTestHelper helper) {
        final ConfigFile missing = file(ConfigFormats.TOML);
        final ConfigFile unknown = file(ConfigFormats.TOML);
        final ConfigFile outside = file(ConfigFormats.TOML);
        final ConfigFile clean = file(ConfigFormats.TOML);
        final Map<String, Object> withUnknown = clean(unknown, true);
        withUnknown.put("forgotten", 1);
        final Map<String, Object> withOutside = clean(outside, true);
        withOutside.put("speed", 99_999);

        final boolean lacking = missing.read(Map.of(ConfigFile.VERSION_KEY, 1, "speed", 100)).corrected();
        final boolean extra = unknown.read(withUnknown).corrected();
        final boolean pulled = outside.read(withOutside).rewrite();

        helper.assertTrue(lacking && missing.get(MENU), "a missing value takes its default and asks for a rewrite");
        helper.assertTrue(extra, "a value nothing declares asks for a rewrite");
        helper.assertTrue(pulled && outside.get(SPEED) == 1024, "a value outside its range is pulled in");
        helper.assertTrue(!clean.read(clean(clean, true)).rewrite(), "a clean file asks for nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void file_writesTheVersionFirstAndEachCommentInPlace(final GameTestHelper helper) {
        final ConfigFile file = file(ConfigFormats.TOML);
        final Map<String, Object> plain = file.toPlain();

        same(helper, List.of(ConfigFile.VERSION_KEY, "boot", "speed", "words"), List.copyOf(plain.keySet()),
                "the order of the file");
        same(helper, Map.of("show_menu", true), plain.get("boot"), "the section");
        same(helper, List.of("A file for the tests."), file.commentAt(List.of()), "the top's comment");
        helper.assertTrue(!file.commentAt(List.of(ConfigFile.VERSION_KEY)).isEmpty(), "the version has a comment");
        same(helper, List.of("Starting up."), file.commentAt(List.of("boot")), "the section's comment");
        same(helper, List.of("Whether the boot menu shows.", "Default: true"),
                file.commentAt(List.of("boot", "show_menu")), "the setting's comment");
        same(helper, List.of(), file.commentAt(List.of("nowhere")), "nowhere's comment");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void file_givesBackWhatItWroteInEveryFormat(final GameTestHelper helper) {
        for (final IConfigFormat format : List.of(ConfigFormats.TOML, ConfigFormats.JSON, ConfigFormats.JSON5,
                ConfigFormats.YAML, NbtConfigFormat.INSTANCE)) {
            final ConfigFile written = file(format);
            written.read(Map.of(ConfigFile.VERSION_KEY, 1, "boot", Map.of("show_menu", false), "speed", 7,
                    "words", List.of("x", "y")));
            final ConfigFile read = file(format);
            final ConfigFile.ReadOutcome outcome;
            try {
                outcome = read.read(written.write());
            } catch (final ConfigFormatException e) {
                helper.fail(format.extension() + " could not read what it wrote: " + e.getMessage());
                return;
            }
            same(helper, false, read.get(MENU), format.extension() + "'s boolean");
            same(helper, 7, read.get(SPEED), format.extension() + "'s number");
            same(helper, List.of("x", "y"), read.get(WORDS), format.extension() + "'s list");
            helper.assertTrue(!outcome.rewrite(), format.extension() + " read back clean");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void file_refusesADeclarationThatDoesNotHoldTogether(final GameTestHelper helper) {
        refused(helper, () -> ConfigFile.builder("t", ConfigSide.SERVER, ConfigFormats.TOML)
                .key(ConfigKey.whole("boot", 1)).key(ConfigKey.whole("boot.wait", 1)).build(),
                "a setting that is a section as well");
        refused(helper, () -> ConfigFile.builder("t", ConfigSide.SERVER, ConfigFormats.TOML)
                .upgrade(1, IConfigUpgrade.remove("x")).build(), "a step from a layout not reached");
        refused(helper, () -> ConfigFile.builder("t", ConfigSide.SERVER, ConfigFormats.TOML)
                .key(ConfigKey.whole("a", 1)).key(ConfigKey.flag("a", true)), "two settings at one place");
        refused(helper, () -> ConfigFile.builder("t", ConfigSide.SERVER, ConfigFormats.TOML)
                .key(ConfigKey.whole(ConfigFile.VERSION_KEY, 1)).build(), "the version as a setting");
        refused(helper, () -> ConfigFile.builder("test.toml", ConfigSide.SERVER, ConfigFormats.TOML),
                "a name with its extension");
        refused(helper, () -> file(ConfigFormats.TOML).get(ConfigKey.whole("speed", 512)),
                "a setting of another file");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void worldFiles_areWrittenBesideTheWorldWithTheirComments(final GameTestHelper helper) {
        final Path folder = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("serverconfig");
        try {
            final String yaml = Files.readString(folder.resolve(TestSettings.YAML.fileName()), StandardCharsets.UTF_8);
            final String json5 = Files.readString(folder.resolve(TestSettings.JSON5.fileName()),
                    StandardCharsets.UTF_8);
            helper.assertTrue(yaml.startsWith("# A settings file of the test mod.")
                    && yaml.contains("# Whether the boot menu shows."), "the YAML file's comments:\n" + yaml);
            helper.assertTrue(json5.startsWith("// A settings file of the test mod.")
                    && json5.contains("// Whether the boot menu shows."), "the JSON5 file's comments:\n" + json5);
            helper.assertTrue(Files.isRegularFile(folder.resolve(TestSettings.JSON.fileName())), "the JSON file");
            final Map<String, Object> nbt = NbtConfigFormat.INSTANCE.read(
                    Files.readAllBytes(folder.resolve(TestSettings.NBT.fileName())));
            same(helper, 1, nbt.get(ConfigFile.VERSION_KEY), "the NBT file's version");
        } catch (final IOException | ConfigFormatException e) {
            helper.fail("a world's settings file could not be read: " + e.getMessage());
            return;
        }
        same(helper, 512, TestSettings.YAML.get(TestSettings.SPEED), "the YAML file's speed as read");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void worldFiles_inTomlAreNeoForgesWithTheSettingsComments(final GameTestHelper helper) {
        final String name = TestSettings.TOML.fileName();
        final Path world = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("serverconfig")
                .resolve(name);
        final Path global = FMLPaths.CONFIGDIR.get().resolve(name);
        final Path file = Files.isRegularFile(world) ? world : global;
        try {
            final String toml = Files.readString(file, StandardCharsets.UTF_8);
            helper.assertTrue(toml.contains("config_version = 1") && toml.contains("Whether the boot menu shows.")
                    && toml.contains("[boot]"), "the TOML file NeoForge wrote:\n" + toml);
        } catch (final IOException e) {
            helper.fail("the TOML file is at neither " + world + " nor " + global);
            return;
        }
        same(helper, 512, TestSettings.TOML.get(TestSettings.SPEED), "the TOML file's speed as read");
        same(helper, List.of("a", "b"), TestSettings.TOML.get(TestSettings.WORDS), "the TOML file's list as read");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void worldFiles_areWrittenAgainWhenASettingChanges(final GameTestHelper helper) {
        final Path file = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("serverconfig")
                .resolve(TestSettings.YAML.fileName());
        TestSettings.YAML.set(TestSettings.NOTE, "changed in the game");
        try {
            final Map<String, Object> read = ConfigFormats.YAML.read(Files.readAllBytes(file));
            same(helper, "changed in the game", read.get("note"), "the note on disk");
        } catch (final IOException | ConfigFormatException e) {
            helper.fail("the YAML file could not be read back: " + e.getMessage());
            return;
        } finally {
            TestSettings.YAML.set(TestSettings.NOTE, "untouched");
        }
        helper.succeed();
    }

    /**
     * The series' own files keep the names and the settings they had before they moved onto the settings layer, so
     * what a server or a player set in them is read as it was; a setting renamed without an upgrade step fails here.
     */
    @GameTest(template = ARENA)
    public static void seriesFiles_keepTheirNamesAndTheirSettings(final GameTestHelper helper) {
        same(helper, "jstech-balance.toml", CoreConfigKeys.FILE.fileName(), "the balance file");
        same(helper, "jscomputers-server.toml", ComputersServerConfig.FILE.fileName(), "the computers' file");
        same(helper, List.of("boot.show_boot_menu", "install_by_hand.gentoo_every_step",
                        "install_by_hand.arch_every_step", "prompt.list_commands", "soundfoundry.catalog",
                        "soundfoundry.ethernet_kilobytes_per_second", "programs.outside_components"),
                ComputersServerConfig.FILE.keys().stream().map(ConfigKey::dottedPath).toList(),
                "the computers' settings");
        same(helper, "jstech-audio.json", AudioSettings.FILE.fileName(), "the player's sound file");
        same(helper, ConfigSide.CLIENT, AudioSettings.FILE.side(), "whose the sound file is");
        same(helper, List.of("volumes", "muted", "visual_cues", "occlusion", "duck_under_alerts"),
                AudioSettings.FILE.keys().stream().map(ConfigKey::dottedPath).toList(), "the sound settings");
        helper.succeed();
    }

    /* A faster cable carries a song faster in every era, and a backbone older than the Standard is not the faster. */
    @GameTest(template = ARENA)
    public static void songBytesPerSecond_followsTheThroughputOfTheLine(final GameTestHelper helper) {
        final long ethernet = ComputersServerConfig.songBytesPerSecond(
                new DataLink(DataLine.ACCESS, HardwareEra.STANDARD), 512);
        same(helper, 512L * 1_024L, ethernet, "the Ethernet's own speed");
        same(helper, ethernet, ComputersServerConfig.songBytesPerSecond(null, 512), "a way not yet known");
        final long legacyBackbone = ComputersServerConfig.songBytesPerSecond(
                new DataLink(DataLine.BACKBONE, HardwareEra.LEGACY), 512);
        final long standardBackbone = ComputersServerConfig.songBytesPerSecond(
                new DataLink(DataLine.BACKBONE, HardwareEra.STANDARD), 512);
        final long advancedHpc = ComputersServerConfig.songBytesPerSecond(
                new DataLink(DataLine.HPC, HardwareEra.ADVANCED), 512);
        helper.assertTrue(standardBackbone > legacyBackbone, "a Standard backbone is faster than a Legacy one");
        helper.assertTrue(advancedHpc > standardBackbone, "an Advanced HPC fabric is faster than a backbone");
        helper.succeed();
    }

    private static ConfigFile file(final IConfigFormat format) {
        return ConfigFile.builder("test", ConfigSide.SERVER, format)
                .comment("A file for the tests.")
                .sectionComment("boot", "Starting up.")
                .key(MENU)
                .key(SPEED)
                .key(WORDS)
                .logger(IConfigLogger.NOOP)
                .build();
    }

    /** A file holding every setting at its default, with or without its version. */
    private static Map<String, Object> clean(final ConfigFile file, final boolean withVersion) {
        final Map<String, Object> plain = new LinkedHashMap<>(file.toPlain());
        if (!withVersion) {
            plain.remove(ConfigFile.VERSION_KEY);
        }
        return plain;
    }

    private static void same(final GameTestHelper helper, final Object expected, final Object actual,
                             final String what) {
        helper.assertTrue(Objects.equals(expected, actual), what + ": expected " + expected + ", got " + actual);
    }

    private static void refused(final GameTestHelper helper, final Runnable declaration, final String what) {
        try {
            declaration.run();
        } catch (final IllegalArgumentException expected) {
            return;
        }
        helper.fail(what + " was not refused");
    }
}
