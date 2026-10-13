/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import dev.jstech.core.persistence.ISaveUpgrade;
import dev.jstech.core.persistence.SaveLayout;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.Objects;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The layout of a saved thing: today's version written beside what is saved, something saved in an older version
 * brought up by its steps in order, something saved before there were versions read as version 0, and something
 * saved by a newer mod read as it is; for a compound, for a value saved whole, and for a value a codec writes.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SaveLayoutGameTests {

    private static final String ARENA = "empty";

    private SaveLayoutGameTests() {
    }

    @GameTest(template = ARENA)
    public static void read_leavesACompoundOfTodaysVersionAsItIs(final GameTestHelper helper) {
        final SaveLayout layout = renames();
        final CompoundTag saved = layout.stamp(named("New", 5));

        same(helper, 3, SaveLayout.versionOf(saved), "the version written");
        helper.assertTrue(layout.read(saved) == saved, "today's compound is handed back as it is");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void read_bringsAnOldCompoundUpStepByStepOnACopy(final GameTestHelper helper) {
        final CompoundTag old = named("Old", 5);

        final CompoundTag read = renames().read(old);

        same(helper, 5, read.getInt("New"), "the value, renamed by both steps in order");
        helper.assertTrue(!read.contains("Old") && !read.contains("Mid"), "the older names are gone");
        same(helper, named("Old", 5), old, "the compound handed in, untouched");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void read_takesACompoundFromBeforeVersionsAsVersionZero(final GameTestHelper helper) {
        final CompoundTag before = named("Speed", 7);

        same(helper, 0, SaveLayout.versionOf(before), "the version of a compound that says none");
        helper.assertTrue(SaveLayout.of("first").read(before) == before,
                "a layout at its first version reads it as it is");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void read_takesANewerCompoundAsItIs(final GameTestHelper helper) {
        final CompoundTag newer = named("Later", 1);
        newer.putInt(SaveLayout.VERSION_KEY, 9);

        helper.assertTrue(renames().isNewer(9), "version 9 is newer than 3");
        helper.assertTrue(renames().read(newer) == newer, "a newer compound is read as it is");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void unwrap_givesBackAWrappedValueWithItsVersion(final GameTestHelper helper) {
        final SaveLayout layout = SaveLayout.builder("whole").version(2).build();
        final ListTag value = numbers(1, 2);

        final SaveLayout.Found found = layout.unwrap(layout.wrap(value));

        same(helper, 2, found.version(), "the version it was saved in");
        same(helper, value, found.value(), "the value");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void unwrap_readsAFileFromBeforeVersionsThroughItsFirstStep(final GameTestHelper helper) {
        // A file from before versions held its list under a name of its own; version 1 holds the list itself.
        final SaveLayout layout = SaveLayout.builder("legacy").version(1)
                .upgrade(0, before -> before instanceof CompoundTag old ? old.getList("Numbers", Tag.TAG_INT)
                        : before)
                .build();
        final CompoundTag old = new CompoundTag();
        old.put("Numbers", numbers(4, 5));

        final SaveLayout.Found found = layout.unwrap(old);

        same(helper, 0, found.version(), "the version of a file from before versions");
        same(helper, numbers(4, 5), found.value(), "the list, brought out by the first step");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void codec_writesTheVersionBesideTheValueAndReadsItBack(final GameTestHelper helper) {
        final Codec<List<Integer>> codec = SaveLayout.builder("codec").version(2).build()
                .codec(Codec.INT.listOf());

        final Tag written = codec.encodeStart(NbtOps.INSTANCE, List.of(1, 2, 3)).getOrThrow();

        helper.assertTrue(written instanceof CompoundTag compound && SaveLayout.versionOf(compound) == 2,
                "the version beside the value: " + written);
        same(helper, List.of(1, 2, 3), codec.parse(NbtOps.INSTANCE, written).getOrThrow(), "the value read back");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void codec_readsAValueSavedBeforeItHadALayout(final GameTestHelper helper) {
        final Codec<List<Integer>> codec = SaveLayout.of("plain").codec(Codec.INT.listOf());

        same(helper, List.of(1, 2), codec.parse(NbtOps.INSTANCE, numbers(1, 2)).getOrThrow(),
                "a list written bare, as the codec wrote it before");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void codec_runsTheStepsOnAnOlderValueWithTheWorldsRegistries(final GameTestHelper helper) {
        // Version 1 counted from 0, version 2 counts from 10: the step adds 10 to every number.
        final Codec<List<Integer>> codec = SaveLayout.builder("shifted").version(2)
                .upgrade(1, before -> {
                    final ListTag after = new ListTag();
                    for (final Tag each : (CollectionTag<?>) before) {
                        after.add(IntTag.valueOf(((NumericTag) each).getAsInt() + 10));
                    }
                    return after;
                })
                .build()
                .codec(Codec.INT.listOf());
        final DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess());
        final CompoundTag old = new CompoundTag();
        old.putInt(SaveLayout.VERSION_KEY, 1);
        old.put(SaveLayout.VALUE_KEY, numbers(1, 2));

        same(helper, List.of(11, 12), codec.parse(ops, old).getOrThrow(), "the numbers taken by the step");
        same(helper, List.of(1, 2), codec.parse(ops, codec.encodeStart(ops, List.of(1, 2)).getOrThrow())
                .getOrThrow(), "today's value, which takes no step");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void compoundStep_leavesWhatIsNotACompoundAlone(final GameTestHelper helper) {
        final Tag word = StringTag.valueOf("disk");

        helper.assertTrue(ISaveUpgrade.remove("x").upgrade(word) == word, "a word passes a compound's step as it is");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void layout_refusesAStepFromAVersionNotReached(final GameTestHelper helper) {
        try {
            SaveLayout.builder("ahead").version(2).upgrade(2, ISaveUpgrade.remove("x")).build();
        } catch (final IllegalArgumentException expected) {
            helper.succeed();
            return;
        }
        helper.fail("a step from version 2 of a layout at version 2 was not refused");
    }

    /** A layout at version 3 whose first step renames Old to Mid and whose third renames Mid to New. */
    private static SaveLayout renames() {
        return SaveLayout.builder("renames").version(3)
                .upgrade(2, ISaveUpgrade.rename("Mid", "New"))
                .upgrade(0, ISaveUpgrade.rename("Old", "Mid"))
                .build();
    }

    private static CompoundTag named(final String key, final int value) {
        final CompoundTag tag = new CompoundTag();
        tag.putInt(key, value);
        return tag;
    }

    private static ListTag numbers(final int... values) {
        final ListTag list = new ListTag();
        for (final int value : values) {
            list.add(IntTag.valueOf(value));
        }
        return list;
    }

    private static void same(final GameTestHelper helper, final Object expected, final Object actual,
                             final String what) {
        helper.assertTrue(Objects.equals(expected, actual), what + ": expected " + expected + ", got " + actual);
    }
}
