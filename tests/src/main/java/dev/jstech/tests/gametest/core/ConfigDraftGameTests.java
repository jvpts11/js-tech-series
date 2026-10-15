/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.serialization.Codec;
import dev.jstech.core.config.ConfigControl;
import dev.jstech.core.config.ConfigDraft;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;
import dev.jstech.core.text.TextKey;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.Map;
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
    private static final ConfigKey<String> TINT = ConfigKey.colour("tint", "#39D6C4");
    private static final ConfigKey<List<String>> FUELS = ConfigKey.ids("fuels", List.of("minecraft:coal"),
            ConfigControl.ITEMS);
    private static final ConfigKey<Map<String, Double>> RATES = ConfigKey.of("rates",
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE), Map.of("copper", 1.0));
    private static final ConfigKey<Boolean> VEINS = ConfigKey.flag("veins.on", true);
    private static final ConfigKey<Double> RARITY = ConfigKey.number("veins.rarity", 1.0).activeWhen(VEINS);
    private static final ConfigKey<Integer> SECRET = ConfigKey.whole("veins.secret", 3).shownWhen(VEINS);
    /** What a broken rule between two of the settings says. */
    private static final TextKey RULE = TextKey.of("jstests.config.rule", "The wait is at most the bytes");

    private ConfigDraftGameTests() {
    }

    @GameTest(template = ARENA)
    public static void controlOf_givesEachKindOfSettingItsControl(final GameTestHelper helper) {
        helper.assertTrue(ConfigDraft.controlOf(FLAG) == ConfigDraft.Control.TOGGLE, "a flag is a switch");
        helper.assertTrue(ConfigDraft.controlOf(WAIT) == ConfigDraft.Control.SLIDER
                && ConfigDraft.controlOf(SHARE) == ConfigDraft.Control.SLIDER, "a number in a range is slid");
        helper.assertTrue(ConfigDraft.controlOf(BYTES) == ConfigDraft.Control.NUMBER, "one with none is typed");
        helper.assertTrue(ConfigDraft.controlOf(LOOK) == ConfigDraft.Control.CHOICE, "a word of a list is a choice");
        helper.assertTrue(ConfigDraft.controlOf(GREETING) == ConfigDraft.Control.TEXT, "a free word is typed");
        helper.assertTrue(ConfigDraft.controlOf(NAMES) == ConfigDraft.Control.LIST, "a list has its entries edited");
        helper.assertTrue(ConfigDraft.controlOf(TINT) == ConfigDraft.Control.COLOUR, "a colour is picked");
        helper.assertTrue(ConfigDraft.controlOf(FUELS) == ConfigDraft.Control.IDS, "items are picked from the game's");
        helper.assertTrue(ConfigDraft.controlOf(RATES) == ConfigDraft.Control.MAP, "a map has its entries edited");
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

    @GameTest(template = ARENA)
    public static void undoAndRedo_takeEachStepBackAndAgain(final GameTestHelper helper) {
        final ConfigDraft draft = new ConfigDraft(file(new AtomicInteger()));
        draft.toggle(FLAG);
        draft.step(WAIT, 1, false);
        draft.undo();
        helper.assertTrue(!draft.value(FLAG) && draft.value(WAIT) == 5, "undo takes the last step back");
        draft.undo();
        helper.assertTrue(draft.value(FLAG) && draft.changeCount() == 0, "and the one before");
        helper.assertFalse(draft.canUndo(), "until there is none");
        draft.redo();
        draft.redo();
        helper.assertTrue(!draft.value(FLAG) && draft.value(WAIT) == 6, "redo takes both again");
        draft.undo();
        draft.cycle(LOOK, 1);
        helper.assertFalse(draft.canRedo(), "a new step forgets what was taken back");
        draft.defaults();
        helper.assertTrue(draft.value(LOOK).equals("classic") && !draft.isChanged(FLAG), "defaults is one step");
        draft.undo();
        helper.assertTrue(draft.value(LOOK).equals("modern") && !draft.value(FLAG), "and undone as one");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void slide_isOneStepForADrag(final GameTestHelper helper) {
        final ConfigDraft draft = new ConfigDraft(file(new AtomicInteger()));
        for (int wait = 6; wait <= 12; wait++) {
            draft.slide(WAIT, wait);
        }
        draft.endSlide();
        draft.slide(WAIT, 20);
        draft.undo();
        helper.assertTrue(draft.value(WAIT) == 12, "the second drag is taken back alone");
        draft.undo();
        helper.assertTrue(draft.value(WAIT) == 5, "the first, however many values it passed, at once");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void listsMapsAndColours_areEditedEntryByEntry(final GameTestHelper helper) {
        final ConfigDraft draft = new ConfigDraft(file(new AtomicInteger()));
        helper.assertTrue(draft.addEntry(NAMES, "b") && draft.entries(NAMES).equals(List.of("a", "b")),
                "an entry is added to a list");
        helper.assertTrue(draft.addEntry(NAMES, "12") && draft.value(NAMES).get(2).equals("12"),
                "a number typed into a list of words is taken as the word");
        draft.removeEntry(NAMES, 0);
        helper.assertTrue(draft.value(NAMES).equals(List.of("b", "12")), "and one is taken out");
        helper.assertTrue(draft.putMapEntry(RATES, "tin", "0.5") && draft.value(RATES).get("tin") == 0.5,
                "a map takes a named value");
        helper.assertFalse(draft.putMapEntry(RATES, "lead", "lots"), "but not a word where it holds numbers");
        draft.removeMapEntry(RATES, "copper");
        helper.assertTrue(draft.value(RATES).equals(Map.of("tin", 0.5)), "and loses an entry taken out");
        draft.setColour(TINT, 0xF0B23A);
        helper.assertTrue(draft.value(TINT).equals("#F0B23A") && draft.colourOf(TINT) == 0xF0B23A,
                "a colour is written #RRGGBB and read back");
        helper.assertFalse(draft.typed(TINT, "orange"), "a colour holds nothing but #RRGGBB");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void dependentSettings_followTheSettingTheyStandUnder(final GameTestHelper helper) {
        final ConfigDraft draft = new ConfigDraft(file(new AtomicInteger()));
        helper.assertTrue(draft.isActive(RARITY) && draft.isShown(SECRET), "both are there while the veins are on");
        draft.toggle(VEINS);
        helper.assertFalse(draft.isActive(RARITY), "the rarity is greyed with the veins off");
        helper.assertTrue(draft.isShown(RARITY), "but still shown");
        helper.assertFalse(draft.isShown(SECRET), "and the other is left out");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void rules_sayWhatTheChangesBreak(final GameTestHelper helper) {
        final ConfigFile file = ConfigFile.builder("rules-test", ConfigSide.CLIENT, ConfigFormats.JSON).version(1)
                .key(WAIT).key(BYTES)
                .rule(RULE, values -> values.get(WAIT) <= values.get(BYTES))
                .build();
        file.reset();
        final ConfigDraft draft = new ConfigDraft(file);
        helper.assertTrue(draft.broken().isEmpty(), "the defaults keep the rule");
        draft.typed(BYTES, "2");
        helper.assertTrue(draft.broken().size() == 1 && draft.broken().get(0).message() == RULE,
                "a change that breaks it says so");
        helper.assertTrue(file.broken(file).isEmpty(), "while the file, unchanged, still keeps it");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void plain_exportsAndTakesBackEverySetting(final GameTestHelper helper) {
        final ConfigDraft draft = new ConfigDraft(file(new AtomicInteger()));
        draft.toggle(FLAG);
        draft.typed(GREETING, "Hello");
        final Map<String, Object> exported = draft.toPlain();
        final ConfigDraft other = new ConfigDraft(file(new AtomicInteger()));
        helper.assertTrue(other.takePlain(exported) == 13, "every setting is taken back, " + other.changeCount());
        helper.assertTrue(!other.value(FLAG) && other.value(GREETING).equals("Hello"), "with its value");
        helper.assertTrue(other.pendingPlain().equals(Map.of("boot.menu", false, "greeting", "Hello")),
                "and the changes are what a server would be sent");
        helper.succeed();
    }

    /* A player's file of every kind of setting, kept nowhere, counting its writes. */
    private static ConfigFile file(final AtomicInteger saves) {
        final ConfigFile file = ConfigFile.builder("draft-test", ConfigSide.CLIENT, ConfigFormats.JSON).version(1)
                .key(FLAG).key(WAIT).key(SHARE).key(BYTES).key(LOOK).key(GREETING).key(NAMES).key(TINT).key(FUELS)
                .key(RATES).key(VEINS).key(RARITY).key(SECRET).build();
        file.reset();
        file.onSave(saves::incrementAndGet);
        return file;
    }
}
