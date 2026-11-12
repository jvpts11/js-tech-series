/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.client.monitor.TouchScreens;
import dev.jstech.computers.client.monitor.TouchTypingScreen;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.operation.payload.files.FileAccess;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The touch monitors at work (computers/docs/design/peripherals.md): a wall of six touch monitors against a computer of
 * Frames 10, and one of Frames 11; a touch on the glass sits the player down at it, the Editor open on a file takes
 * typing, so the system's touch keyboard comes up on the glass (docked along the foot on Frames 10, floating on Frames
 * 11), and a tap on the glass opens the line the player types into, which types into the Editor as it goes.
 */
public final class TouchMonitorClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 160;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    /* The wall: three monitors across and two high, its glass to the east, its middle column against the computer. */
    private static final BlockPos WALL = COMPUTER.east();
    private static final BlockPos PLAYER = WALL.east(3);
    private static final String DOCUMENTS = "Users/Public/Documents";
    private static final String NOTES = DOCUMENTS + "/Base notes.txt";
    private static final String EDITOR = "jsc:editor";

    private TouchMonitorClientTests() {
    }

    /* Frames 10's keyboard lies along the foot of the desktop. */
    @ClientTest(timeoutTicks = 1600)
    public static void frames10_docksItsTouchKeyboardOnTheWall(final ClientTestContext ctx) {
        typingOnTheWall(ctx, "frames_10", ComputingModule.TOUCH_MONITOR, WALL.above(), 62, "touch-frames-10");
    }

    /* Frames 11's floats in the middle. */
    @ClientTest(timeoutTicks = 1600)
    public static void frames11_floatsItsTouchKeyboardOnTheWall(final ClientTestContext ctx) {
        typingOnTheWall(ctx, "frames_11", ComputingModule.ADVANCED_TOUCH_MONITOR, WALL.above(), 60,
                "touch-frames-11");
    }

    /*
     * A computer of {@code system} with the wall of {@code monitor} against it and notes on its disk; touched, the
     * Editor opened on the notes, the typing line opened by a tap on the Editor's page below them ({@code tapY}
     * sixty-fourths down the middle of the wall's monitor at {@code tapAt}) and a line typed into it; shot from where
     * the player stands.
     */
    private static void typingOnTheWall(final ClientTestContext ctx, final String system,
                                        final BlockEntry<MonitorBlock> monitor, final BlockPos tapAt, final int tapY,
                                        final String label) {
        ctx.afterTest(() -> ctx.mc().options.keyUse.setDown(false));
        ctx.thenServer(0, level -> ctx.serverPlayer().getInventory().clearContent())
                .thenBuild(0, world -> {
                    world.placeRunningPersonalComputer(COMPUTER,
                            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, system));
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int dy = 0; dy <= 1; dy++) {
                            // A monitor's facing is where its back looks: this wall's back to the computer.
                            world.setBlock(WALL.offset(0, dy, dz), monitor.get().defaultBlockState()
                                    .setValue(MonitorBlock.FACING, Direction.WEST));
                        }
                    }
                })
                .thenServer(SETTLE * 5, level -> {
                    final IOsHost host = (IOsHost) level.getBlockEntity(ctx.abs(COMPUTER));
                    final ItemStack disk = host.systemDisk();
                    final FilesystemKind kind = FileAccess.filesystemKindOf(host);
                    DiskFilesystem.mkdirs(disk, DOCUMENTS, kind);
                    DiskFilesystem.write(disk, NOTES, FileType.TXT, "Base notes\n\nIron farm: 4 more golems\n"
                            + "Wheat: replant row 3\n", Long.MAX_VALUE, kind);
                    host.setChanged();
                })
                .thenTeleport(SETTLE, PLAYER, Direction.WEST)
                .then(SETTLE * 10, () -> ctx.lookAtFront(WALL.above(), 32, 40))
                .then(SETTLE, () -> touch(ctx))
                .then(2, () -> ctx.mc().options.keyUse.setDown(false))
                .thenWaitUntil(() -> TouchScreens.held() instanceof DesktopScreen, SCREEN_WAIT,
                        "a touch on the glass to sit the player down at the wall's desktop")
                .then(SETTLE * 10, () -> DesktopScreen.requestOpenFile(NOTES))
                .thenWaitUntil(() -> TouchScreens.held() instanceof DesktopScreen desktop
                        && desktop.windowFor(EDITOR) != null, SCREEN_WAIT, "the notes to open in the Editor")
                // A touch on the Editor's page below the notes puts the caret on the empty line after them.
                .then(SETTLE * 2, () -> ctx.lookAtFront(tapAt, 32, tapY))
                .then(SETTLE, () -> touch(ctx))
                .then(2, () -> ctx.mc().options.keyUse.setDown(false))
                .thenWaitUntil(() -> ctx.mc().screen instanceof TouchTypingScreen, SCREEN_WAIT,
                        "a tap on the Editor to open the line typed into")
                .then(SETTLE, () -> ctx.type("Wall: a touch panel by the d"))
                .then(SETTLE * 2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(SETTLE, label);
    }

    /* The use key pressed with the crosshair on the glass, as a finger touches it; released by the next step. */
    private static void touch(final ClientTestContext ctx) {
        KeyMapping.click(ctx.mc().options.keyUse.getKey());
        ctx.mc().options.keyUse.setDown(true);
    }
}
