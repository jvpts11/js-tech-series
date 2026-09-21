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
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.EditorApp;
import dev.jstech.computers.client.os.OpenWithPopup;
import dev.jstech.computers.program.ServerCliComputer;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/**
 * Open with, the way a player meets it: a double-click on a file of a kind the computer does not know asks which
 * program opens it, and Always makes the answer stick for that extension on that computer.
 */
public final class OpenWithClientTests {

    private OpenWithClientTests() {
    }

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 400;

    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final String FILE = "thing.fk";

    private static CraftingComputerBlockEntity computer(final ClientTestContext ctx, final ServerLevel level) {
        if (level.getBlockEntity(ctx.abs(COMPUTER)) instanceof CraftingComputerBlockEntity be) {
            return be;
        }
        throw new ClientTestFailure("no computer at " + ctx.abs(COMPUTER));
    }

    private static List<DesktopWindow> editors(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        return desktop == null ? List.of() : desktop.windowsFor("Editor");
    }

    private static boolean newestEditorHas(final ClientTestContext ctx, final String name) {
        final List<DesktopWindow> windows = editors(ctx);
        return !windows.isEmpty() && windows.getLast().app() instanceof EditorApp editor
                && editor.openFile().endsWith(name);
    }

    @ClientTest(timeoutTicks = 2400)
    public static void openWith_asksAboutAnUnknownKindAndAlwaysMakesTheChoiceStick(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    final CraftingComputerBlockEntity computer = world.placeRunningCraftingComputer(COMPUTER);
                    computer.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_11"));
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenServer(SETTLE * 3, level -> ctx.assertTrue(new ServerCliComputer(computer(ctx, level), level)
                                .writeFile("C:\\" + FILE, "made by a program").ok(),
                        "a program's file of a kind of its own is on the disk"))
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                // Opened the way a double-click opens it.
                .then(SETTLE, () -> DesktopScreen.requestOpenFile(FILE))
                .thenWaitUntil(() -> DesktopScreen.openWithChooser() != null, SCREEN_WAIT,
                        "Open with to ask which program opens " + FILE)
                .thenAssert(0, () -> DesktopScreen.openWithChooser().programs().contains("editor"),
                        "the Editor is offered")
                .thenAssert(0, () -> DesktopScreen.openWithChooser().question().contains(FILE),
                        "the chooser names the file")
                .thenAssert(0, () -> "Nothing on this computer opens .fk files yet.".equals(
                                DesktopScreen.openWithChooser().note()), "the line under the question shows whole")
                .thenScreenshot(2, "open-with-chooser")
                .then(SETTLE, () -> {
                    final OpenWithPopup chooser = DesktopScreen.openWithChooser();
                    chooser.pick(chooser.programs().indexOf("editor"));
                    chooser.always();
                })
                .thenWaitUntil(() -> newestEditorHas(ctx, FILE), SCREEN_WAIT, "Always to open the file in the Editor")
                .thenWaitUntilServer(
                        level -> "editor".equals(computer(ctx, level).console().settings().defaultApp("fk")),
                        SCREEN_WAIT, "the computer to keep the Editor for .fk files",
                        level -> "it keeps " + computer(ctx, level).console().settings().defaultApps())
                // The next double-click opens it straight away, with nothing asked.
                .then(SETTLE, () -> DesktopScreen.requestOpenFile(FILE))
                .thenWaitUntil(() -> editors(ctx).size() == 2 && newestEditorHas(ctx, FILE), SCREEN_WAIT,
                        "a second Editor to open the file without asking")
                .thenAssert(0, () -> DesktopScreen.openWithChooser() == null, "no chooser came up the second time")
                .thenScreenshot(2, "opened-without-asking");
    }
}
