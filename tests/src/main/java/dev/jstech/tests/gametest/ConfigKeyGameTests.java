/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.mojang.serialization.Codec;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigValidator;
import dev.jstech.core.config.IConfigLogger;
import dev.jstech.core.config.IConfigValidationResult;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * One setting of a settings file, and what reading a value for it gives: a value written through its codec, a number
 * pulled into its range, a word held to its list, and anything that cannot be read set aside for the default. Here
 * rather than in the unit tests because a setting's codec is the game's.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ConfigKeyGameTests {

    private static final String ARENA = "empty";

    private ConfigKeyGameTests() {
    }

    @GameTest(template = ARENA)
    public static void key_splitsADottedPathAndRefusesAnEmptyPart(final GameTestHelper helper) {
        final ConfigKey<Boolean> key = ConfigKey.flag("boot.show_boot_menu", true);

        same(helper, List.of("boot", "show_boot_menu"), key.path(), "the path's parts");
        same(helper, "boot.show_boot_menu", key.dottedPath(), "the dotted path");
        same(helper, "show_boot_menu", key.name(), "the name");
        refused(helper, () -> ConfigKey.flag("boot..menu", true), "an empty part of a path");
        refused(helper, () -> ConfigKey.flag("", true), "an empty path");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void key_refusesARangeOrAListThatDoesNotFitIt(final GameTestHelper helper) {
        refused(helper, () -> ConfigKey.whole("a", 5).range(0L, 10L), "a range in another kind of number");
        refused(helper, () -> ConfigKey.whole("a", 50).range(100, 200), "a default outside its range");
        refused(helper, () -> ConfigKey.flag("a", true).range(0, 1), "a range on a setting that is no number");
        refused(helper, () -> ConfigKey.whole("a", 1).allowing("1"), "a list of words on a setting that is no text");
        refused(helper, () -> ConfigKey.text("a", "MYSQL").allowing("SIMPLE"), "a default the list does not allow");
        refused(helper, () -> ConfigKey.text("a", "SIMPLE").allowing(), "a list of no words");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void key_describesItsBoundsAndItsDefaultUnderItsComment(final GameTestHelper helper) {
        same(helper, List.of("How fast it goes.", "Range: 1 to 1024", "Default: 512"),
                ConfigKey.whole("speed", 512).range(1, 1024).comment("How fast it goes.").describedComment(),
                "a number's comment");
        same(helper, List.of("One of: SIMPLE, STANDARD", "Default: SIMPLE"),
                ConfigKey.text("dialect", "SIMPLE").allowing("SIMPLE", "STANDARD").describedComment(),
                "a word's comment");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void key_readsAnyValueItsCodecReads(final GameTestHelper helper) {
        final ConfigKey<Map<String, Float>> volumes = ConfigKey.of("volumes",
                Codec.unboundedMap(Codec.STRING, Codec.FLOAT), Map.of("master", 1.0f));

        same(helper, Map.of("master", 0.5f, "music", 0.25f),
                volumes.read(Map.of("master", 0.5, "music", 0.25)).result().orElse(null), "a map read back");
        same(helper, Map.of("master", 1.0), volumes.plain(Map.of("master", 1.0f)), "a map written");
        same(helper, true, ConfigKey.flag("on", false).read((byte) 1).result().orElse(null),
                "a number where a boolean belongs");
        same(helper, 2.0, ConfigKey.number("factor", 0.5).read(2).result().orElse(null),
                "a whole number for a fraction");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void validator_keepsAValueThatReadsAndKeepsToItsBounds(final GameTestHelper helper) {
        final List<String> log = new ArrayList<>();
        final ConfigValidator validator = new ConfigValidator(log::add);

        final IConfigValidationResult<Long> inRange = validator.validate(ConfigKey.wholeLong("k", 50L).range(0L, 100L),
                75L);
        final IConfigValidationResult<Long> smaller = validator.validate(ConfigKey.wholeLong("k", 50L), 75);
        final IConfigValidationResult<String> allowed = validator.validate(
                ConfigKey.text("k", "SIMPLE").allowing("SIMPLE", "STANDARD"), "STANDARD");

        helper.assertTrue(inRange instanceof IConfigValidationResult.Valid<Long>, "a value in its range is kept");
        same(helper, 75L, smaller.value(), "a whole number of a smaller kind");
        same(helper, "STANDARD", allowed.value(), "an allowed word");
        same(helper, List.of(), log, "what was said in the log");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void validator_refusesANumberThatIsNotANumber(final GameTestHelper helper) {
        final List<String> log = new ArrayList<>();
        final ConfigValidator validator = new ConfigValidator(log::add);

        final IConfigValidationResult<Double> result =
                validator.validate(ConfigKey.number("share", 0.5).range(0.0, 1.0), Double.NaN);

        helper.assertTrue(result instanceof IConfigValidationResult.Rejected<Double>,
                "not a number is refused rather than pulled to the top of the range: " + result);
        same(helper, 0.5, result.value(), "the default is used instead");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void validator_pullsANumberOutsideItsRangeToTheNearerEnd(final GameTestHelper helper) {
        final List<String> log = new ArrayList<>();
        final ConfigValidator validator = new ConfigValidator(log::add);
        final ConfigKey<Long> key = ConfigKey.wholeLong("balance.macerator.fe_per_tick", 100L).range(10L, 1000L);

        final IConfigValidationResult<Long> below = validator.validate(key, 5L);
        final IConfigValidationResult<Long> above = validator.validate(key, 5000L);
        final IConfigValidationResult<Double> fraction =
                validator.validate(ConfigKey.number("k", 1.0).range(0.5, 2.0), 0.25);

        helper.assertTrue(below instanceof IConfigValidationResult.Clamped<Long> clamped
                && clamped.value() == 10L && clamped.original() == 5L, "below the range: " + below);
        same(helper, 1000L, above.value(), "above the range");
        same(helper, 0.5, fraction.value(), "a fraction below its range");
        helper.assertTrue(log.size() == 3 && log.get(0).contains("balance.macerator.fe_per_tick")
                && log.get(0).contains("clamped to 10"), "the log names the setting and the end: " + log);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void validator_setsAsideWhatCannotBeReadForTheDefault(final GameTestHelper helper) {
        final List<String> log = new ArrayList<>();
        final ConfigValidator validator = new ConfigValidator(log::add);

        final IConfigValidationResult<Long> nothing = validator.validate(ConfigKey.wholeLong("k", 42L), null);
        final IConfigValidationResult<Long> word = validator.validate(ConfigKey.wholeLong("k", 42L), "not a number");
        final IConfigValidationResult<String> unknown = validator.validate(
                ConfigKey.text("computing.sql_dialect", "SIMPLE").allowing("SIMPLE", "STANDARD"), "ORACLE");
        final IConfigValidationResult<String> wrongCase = new ConfigValidator(IConfigLogger.NOOP).validate(
                ConfigKey.text("k", "SIMPLE").allowing("SIMPLE", "STANDARD"), "standard");

        helper.assertTrue(nothing instanceof IConfigValidationResult.Rejected<Long> && nothing.value() == 42L,
                "no value: " + nothing);
        helper.assertTrue(word instanceof IConfigValidationResult.Rejected<Long> && word.value() == 42L,
                "a word for a number: " + word);
        same(helper, "SIMPLE", unknown.value(), "a word not allowed");
        same(helper, "SIMPLE", wrongCase.value(), "a word in the wrong case");
        helper.assertTrue(log.size() == 3 && log.get(0).contains("no value") && log.get(1).contains("cannot be read")
                && log.get(2).contains("computing.sql_dialect"), "the log says why each was set aside: " + log);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void validator_keepsWhatCanBeReadOfAMapOrAList(final GameTestHelper helper) {
        final List<String> log = new ArrayList<>();
        final ConfigKey<Map<String, Float>> volumes = ConfigKey.of("volumes",
                Codec.unboundedMap(Codec.STRING, Codec.FLOAT), Map.of());

        final IConfigValidationResult<Map<String, Float>> result = new ConfigValidator(log::add)
                .validate(volumes, Map.of("machines", "loud", "music", 0.25));

        helper.assertTrue(result instanceof IConfigValidationResult.Repaired<Map<String, Float>>,
                "a map with one entry written wrong is repaired: " + result);
        same(helper, Map.of("music", 0.25f), result.value(), "what was kept of the map");
        helper.assertTrue(log.size() == 1 && log.get(0).contains("the rest is kept"), "the log says so: " + log);
        helper.succeed();
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
