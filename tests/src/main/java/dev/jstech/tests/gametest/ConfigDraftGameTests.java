/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.mojang.serialization.Codec;
import dev.jstech.core.config.ConfigDraft;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What a player changes on the Core's settings screen, held in its draft: each kind of setting gets its control,
 * nothing reaches the file before Done, numbers stop at their ends, words go round their list, what is typed is read
 * as the setting's kind, and the defaults come back as changes like any other.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ConfigDraftGameTests {

    private static final String ARENA = "empty";
    private static final ConfigKey<Boolean> FLAG = ConfigKey.flag("boot.menu", true);
    private static final ConfigKey<Integer> WAIT = ConfigKey.whole("boot.wait", 5).range(0, 30);
    private static final ConfigKey<Double> SHARE = ConfigKey.number("share", 0.5).range(0.0, 1.0);
    private static final ConfigKey<Long> BYTES = ConfigKey.wholeLong("bytes", 100L);
    private static final ConfigKey<String> LOOK = ConfigKey.text("look", "classic").allowing("classic", "modern",
            "plain");
    private static final ConfigKey<String> GREETING = ConfigKey.text("greeting", "Ready.");
    private static final ConfigKey<List<String>> NAMES = ConfigKey.of("names", Codec.STRING.listOf(), List.of("a"));

    private ConfigDraftGameTests() {
    }

    @GameTest(template = ARENA)
    public static void controlOf_givesEachKindOfSettingItsControl(final GameTestHelper helper) {
        helper.assertTrue(ConfigDraft.controlOf(FLAG) == ConfigDraft.Control.TOGGLE, "a flag is a switch");
        helper.assertTrue(ConfigDraft.controlOf(WAIT) == ConfigDraft.Control.NUMBER
                && ConfigDraft.controlOf(SHARE) == ConfigDraft.Control.NUMBER
                && ConfigDraft.controlOf(BYTES) == ConfigDraft.Control.NUMBER, "every number is typed or stepped");
        helper.assertTrue(ConfigDraft.controlOf(LOOK) == ConfigDraft.Control.CHOICE, "a word of a list is a choice");
        helper.assertTrue(ConfigDraft.controlOf(GREETING) == ConfigDraft.Control.TEXT, "a free word is typed");
        helper.assertTrue(ConfigDraft.controlOf(NAMES) == ConfigDraft.Control.FIXED, "a list is changed in its file");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void changes_stayInTheDraftUntilApplied(final GameTestHelper helper) {
        final AtomicInteger saves = new AtomicInteger();
        final ConfigFile file = file(saves);
        final ConfigDraft draft = new ConfigDraft(file);
        draft.toggle(FLAG);
        draft.step(WAIT, 1, false);
        helper.assertTrue(!draft.value(FLAG) && draft.value(WAIT) == 6, "the draft shows the changes");
        helper.assertTrue(file.get(FLAG) && saves.get() == 0, "while the file is untouched");
        draft.apply();
        helper.assertTrue(!file.get(FLAG) && file.get(WAIT) == 6, "applied, the file holds them");
        helper.assertTrue(saves.get() == 1, "and writes itself once for them all; it wrote " + saves.get());
        helper.assertTrue(draft.changed().isEmpty(), "and the draft is clear again");
        draft.apply();
        helper.assertTrue(saves.get() == 1, "with nothing changed, applying writes nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void stepsAndWords_stayInsideWhatTheSettingAllows(final GameTestHelper helper) {
        final ConfigDraft draft = new ConfigDraft(file(new AtomicInteger()));
        for (int i = 0; i < 4; i++) {
            draft.step(WAIT, 1, true);
        }
        helper.assertTrue(draft.value(WAIT) == 30, "a number stops at the top of its range");
        draft.step(SHARE, -1, true);
        draft.step(SHARE, 1, false);
        helper.assertTrue(Math.abs(draft.value(SHARE) - 0.1) < 1e-9, "a fraction steps by a tenth from its floor");
        draft.cycle(LOOK, -1);
        helper.assertTrue(draft.value(LOOK).equals("plain"), "a word goes round its list backwards");
        draft.cycle(LOOK, 1);
        helper.assertTrue(!draft.isChanged(LOOK), "and a change back to the file's value is no change");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void typed_isReadAsTheSettingsKind(final GameTestHelper helper) {
        final ConfigDraft draft = new ConfigDraft(file(new AtomicInteger()));
        helper.assertTrue(draft.typed(WAIT, " 12 ") && draft.value(WAIT) == 12, "a number is read");
        helper.assertTrue(draft.typed(WAIT, "99") && draft.value(WAIT) == 30, "and held to its range");
        helper.assertFalse(draft.typed(WAIT, "twelve"), "a word is no number");
        helper.assertFalse(draft.typed(SHARE, "NaN"), "nor is not-a-number");
        helper.assertFalse(draft.typed(LOOK, "fancy"), "a word outside its list is refused");
        helper.assertTrue(draft.typed(GREETING, "Hello") && draft.value(GREETING).equals("Hello"),
                "a free word is taken as it is");
        helper.assertFalse(draft.typed(NAMES, "b"), "a list is not typed");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void defaults_comeBackAsChangesToKeep(final GameTestHelper helper) {
        final ConfigFile file = file(new AtomicInteger());
        file.set(WAIT, 20);
        file.set(FLAG, false);
        final ConfigDraft draft = new ConfigDraft(file);
        draft.defaults();
        helper.assertTrue(draft.value(WAIT) == 5 && draft.value(FLAG), "the draft holds the defaults");
        helper.assertTrue(file.get(WAIT) == 20, "the file keeps its values until the draft is applied");
        draft.apply();
        helper.assertTrue(file.get(WAIT) == 5 && file.get(FLAG), "then it holds the defaults");
        helper.succeed();
    }

    /* A player's file of every kind of setting, kept nowhere, counting its writes. */
    private static ConfigFile file(final AtomicInteger saves) {
        final ConfigFile file = ConfigFile.builder("draft-test", ConfigSide.CLIENT, ConfigFormats.JSON).version(1)
                .key(FLAG).key(WAIT).key(SHARE).key(BYTES).key(LOOK).key(GREETING).key(NAMES).build();
        file.reset();
        file.onSave(saves::incrementAndGet);
        return file;
    }
}
