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
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.program.install.LiveInstallState;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * A live medium's terminal as a player sits at it: the prompt that fills the monitor, and the editor the
 * medium carries taking that glass over to edit a file of the installation.
 */
public final class LiveInstallClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 600;

    private static final BlockPos MACHINE = new BlockPos(5, 2, 2);
    private static final BlockPos DRIVE = new BlockPos(4, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    private LiveInstallClientTests() {
    }

    /**
     * nano on the live medium: it opens on a file that is not there yet and says so, what is typed goes in,
     * writing it out asks for the name and says how many lines went, leaving gives the prompt back, and the
     * shell reads back what was written.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void nano_writesAFileOfTheInstallationAndGivesThePromptBack(final ClientTestContext ctx) {
        atTheLivePrompt(ctx, "gentoo", LiveInstallState.Distro.GENTOO)
                .thenScreenshot(2, "live-prompt")
                .then(SETTLE, () -> ctx.type("nano /root/notes.txt"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> prompt(ctx).editing(), SCREEN_WAIT, "nano to take the terminal")
                .thenScreenshot(2, "nano-new-file")
                .then(SETTLE, () -> ctx.type("# <fs>      <mountpoint>  <type>  <opts>    <dump> <pass>"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .then(1, () -> ctx.type("UUID=3a492b72  /             ext4    noatime   0 1"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .then(1, () -> ctx.type("this line goes"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_K, GLFW.GLFW_MOD_CONTROL))
                .thenScreenshot(2, "nano-modified")
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_O, GLFW.GLFW_MOD_CONTROL))
                .thenScreenshot(2, "nano-file-name-to-write")
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntilServer(level -> written(ctx, level).contains("UUID=3a492b72")
                                && !written(ctx, level).contains("this line goes"), SCREEN_WAIT,
                        "the file to be in the session without the cut line", level -> written(ctx, level))
                .thenScreenshot(2, "nano-wrote")
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_G, GLFW.GLFW_MOD_CONTROL))
                .thenScreenshot(2, "nano-help")
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_X, GLFW.GLFW_MOD_CONTROL))
                .thenAssert(1, () -> prompt(ctx).editing(), "leaving the help text does not leave the editor")
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_X, GLFW.GLFW_MOD_CONTROL))
                .thenWaitUntil(() -> !prompt(ctx).editing(), SCREEN_WAIT, "nano to give the terminal back")
                .then(SETTLE, () -> ctx.type("cat /root/notes.txt"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> prompt(ctx).scrollbackText().stream().anyMatch(l -> l.contains("UUID=3a492b72")),
                        SCREEN_WAIT, "the shell to read back what the editor wrote")
                .thenScreenshot(2, "nano-read-back");
    }

    /**
     * Escape in nano is the player looking away from the monitor, not the editor closing: the screen goes,
     * and the next look at the same machine finds the editor open on the same file with what was typed and
     * never written still in it, ready to be written.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void nano_lookedAwayFromWithEscape_isStillOpenOnTheNextLook(final ClientTestContext ctx) {
        atTheLivePrompt(ctx, "gentoo", LiveInstallState.Distro.GENTOO)
                .then(SETTLE, () -> ctx.type("nano /root/notes.txt"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> prompt(ctx).editing(), SCREEN_WAIT, "nano to take the terminal")
                .then(SETTLE, () -> ctx.type("typed and never written"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(CommandPromptScreen.class, BOOT_WAIT)
                .thenAssert(SETTLE, () -> prompt(ctx).editing(), "the editor is still open on the machine")
                .thenScreenshot(2, "nano-found-again")
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_O, GLFW.GLFW_MOD_CONTROL))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntilServer(level -> written(ctx, level).contains("typed and never written"), SCREEN_WAIT,
                        "what was typed before looking away to be written", level -> written(ctx, level))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_X, GLFW.GLFW_MOD_CONTROL))
                .thenWaitUntil(() -> !prompt(ctx).editing(), SCREEN_WAIT, "nano to give the terminal back");
    }

    /**
     * The partition editor's longest question is wider than the glass. It carries on at the start of the next
     * row, in the same cells as everything above it, and the answer is typed after it.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void fdisk_aQuestionWiderThanTheGlassTakesTwoRows(final ClientTestContext ctx) {
        atTheLivePrompt(ctx, "gentoo", LiveInstallState.Distro.GENTOO)
                .then(SETTLE, () -> ctx.type("fdisk /dev/sda"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> said(ctx, "Welcome to fdisk"), SCREEN_WAIT, "the partition editor to open")
                .then(SETTLE, () -> ctx.type("g"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .then(SETTLE, () -> ctx.type("n"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .then(SETTLE, () -> ctx.type("+512M"))
                .thenScreenshot(2, "fdisk-long-question")
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> said(ctx, "of size 512 MiB"), SCREEN_WAIT, "the partition to be made")
                .thenScreenshot(2, "fdisk-partition-made");
    }

    /**
     * Tab goes round the commands that start with what was typed: a second press moves on to the next one
     * rather than finishing the one the first press put on the line, and typing ends the round.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void tab_goesRoundTheCommandsThatStartWithWhatWasTyped(final ClientTestContext ctx) {
        final String[] first = new String[1];
        atTheLivePrompt(ctx, "gentoo", LiveInstallState.Distro.GENTOO)
                .then(SETTLE * 3, () -> ctx.type("mkfs."))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_TAB))
                .thenAssert(1, () -> prompt(ctx).typed().startsWith("mkfs.") && prompt(ctx).typed().length() > 5,
                        "the first press finishes a command that starts that way")
                .then(1, () -> first[0] = prompt(ctx).typed())
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_TAB))
                .thenAssert(1, () -> prompt(ctx).typed().startsWith("mkfs.") && !prompt(ctx).typed().equals(first[0]),
                        "the second press moves on to another command that starts that way")
                .then(1, () -> ctx.type(" "))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_TAB))
                .thenAssert(1, () -> prompt(ctx).typed().contains(" /dev/sd"),
                        "after the command, the press offers the machine's drives");
    }

    private static boolean said(final ClientTestContext ctx, final String what) {
        final CommandPromptScreen<?> screen = ctx.screen(CommandPromptScreen.class);
        return screen != null && screen.scrollbackText().stream().anyMatch(line -> line.contains(what));
    }

    private static CommandPromptScreen<?> prompt(final ClientTestContext ctx) {
        return ctx.screen(CommandPromptScreen.class);
    }

    private static String written(final ClientTestContext ctx, final ServerLevel level) {
        final LiveInstallState live = TestWorldBuilder.at(level, ctx.origin())
                .blockEntity(MACHINE, MainframeBlockEntity.class).console().liveInstall();
        final String text = live == null ? null : live.fileAt("/root/notes.txt");
        return text == null ? "" : text;
    }

    /**
     * A machine started from that distribution's live medium, with the player at its monitor.
     *
     * <p>The medium is in a drive beside the machine, as it has to be: a session whose medium is in no drive
     * is one the machine ends by itself on its next tick.
     */
    private static ClientTestContext atTheLivePrompt(final ClientTestContext ctx, final String medium,
                                                     final LiveInstallState.Distro distro) {
        return ctx.thenBuild(0, world -> {
                    final MainframeBlockEntity machine = world.placeRunningMainframe(MACHINE);
                    // A graphics card gives the machine peripheral ports, which is what the drive links to.
                    machine.getInventory().setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                            new ItemStack(ComputingModule.GPU_HD_7970.get()));
                    world.setBlock(DRIVE, ComputingModule.CD_DRIVE.get());
                    final ItemStack disc = new ItemStack(ComputingModule.CD_ROM.get());
                    MediaItem.setKind(disc, MediaKind.OS_INSTALL);
                    MediaItem.setPayload(disc, ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, medium));
                    world.blockEntity(DRIVE, MediaReaderBlockEntity.class).mediaSlot().setStackInSlot(0, disc);
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenServer(SETTLE * 3, level -> TestWorldBuilder.at(level, ctx.origin())
                        .blockEntity(MACHINE, MainframeBlockEntity.class).console().startLiveInstall(distro))
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(CommandPromptScreen.class, BOOT_WAIT);
    }
}
