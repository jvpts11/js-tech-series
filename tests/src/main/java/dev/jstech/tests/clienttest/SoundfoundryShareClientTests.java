/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.audio.MusicImports;
import dev.jstech.computers.audio.SoundfoundryShare;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.audio.SoundfoundryShares;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.SoundfoundryApp;
import dev.jstech.computers.client.os.SoundfoundryShareApp;
import dev.jstech.computers.gui.layout.SoundfoundryLayout;
import dev.jstech.computers.gui.layout.SoundfoundryShareLayout;
import dev.jstech.computers.operation.payload.SoundfoundryShareStatePayload;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.SongDownload;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.tests.testkit.TestMedia;
import java.io.IOException;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Soundfoundry's sharing window at the desktop, the whole way: NET opens it in the player's skin; a search finds the
 * catalogue's songs and those another computer of the network shares, each with the slowest cable on its way; a song
 * picked and downloaded comes in and goes on the playlist; and the downloads and the songs this computer shares each
 * have a tab of their own.
 */
public final class SoundfoundryShareClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 400;
    private static final int WAIT = 300;
    private static final String LABEL = "Soundfoundry";
    private static final String SHARER_NAME = "studio-pc";

    private static final BlockPos ASKER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final BlockPos SHARER = new BlockPos(5, 2, 10);

    private SoundfoundryShareClientTests() {
    }

    @ClientTest(timeoutTicks = 3000)
    public static void share_searchesDownloadsAndListsWhatIsShared(final ClientTestContext ctx) {
        final String[] downloaded = {""};
        atTheDesktop(ctx)
                .then(SETTLE, () -> DesktopScreen.requestOpen(LABEL))
                .thenWaitUntil(() -> player(ctx) != null, SCREEN_WAIT, "Soundfoundry to open")
                .then(SETTLE, () -> ctx.clickDesktop(player(ctx).playerPoint(SoundfoundryLayout.NET)))
                .thenWaitUntil(() -> share(ctx) != null, SCREEN_WAIT, "NET to open the sharing window")
                .thenWaitUntil(() -> known(ctx) != null && known(ctx).state().computers() == 1, WAIT,
                        "the window to reach the other computer running Soundfoundry")
                .then(SETTLE, () -> ctx.clickDesktop(share(ctx).point(SoundfoundryShareLayout.QUERY)))
                .thenAssert(2, () -> share(ctx).typing(), "a click on the search takes what is typed")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> known(ctx) != null && known(ctx).found().size() >= 3, WAIT,
                        "searching for nothing to find every song it reaches")
                .thenAssert(0, () -> known(ctx).found().stream().anyMatch(found -> found.source()
                                == SongDownload.FROM_CATALOG) && known(ctx).found().stream().anyMatch(found ->
                                found.from().equals(SHARER_NAME) && !found.link().isEmpty()),
                        "the catalogue's songs and the other computer's, over a cable")
                .then(SETTLE, () -> {
                    final int row = firstShared(ctx);
                    downloaded[0] = known(ctx).found().get(row).title();
                    ctx.clickDesktop(share(ctx).foundPoint(row));
                })
                .thenAssert(2, () -> share(ctx).pickedFound() == firstShared(ctx), "a click picks a song")
                .then(SETTLE, () -> ctx.clickDesktop(share(ctx).point(SoundfoundryShareLayout.DOWNLOAD)))
                .thenWaitUntil(() -> known(ctx).state().downloads().size() == 1, WAIT, "the download to start")
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(6, "soundfoundry_share_search")
                .thenWaitUntilServer(level -> kept(level, ctx, downloaded[0]), WAIT,
                        "the song to come in and go on the playlist", level -> "not on the playlist")
                .then(SETTLE, () -> ctx.clickDesktop(share(ctx).tabPoint(SoundfoundryShareApp.TAB_DOWNLOADS)))
                .thenAssert(2, () -> share(ctx).tab() == SoundfoundryShareApp.TAB_DOWNLOADS, "the downloads tab")
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(4, "soundfoundry_share_downloads")
                .then(SETTLE, () -> ctx.clickDesktop(share(ctx).tabPoint(SoundfoundryShareApp.TAB_SHARED)))
                .thenWaitUntil(() -> known(ctx).shared().size() == 1, WAIT, "the songs this computer shares")
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(4, "soundfoundry_share_shared")
                .then(SETTLE, () -> ctx.clickDesktop(window(ctx).buttonCentre(DesktopWindow.BUTTON_CLOSE)))
                .thenWaitUntil(() -> share(ctx) == null, SCREEN_WAIT, "the close button to close it");
    }

    /*
     * A personal computer with Soundfoundry and a monitor, joined through a Mainframe to another personal computer
     * that shares two songs; a third shared by the first; and the player at its desktop.
     */
    private static ClientTestContext atTheDesktop(final ClientTestContext ctx) {
        return ctx.thenBuild(0, builder -> {
                    builder.setBlock(new BlockPos(5, 2, 3), ComputingModule.ETHERNET_CABLE);
                    builder.setBlock(new BlockPos(5, 2, 4), ComputingModule.PERSONAL_ROUTER.get());
                    builder.setBlock(new BlockPos(5, 2, 5), ComputingModule.HBW_CABLE);
                    builder.placeRunningMainframe(new BlockPos(5, 2, 6));
                    builder.setBlock(new BlockPos(5, 2, 7), ComputingModule.HBW_CABLE);
                    builder.setBlock(new BlockPos(5, 2, 8), ComputingModule.PERSONAL_ROUTER.get());
                    builder.setBlock(new BlockPos(5, 2, 9), ComputingModule.ETHERNET_CABLE);
                    final PersonalComputerBlockEntity sharer = builder.placeRunningPersonalComputer(SHARER);
                    sharer.console().install(Programs.SOUNDFOUNDRY.toString());
                    sharer.console().setComputerName(SHARER_NAME);
                    // A Legacy desktop, whose Soundfoundry is the player with the NET button.
                    final PersonalComputerBlockEntity asker = builder.placeRunningPersonalComputer(ASKER,
                            ResourceLocation.fromNamespaceAndPath("jsc", "frames_xp"));
                    asker.console().install(Programs.SOUNDFOUNDRY.toString());
                    builder.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenServer(0, level -> {
                    final PersonalComputerBlockEntity sharer = computer(level, ctx.abs(SHARER));
                    keep(level, sharer, "Harbour Lights - Low Tide.wav", 60_000);
                    keep(level, sharer, "Harbour Lights - Signal Fires.wav", 20_000);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenServer(SETTLE, level -> keep(level, computer(level, ctx.abs(ASKER)), "Copper Rain.wav", 5_000));
    }

    /* A song of that length put in the computer's shared folder. */
    private static void keep(final ServerLevel level, final PersonalComputerBlockEntity computer, final String name,
                             final int millis) {
        try {
            final MediaStore store = MediaStore.current().orElseThrow();
            final MediaId song = store.put(TestMedia.tone(millis, name + " " + System.nanoTime()), "wav");
            MusicImports.keep(level, computer.getBlockPos(), SoundfoundryShare.sharedFolderOf(computer), name, song,
                    store.info(song));
        } catch (final IOException unexpected) {
            throw new IllegalStateException(unexpected);
        }
    }

    /* Whether the song downloaded is on the asking computer's playlist. */
    private static boolean kept(final ServerLevel level, final ClientTestContext ctx, final String name) {
        final PersonalComputerBlockEntity asker = computer(level, ctx.abs(ASKER));
        return !name.isEmpty() && asker.console().soundfoundry().songs().stream().anyMatch(path -> path.endsWith(name))
                && asker.console().soundfoundry().downloads().stream()
                .allMatch(download -> download.status() == SongDownload.Status.DONE);
    }

    /* The row of the first song the other computer shares, among what the search found. */
    private static int firstShared(final ClientTestContext ctx) {
        final List<SoundfoundryShareStatePayload.Found> found = known(ctx).found();
        for (int i = 0; i < found.size(); i++) {
            if (found.get(i).from().equals(SHARER_NAME)) {
                return i;
            }
        }
        return -1;
    }

    private static PersonalComputerBlockEntity computer(final ServerLevel level, final BlockPos at) {
        return (PersonalComputerBlockEntity) level.getBlockEntity(at);
    }

    @Nullable
    private static SoundfoundryApp player(final ClientTestContext ctx) {
        if (!(ctx.mc().screen instanceof DesktopScreen desktop)) {
            return null;
        }
        final DesktopWindow window = desktop.windowFor(LABEL);
        return window != null && window.app() instanceof SoundfoundryApp app ? app : null;
    }

    @Nullable
    private static DesktopWindow window(final ClientTestContext ctx) {
        return ctx.mc().screen instanceof DesktopScreen desktop ? desktop.windowFor(SoundfoundryShareApp.KEY) : null;
    }

    @Nullable
    private static SoundfoundryShareApp share(final ClientTestContext ctx) {
        final DesktopWindow window = window(ctx);
        return window != null && window.app() instanceof SoundfoundryShareApp app ? app : null;
    }

    @Nullable
    private static SoundfoundryShares.Known known(final ClientTestContext ctx) {
        return SoundfoundryShares.of(ctx.abs(ASKER));
    }

}
