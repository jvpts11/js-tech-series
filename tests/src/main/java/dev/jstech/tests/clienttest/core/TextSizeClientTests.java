/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.config.CoreConfigScreen;
import dev.jstech.core.client.gui.data.DataContainerScreen;
import dev.jstech.core.client.notification.NotificationsClient;
import dev.jstech.core.config.ConfigDraft;
import dev.jstech.core.diagnostic.DiagnosticNotifications;
import dev.jstech.core.gui.AccessibilitySettings;
import dev.jstech.core.gui.TextSizes;
import dev.jstech.core.text.Text;
import dev.jstech.tests.TestScreens;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * The text a step larger than the normal, apart from the GUI scale: a screen declared in a file measures its text
 * larger and its boxes grow with it, and a notification and the settings screen draw theirs larger too.
 */
public final class TextSizeClientTests {

    private static final int WAIT = 60;
    private static final int SETTLE = 3;
    /* A label of the test machine's screen, one line of text. */
    private static final String LABEL = "word";

    private TextSizeClientTests() {
    }

    @ClientTest(timeoutTicks = 800)
    public static void larger_growsTheTextAndWhatHoldsIt(final ClientTestContext ctx) {
        final int[] normal = new int[1];
        ctx.afterTest(() -> {
            ctx.mc().setScreen(null);
            NotificationsClient.board().clear();
            size(TextSizes.NORMAL);
        });
        ctx.then(0, () -> size(TextSizes.NORMAL))
                .thenServer(0, level -> ctx.serverPlayer().openMenu(TestScreens.provider()))
                .thenAwaitScreen(DataContainerScreen.class, WAIT)
                .then(SETTLE, () -> normal[0] = labelHeight(ctx))
                .then(0, () -> ctx.player().closeContainer())
                .thenAwaitNoScreen(WAIT)
                .then(0, () -> size(TextSizes.LARGER))
                .thenServer(0, level -> ctx.serverPlayer().openMenu(TestScreens.provider()))
                .thenAwaitScreen(DataContainerScreen.class, WAIT)
                .then(SETTLE, () -> ctx.assertTrue(labelHeight(ctx) > normal[0],
                        "the label's box grew with its text: " + normal[0] + " to " + labelHeight(ctx)))
                .thenScreenshot(SETTLE, "test-machine-larger")
                .then(0, () -> ctx.player().closeContainer())
                .thenAwaitNoScreen(WAIT)
                .then(0, () -> NotificationsClient.show(DiagnosticNotifications.SLOWED.of(
                        DiagnosticNotifications.SLOWED_NAME.text(),
                        Text.literal("jscore:fluid_grid runs one tick in 4"))))
                .thenScreenshot(SETTLE, "notification-larger")
                .then(0, () -> {
                    NotificationsClient.board().clear();
                    final ModContainer core = ModList.get().getModContainerById(JsCore.MODID).orElseThrow();
                    ctx.mc().setScreen(core.getCustomExtension(IConfigScreenFactory.class).orElseThrow()
                            .createScreen(core, null));
                })
                .thenAwaitScreen(CoreConfigScreen.class, WAIT)
                .then(1, () -> ctx.assertTrue(ctx.screen(CoreConfigScreen.class).openSection("Text"),
                        "the player's text settings open"))
                .thenScreenshot(2, "settings-larger");
    }

    private static int labelHeight(final ClientTestContext ctx) {
        return ctx.screen(DataContainerScreen.class).find(LABEL).orElseThrow().component().height();
    }

    private static void size(final String word) {
        final ConfigDraft draft = new ConfigDraft(AccessibilitySettings.FILE);
        draft.choose(AccessibilitySettings.TEXT_SIZE, word);
        draft.apply();
    }
}
