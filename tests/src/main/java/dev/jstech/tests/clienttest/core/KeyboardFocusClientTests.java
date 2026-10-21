/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.gui.component.KeyboardFocus;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.data.DataContainerScreen;
import dev.jstech.tests.TestScreens;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.lwjgl.glfw.GLFW;

/**
 * The keyboard on a screen declared in a file, as the game moves it among its own widgets: Tab to the first
 * component and on in the order they read, the arrows to the one beside, Enter pressing what has it; and what the
 * game's narrator says of each, a name the file gave among it.
 */
public final class KeyboardFocusClientTests {

    private static final int WAIT = 60;

    private KeyboardFocusClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void tab_movesInReadingOrderTheArrowsBesideAndEnterPresses(final ClientTestContext ctx) {
        final int[] before = new int[1];
        open(ctx).then(2, () -> {
            final UiComponent press = component(ctx, "press");
            final UiComponent ready = component(ctx, "ready");
            ctx.key(GLFW.GLFW_KEY_TAB);
            ctx.assertTrue(press.isFocused(), "Tab gives the keyboard to the first component, the button");
            ctx.key(GLFW.GLFW_KEY_TAB);
            ctx.assertTrue(ready.isFocused() && !press.isFocused(), "Tab again goes on to the box beside it");
            ctx.key(GLFW.GLFW_KEY_LEFT);
            ctx.assertTrue(press.isFocused(), "the left arrow goes back to the button on its left");
            before[0] = TestScreens.PRESSED.get();
            ctx.key(GLFW.GLFW_KEY_ENTER);
        }).thenWaitUntil(() -> TestScreens.PRESSED.get() == before[0] + 1, WAIT, "Enter pressed the button");
    }

    @ClientTest(timeoutTicks = 400)
    public static void narrator_readsEachAsTheGameReadsItsOwn(final ClientTestContext ctx) {
        open(ctx).then(2, () -> {
            final DataContainerScreen<?> screen = ctx.screen(DataContainerScreen.class);
            final List<String> read = new ArrayList<>();
            for (final GuiEventListener widget : screen.children()) {
                if (widget instanceof KeyboardFocus.Entry entry) {
                    read.add(entry.component().narration().getString());
                }
            }
            ctx.assertTrue(read.contains("Press button"), "the button, as the game says its own: " + read);
            ctx.assertTrue(read.stream().anyMatch(said -> said.startsWith("Checkbox: Ready to run")),
                    "the box by the name its file gave it: " + read);
        });
    }

    private static ClientTestContext open(final ClientTestContext ctx) {
        return ctx.thenServer(0, level -> ctx.serverPlayer().openMenu(TestScreens.provider()))
                .thenAwaitScreen(DataContainerScreen.class, WAIT);
    }

    private static UiComponent component(final ClientTestContext ctx, final String id) {
        return ctx.screen(DataContainerScreen.class).view().find(id).orElseThrow().component();
    }
}
