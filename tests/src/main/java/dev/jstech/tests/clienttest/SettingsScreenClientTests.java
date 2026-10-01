/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.JsCore;
import dev.jstech.core.config.ConfigTexts;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge's settings screen, reached from the mods list, for the series' mods: each opens it, its world settings are
 * there, and every section and setting is shown under its own name rather than the key it is translated by, a name
 * short enough to be read whole in every language the series ships.
 */
public final class SettingsScreenClientTests {

    /** How a key that found no translation shows: as itself, the mod's settings prefix and all. */
    private static final String RAW_KEY = ".configuration.";
    /** The languages the series ships its names in, each read on its own beside English. */
    private static final List<String> LANGUAGES = List.of("en_us", "pt_br");
    /** How the screen writes a section's or a list's name: with three dots after it. */
    private static final String SECTION = "neoforge.configuration.uitext.section";

    private SettingsScreenClientTests() {
    }

    @ClientTest(timeoutTicks = 200)
    public static void settingsScreen_everyNameFitsBesideItsSettingInEveryLanguage(final ClientTestContext ctx) {
        ctx.then(0, () -> {
            final List<String> wide = new ArrayList<>();
            for (final String language : LANGUAGES) {
                final Language names = ClientLanguage.loadFrom(ctx.mc().getResourceManager(),
                        List.of("en_us", language), false);
                for (final String modId : List.of(JsCore.MODID, JsComputers.MODID)) {
                    for (final String key : ConfigTexts.english(modId).keySet()) {
                        // Every name is measured with the dots a section's gets, so a setting can become a list
                        // or a section without its name outgrowing the room the screen gives it.
                        final String shown = names.getOrDefault(SECTION).formatted(names.getOrDefault(key));
                        if (!key.endsWith(".tooltip") && ctx.mc().font.width(shown) > Button.DEFAULT_WIDTH) {
                            wide.add(language + " " + shown);
                        }
                    }
                }
            }
            if (!wide.isEmpty()) {
                ctx.fail("names wider than the screen shows: " + wide);
            }
        });
    }

    @ClientTest(timeoutTicks = 600)
    public static void settingsScreen_showsTheComputersSettingsByTheirNames(final ClientTestContext ctx) {
        // A mod with one settings file on the screen opens straight onto that file, as both of these have.
        ctx.then(0, () -> open(ctx, JsComputers.MODID))
                .thenAwaitScreen(ConfigurationScreen.ConfigurationSectionScreen.class, 40)
                .thenAssert(1, () -> {
                    final List<String> labels = labels(ctx.mc().screen);
                    return named(labels, "Layout version") && named(labels, "Starting up")
                            && named(labels, "Installing by hand") && named(labels, "The prompt")
                            && named(labels, "Soundfoundry") && untranslated(labels).isEmpty();
                }, "the computers' world settings, each section under its name")
                .thenScreenshot(2, "computers-settings")
                .then(0, () -> sectionButton(ctx.mc().screen, "Starting up").onPress())
                .thenWaitUntil(() -> named(labels(ctx.mc().screen), "Show the boot menu")
                                && untranslated(labels(ctx.mc().screen)).isEmpty(), 40,
                        "the section to list its setting under its name")
                .thenScreenshot(2, "computers-boot-settings")
                .then(0, () -> ctx.mc().setScreen(null));
    }

    @ClientTest(timeoutTicks = 600)
    public static void settingsScreen_showsTheCoresBalanceByItsNames(final ClientTestContext ctx) {
        ctx.then(0, () -> open(ctx, JsCore.MODID))
                .thenAwaitScreen(ConfigurationScreen.ConfigurationSectionScreen.class, 40)
                .thenAssert(1, () -> {
                    final List<String> labels = labels(ctx.mc().screen);
                    return named(labels, "Layout version") && named(labels, "Operations and programs")
                            && named(labels, "Recordings") && untranslated(labels).isEmpty();
                }, "the Core's world settings, each section under its name")
                .thenScreenshot(2, "core-settings")
                .then(0, () -> sectionButton(ctx.mc().screen, "Recordings").onPress())
                .thenWaitUntil(() -> named(labels(ctx.mc().screen), "Download speed")
                                && named(labels(ctx.mc().screen), "Largest recording")
                                && untranslated(labels(ctx.mc().screen)).isEmpty(), 40,
                        "the recordings' settings under their names")
                .thenScreenshot(2, "core-recordings-settings")
                .then(0, () -> ctx.mc().setScreen(null));
    }

    /** Opens a mod's settings screen the way the mods list does. */
    private static void open(final ClientTestContext ctx, final String modId) {
        final ModContainer container = ModList.get().getModContainerById(modId).orElseThrow();
        final IConfigScreenFactory factory = container.getCustomExtension(IConfigScreenFactory.class).orElseThrow();
        ctx.mc().setScreen(factory.createScreen(container, null));
    }

    /** The button beside a section's name, which opens the section. */
    private static AbstractButton sectionButton(final Screen screen, final String name) {
        for (final List<AbstractWidget> row : rows(screen)) {
            if (row.size() == 2 && row.get(0).getMessage().getString().contains(name)
                    && row.get(1) instanceof AbstractButton button) {
                return button;
            }
        }
        throw new AssertionError("no section called " + name + " on the screen");
    }

    /** What every row of the screen's list says on its left, its label. */
    private static List<String> labels(@Nullable final Screen screen) {
        final List<String> out = new ArrayList<>();
        for (final List<AbstractWidget> row : rows(screen)) {
            if (!row.isEmpty()) {
                out.add(row.get(0).getMessage().getString());
            }
        }
        return out;
    }

    private static boolean named(final List<String> labels, final String name) {
        return labels.stream().anyMatch(label -> label.contains(name));
    }

    private static List<String> untranslated(final List<String> labels) {
        return labels.stream().filter(label -> label.contains(RAW_KEY)).toList();
    }

    /** The widgets of each row of the screen's list, left to right. */
    private static List<List<AbstractWidget>> rows(@Nullable final Screen screen) {
        final List<List<AbstractWidget>> out = new ArrayList<>();
        if (screen == null) {
            return out;
        }
        for (final GuiEventListener child : screen.children()) {
            if (!(child instanceof AbstractSelectionList<?> list)) {
                continue;
            }
            for (final GuiEventListener entry : list.children()) {
                if (entry instanceof ContainerEventHandler row) {
                    final List<AbstractWidget> widgets = new ArrayList<>();
                    for (final GuiEventListener widget : row.children()) {
                        if (widget instanceof AbstractWidget shown) {
                            widgets.add(shown);
                        }
                    }
                    out.add(widgets);
                }
            }
        }
        return out;
    }
}
