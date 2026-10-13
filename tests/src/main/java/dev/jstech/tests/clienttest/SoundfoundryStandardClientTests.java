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
import dev.jstech.computers.audio.MusicPlayer;
import dev.jstech.computers.audio.catalog.SoundfoundryCatalog;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.client.audio.SoundfoundryPages;
import dev.jstech.computers.client.audio.SoundfoundryStates;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.SoundfoundryStandardApp;
import dev.jstech.computers.gui.layout.SoundfoundryLayout.Rect;
import dev.jstech.computers.gui.layout.SoundfoundryStandardLayout;
import dev.jstech.computers.operation.payload.SoundfoundryPagePayload;
import dev.jstech.computers.operation.payload.SoundfoundryStatePayload;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.core.client.audio.media.MediaPlayer;
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
 * The Standard Soundfoundry on a Frames 11 desktop, drawn as the approved mock draws it: the home page with the
 * catalogue, the network's songs and those downloaded; an album's page, played through the Soundfoundry Server; the
 * machine's own files; the window filling the desktop; and, on a network with no server, only the files.
 */
public final class SoundfoundryStandardClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 400;
    private static final int WAIT = 200;
    private static final String LABEL = "Soundfoundry";

    private static final BlockPos RACK = new BlockPos(2, 2, 1);
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    private SoundfoundryStandardClientTests() {
    }

    @ClientTest(timeoutTicks = 3000)
    public static void standard_showsTheCatalogueAndStreamsAnAlbumThroughTheServer(final ClientTestContext ctx) {
        atTheDesktop(ctx, true)
                .thenWaitUntilServer(level -> SoundfoundryCatalog.current().songs() > 0, WAIT,
                        "the test catalogue to be read", level -> "no catalogue")
                .then(SETTLE, () -> DesktopScreen.requestOpen(LABEL))
                .thenWaitUntil(() -> app(ctx) != null, SCREEN_WAIT, "the Standard Soundfoundry to open")
                .thenWaitUntil(() -> page(ctx) != null && !page(ctx).albums().isEmpty()
                                && !page(ctx).section(SoundfoundryPagePayload.NETWORK).isEmpty()
                                && !page(ctx).section(SoundfoundryPagePayload.DOWNLOADED).isEmpty(), WAIT,
                        "the home page to list the catalogue, the network's songs and the downloaded one")
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(6, "soundfoundry_standard_home")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).point(SoundfoundryStandardLayout.SIDE_W
                        + SoundfoundryStandardLayout.PAD + 30, SoundfoundryStandardLayout.TITLE_H
                        + SoundfoundryStandardLayout.CARD_Y + 30)))
                .thenWaitUntil(() -> app(ctx).page() == SoundfoundryPagePayload.ALBUM && page(ctx) != null
                        && !page(ctx).rows().isEmpty(), WAIT, "a click on an album to open its page")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).point(inPage(SoundfoundryStandardLayout.PLAY))))
                .thenWaitUntil(() -> status(ctx) == SoundfoundryStatePayload.PLAYING && streaming(ctx), WAIT,
                        "the album to play, streamed through the server")
                .thenWaitUntil(() -> MediaPlayer.heard(MusicPlayer.keyOf(ctx.abs(COMPUTER))), WAIT,
                        "and to be heard out of the machine")
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(20, "soundfoundry_standard_album")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).point(SoundfoundryStandardLayout.nav(1))))
                .then(SETTLE, () -> {
                    for (final char c : "tone".toCharArray()) {
                        app(ctx).charTyped(c);
                    }
                    app(ctx).keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
                })
                .thenWaitUntil(() -> app(ctx).page() == SoundfoundryPagePayload.SEARCH && page(ctx) != null
                        && !page(ctx).rows().isEmpty(), WAIT, "a search to find the catalogue's tones")
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(6, "soundfoundry_standard_search")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).point(SoundfoundryStandardLayout.nav(2))))
                .thenWaitUntil(() -> app(ctx).page() == SoundfoundryPagePayload.LIBRARY && page(ctx) != null
                        && !page(ctx).rows().isEmpty(), WAIT, "Your Library to list the machine's own files")
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(6, "soundfoundry_standard_library")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).point(SoundfoundryStandardLayout.NEW_PLAYLIST)))
                .thenWaitUntil(() -> page(ctx) != null && page(ctx).sidebar().playlists().size() == 2, WAIT,
                        "the plus to make a playlist, listed under the liked songs")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).point(SoundfoundryStandardLayout.playlist(1))))
                .thenWaitUntil(() -> app(ctx).page() == SoundfoundryPagePayload.PLAYLIST && page(ctx) != null,
                        WAIT, "a click on it to open its page")
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(6, "soundfoundry_standard_playlist")
                .then(SETTLE, () -> ctx.clickDesktop(window(ctx).buttonCentre(DesktopWindow.BUTTON_MAXIMIZE)))
                .thenWaitUntil(() -> window(ctx).maximized() && window(ctx).y() == 0
                                && app(ctx).size()[0] == window(ctx).width()
                                && app(ctx).size()[1] == window(ctx).height(), SCREEN_WAIT,
                        "the maximize button to fill the desktop, the window drawn at the size it fills")
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(6, "soundfoundry_standard_maximized")
                .then(SETTLE, () -> ctx.clickDesktop(window(ctx).buttonCentre(DesktopWindow.BUTTON_CLOSE)))
                .thenWaitUntil(() -> app(ctx) == null, SCREEN_WAIT, "the close button to close it");
    }

    @ClientTest(timeoutTicks = 2000)
    public static void standard_withNoServerHasOnlyTheFilesOfTheComputer(final ClientTestContext ctx) {
        atTheDesktop(ctx, false)
                .then(SETTLE, () -> DesktopScreen.requestOpen(LABEL))
                .thenWaitUntil(() -> app(ctx) != null, SCREEN_WAIT, "the Standard Soundfoundry to open")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).point(SoundfoundryStandardLayout.nav(2))))
                .thenWaitUntil(() -> app(ctx).page() == SoundfoundryPagePayload.LIBRARY && page(ctx) != null
                                && page(ctx).sidebar().servers().isEmpty() && !page(ctx).rows().isEmpty(), WAIT,
                        "Your Library to list the files, with no server to reach")
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(6, "soundfoundry_standard_no_server")
                .then(SETTLE, () -> ctx.clickDesktop(window(ctx).buttonCentre(DesktopWindow.BUTTON_CLOSE)))
                .thenWaitUntil(() -> app(ctx) == null, SCREEN_WAIT, "the close button to close it");
    }

    /*
     * A Frames 11 personal computer with Soundfoundry, its monitor and a song of its own, on a Mainframe's network with
     * a server rack whose server runs a Soundfoundry Server keeping two songs another computer sent, when asked; and
     * the player at the desktop.
     */
    private static ClientTestContext atTheDesktop(final ClientTestContext ctx, final boolean serving) {
        return ctx.thenBuild(0, builder -> {
                    builder.placeRunningMainframe(new BlockPos(1, 2, 2));
                    builder.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
                    final ServerRackBlockEntity rack = builder.placeSeededRack(RACK);
                    builder.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
                    builder.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE);
                    final PersonalComputerBlockEntity pc = builder.placeRunningPersonalComputer(COMPUTER);
                    pc.console().install(Programs.SOUNDFOUNDRY.toString());
                    builder.placeMonitor(MONITOR, Direction.EAST);
                    rack.unitHost(0).installOs(ResourceLocation.fromNamespaceAndPath("jsc", "debian"));
                    rack.consoleOf(0).setComputerName("sf-server-01");
                    if (serving) {
                        rack.consoleOf(0).install(Programs.SOUNDFOUNDRY_SERVER.toString());
                    }
                })
                .thenServer(0, level -> {
                    final ServerRackBlockEntity rack = (ServerRackBlockEntity) level.getBlockEntity(ctx.abs(RACK));
                    final IOsHost server = rack.unitHost(0);
                    final String sent = FsPaths.join(MusicImports.musicFolderOf(server), "attic-pc");
                    keep(level, server, sent, "3 AM Backup.wav", 60_000);
                    keep(level, server, sent, "Tin Roof.wav", 60_000);
                    keep(level, (IOsHost) level.getBlockEntity(ctx.abs(COMPUTER)), "", "Copper Rain.wav", 60_000);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT);
    }

    private static void keep(final ServerLevel level, final IOsHost machine, final String folder, final String name,
                             final int millis) {
        try {
            final MediaStore store = MediaStore.current().orElseThrow();
            final MediaId song = store.put(TestMedia.tone(millis, name + " " + System.nanoTime()), "wav");
            MusicImports.keep(level, machine, folder, false, name, song, store.info(song));
        } catch (final IOException unexpected) {
            throw new IllegalStateException(unexpected);
        }
    }

    /* A part of a page, measured from the page's corner, as a part of the window. */
    private static Rect inPage(final Rect part) {
        return new Rect(SoundfoundryStandardLayout.SIDE_W + part.x(), SoundfoundryStandardLayout.TITLE_H + part.y(),
                part.w(), part.h());
    }

    @Nullable
    private static DesktopWindow window(final ClientTestContext ctx) {
        if (!(ctx.mc().screen instanceof DesktopScreen desktop)) {
            return null;
        }
        final List<DesktopWindow> windows = desktop.windowsFor(LABEL);
        return windows.isEmpty() ? null : windows.getFirst();
    }

    @Nullable
    private static SoundfoundryStandardApp app(final ClientTestContext ctx) {
        final DesktopWindow window = window(ctx);
        return window != null && window.app() instanceof SoundfoundryStandardApp app ? app : null;
    }

    /* The page the window shows, once the machine has answered it. */
    @Nullable
    private static SoundfoundryPagePayload page(final ClientTestContext ctx) {
        final SoundfoundryStandardApp app = app(ctx);
        final SoundfoundryPages.Known known = SoundfoundryPages.of(ctx.abs(COMPUTER));
        return app == null || known == null || !known.is(app.page(), app.pageArg()) ? null : known.page();
    }

    private static int status(final ClientTestContext ctx) {
        final SoundfoundryStates.Known known = SoundfoundryStates.of(ctx.abs(COMPUTER));
        return known == null ? -1 : known.state().status();
    }

    private static boolean streaming(final ClientTestContext ctx) {
        final SoundfoundryStates.Known known = SoundfoundryStates.of(ctx.abs(COMPUTER));
        return known != null && known.state().stream();
    }
}
