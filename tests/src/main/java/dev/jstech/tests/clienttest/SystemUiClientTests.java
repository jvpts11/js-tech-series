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
import dev.jstech.computers.client.os.CannonWindowApp;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.IDesktopApp;
import dev.jstech.computers.client.os.ShellApp;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

/**
 * A Cannon program's own window on the desktop: it opens when the program says so, is drawn by the
 * machine's system, hears what the player does to its widgets, and goes when the program or the player
 * closes it.
 */
public final class SystemUiClientTests {

    private SystemUiClientTests() {
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

    /** A panel with a label, a button, a box to type in and a list, laid out in a row and a column. */
    private static final String PANEL = """
            using System.*;
            using System.UI.*;
            namespace Plant;
            class Panel : IScript {
                Window window;
                Label heat;
                TextBox note;
                ListBox log;
                public void OnInit() {
                    heat = new Label("812 K");
                    Button scram = new Button("SCRAM");
                    scram.OnClick += Scram;
                    note = new TextBox("");
                    note.OnSubmit += Noted;
                    log = new ListBox();
                    Row top = new Row();
                    top.Add(heat);
                    top.Add(scram);
                    Row typing = new Row();
                    typing.Add(note, 1);
                    Column page = new Column();
                    page.Add(top);
                    page.Add(typing);
                    page.Add(log, 1);
                    window = new Window("Reactor", 240, 150);
                    window.Content = page;
                    window.Show();
                }
                void Scram() { heat.Text = "cold"; log.Add("scram", "now"); }
                void Noted() { log.Add(note.Text, "said"); }
                public void OnTick() { }
                public void OnDestroy() { window.Close(); }
            }
            """;

    private static ResourceLocation program(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }

    private static ShellApp shell(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        final var window = desktop == null ? null : desktop.windowFor(TERMINAL);
        final IDesktopApp app = window == null ? null : window.app();
        return app instanceof ShellApp s ? s : null;
    }

    private static CannonWindowApp panel(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        return desktop == null ? null : desktop.programWindow();
    }

    private static void launch(final ClientTestContext ctx, final String label) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        ctx.click(desktop.startButtonX(), desktop.startButtonY());
        final int item = desktop.launcherLabels().indexOf(label);
        ctx.click(desktop.startMenuItemX(item), desktop.startMenuItemY(item));
    }

    /**
     * The window opens on the desktop with the widgets the program asked for, a click on its button runs
     * the program's handler and what that changed is drawn, and a line typed reaches it too.
     */
    @ClientTest(timeoutTicks = 3000)
    public static void programWindow_opensOnTheDesktopAndAnswersThePlayer(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    final CraftingComputerBlockEntity computer = world.placeRunningCraftingComputer(COMPUTER);
                    computer.installOs(FRAMES_XP);
                    for (final String id : new String[] {"cannonc", "cannonrt"}) {
                        computer.console().install(program(id).toString());
                    }
                    DiskFilesystem.write(computer.systemDisk(), "progs/panel.can", FileType.CAN, PANEL,
                            Long.MAX_VALUE, FilesystemKind.HIERARCHICAL);
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).launcherLabels().contains(TERMINAL),
                        SCREEN_WAIT, "the terminal to be listed in Start")
                .then(0, () -> launch(ctx, TERMINAL))
                .thenWaitUntil(() -> shell(ctx) != null, SCREEN_WAIT, "the terminal window to open")
                .then(SETTLE, () -> ctx.type("cannon run progs/panel.can"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> panel(ctx) != null, SCREEN_WAIT * 3, "the program's window to open")
                .thenAssert(SETTLE, () -> "Reactor".equals(panel(ctx).title()), "it is called what the program said")
                .thenAssert(0, () -> "812 K".equals(panel(ctx).said("Label")), "and shows what it was given")
                .thenScreenshot(2, "window")
                .then(SETTLE, () -> ctx.clickDesktop(panel(ctx).pointAt("Button")))
                .thenWaitUntil(() -> "cold".equals(panel(ctx).said("Label")), SCREEN_WAIT,
                        "the click to reach the program and its label to change")
                .thenAssert(0, () -> panel(ctx).rows() == 1, "and its list to have the row it wrote")
                .then(SETTLE, () -> ctx.clickDesktop(panel(ctx).pointAt("TextBox")))
                .then(1, () -> ctx.type("hot"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> panel(ctx).rows() == 2, SCREEN_WAIT, "the line typed to reach the program")
                .thenScreenshot(2, "answered")
                .then(SETTLE, () -> {
                    final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
                    final var window = desktop.windowFor(panel(ctx).key());
                    ctx.clickDesktop(new int[] {window.x() + window.width() - 8, window.y() + 6});
                })
                .thenWaitUntil(() -> panel(ctx) == null, SCREEN_WAIT, "the window to close when the player shuts it");
    }
}
