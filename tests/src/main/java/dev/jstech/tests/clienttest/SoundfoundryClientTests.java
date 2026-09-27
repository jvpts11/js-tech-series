/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.audio.MusicImports;
import dev.jstech.computers.audio.MusicPlayer;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.client.audio.SoundfoundryStates;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.SoundfoundryApp;
import dev.jstech.computers.gui.layout.SoundfoundryLayout;
import dev.jstech.computers.operation.payload.SoundfoundryStatePayload;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaSessions;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.core.client.audio.media.MediaPlayer;
import dev.jstech.tests.testkit.TestMedia;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.io.IOException;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Soundfoundry at the desktop, the whole way: its window opens in its own skin, a song on its playlist plays out of
 * the machine and is heard, it pauses and stops, the eject button offers to bring songs from the player's own
 * computer, the playlist is put away and brought back, and the music plays on after the window is closed.
 */
public final class SoundfoundryClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 400;
    private static final int WAIT = 200;
    private static final String LABEL = "Soundfoundry";

    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    private SoundfoundryClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void player_playsPausesStopsAndPlaysOnWithTheWindowClosed(final ClientTestContext ctx) {
        final CraftingComputerBlockEntity[] machine = new CraftingComputerBlockEntity[1];
        final byte[] wav = TestMedia.tone(60_000, "Copper Rain " + System.nanoTime());
        atTheDesktop(ctx, machine, wav)
                .then(SETTLE, () -> DesktopScreen.requestOpen(LABEL))
                .thenWaitUntil(() -> app(ctx) != null, SCREEN_WAIT, "Soundfoundry to open")
                .thenWaitUntil(() -> known(ctx) != null && known(ctx).songs().size() == 1, SCREEN_WAIT,
                        "the window to know the playlist")
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(4, "soundfoundry_player")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).playerPoint(SoundfoundryLayout.PLAY)))
                .thenWaitUntil(() -> MediaPlayer.heard(MusicPlayer.keyOf(ctx.abs(COMPUTER))), WAIT,
                        "the song to be heard out of the machine")
                .thenWaitUntil(() -> status(ctx) == SoundfoundryStatePayload.PLAYING, WAIT,
                        "and the window to say it plays")
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(20, "soundfoundry_playing")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).playerPoint(SoundfoundryLayout.PAUSE)))
                .thenWaitUntil(() -> status(ctx) == SoundfoundryStatePayload.PAUSED, WAIT, "a pause to hold it")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).playerPoint(SoundfoundryLayout.STOP)))
                .thenWaitUntil(() -> status(ctx) == SoundfoundryStatePayload.STOPPED
                                && !MediaPlayer.heard(MusicPlayer.keyOf(ctx.abs(COMPUTER))), WAIT,
                        "stop to end it")
                .then(SETTLE, () -> {
                    ctx.clickDesktop(app(ctx).rowPoint(0));
                    ctx.clickDesktop(app(ctx).rowPoint(0));
                })
                .thenWaitUntil(() -> status(ctx) == SoundfoundryStatePayload.PLAYING, WAIT,
                        "a double click on a song plays it")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).playerPoint(SoundfoundryLayout.EJECT)))
                .thenAssert(2, () -> app(ctx).menuOpen()
                        && app(ctx).menuPoint("Import from Your Computer...") != null,
                        "the eject button offers to bring songs from the player's own computer")
                .thenScreenshot(2, "soundfoundry_eject_menu")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).playerPoint(SoundfoundryLayout.EJECT)))
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).playerPoint(SoundfoundryLayout.PL)))
                .thenAssert(2, () -> !app(ctx).playlistShown() && window(ctx).height() == SoundfoundryLayout.PLAYER_H,
                        "PL puts the playlist away and the window shrinks to the player")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).playerPoint(SoundfoundryLayout.PL)))
                .thenAssert(2, () -> app(ctx).playlistShown(), "and brings it back")
                .then(SETTLE, () -> ctx.clickDesktop(window(ctx).buttonCentre(DesktopWindow.BUTTON_CLOSE)))
                .thenWaitUntil(() -> app(ctx) == null, SCREEN_WAIT, "the close button to close it")
                .thenWaitUntilServer(level -> MediaSessions.has(level, MusicPlayer.keyOf(ctx.abs(COMPUTER)))
                                && !MediaSessions.paused(level, MusicPlayer.keyOf(ctx.abs(COMPUTER))), SETTLE * 4,
                        "the music plays on with the window closed", level -> "no music under the key");
    }

    /* A Frames XP machine with Soundfoundry and a monitor, a song on its playlist, and the player at its desktop. */
    private static ClientTestContext atTheDesktop(final ClientTestContext ctx,
                                                  final CraftingComputerBlockEntity[] machine, final byte[] wav) {
        return ctx.thenBuild(0, builder -> {
                    final CraftingComputerBlockEntity computer = builder.placeRunningCraftingComputer(COMPUTER);
                    computer.togglePower();
                    computer.formatDisk(0);
                    TestWorldBuilder.installDesktop(computer, jsc("frames_xp"), jsc("soundfoundry"));
                    computer.togglePower();
                    builder.placeMonitor(MONITOR, Direction.EAST);
                    machine[0] = computer;
                })
                .thenServer(0, level -> {
                    try {
                        final MediaStore store = MediaStore.current().orElseThrow();
                        final MediaId song = store.put(wav, "wav");
                        MusicImports.keep(level, ctx.abs(COMPUTER), "", true, "Copper Rain.wav", song,
                                store.info(song));
                    } catch (final IOException unexpected) {
                        throw new IllegalStateException(unexpected);
                    }
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT);
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
    private static SoundfoundryApp app(final ClientTestContext ctx) {
        final DesktopWindow window = window(ctx);
        return window != null && window.app() instanceof SoundfoundryApp app ? app : null;
    }

    @Nullable
    private static SoundfoundryStates.Known known(final ClientTestContext ctx) {
        return SoundfoundryStates.of(ctx.abs(COMPUTER));
    }

    private static int status(final ClientTestContext ctx) {
        final SoundfoundryStates.Known known = known(ctx);
        return known == null ? -1 : known.state().status();
    }

    private static ResourceLocation jsc(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }
}
