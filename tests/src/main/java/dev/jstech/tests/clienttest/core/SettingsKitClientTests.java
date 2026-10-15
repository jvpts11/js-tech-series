/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.config.ConfigPicker;
import dev.jstech.core.client.config.ConfigPresetsPopup;
import dev.jstech.core.client.config.ConfigTextPopup;
import dev.jstech.core.client.config.CoreConfigScreen;
import dev.jstech.core.config.ConfigDraft;
import dev.jstech.core.config.ConfigScreenTexts;
import dev.jstech.core.text.GameText;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.TestShowcaseSettings;
import java.util.List;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.lwjgl.glfw.GLFW;

/**
 * The Core's settings screen on a player's game, opened on a file with every kind of control: in the game's own look
 * for a person to judge, its groups folding, the filter of what changed, settings under others and a rule between two,
 * the picker of the game's things, the presets, the settings carried as text, undo and redo, and the keyboard.
 */
public final class SettingsKitClientTests {

    private SettingsKitClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void settingsScreen_showsEveryControlInTheGamesLook(final ClientTestContext ctx) {
        ctx.afterTest(() -> close(ctx));
        ctx.then(0, () -> open(ctx))
                .thenAwaitScreen(CoreConfigScreen.class, 40)
                .thenAssert(1, () -> screen(ctx).scopeLabels().equals(List.of("This player")),
                        "the file is a player's: one tab")
                .thenAssert(0, () -> screen(ctx).railLabels().equals(List.of("World", "Look")),
                        "its sections by their names")
                .then(0, () -> screen(ctx).openSection("World"))
                .thenAssert(1, () -> screen(ctx).rowNames().containsAll(List.of("The vein system",
                        "Each deposit type's rarity", "Don't generate these items' ores",
                        "Loaded chunks, whole server")), "the world's settings in their groups")
                .thenScreenshot(3, "settings_world")
                .then(0, () -> screen(ctx).openSection("Look"))
                .thenScreenshot(3, "settings_look");
    }

    @ClientTest(timeoutTicks = 400)
    public static void settingsScreen_foldsGroupsAndFiltersWhatChanged(final ClientTestContext ctx) {
        ctx.afterTest(() -> close(ctx));
        ctx.then(0, () -> open(ctx))
                .thenAwaitScreen(CoreConfigScreen.class, 40)
                .then(1, () -> {
                    screen(ctx).openSection("World");
                    ctx.assertTrue(screen(ctx).foldGroup("Veins"), "the veins' header folds them");
                    ctx.assertTrue(!screen(ctx).rowNames().contains("The vein system"), "folded, they are hidden");
                    screen(ctx).foldGroup("Veins");
                    ctx.assertTrue(screen(ctx).rowNames().contains("The vein system"), "and open again");
                    screen(ctx).setOnlyChanged(true);
                    ctx.assertTrue(screen(ctx).rowNames().isEmpty(), "every setting is at its default");
                    screen(ctx).openDraft().toggle(TestShowcaseSettings.VEINS);
                    ctx.assertEquals(List.of("The vein system"), screen(ctx).rowNames(),
                            "the filter shows the one changed");
                    ctx.assertEquals("1 changed, 1 on restart", screen(ctx).statusText(),
                            "and the footer counts it, and that it waits for a restart");
                    screen(ctx).undo();
                    ctx.assertEquals(GameText.resolve(ConfigScreenTexts.NOTHING_CHANGED), screen(ctx).statusText(),
                            "undo takes it back");
                    screen(ctx).redo();
                    ctx.assertTrue(screen(ctx).openDraft().isChanged(TestShowcaseSettings.VEINS), "redo again");
                });
    }

    @ClientTest(timeoutTicks = 400)
    public static void settingsScreen_followsDependenciesAndRules(final ClientTestContext ctx) {
        ctx.afterTest(() -> close(ctx));
        ctx.then(0, () -> open(ctx))
                .thenAwaitScreen(CoreConfigScreen.class, 40)
                .then(1, () -> {
                    screen(ctx).openSection("World");
                    ctx.assertTrue(screen(ctx).rowNames().contains("Reservoirs run dry"), "shown with the veins on");
                    final ConfigDraft draft = screen(ctx).openDraft();
                    draft.toggle(TestShowcaseSettings.VEINS);
                    ctx.assertTrue(!screen(ctx).rowNames().contains("Reservoirs run dry"),
                            "a setting shown only under the veins leaves with them");
                    ctx.assertTrue(screen(ctx).rowNames().contains("Each deposit type's rarity"),
                            "one active under them stays, greyed");
                    draft.typed(TestShowcaseSettings.FEWEST_CHUNKS, "900");
                    draft.typed(TestShowcaseSettings.MOST_CHUNKS, "100");
                })
                .thenAssert(2, () -> screen(ctx).bannerText().equals(GameText.resolve(
                        TestShowcaseSettings.FEWER_THAN_MOST)), "the broken rule is said over the settings")
                .thenScreenshot(1, "settings_rule")
                .then(0, () -> {
                    screen(ctx).done();
                    ctx.assertTrue(ctx.mc().screen instanceof CoreConfigScreen, "Done keeps nothing that breaks it");
                    ctx.assertTrue(TestShowcaseSettings.FILE.get(TestShowcaseSettings.VEINS), "and the file is as was");
                });
    }

    @ClientTest(timeoutTicks = 500)
    public static void settingsScreen_picksPresetsAndCarriesText(final ClientTestContext ctx) {
        ctx.afterTest(() -> close(ctx));
        ctx.then(0, () -> open(ctx))
                .thenAwaitScreen(CoreConfigScreen.class, 40)
                .then(1, () -> {
                    screen(ctx).openSection("World");
                    ctx.assertTrue(screen(ctx).pressFor("Don't generate these items' ores",
                            GameText.resolve(ConfigScreenTexts.ADD)), "the list has its + Add");
                })
                .thenScreenshot(2, "settings_picker")
                .then(0, () -> {
                    final ConfigPicker picker = (ConfigPicker) screen(ctx).openPopup();
                    picker.searchFor("diamond");
                    ctx.assertTrue(picker.shownIds().contains("minecraft:diamond"), "the search finds the diamond");
                    picker.choose(picker.shownIds().indexOf("minecraft:diamond"));
                    ctx.assertTrue(screen(ctx).openDraft().value(TestShowcaseSettings.SKIPPED)
                            .contains("minecraft:diamond"), "and choosing adds it to the list");
                    screen(ctx).press(GameText.resolve(ConfigScreenTexts.PRESETS));
                })
                .then(1, () -> {
                    final ConfigPresetsPopup presets = (ConfigPresetsPopup) screen(ctx).openPopup();
                    ctx.assertTrue(presets.names().contains("Gentle consequences"), "the mod's preset is offered");
                    presets.pick("Gentle consequences");
                    ctx.assertTrue(!screen(ctx).openDraft().value(TestShowcaseSettings.VEINS)
                            && screen(ctx).openDraft().value(TestShowcaseSettings.MOST_CHUNKS) == 100,
                            "picking it sets what it names");
                    screen(ctx).press(GameText.resolve(ConfigScreenTexts.EXPORT));
                })
                .then(1, () -> {
                    final ConfigTextPopup exported = (ConfigTextPopup) screen(ctx).openPopup();
                    ctx.assertTrue(exported.text().contains("most"), "the export holds the settings as text");
                    exported.close();
                    screen(ctx).press(GameText.resolve(ConfigScreenTexts.IMPORT));
                })
                .then(1, () -> {
                    final ConfigTextPopup importing = (ConfigTextPopup) screen(ctx).openPopup();
                    importing.setText("{\"look\": {\"greeting\": \"Imported\"}}");
                    importing.take();
                    ctx.assertEquals("Imported", screen(ctx).openDraft().value(TestShowcaseSettings.GREETING),
                            "pasted text is taken");
                });
    }

    @ClientTest(timeoutTicks = 300)
    public static void settingsScreen_isReachedByTheKeyboard(final ClientTestContext ctx) {
        ctx.afterTest(() -> close(ctx));
        ctx.then(0, () -> open(ctx))
                .thenAwaitScreen(CoreConfigScreen.class, 40)
                .then(1, () -> {
                    ctx.mc().screen.keyPressed(GLFW.GLFW_KEY_F, 0, GLFW.GLFW_MOD_CONTROL);
                    for (final char c : "accent".toCharArray()) {
                        ctx.mc().screen.charTyped(c, 0);
                    }
                })
                .thenAssert(1, () -> screen(ctx).rowNames().equals(List.of("Accent colour")),
                        "Ctrl+F reaches the search, and typing finds")
                .then(0, () -> {
                    ctx.mc().screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
                    ctx.mc().screen.keyPressed(GLFW.GLFW_KEY_TAB, 0, 0);
                    ctx.mc().screen.keyPressed(GLFW.GLFW_KEY_TAB, 0, 0);
                })
                .thenScreenshot(1, "settings_keyboard");
    }

    /** Opens the test mod's settings the way the mods list does, every setting at its default first. */
    private static void open(final ClientTestContext ctx) {
        final ConfigDraft reset = new ConfigDraft(TestShowcaseSettings.FILE);
        reset.defaults();
        reset.apply();
        final ModContainer container = ModList.get().getModContainerById(JsTests.MODID).orElseThrow();
        final IConfigScreenFactory factory = container.getCustomExtension(IConfigScreenFactory.class).orElseThrow();
        ctx.mc().setScreen(factory.createScreen(container, null));
    }

    private static void close(final ClientTestContext ctx) {
        ctx.mc().setScreen(null);
        final ConfigDraft reset = new ConfigDraft(TestShowcaseSettings.FILE);
        reset.defaults();
        reset.apply();
    }

    private static CoreConfigScreen screen(final ClientTestContext ctx) {
        return ctx.screen(CoreConfigScreen.class);
    }
}
