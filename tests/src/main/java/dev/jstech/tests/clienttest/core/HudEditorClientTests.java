/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.hud.HudEditorScreen;
import dev.jstech.core.client.hud.HudElements;
import dev.jstech.core.config.ConfigDraft;
import dev.jstech.core.gui.HudPlacement;
import dev.jstech.core.gui.HudSettings;
import dev.jstech.core.gui.HudSnap;
import dev.jstech.core.gui.HudStack;
import dev.jstech.core.gui.layout.HudEditorLayout;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.clienttest.TestHud;
import java.util.Map;
import net.minecraft.client.gui.screens.Screen;

/**
 * The HUD editor: a box dragged moves its element out of its corner's stack to where it was left, its eye hides it,
 * a corner of the chosen one makes it bigger, and Done keeps all of it in the player's settings, which the HUD then
 * follows.
 */
public final class HudEditorClientTests {

    private static final int WAIT = 40;
    private static final int SETTLE = 3;
    /* Where the tall bar is dragged to, on a line of the grid across and down. */
    private static final int TO_X = 96;
    private static final int TO_Y = 96;

    private HudEditorClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void editor_movesHidesResizesAndDoneKeepsIt(final ClientTestContext ctx) {
        ctx.afterTest(() -> {
            ctx.mc().setScreen(null);
            TestHud.show(false);
            final ConfigDraft reset = new ConfigDraft(HudSettings.FILE);
            reset.choose(HudSettings.PLACED, Map.of());
            reset.apply();
        });
        ctx.then(0, () -> {
            TestHud.show(true);
            ctx.mc().setScreen(new HudEditorScreen(null, new ConfigDraft(HudSettings.FILE), HudSettings.PLACED,
                    true));
        }).thenAwaitScreen(HudEditorScreen.class, WAIT)
                .thenScreenshot(SETTLE, "hud-editor")
                .then(0, () -> {
                    final HudEditorScreen editor = ctx.screen(HudEditorScreen.class);
                    final HudSnap.Box tall = editor.boxOf(TestHud.TALL);
                    drag(editor, tall.x() + tall.width() / 2.0, tall.y() + tall.height() / 2.0,
                            TO_X + tall.width() / 2.0, TO_Y + tall.height() / 2.0);
                    final HudSnap.Box moved = editor.boxOf(TestHud.TALL);
                    ctx.assertTrue(moved.x() == TO_X && moved.y() == TO_Y,
                            "the tall bar went where it was left, on the grid: " + moved);
                    drag(editor, moved.x() + moved.width(), moved.y() + moved.height(),
                            moved.x() + 2 * moved.width(), moved.y() + 2 * moved.height());
                    final HudPlacement grown = editor.placements().get(TestHud.TALL.toString());
                    ctx.assertTrue(grown != null && grown.scale() == 2.0F,
                            "its corner made it twice as big: " + grown);
                    final HudSnap.Box shortBar = editor.boxOf(TestHud.SHORT);
                    ctx.click(shortBar.x() + shortBar.width() - HudEditorLayout.EYE_IN + 2,
                            shortBar.y() + HudEditorLayout.EYE_DOWN + 2);
                    ctx.assertTrue(editor.placements().get(TestHud.SHORT.toString()).hidden(),
                            "the short bar's eye hid it");
                })
                .thenScreenshot(SETTLE, "hud-editor-edited")
                .then(0, () -> {
                    final HudEditorScreen editor = ctx.screen(HudEditorScreen.class);
                    final int left = HudEditorLayout.left(editor.width);
                    final int top = HudEditorLayout.top(editor.height) + HudEditorLayout.BUTTONS_Y;
                    ctx.click(left + HudEditorLayout.doneX() + 4, top + 4);
                })
                .thenAwaitNoScreen(WAIT)
                .thenAssert(SETTLE, () -> HudSettings.placement(TestHud.TALL.toString()).isPresent()
                        && HudSettings.placement(TestHud.SHORT.toString()).map(HudPlacement::hidden).orElse(false),
                        "Done kept both in the player's settings")
                .thenAssert(0, () -> !HudElements.placedIn(HudStack.Corner.TOP_RIGHT).containsKey(TestHud.TALL)
                        && !HudElements.placedIn(HudStack.Corner.TOP_RIGHT).containsKey(TestHud.SHORT),
                        "neither stays in its corner's stack")
                .thenScreenshot(SETTLE, "hud-after-editing");
    }

    private static void drag(final Screen screen, final double fromX, final double fromY, final double toX,
                             final double toY) {
        screen.mouseClicked(fromX, fromY, 0);
        screen.mouseDragged(toX, toY, 0, toX - fromX, toY - fromY);
        screen.mouseReleased(toX, toY, 0);
    }
}
