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
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.FilesApp;
import dev.jstech.computers.client.os.IDesktopApp;
import dev.jstech.computers.client.os.QuestionPopup;
import dev.jstech.computers.client.os.TrashApp;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The trash as a player meets it on each family of desktops: a deleted file waits in it, comes back when it is
 * restored, and is only ever deleted for good after a question, as a file on a disc in a drive is.
 */
public final class TrashClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 400;
    private static final int CDE_BOOT_WAIT = 1_200;

    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos DRIVE = new BlockPos(4, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    private static final String NOTES = "notes.txt";

    private TrashClientTests() {
    }

    /**
     * On Frames XP the Recycle Bin is the first icon on the desktop; a file deleted from its menu goes into the bin,
     * the bin wears its full picture, and Restore this item on the bin's own tasks puts the file back.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void recycleBin_keepsADeletedFileAndPutsItBack(final ClientTestContext ctx) {
        framesDesktop(ctx, "Users/Public/Desktop/" + NOTES)
                .thenAssert(0, () -> desktop(ctx).deskIconLabels().getFirst().equals("Recycle Bin"),
                        "the Recycle Bin is the first icon on the desktop")
                .thenAssert(0, () -> !desktop(ctx).trashFull(), "and it starts empty")
                .then(SETTLE, () -> ctx.rightClickDesktop(desktop(ctx).deskIconPoint(NOTES)))
                .thenWaitUntil(() -> desktop(ctx).deskMenuOpen(), SCREEN_WAIT, "the file's menu to open")
                .then(SETTLE, () -> ctx.clickDesktop(desktop(ctx).deskMenuItemCenter(
                        desktop(ctx).deskMenuLabels().indexOf("Delete"))))
                .thenWaitUntil(() -> !desktop(ctx).desktopItemNames().contains(NOTES) && desktop(ctx).trashFull(),
                        SCREEN_WAIT, "Delete to take the file into the bin without asking")
                .thenScreenshot(SETTLE, "trash-xp-desktop-full")
                .then(SETTLE, () -> openTrashIcon(ctx, "Recycle Bin"))
                .thenWaitUntil(() -> trash(ctx) != null && trash(ctx).shownNames().contains(NOTES), SCREEN_WAIT,
                        "the Recycle Bin to open and list the file")
                .then(SETTLE, () -> ctx.clickDesktop(trash(ctx).itemPoint(NOTES)))
                .thenScreenshot(SETTLE, "trash-xp-bin")
                .then(SETTLE, () -> ctx.clickDesktop(trash(ctx).controlPoint("Restore this item")))
                .thenWaitUntil(() -> desktop(ctx).desktopItemNames().contains(NOTES)
                                && trash(ctx).shownNames().isEmpty() && !desktop(ctx).trashFull(), SCREEN_WAIT,
                        "Restore this item to put the file back and leave the bin empty");
    }

    /**
     * On KDE Plasma a file dragged onto the Trash is deleted into it; the Trash lists it with Dolphin's own menu,
     * and Empty Trash asks before it deletes everything for good.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void kdeTrash_takesADroppedFileAndAsksBeforeEmptying(final ClientTestContext ctx) {
        linuxDesktop(ctx, "home/player/Desktop/" + NOTES)
                .thenAssert(0, () -> desktop(ctx).deskIconLabels().getFirst().equals("Trash"),
                        "the Trash is the first icon on the desktop")
                .then(SETTLE, () -> ctx.dragDesktop(desktop(ctx).deskIconPoint(NOTES),
                        desktop(ctx).deskIconPoint("Trash")))
                .thenWaitUntil(() -> !desktop(ctx).desktopItemNames().contains(NOTES) && desktop(ctx).trashFull(),
                        SCREEN_WAIT, "the file dropped on the Trash to go into it")
                .then(SETTLE, () -> openTrashIcon(ctx, "Trash"))
                .thenWaitUntil(() -> trash(ctx) != null && trash(ctx).shownNames().contains(NOTES), SCREEN_WAIT,
                        "the Trash to open and list the file")
                .then(SETTLE, () -> ctx.rightClickDesktop(trash(ctx).itemPoint(NOTES)))
                .thenWaitUntil(() -> trash(ctx).menuLabels().contains("Restore to Former Location"), SCREEN_WAIT,
                        "the file's menu to say what Dolphin says")
                .thenScreenshot(SETTLE, "trash-kde-menu")
                // The first click puts the file's menu away, as a click anywhere does; the second presses the button.
                .then(SETTLE, () -> ctx.clickDesktop(trash(ctx).controlPoint("Empty Trash")))
                .then(SETTLE, () -> ctx.clickDesktop(trash(ctx).controlPoint("Empty Trash")))
                .thenWaitUntil(() -> question(ctx) != null && question(ctx).asks(), SCREEN_WAIT,
                        "Empty Trash to ask before it deletes anything")
                .thenAssert(0, () -> trash(ctx).shownNames().contains(NOTES), "nothing is gone before the answer")
                .thenScreenshot(SETTLE, "trash-kde-empty-question")
                .then(SETTLE, () -> ctx.clickDesktop(question(ctx).firstButtonCentre()))
                .thenWaitUntil(() -> trash(ctx).shownNames().isEmpty() && !desktop(ctx).trashFull(), SCREEN_WAIT,
                        "Yes to delete everything in the Trash for good");
    }

    /**
     * On CDE the trash is no icon on the backdrop but a control at the right end of the Front Panel; it opens the
     * Trash Can, and Put Back on its Selected menu puts a file back where it came from.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void cdeTrashCan_standsOnTheFrontPanelAndPutsBack(final ClientTestContext ctx) {
        cdeDesktop(ctx, "usr/player/Desktop/" + NOTES)
                .thenAssert(0, () -> !desktop(ctx).deskIconLabels().contains("Trash Can"),
                        "CDE stands no trash on its backdrop")
                .then(SETTLE, () -> ctx.rightClickDesktop(desktop(ctx).deskIconPoint(NOTES)))
                .thenWaitUntil(() -> desktop(ctx).deskMenuOpen(), SCREEN_WAIT, "the file's menu to open")
                .then(SETTLE, () -> ctx.clickDesktop(desktop(ctx).deskMenuItemCenter(
                        desktop(ctx).deskMenuLabels().indexOf("Delete"))))
                .thenWaitUntil(() -> desktop(ctx).trashFull(), SCREEN_WAIT, "the file to go into the Trash Can")
                .then(SETTLE, () -> {
                    final int[] at = desktop(ctx).frontPanelPoint(CdeFrontPanelLayout.Control.TRASH);
                    ctx.click(at[0] + 0.5, at[1] + 0.5);
                })
                .thenWaitUntil(() -> trash(ctx) != null && trash(ctx).shownNames().contains(NOTES), SCREEN_WAIT,
                        "the Front Panel's control to open the Trash Can with the file in it")
                .then(SETTLE, () -> ctx.clickDesktop(trash(ctx).itemPoint(NOTES)))
                .then(SETTLE, () -> ctx.clickDesktop(trash(ctx).controlPoint("Selected")))
                .thenWaitUntil(() -> trash(ctx).menuLabels().equals(List.of("Put Back", "Shred")), SCREEN_WAIT,
                        "the Selected menu to offer CDE's own Put Back and Shred")
                .thenScreenshot(SETTLE, "trash-cde-can")
                .then(SETTLE, () -> ctx.clickDesktop(trash(ctx).menuPoint("Put Back")))
                .thenWaitUntil(() -> desktop(ctx).desktopItemNames().contains(NOTES) && !desktop(ctx).trashFull(),
                        SCREEN_WAIT, "Put Back to put the file back on the desktop");
    }

    /**
     * A file on a disc in a drive has no trash to go to: deleting it asks first, No leaves it where it is, and Yes
     * deletes it for good.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void aFileOnADisc_isDeletedForGoodOnlyAfterAQuestion(final ClientTestContext ctx) {
        final String[] medium = {""};
        mainframeDesktop(ctx, "frames_xp", null, "Users/Public/Desktop/readme.txt")
                .thenBuild(0, world -> {
                    world.setBlock(DRIVE, ComputingModule.CD_DRIVE.get());
                    final ItemStack disc = new ItemStack(ComputingModule.CD_ROM.get());
                    MediaItem.setKind(disc, MediaKind.DATA);
                    DiskFilesystem.write(disc, NOTES, FileType.TXT, "on a disc", Long.MAX_VALUE,
                            FilesystemKind.HIERARCHICAL);
                    world.blockEntity(DRIVE, MediaReaderBlockEntity.class).mediaSlot().setStackInSlot(0, disc);
                    medium[0] = "media:" + ctx.abs(DRIVE).asLong();
                })
                .then(SETTLE * 2, () -> DesktopScreen.requestOpenFiles(medium[0]))
                .thenWaitUntil(() -> files(ctx) != null && files(ctx).names().contains(NOTES), SCREEN_WAIT,
                        "the explorer to open on the disc and list the file")
                .then(SETTLE, () -> ctx.rightClickDesktop(files(ctx).rowPoint(NOTES)))
                .thenWaitUntil(() -> files(ctx).contextOpen(), SCREEN_WAIT, "the file's menu to open")
                .then(SETTLE, () -> ctx.clickDesktop(files(ctx).contextPoint("Delete")))
                .thenWaitUntil(() -> question(ctx) != null && String.join(" ", question(ctx).text())
                                .contains("cannot go to the Recycle Bin"), SCREEN_WAIT,
                        "Delete to say the file cannot go to the Recycle Bin, and ask")
                .thenScreenshot(SETTLE, "trash-confirm-file-delete")
                .then(SETTLE, () -> ctx.clickDesktop(question(ctx).firstButtonCentre()))
                .thenWaitUntil(() -> !files(ctx).names().contains(NOTES), SCREEN_WAIT,
                        "Yes to delete the file for good")
                .thenAssert(SETTLE, () -> !desktop(ctx).trashFull(), "and nothing went into the Recycle Bin");
    }

    /** A Frames XP crafting computer at its desktop, with a file written on its disk at {@code path}. */
    private static ClientTestContext framesDesktop(final ClientTestContext ctx, final String path) {
        return ctx.thenBuild(0, world -> {
                    final CraftingComputerBlockEntity computer = world.placeRunningCraftingComputer(COMPUTER);
                    computer.installOs(jsc("frames_xp"));
                    write(computer.systemDisk(), path);
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> desktop(ctx).desktopItemNames().contains(FsPaths.fileName(path)), SCREEN_WAIT,
                        "the desktop to show the file");
    }

    /** An Ubuntu crafting computer at KDE Plasma, with a file written on its disk at {@code path}. */
    private static ClientTestContext linuxDesktop(final ClientTestContext ctx, final String path) {
        return ctx.thenBuild(0, world -> {
                    final CraftingComputerBlockEntity computer = world.placeRunningCraftingComputer(COMPUTER);
                    computer.installOs(jsc("ubuntu"));
                    computer.console().install("jsc:kde_plasma");
                    write(computer.systemDisk(), path);
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> desktop(ctx).desktopItemNames().contains(FsPaths.fileName(path)), SCREEN_WAIT,
                        "the desktop to show the file");
    }

    /** A UNIX machine brought up at CDE, with a file written on its disk at {@code path}. */
    private static ClientTestContext cdeDesktop(final ClientTestContext ctx, final String path) {
        return mainframeDesktop(ctx, "unix", "jsc:cde", path);
    }

    /**
     * A Mainframe carrying {@code system}, and {@code desktopPackage} on it when one is named, brought up at its
     * desktop with a file written on its disk at {@code path}. A drive set beside it is the Mainframe's to read.
     */
    private static ClientTestContext mainframeDesktop(final ClientTestContext ctx, final String system,
                                                      final String desktopPackage, final String path) {
        return ctx.thenBuild(0, world -> {
                    world.setBlock(COMPUTER, ComputingModule.MAINFRAME.get());
                    final MainframeBlockEntity machine = world.blockEntity(COMPUTER, MainframeBlockEntity.class);
                    final ItemStackHandler inv = machine.getInventory();
                    inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                            new ItemStack(ComputingModule.MOTHERBOARD_MTX_P.get()));
                    inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START,
                            new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
                    inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START,
                            new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
                    inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
                    // A graphics card gives the machine peripheral ports, which is what the monitor links to.
                    inv.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                            new ItemStack(ComputingModule.GPU_HD_7970.get()));
                    inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                            new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
                    machine.installOs(jsc(system));
                    if (desktopPackage != null) {
                        machine.console().install(desktopPackage);
                    }
                    write(machine.systemDisk(), path);
                    machine.togglePower();
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, CDE_BOOT_WAIT)
                .thenWaitUntil(() -> desktop(ctx).desktopItemNames().contains(FsPaths.fileName(path)), SCREEN_WAIT,
                        "the desktop to show the file");
    }

    /** Opens the trash from its icon on the wallpaper, with the double click a player gives it. */
    private static void openTrashIcon(final ClientTestContext ctx, final String name) {
        final int[] at = desktop(ctx).deskIconPoint(name);
        ctx.clickDesktop(at);
        ctx.clickDesktop(at);
    }

    private static void write(final ItemStack disk, final String path) {
        String built = "";
        final String parent = path.substring(0, path.lastIndexOf('/'));
        for (final String segment : parent.split("/")) {
            built = built.isEmpty() ? segment : built + "/" + segment;
            DiskFilesystem.mkdir(disk, built, FilesystemKind.HIERARCHICAL);
        }
        DiskFilesystem.write(disk, path, FileType.TXT, "a line", Long.MAX_VALUE, FilesystemKind.HIERARCHICAL);
    }

    private static DesktopScreen desktop(final ClientTestContext ctx) {
        return ctx.screen(DesktopScreen.class);
    }

    private static TrashApp trash(final ClientTestContext ctx) {
        return desktop(ctx) == null ? null : desktop(ctx).trashWindow();
    }

    private static QuestionPopup question(final ClientTestContext ctx) {
        return desktop(ctx) == null ? null : desktop(ctx).question();
    }

    private static FilesApp files(final ClientTestContext ctx) {
        final var window = desktop(ctx) == null ? null : desktop(ctx).windowFor("Files");
        final IDesktopApp app = window == null ? null : window.app();
        return app instanceof FilesApp f ? f : null;
    }

    private static ResourceLocation jsc(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }
}
