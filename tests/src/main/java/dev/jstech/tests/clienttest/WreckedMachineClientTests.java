/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.client.BootSequenceScreen;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.FilesApp;
import dev.jstech.computers.client.os.IDesktopApp;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.os.boot.SystemIntegrity;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * Wrecking a computer the way a player really does it: not at a prompt, but in the file explorer, with the
 * file dropped in the bin, and then starting the machine again to find out what it costs.
 *
 * <p>That path goes nowhere near the one the prompt takes. The explorer asks the machine to move the file
 * into the trash, and the machine is then restarted by somebody standing in front of it. A system a player
 * can delete from a terminal and not from the desktop in front of them is a system they cannot delete, and
 * a machine that says nothing about it at the next start is a machine that was never wrecked at all.
 */
public final class WreckedMachineClientTests {

    private WreckedMachineClientTests() {
    }

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 400;

    /** How long a self-test is given to play out before the machine is asked where it got to. */
    private static final int POST_WAIT = 200;

    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final ResourceLocation FRAMES_11 =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_11");

    /** The folder the newest Frames lives in, and the file in it that starts the system. */
    private static final String SYSTEM_DIR = "Frames";
    private static final String LOADER = "kickmgr.sys";

    private static DesktopScreen desktop(final ClientTestContext ctx) {
        return ctx.screen(DesktopScreen.class);
    }

    private static FilesApp files(final ClientTestContext ctx) {
        final var window = desktop(ctx) == null ? null : desktop(ctx).windowFor("Files");
        final IDesktopApp app = window == null ? null : window.app();
        return app instanceof FilesApp f ? f : null;
    }

    /** The machine under the desktop. Only ever asked for on the server, where the block entity is. */
    private static CraftingComputerBlockEntity machine(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin())
                .blockEntity(COMPUTER, CraftingComputerBlockEntity.class);
    }

    private static SystemIntegrity.State health(final ClientTestContext ctx, final ServerLevel level) {
        return SystemIntegrity.check(machine(ctx, level)).state();
    }

    /**
     * A Frames 11 machine at its desktop, with the monitor open in front of the player.
     *
     * <p>On a fast disk on purpose: it is what anybody reaches for, and the check that reads what is on a
     * disk has no business caring which kind it is. Caring is exactly the sort of thing that would only ever
     * show up on somebody else's machine.
     */
    private static ClientTestContext atTheDesktop(final ClientTestContext ctx) {
        return ctx.thenBuild(0, builder -> {
                    final CraftingComputerBlockEntity computer = builder.placeRunningCraftingComputer(COMPUTER);
                    computer.getHardware().setStackInSlot(CraftingComputerBlockEntity.DISK_SLOTS_START,
                            new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_1)));
                    if (!computer.installOs(FRAMES_11)) {
                        throw new IllegalStateException("the newest Frames would not install on an NVMe");
                    }
                    computer.togglePower();
                    computer.togglePower();
                    builder.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenServer(0, level -> {
                    final CraftingComputerBlockEntity computer = machine(ctx, level);
                    ctx.assertTrue(DiskFilesystem.exists(computer.systemDisk(),
                                    SystemIntegrity.loaderOf(computer.installedOs())),
                            "the system wrote the file that starts it, or there is nothing here to delete");
                });
    }

    /** Opens the explorer on the system folder and deletes the file that starts the system. */
    private static ClientTestContext deleteTheLoader(final ClientTestContext ctx) {
        return ctx.then(SETTLE, () -> DesktopScreen.requestOpenFiles(SYSTEM_DIR))
                .thenWaitUntil(() -> files(ctx) != null && files(ctx).names().contains(LOADER), SCREEN_WAIT,
                        "the explorer to open the system folder and show the file that starts the system")
                .thenScreenshot(SETTLE, "wrecked-loader-in-explorer")
                .then(SETTLE, () -> ctx.rightClickDesktop(files(ctx).rowPoint(LOADER)))
                .thenWaitUntil(() -> files(ctx).contextOpen(), SCREEN_WAIT, "the file's menu to open")
                .then(SETTLE, () -> ctx.assertTrue(files(ctx).contextLabels().contains("Delete"),
                        "and its menu offers to delete it; got " + files(ctx).contextLabels()))
                .then(0, () -> ctx.clickDesktop(files(ctx).contextPoint("Delete")))
                .thenWaitUntil(() -> !files(ctx).names().contains(LOADER), SCREEN_WAIT,
                        "Delete to take the file out of the system folder");
    }

    /**
     * The whole way through, as a player does it: delete the file in the explorer, restart the machine, and
     * find it standing at its own failure instead of showing a desktop.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void deletingTheLoaderInTheExplorer_wrecksTheMachine(final ClientTestContext ctx) {
        deleteTheLoader(atTheDesktop(ctx))
                .thenWaitUntilServer(level -> health(ctx, level) == SystemIntegrity.State.NO_LOADER,
                        SCREEN_WAIT, "the machine to find the file that starts it gone",
                        level -> "health is " + health(ctx, level))
                /* Restarted by somebody standing in front of it, which is when this is ever found out. */
                .thenServer(SETTLE, level -> machine(ctx, level).setNeedsPost(true))
                .thenWaitUntilServer(level -> machine(ctx, level).haltedAtPost(), POST_WAIT,
                        "the machine to stand at its own failure rather than show a desktop",
                        level -> "halted=" + machine(ctx, level).haltedAtPost()
                                + " needsPost=" + machine(ctx, level).needsPost()
                                + " health=" + health(ctx, level))
                /*
                 * And what the player sees when they look at it: the failure, not a desktop. This is the
                 * whole of it to somebody standing in front of the machine, and a machine that halted
                 * without saying so on its own glass is a machine that looks like nothing happened.
                 */
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(BootSequenceScreen.class, BOOT_WAIT)
                .thenScreenshot(SETTLE, "wrecked-machine-will-not-start");
    }

    /**
     * And the other way a player wrecks one: the whole system folder taken away.
     *
     * <p>The folder is deleted from the drive's own listing rather than from inside itself, which is where a
     * player deleting a folder is standing.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void deletingTheSystemFolderInTheExplorer_wrecksTheMachine(final ClientTestContext ctx) {
        atTheDesktop(ctx)
                .then(SETTLE, () -> DesktopScreen.requestOpenFiles(""))
                .thenWaitUntil(() -> files(ctx) != null && files(ctx).names().contains(SYSTEM_DIR), SCREEN_WAIT,
                        "the explorer to open the disk and show the system folder")
                .then(SETTLE, () -> ctx.rightClickDesktop(files(ctx).rowPoint(SYSTEM_DIR)))
                .thenWaitUntil(() -> files(ctx).contextOpen(), SCREEN_WAIT, "the folder's menu to open")
                .then(SETTLE, () -> ctx.assertTrue(files(ctx).contextLabels().contains("Delete"),
                        "a folder can be deleted too; got " + files(ctx).contextLabels()))
                .then(0, () -> ctx.clickDesktop(files(ctx).contextPoint("Delete")))
                .thenWaitUntil(() -> !files(ctx).names().contains(SYSTEM_DIR), SCREEN_WAIT,
                        "Delete to take the system folder off the disk")
                .thenWaitUntilServer(level -> health(ctx, level) != SystemIntegrity.State.WHOLE,
                        SCREEN_WAIT, "the machine to find its system gone",
                        level -> "health is " + health(ctx, level))
                .thenServer(SETTLE, level -> machine(ctx, level).setNeedsPost(true))
                .thenWaitUntilServer(level -> machine(ctx, level).haltedAtPost(), POST_WAIT,
                        "the machine to stand at its own failure",
                        level -> "halted=" + machine(ctx, level).haltedAtPost()
                                + " health=" + health(ctx, level));
    }
}
