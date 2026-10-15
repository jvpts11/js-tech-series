/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.data.DataScreen;
import dev.jstech.core.client.gui.data.DataScreenView;
import dev.jstech.core.client.gui.data.IScreenLogic;
import dev.jstech.core.client.gui.style.Styles;
import dev.jstech.core.config.ConfigDraft;
import dev.jstech.core.gui.style.Style;
import dev.jstech.core.gui.style.StyleSettings;
import dev.jstech.core.text.GameText;
import dev.jstech.tests.TestScreens;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * The richer components on a player's game, in screens declared in files: laid out clean in every style, a screenshot
 * of each for a person to look at, and each doing what it is for when used as a player uses it.
 */
public final class ComponentsClientTests {

    private ComponentsClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void controlsAndViews_areLaidOutCleanInEveryStyle(final ClientTestContext ctx) {
        ctx.afterTest(() -> {
            choose("");
            ctx.mc().setScreen(null);
        });
        ctx.then(0, () -> {
            final List<String> wrong = new ArrayList<>();
            for (final Style style : Styles.all()) {
                choose(style.id().toString());
                final List<DataScreen> screens = List.of(
                        open(ctx, TestScreens.CONTROLS_FILE, TestControlScreens.controls()),
                        open(ctx, TestScreens.VIEWS_FILE, TestControlScreens.views()));
                for (final DataScreen screen : screens) {
                    final DataScreenView view = screen.view();
                    if (!view.guiLayout().isClean()) {
                        wrong.add(style.id() + " " + screen.file() + ": " + view.guiLayout().overlaps() + " "
                                + view.guiLayout().outOfBounds());
                    }
                    if (!view.unwired().isEmpty()) {
                        wrong.add(screen.file() + " uses names nobody handed over: " + view.unwired());
                    }
                }
            }
            ctx.assertTrue(wrong.isEmpty(), "the screens are clean in every style: " + wrong);
        });
    }

    @ClientTest(timeoutTicks = 300)
    public static void controls_inTheGamesStyle_forAPerson(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.then(0, () -> open(ctx, TestScreens.CONTROLS_FILE, TestControlScreens.controls()))
                .thenScreenshot(5, "components_controls")
                .then(0, () -> open(ctx, TestScreens.VIEWS_FILE, TestControlScreens.views()))
                .thenScreenshot(5, "components_views");
    }

    @ClientTest(timeoutTicks = 300)
    public static void controls_doWhatTheyAreFor(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.then(0, () -> open(ctx, TestScreens.CONTROLS_FILE, TestControlScreens.controls()))
                .then(2, () -> {
                    final UiComponent speed = component(ctx, "speed");
                    ctx.click(speed.x() + 4 + (speed.width() - 8) * 3 / 4, speed.y() + 4);
                    final UiComponent eject = component(ctx, "eject");
                    final boolean ejected = TestControlScreens.EJECT[0];
                    ctx.click(eject.x() + 4, eject.center()[1]);
                    final UiComponent mode = component(ctx, "mode");
                    ctx.click(mode.x() + 3, mode.y() + 13 + 4);
                    ctx.assertTrue(Math.abs(TestControlScreens.SPEED[0] - 150) < 6,
                            "a click three quarters along the slider sets 150: " + TestControlScreens.SPEED[0]);
                    ctx.assertTrue(TestControlScreens.EJECT[0] != ejected, "a click turns the switch");
                    ctx.assertEquals(1, TestControlScreens.MODE[0], "a click on the second option chooses it");
                })
                .then(1, () -> {
                    final UiComponent sort = component(ctx, "sort");
                    ctx.click(sort.center()[0], sort.center()[1]);
                })
                .then(1, () -> {
                    final UiComponent sort = component(ctx, "sort");
                    ctx.assertTrue(sort.overlayOpen(), "a click opens the list");
                    ctx.click(sort.x() + 6, sort.bottom() + 1 + 2 + 12 + 4);
                    ctx.assertEquals(1, TestControlScreens.SORT[0], "a click on the list's second line chooses it");
                    ctx.assertTrue(!sort.overlayOpen(), "and closes the list");
                })
                .then(1, () -> {
                    final UiComponent colour = component(ctx, "colour");
                    ctx.click(colour.center()[0], colour.center()[1]);
                })
                .then(1, () -> {
                    final UiComponent colour = component(ctx, "colour");
                    final int swatchY = colour.bottom() + 1 + 4 + 64 + 4 + 4;
                    ctx.click(colour.x() + 4 + 10 + 4, swatchY);
                    ctx.assertEquals(0xFFEF6A5A, TestControlScreens.COLOUR[0], "a ready colour is picked");
                });
    }

    @ClientTest(timeoutTicks = 300)
    public static void views_answerAClick(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        final int[] added = new int[1];
        ctx.then(0, () -> open(ctx, TestScreens.VIEWS_FILE, TestControlScreens.views()))
                .then(2, () -> {
                    final UiComponent tree = component(ctx, "tree");
                    ctx.click(tree.x() + 40, tree.y() + 2 + 19 + 8);
                    ctx.assertEquals("c1", TestControlScreens.PICKED[0], "a click on a row chooses its node");
                    added[0] = TestControlScreens.ADDED[0];
                    final UiComponent graph = component(ctx, "graph");
                    ctx.mc().screen.mouseClicked(graph.right() - 6, graph.bottom() - 6, 1);
                    ctx.assertEquals(added[0] + 1, TestControlScreens.ADDED[0],
                            "the right button on the ground asks for a node");
                });
    }

    private static DataScreen open(final ClientTestContext ctx, final ResourceLocation file,
                                   final IScreenLogic<Void> logic) {
        final DataScreen screen = new DataScreen(file, GameText.component(file == TestScreens.CONTROLS_FILE
                ? TestScreens.CONTROLS : TestScreens.VIEWS), logic, null);
        ctx.mc().setScreen(screen);
        return screen;
    }

    private static UiComponent component(final ClientTestContext ctx, final String id) {
        return ctx.screen(DataScreen.class).find(id).orElseThrow().component();
    }

    private static void choose(final String style) {
        final ConfigDraft draft = new ConfigDraft(StyleSettings.FILE);
        draft.choose(StyleSettings.EVERYTHING, style);
        draft.apply();
    }
}
