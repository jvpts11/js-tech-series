/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.config.CoreConfigScreen;
import dev.jstech.core.config.ConfigDraft;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigTexts;
import dev.jstech.core.config.format.ConfigFormats;
import dev.jstech.core.gui.layout.ConfigScreenLayout;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * The Core's settings screen, reached from the mods list, for the series' mods: each opens it, its world's and its
 * player's settings are there, every section and setting under its own name rather than the key it is translated by
 * and short enough to be read whole in every language the series ships, and a change is kept by Done and dropped by
 * Cancel.
 */
public final class SettingsScreenClientTests {

    /** How a key that found no translation shows: as itself, the mod's settings prefix and all. */
    private static final String RAW_KEY = ".configuration.";
    /** The languages the series ships its names in, each read on its own beside English. */
    private static final List<String> LANGUAGES = List.of("en_us", "pt_br");
    private SettingsScreenClientTests() {
    }

    @ClientTest(timeoutTicks = 200)
    public static void settingsScreen_everyNameFitsBesideItsSettingInEveryLanguage(final ClientTestContext ctx) {
        ctx.then(0, () -> {
            final int width = ctx.mc().getWindow().getGuiScaledWidth();
            final List<String> wide = new ArrayList<>();
            for (final String language : LANGUAGES) {
                final Language names = ClientLanguage.loadFrom(ctx.mc().getResourceManager(),
                        List.of("en_us", language), false);
                for (final String modId : List.of(JsCore.MODID, JsComputers.MODID)) {
                    for (final ConfigFile file : ConfigFiles.of(modId)) {
                        if (file.format() != ConfigFormats.TOML) {
                            continue;
                        }
                        for (final String section : ConfigTexts.sectionsOf(file)) {
                            final String shown = names.getOrDefault(ConfigTexts.key(modId, section));
                            if (ctx.mc().font.width(shown) > ConfigScreenLayout.railLabelWidth(width)) {
                                wide.add(language + " section " + shown);
                            }
                        }
                        for (final ConfigKey<?> key : file.keys()) {
                            final String shown = names.getOrDefault(ConfigTexts.key(modId, key.dottedPath()));
                            // The worst a card leaves its name: a unit after its number and a Default button.
                            if (ctx.mc().font.width(shown)
                                    > ConfigScreenLayout.nameWidth(width, ConfigDraft.controlOf(key), true, true)) {
                                wide.add(language + " setting " + shown);
                            }
                        }
                    }
                }
            }
            if (!wide.isEmpty()) {
                ctx.fail("names wider than the screen shows: " + wide);
            }
        });
    }

    @ClientTest(timeoutTicks = 400)
    public static void settingsScreen_showsTheComputersSettingsByTheirNames(final ClientTestContext ctx) {
        ctx.then(0, () -> open(ctx, JsComputers.MODID))
                .thenAwaitScreen(CoreConfigScreen.class, 40)
                .thenAssert(1, () -> screen(ctx).scopeLabels().equals(List.of("World", "This player")),
                        "a tab for the world's settings and one for the player's")
                .thenAssert(0, () -> {
                    final List<String> rail = screen(ctx).railLabels();
                    return rail.equals(List.of("Starting up", "Installing by hand", "The prompt", "Soundfoundry",
                            "Programs")) && untranslated(rail).isEmpty();
                }, "the world's tab lists its sections under their names")
                .then(0, () -> ctx.assertTrue(screen(ctx).openSection("Soundfoundry"), "the music section opens"))
                .thenAssert(1, () -> screen(ctx).rowNames().containsAll(List.of("Music catalogue",
                        "Songs over Ethernet")) && untranslated(screen(ctx).rowNames()).isEmpty(),
                        "its settings under their names")
                .thenScreenshot(2, "computers-soundfoundry-settings")
                .then(0, () -> ctx.assertTrue(screen(ctx).openSection("Client"), "the player's section opens"))
                .thenAssert(1, () -> screen(ctx).rowNames().containsAll(List.of("Reduce motion", "Desktop cursors")),
                        "the player's own settings: Reduce motion and Desktop cursors")
                .thenScreenshot(2, "computers-client-settings")
                // The search finds a setting under any tab by what it does.
                .then(0, () -> screen(ctx).search("boot manager"))
                .thenAssert(1, () -> screen(ctx).rowNames().equals(List.of("Show the boot menu")),
                        "the search finds the boot menu by its description")
                .thenScreenshot(2, "computers-search")
                .then(0, () -> ctx.mc().setScreen(null));
    }

    @ClientTest(timeoutTicks = 400)
    public static void settingsScreen_showsTheCoresBalanceByItsNames(final ClientTestContext ctx) {
        ctx.then(0, () -> open(ctx, JsCore.MODID))
                .thenAwaitScreen(CoreConfigScreen.class, 40)
                .thenAssert(1, () -> {
                    final List<String> rail = screen(ctx).railLabels();
                    return rail.containsAll(List.of("Engine and programs", "Recordings"))
                            && untranslated(rail).isEmpty();
                }, "the Core's world settings, each section under its name")
                .then(0, () -> ctx.assertTrue(screen(ctx).openSection("Recordings"), "the recordings open"))
                .thenAssert(1, () -> screen(ctx).rowNames().containsAll(List.of("Download speed",
                        "Largest recording")), "the recordings' settings under their names")
                .thenScreenshot(2, "core-recordings-settings")
                .then(0, () -> ctx.mc().setScreen(null));
    }

    @ClientTest(timeoutTicks = 400)
    public static void settingsScreen_keepsAChangeOnlyWhenDone(final ClientTestContext ctx) {
        final ConfigFile file = fileOf(ComputersServerConfig.SHOW_BOOT_MENU);
        final boolean before = file.get(ComputersServerConfig.SHOW_BOOT_MENU);
        // Put back also when a check below fails, so later boot tests see the value they expect.
        ctx.afterTest(() -> file.set(ComputersServerConfig.SHOW_BOOT_MENU, before));
        ctx.then(0, () -> open(ctx, JsComputers.MODID))
                .thenAwaitScreen(CoreConfigScreen.class, 40)
                .then(1, () -> {
                    screen(ctx).openSection("Starting up");
                    screen(ctx).openDraft().toggle(ComputersServerConfig.SHOW_BOOT_MENU);
                    screen(ctx).onClose();
                })
                .thenAssert(1, () -> file.get(ComputersServerConfig.SHOW_BOOT_MENU) == before,
                        "leaving without Done drops the change")
                .then(0, () -> open(ctx, JsComputers.MODID))
                .thenAwaitScreen(CoreConfigScreen.class, 40)
                .then(1, () -> {
                    screen(ctx).openSection("Starting up");
                    screen(ctx).openDraft().toggle(ComputersServerConfig.SHOW_BOOT_MENU);
                })
                .thenScreenshot(2, "computers-boot-changed")
                .then(0, () -> screen(ctx).done())
                .thenAssert(1, () -> file.get(ComputersServerConfig.SHOW_BOOT_MENU) != before,
                        "Done keeps it in the world's settings");
    }

    /** Opens a mod's settings screen the way the mods list does. */
    private static void open(final ClientTestContext ctx, final String modId) {
        final ModContainer container = ModList.get().getModContainerById(modId).orElseThrow();
        final IConfigScreenFactory factory = container.getCustomExtension(IConfigScreenFactory.class).orElseThrow();
        ctx.mc().setScreen(factory.createScreen(container, null));
    }

    private static CoreConfigScreen screen(final ClientTestContext ctx) {
        return ctx.screen(CoreConfigScreen.class);
    }

    private static ConfigFile fileOf(final ConfigKey<?> key) {
        for (final ConfigFile file : ConfigFiles.of(JsComputers.MODID)) {
            if (file.keys().contains(key)) {
                return file;
            }
        }
        throw new AssertionError("no file holds " + key.dottedPath());
    }

    private static List<String> untranslated(final List<String> labels) {
        return labels.stream().filter(label -> label.contains(RAW_KEY)).toList();
    }
}
