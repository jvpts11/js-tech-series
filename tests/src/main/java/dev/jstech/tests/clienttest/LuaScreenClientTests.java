/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.IDesktopApp;
import dev.jstech.computers.client.os.ShellApp;
import dev.jstech.computers.client.os.VirtualStudioCodeApp;
import dev.jstech.computers.gui.layout.LuaScreenLayout;
import dev.jstech.computers.operation.payload.LuaScreenPayload;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

/**
 * A Lua program at a terminal gets ComputerCraft's screen: the Command Prompt window grows to show it
 * whole and names the program, the keyboard and the mouse reach it as events, Ctrl+T asks it to end,
 * and when it returns what it left on its screen stays in the scrollback under the prompt. In an
 * editor's short panel the screen is drawn smaller and follows the cursor.
 */
public final class LuaScreenClientTests {

    private LuaScreenClientTests() {
    }

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 400;

    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final ResourceLocation FRAMES_XP =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp");
    private static final String TERMINAL = "Command Prompt";
    private static final String EDITOR_LAUNCHER = "Virtual Studio Code";

    /** A dashboard: a blue band with a title, a read at the bottom, then a key and a click. */
    private static final String DASHBOARD = """
            term.setBackgroundColour(colours.black)
            term.clear()
            term.setBackgroundColour(colours.blue)
            term.setCursorPos(1, 1)
            term.clearLine()
            term.setTextColour(colours.yellow)
            term.write(" REACTOR 1")
            term.setBackgroundColour(colours.black)
            term.setTextColour(colours.white)
            term.setCursorPos(2, 3)
            term.write("core temp  812 K")
            term.setCursorPos(1, 17)
            write("name? ")
            local name = read()
            term.setCursorPos(2, 5)
            term.write("hi " .. name)
            local _, key = os.pullEvent("key")
            term.setCursorPos(2, 6)
            term.write("key " .. key)
            local _, button, x, y = os.pullEvent("mouse_click")
            term.setCursorPos(2, 7)
            term.write("click " .. button .. " " .. x .. " " .. y)
            term.setCursorPos(1, 9)
            """;

    /** A program that waits for ever until it is asked to end. */
    private static final String WAITER = """
            term.clear()
            term.setCursorPos(1, 1)
            term.write("waiting")
            while true do os.pullEvent() end
            """;

    /** A program that writes down the page, its cursor ending near the bottom, and waits for a key. */
    private static final String TALL = """
            term.clear()
            for i = 1, 18 do
              term.setCursorPos(1, i)
              term.write("line " .. i)
            end
            term.setCursorPos(1, 19)
            os.pullEvent("key")
            """;

    private static ResourceLocation program(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }

    private static <T extends IDesktopApp> T app(final ClientTestContext ctx, final String label, final Class<T> type) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        final var window = desktop == null ? null : desktop.windowFor(label);
        return window != null && type.isInstance(window.app()) ? type.cast(window.app()) : null;
    }

    private static ShellApp shell(final ClientTestContext ctx) {
        return app(ctx, TERMINAL, ShellApp.class);
    }

    private static VirtualStudioCodeApp editor(final ClientTestContext ctx) {
        return app(ctx, EDITOR_LAUNCHER, VirtualStudioCodeApp.class);
    }

    private static void launch(final ClientTestContext ctx, final String label) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        ctx.click(desktop.startButtonX(), desktop.startButtonY());
        final int item = desktop.launcherLabels().indexOf(label);
        ctx.click(desktop.startMenuItemX(item), desktop.startMenuItemY(item));
    }

    private static void seed(final CraftingComputerBlockEntity computer, final String path, final String text) {
        DiskFilesystem.write(computer.systemDisk(), path, FileType.LUA, text, Long.MAX_VALUE, FilesystemKind.HIERARCHICAL);
    }

    private static boolean shows(final LuaScreenPayload screen, final String text) {
        return screen != null && String.join("\n", screen.text()).contains(text);
    }

    /** A machine with the Lua runtime and the programs above, at its desktop with a program opened. */
    private static ClientTestContext atTheDesktop(final ClientTestContext ctx, final String opened) {
        return ctx.thenBuild(0, world -> {
                    final CraftingComputerBlockEntity computer = world.placeRunningCraftingComputer(COMPUTER);
                    computer.installOs(FRAMES_XP);
                    for (final String id : new String[] {"lrt", "virtual_studio_code"}) {
                        computer.console().install(program(id).toString());
                    }
                    seed(computer, "progs/dash.lua", DASHBOARD);
                    seed(computer, "progs/wait.lua", WAITER);
                    seed(computer, "progs/tall.lua", TALL);
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).launcherLabels().contains(opened),
                        SCREEN_WAIT, opened + " to be listed in Start")
                .then(0, () -> launch(ctx, opened));
    }

    /**
     * The Command Prompt shows a Lua program's screen whole and at full size, named in the title; what is
     * typed reaches its read, a key and a click reach it as events, and when it returns its screen stays
     * in the scrollback under the prompt.
     */
    @ClientTest(timeoutTicks = 3000)
    public static void commandPrompt_showsALuaProgramsScreenAndGivesItTheKeyboard(final ClientTestContext ctx) {
        final int[] clicked = new int[2];
        atTheDesktop(ctx, TERMINAL)
                .thenWaitUntil(() -> shell(ctx) != null, SCREEN_WAIT, "the terminal window to open")
                .then(SETTLE, () -> ctx.type("lrt run progs/dash.lua"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> shows(shell(ctx).screen(), "REACTOR 1") && shows(shell(ctx).screen(), "name?"),
                        SCREEN_WAIT, "the program's screen to show in the window")
                .thenAssert(SETTLE, () -> shell(ctx).title().equals("Command Prompt: dash.lua"),
                        "the title names the program")
                .thenAssert(0, () -> shell(ctx).screenScale() == 1f, "the window grew to show it at full size")
                .thenAssert(0, () -> shell(ctx).screen().groundColours().getFirst().charAt(0) == 'b',
                        "the band is blue")
                .thenScreenshot(2, "dashboard")
                .then(SETTLE, () -> ctx.type("Ada"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> shows(shell(ctx).screen(), "hi Ada"), SCREEN_WAIT, "the read to take the line typed")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_K))
                .thenWaitUntil(() -> shows(shell(ctx).screen(), "key " + GLFW.GLFW_KEY_K), SCREEN_WAIT,
                        "the key to reach the program with its number")
                .then(SETTLE, () -> {
                    final int[] point = shell(ctx).screenCellPoint(12, 4);
                    ctx.assertTrue(point != null, "the cell is on the screen");
                    clicked[0] = 12;
                    clicked[1] = 4;
                    ctx.clickDesktop(point);
                })
                .thenWaitUntil(() -> shell(ctx).screen() == null, SCREEN_WAIT, "the program to return")
                .thenAssert(0, () -> shell(ctx).scrollbackText().contains("click 1 " + clicked[0] + " " + clicked[1]),
                        "the click reached it at its cell")
                .thenAssert(0, () -> shell(ctx).scrollbackText().contains("REACTOR 1")
                                && shell(ctx).scrollbackText().contains("hi Ada"),
                        "what it left on its screen stays in the scrollback")
                .thenAssert(0, () -> shell(ctx).title().equals("Command Prompt"), "the title is the terminal's again")
                .thenScreenshot(2, "returned");
    }

    /** Ctrl+T asks the program to end, which ends one that does not catch it, in red under its screen. */
    @ClientTest(timeoutTicks = 3000)
    public static void commandPrompt_ctrlTAsksTheProgramToEnd(final ClientTestContext ctx) {
        atTheDesktop(ctx, TERMINAL)
                .thenWaitUntil(() -> shell(ctx) != null, SCREEN_WAIT, "the terminal window to open")
                .then(SETTLE, () -> ctx.type("lrt run progs/wait.lua"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> shows(shell(ctx).screen(), "waiting"), SCREEN_WAIT, "the program to be waiting")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_T, GLFW.GLFW_MOD_CONTROL))
                .thenWaitUntil(() -> shell(ctx).screen() == null, SCREEN_WAIT, "Ctrl+T to end it")
                .thenAssert(0, () -> shell(ctx).scrollbackText().contains("Terminated"), "it ended as terminated")
                .then(SETTLE, () -> ctx.type("lrt run progs/wait.lua"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> shows(shell(ctx).screen(), "waiting"), SCREEN_WAIT, "the program to wait again")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_C, GLFW.GLFW_MOD_CONTROL))
                .thenWaitUntil(() -> shell(ctx).screen() == null, SCREEN_WAIT, "Ctrl+C to stop it outright")
                .thenAssert(0, () -> shell(ctx).scrollbackText().contains("^C"), "and the terminal says so");
    }

    /**
     * In the editor's panel, F5 on a Lua file shows its screen at three quarters, and when the panel is
     * too short for all of it, the rows round the cursor.
     */
    @ClientTest(timeoutTicks = 3000)
    public static void editorPanel_showsTheRowsRoundTheCursor(final ClientTestContext ctx) {
        atTheDesktop(ctx, EDITOR_LAUNCHER)
                .thenWaitUntil(() -> editor(ctx) != null, SCREEN_WAIT, "the editor window to open")
                .then(SETTLE, () -> editor(ctx).openFolder("progs"))
                .thenWaitUntil(() -> editor(ctx).sideLabels().contains("tall.lua"), SCREEN_WAIT, "the tree to list it")
                .then(SETTLE, () -> editor(ctx).openFile("progs/tall.lua"))
                .thenWaitUntil(() -> "progs/tall.lua".equals(editor(ctx).openFile()), SCREEN_WAIT, "the file on a tab")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_F5))
                .thenWaitUntil(() -> shows(editor(ctx).terminalScreen(), "line 18"), SCREEN_WAIT * 3,
                        "the program's screen in the panel")
                .thenAssert(SETTLE, () -> editor(ctx).terminalScreenRows() != null, "the panel draws it")
                .thenAssert(0, () -> {
                    final int[] rows = editor(ctx).terminalScreenRows();
                    return rows[1] >= LuaScreenLayout.ROWS || rows[0] + rows[1] - 1 == LuaScreenLayout.ROWS;
                }, "the rows showing reach the cursor at the bottom")
                .thenScreenshot(2, "panel")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> editor(ctx).terminalScreen() == null, SCREEN_WAIT, "the key to end it")
                .thenAssert(0, () -> editor(ctx).terminalText().contains("line 18"),
                        "its screen stays in the panel's scrollback");
    }
}
