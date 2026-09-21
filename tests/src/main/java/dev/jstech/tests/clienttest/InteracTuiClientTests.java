/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.IDesktopApp;
import dev.jstech.computers.client.os.ShellApp;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/**
 * Working the network from a terminal, the way a player does it: type the word, watch the view take the
 * glass, move through it with the arrows and the headings with Tab, look for something by typing it, and
 * give the terminal back.
 *
 * <p>A real network behind a real terminal window, because what is being held to account is the round trip:
 * a key changes where the view stands, the machine is asked for that view, and what comes back is what is on
 * the glass. None of that shows up in a screen drawn on its own.
 */
public final class InteracTuiClientTests {

    private InteracTuiClientTests() {
    }

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 400;

    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final ResourceLocation FRAMES_XP =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp");
    private static final String TERMINAL = "Command Prompt";

    private static ShellApp shell(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        final DesktopWindow window = desktop == null ? null : desktop.windowFor(TERMINAL);
        final IDesktopApp app = window == null ? null : window.app();
        return app instanceof ShellApp s ? s : null;
    }

    private static String glass(final ClientTestContext ctx) {
        final ShellApp app = shell(ctx);
        return app == null ? "" : app.editorText();
    }

    private static void launch(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        ctx.click(desktop.startButtonX(), desktop.startButtonY());
        final int item = desktop.launcherLabels().indexOf(TERMINAL);
        ctx.click(desktop.startMenuItemX(item), desktop.startMenuItemY(item));
    }

    /**
     * The word on its own takes the glass, the headings and the search work off the machine, and the key
     * along the foot gives the terminal back.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void interac_takesTheGlassAndIsWorkedWithTheKeyboard(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
                    TestWorldBuilder.installDesktop(net.cc(), FRAMES_XP);
                    net.cc().togglePower();
                    net.cc().togglePower();
                    world.placeMonitor(MONITOR, Direction.EAST);
                    net.seed(Items.IRON_INGOT, 64);
                    net.seed(Items.OAK_LOG, 16);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).launcherLabels().contains(TERMINAL),
                        SCREEN_WAIT, "the terminal to be listed in Start")
                .then(0, () -> launch(ctx))
                .thenWaitUntil(() -> shell(ctx) != null, SCREEN_WAIT, "the terminal window to open")
                .then(SETTLE, () -> ctx.type("interac"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> shell(ctx).editing() && glass(ctx).contains("[Network]"),
                        SCREEN_WAIT, "the view to take the glass")
                .thenScreenshot(2, "interac-tui-network")
                .then(0, () -> ctx.assertTrue(
                        glass(ctx).contains("Iron Ingot") && glass(ctx).contains("Oak Log"),
                        "the rows come off the network; got\n" + glass(ctx)))
                .then(0, () -> ctx.assertTrue(
                        glass(ctx).contains("1Help") && glass(ctx).contains("2Get"),
                        "and the keys are along the foot; got\n" + glass(ctx)))
                /* Tab goes to the next heading, which is another list off the same machine. */
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_TAB))
                .thenWaitUntil(() -> glass(ctx).contains("[Servers]"), SCREEN_WAIT,
                        "Tab to go to the next heading")
                .then(0, () -> ctx.assertTrue(glass(ctx).contains("% full"),
                        "which says how full each server is; got\n" + glass(ctx)))
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_TAB, GLFW.GLFW_MOD_SHIFT))
                .thenWaitUntil(() -> glass(ctx).contains("[Network]"), SCREEN_WAIT,
                        "Shift with it to go back")
                /* Letters look for something, and the machine narrows the list to it. */
                .then(SETTLE, () -> ctx.type("iron"))
                .thenWaitUntil(() -> glass(ctx).contains("Search: iron_"), SCREEN_WAIT,
                        "what is being looked for to reach the machine")
                .then(0, () -> ctx.assertTrue(
                        glass(ctx).contains("Iron Ingot") && !glass(ctx).contains("Oak Log"),
                        "the list narrows to it; got\n" + glass(ctx)))
                .thenScreenshot(2, "interac-tui-search")
                /* Backspace takes a letter off it again. */
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_BACKSPACE))
                .thenWaitUntil(() -> glass(ctx).contains("Search: iro_"), SCREEN_WAIT,
                        "Backspace to take a letter off")
                /* The help page is shown and any key puts it away. */
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_F1))
                .thenAssert(2, () -> shell(ctx).editing(), "the help page does not give the terminal back")
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAssert(2, () -> shell(ctx).editing(),
                        "and the key that puts it away does not give it back either")
                /* The last key along the foot gives the terminal back to the prompt. */
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_F10))
                .thenWaitUntil(() -> !shell(ctx).editing(), SCREEN_WAIT, "the terminal to come back")
                .thenAssert(0, () -> ctx.screen(DesktopScreen.class) != null,
                        "and the desktop is still there");
    }
}
