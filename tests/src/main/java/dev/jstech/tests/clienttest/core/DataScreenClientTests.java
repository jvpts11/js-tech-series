/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.gui.data.BuiltNode;
import dev.jstech.core.client.gui.data.DataContainerScreen;
import dev.jstech.core.client.gui.data.DataScreen;
import dev.jstech.core.client.gui.data.DataScreenView;
import dev.jstech.core.client.gui.data.IScreenLogic;
import dev.jstech.core.client.gui.data.ScreenFiles;
import dev.jstech.core.client.gui.data.TextView;
import dev.jstech.core.client.gui.style.StyleChooserScreen;
import dev.jstech.core.client.gui.style.Styles;
import dev.jstech.core.client.gui.theme.EraThemes;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.config.ConfigDraft;
import dev.jstech.core.developer.LiveReload;
import dev.jstech.core.developer.ModFolders;
import dev.jstech.core.gui.flex.Placed;
import dev.jstech.core.gui.style.Style;
import dev.jstech.core.gui.style.StyleRole;
import dev.jstech.core.gui.style.StyleSettings;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestScreens;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;

/**
 * A screen declared in a file, on a player's game: laid out clean in every style the game has, its slots under their
 * frames, the menu's values arriving whole by name, its button doing its action, the player's choice of style worn by
 * the series' computing screens too, and the screen built again when its file is changed.
 */
public final class DataScreenClientTests {

    /* Marked as this run's, so the clients of a battery that share the tests mod's folders do not reload for it. */
    private static final String LIVE_FILE = LiveReload.ownFile("live_screen", "json");

    private DataScreenClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void testMachine_isLaidOutCleanInEveryStyle(final ClientTestContext ctx) {
        ctx.afterTest(() -> choose(""));
        open(ctx).then(2, () -> {
            final List<String> wrong = new ArrayList<>();
            for (final Style style : Styles.all()) {
                choose(style.id().toString());
                final DataContainerScreen<?> screen = ctx.screen(DataContainerScreen.class);
                screen.rebuild();
                final DataScreenView view = screen.view();
                if (view.style() != style) {
                    wrong.add(style.id() + ": the screen wears " + view.style().id());
                }
                if (!view.guiLayout().isClean()) {
                    wrong.add(style.id() + ": " + view.guiLayout().overlaps() + " " + view.guiLayout().outOfBounds());
                }
                if (!view.placed().orElseThrow().overflows().isEmpty()) {
                    wrong.add(style.id() + ": too little room in " + view.placed().orElseThrow().overflows());
                }
                checkSlot(wrong, style, screen, 0, "input");
                checkSlot(wrong, style, screen, 1, "output");
            }
            ctx.assertTrue(wrong.isEmpty(), "the screen is clean in every style: " + wrong);
        });
    }

    @ClientTest(timeoutTicks = 400)
    public static void boundValues_arriveWholeByName(final ClientTestContext ctx) {
        open(ctx).thenWaitUntil(() -> menu(ctx).big() == TestScreens.BIG, 100,
                        "a number past a short's reach to arrive whole")
                .thenAssert(0, () -> TestScreens.WORD.equals(menu(ctx).word()), "and a word")
                .thenAssert(0, () -> menu(ctx).progress() == TestScreens.PROGRESS, "and the bar's fill");
    }

    @ClientTest(timeoutTicks = 400)
    public static void button_doesTheActionItNames(final ClientTestContext ctx) {
        final int[] before = new int[1];
        open(ctx).then(2, () -> {
            before[0] = TestScreens.PRESSED.get();
            final BuiltNode press = ctx.screen(DataContainerScreen.class).find("press").orElseThrow();
            final int[] centre = press.component().center();
            // A component's centre is where it stands on the screen, not inside the window.
            ctx.click(centre[0], centre[1]);
        }).thenAssert(2, () -> TestScreens.PRESSED.get() == before[0] + 1, "the button's action ran once");
    }

    @ClientTest(timeoutTicks = 300)
    public static void computingScreens_wearTheStyleThePlayerChose(final ClientTestContext ctx) {
        ctx.afterTest(() -> {
            choose("");
            JsTechTheme.unbind();
        });
        ctx.then(0, () -> {
            final Style chosen = Styles.get(ResourceLocation.fromNamespaceAndPath("jscore", "high_contrast"))
                    .orElseThrow();
            choose(chosen.id().toString());
            JsTechTheme.bind(EraThemes.VINTAGE);
            ctx.assertEquals(chosen.colour(StyleRole.SCREEN), JsTechTheme.screen(),
                    "a Vintage screen wears the style the player chose for every screen");
            choose("");
            JsTechTheme.bind(EraThemes.VINTAGE);
            ctx.assertEquals(EraThemes.VINTAGE.screen(), JsTechTheme.screen(), "and its era's with none chosen");
        });
    }

    @ClientTest(timeoutTicks = 300)
    public static void styleChooser_listsEveryScreenAndTheModsAndPicks(final ClientTestContext ctx) {
        final ConfigDraft draft = new ConfigDraft(StyleSettings.FILE);
        ctx.then(0, () -> ctx.mc().setScreen(new StyleChooserScreen(new TitleScreen(), draft)))
                .thenAwaitScreen(StyleChooserScreen.class, 40)
                .then(1, () -> {
                    final StyleChooserScreen chooser = ctx.screen(StyleChooserScreen.class);
                    ctx.assertTrue(chooser.scopeLabels().size() >= 3, "every screen, and each mod built on the Core: "
                            + chooser.scopeLabels());
                    final Style jsTech = Styles.get(ResourceLocation.fromNamespaceAndPath("jscore", "jstech"))
                            .orElseThrow();
                    chooser.choose(jsTech);
                    ctx.assertEquals("jscore:jstech", draft.value(StyleSettings.EVERYTHING),
                            "the choice waits in the settings' changes");
                    chooser.openScope(1);
                    chooser.choose(jsTech);
                    ctx.assertEquals(1, draft.value(StyleSettings.BY_MOD).size(), "and one mod's choice beside it");
                    ctx.mc().setScreen(null);
                });
    }

    @ClientTest(timeoutTicks = 1200)
    public static void editedFile_rebuildsTheOpenScreen(final ClientTestContext ctx) {
        final Path file = resourceRoot().resolve("assets").resolve(JsTests.MODID).resolve("jstech")
                .resolve("screens").resolve(LIVE_FILE);
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                LIVE_FILE.substring(0, LIVE_FILE.length() - ".json".length()));
        ctx.afterTest(() -> delete(file));
        ctx.then(0, () -> write(file, "first"))
                .thenWaitUntil(() -> ctx.mc().getOverlay() == null && ScreenFiles.get(id).isPresent(), 600,
                        "the new screen's file to be read")
                .then(0, () -> ctx.mc().setScreen(new DataScreen(id, Component.literal("live"), IScreenLogic.none(),
                        null)))
                .thenAwaitScreen(DataScreen.class, 40)
                .thenAssert(1, () -> label(ctx).equals("first"), "the screen shows what its file said")
                .then(0, () -> write(file, "second"))
                .thenWaitUntil(() -> ctx.mc().getOverlay() == null && ctx.mc().screen instanceof DataScreen
                        && label(ctx).equals("second"), 600, "the open screen to be built again from its edited file")
                .then(0, () -> ctx.mc().setScreen(null));
    }

    private static ClientTestContext open(final ClientTestContext ctx) {
        return ctx.thenServer(0, level -> ctx.serverPlayer().openMenu(TestScreens.provider()))
                .thenAwaitScreen(DataContainerScreen.class, 60);
    }

    private static TestScreens.TestDataMenu menu(final ClientTestContext ctx) {
        return (TestScreens.TestDataMenu) ctx.screen(DataContainerScreen.class).getMenu();
    }

    /* The slot's item sits a pixel inside the frame the file placed for it. */
    private static void checkSlot(final List<String> wrong, final Style style, final DataContainerScreen<?> screen,
                                  final int index, final String id) {
        final Placed frame = screen.view().placed().orElseThrow().find(id).orElseThrow();
        final Slot slot = screen.getMenu().slots.get(index);
        if (slot.x != frame.x() + 1 || slot.y != frame.y() + 1) {
            wrong.add(style.id() + ": slot " + index + " at " + slot.x + "," + slot.y + ", its frame at " + frame.x()
                    + "," + frame.y());
        }
    }

    private static String label(final ClientTestContext ctx) {
        return ((TextView) ctx.screen(DataScreen.class).find("said").orElseThrow().component()).text();
    }

    private static void choose(final String style) {
        final ConfigDraft draft = new ConfigDraft(StyleSettings.FILE);
        draft.choose(StyleSettings.EVERYTHING, style);
        draft.apply();
    }

    private static Path resourceRoot() {
        for (final Path folder : ModFolders.ofThisRun().getOrDefault(JsTests.MODID, List.of())) {
            if (Files.isDirectory(folder.resolve("assets"))) {
                return folder;
            }
        }
        throw new IllegalStateException("this run loads the tests mod from no folder of assets");
    }

    private static void write(final Path file, final String said) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "{\"root\": {\"type\": \"column\", \"children\": [{\"type\": \"label\","
                    + " \"id\": \"said\", \"text\": \"" + said + "\"}]}}");
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void delete(final Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
