/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.MediaDevices;
import dev.jstech.computers.audio.MusicImports;
import dev.jstech.computers.audio.VinylRecordings;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.VinylDriveBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.SoundfoundryApp;
import dev.jstech.computers.client.os.SoundfoundryStandardApp;
import dev.jstech.computers.client.os.SoundfoundryVinyl;
import dev.jstech.computers.gui.layout.SoundfoundryLayout;
import dev.jstech.computers.gui.layout.SoundfoundryLayout.Rect;
import dev.jstech.computers.gui.layout.SoundfoundryStandardLayout;
import dev.jstech.computers.operation.payload.VinylStatePayload;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.TestMedia;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.io.IOException;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * The Vinyl RW Drive through Soundfoundry (computers/docs/design/programs.md): the classic Soundfoundry on Frames XP
 * records a music disc on the drive in the song's own time, its VNL plate showing the recording under way; and the
 * Standard Soundfoundry on Frames 11 cuts a recording that came from a disc onto a blank vinyl from its Vinyl page.
 */
public final class SoundfoundryVinylClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 400;
    private static final int WAIT = 200;
    /* A few seconds into the song, so the bar and the time have moved. */
    private static final int UNDER_WAY = 80;
    private static final String LABEL = "Soundfoundry";
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final BlockPos DRIVE = new BlockPos(4, 2, 2);
    /* The rows of the Vinyl page and the key under them, measured from the page's corner. */
    private static final int ROWS_Y = 88;
    private static final int ROW = 11;
    private static final int KEYS_DY = 8;
    private static final int KEY_X = 12;

    private SoundfoundryVinylClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void classic_recordsAMusicDiscOnTheVinylPlate(final ClientTestContext ctx) {
        ctx.thenBuild(0, builder -> {
                    final CraftingComputerBlockEntity computer = builder.placeRunningCraftingComputer(COMPUTER);
                    computer.togglePower();
                    computer.formatDisk(0);
                    TestWorldBuilder.installDesktop(computer, jsc("frames_xp"), jsc("soundfoundry"));
                    computer.togglePower();
                    builder.placeMonitor(MONITOR, Direction.EAST);
                    builder.setBlock(DRIVE, MediaDevices.VINYL_DRIVE.get());
                    builder.blockEntity(DRIVE, VinylDriveBlockEntity.class).platter()
                            .setStackInSlot(0, new ItemStack(Items.MUSIC_DISC_CAT));
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> DesktopScreen.requestOpen(LABEL))
                .thenWaitUntil(() -> classic(ctx) != null, SCREEN_WAIT, "Soundfoundry to open")
                .thenWaitUntil(() -> vinyl(ctx) != null && vinyl(ctx).drive() != 0L, WAIT,
                        "the window to know the computer's Vinyl RW Drive")
                .then(SETTLE, () -> ctx.clickDesktop(classic(ctx).playerPoint(SoundfoundryLayout.VNL)))
                .then(SETTLE, () -> ctx.clickDesktop(classic(ctx).playlistPoint(SoundfoundryLayout.REC)))
                .thenWaitUntilServer(level -> drive(ctx, level).work() == VinylDriveBlockEntity.Work.RECORDING,
                        WAIT, "REC to record the music disc", level -> "problem=" + drive(ctx, level).problem())
                .then(UNDER_WAY, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(1, "soundfoundry_vinyl_record")
                .then(SETTLE, () -> ctx.clickDesktop(classic(ctx).playlistPoint(SoundfoundryLayout.VINYL_STOP)))
                .thenWaitUntilServer(level -> drive(ctx, level).work() == VinylDriveBlockEntity.Work.NONE, WAIT,
                        "STOP to let the recording go", level -> "work=" + drive(ctx, level).work());
    }

    @ClientTest(timeoutTicks = 2400)
    public static void standard_cutsARecordingFromADiscOntoABlankVinyl(final ClientTestContext ctx) {
        ctx.thenBuild(0, builder -> {
                    final PersonalComputerBlockEntity pc = builder.placeRunningPersonalComputer(COMPUTER);
                    pc.console().install(Programs.SOUNDFOUNDRY.toString());
                    builder.placeMonitor(MONITOR, Direction.EAST);
                    builder.setBlock(DRIVE, MediaDevices.VINYL_DRIVE.get());
                    builder.blockEntity(DRIVE, VinylDriveBlockEntity.class).platter()
                            .setStackInSlot(0, new ItemStack(MediaDevices.BLANK_VINYL_RECORD.get()));
                })
                .thenServer(0, level -> keepRecording(level, (IOsHost) level.getBlockEntity(ctx.abs(COMPUTER))))
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> DesktopScreen.requestOpen(LABEL))
                .thenWaitUntil(() -> standard(ctx) != null, SCREEN_WAIT, "the Standard Soundfoundry to open")
                .then(SETTLE * 2, () -> ctx.clickDesktop(standard(ctx).point(SoundfoundryStandardLayout.playlist(0))))
                .thenWaitUntil(() -> vinyl(ctx) != null && !vinyl(ctx).recordings().isEmpty(), WAIT,
                        "the Vinyl page to list the recording from the disc")
                .then(SETTLE, () -> ctx.clickDesktop(inPage(ctx, KEY_X + 30, ROWS_Y + 4)))
                .then(SETTLE, () -> ctx.clickDesktop(inPage(ctx, KEY_X + 20,
                        ROWS_Y + vinyl(ctx).recordings().size() * ROW + KEYS_DY + 7)))
                .thenWaitUntilServer(level -> drive(ctx, level).work() == VinylDriveBlockEntity.Work.CUTTING, WAIT,
                        "the key to cut the recording onto the blank", level -> "problem=" + drive(ctx, level)
                                .problem())
                .then(UNDER_WAY, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(1, "soundfoundry_vinyl_cut");
    }

    /* A recording of the music disc cat kept in the computer's Music/Vinyl, noted as recorded from the disc. */
    private static void keepRecording(final ServerLevel level, final IOsHost computer) {
        try {
            final MediaStore store = MediaStore.current().orElseThrow();
            final MediaId media = store.put(TestMedia.tone(60_000, "cat " + System.nanoTime()), "wav");
            MusicImports.keep(level, computer, MusicImports.musicFolderOf(computer) + "/"
                    + VinylDriveBlockEntity.FOLDER, false, "C418 - cat.wav", media, store.info(media));
            VinylRecordings.of(level).recorded(media, ResourceLocation.withDefaultNamespace("cat"), 4L);
        } catch (final IOException unexpected) {
            throw new IllegalStateException(unexpected);
        }
    }

    private static VinylDriveBlockEntity drive(final ClientTestContext ctx, final ServerLevel level) {
        return (VinylDriveBlockEntity) level.getBlockEntity(ctx.abs(DRIVE));
    }

    /* A point of the Standard window's page, measured from the page's corner. */
    private static int[] inPage(final ClientTestContext ctx, final int x, final int y) {
        final SoundfoundryStandardApp app = standard(ctx);
        final Rect page = SoundfoundryStandardLayout.page(app.size()[0], app.size()[1]);
        return app.point(page.x() + x, page.y() + y);
    }

    @Nullable
    private static VinylStatePayload vinyl(final ClientTestContext ctx) {
        return SoundfoundryVinyl.state(ctx.abs(COMPUTER));
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
    private static SoundfoundryApp classic(final ClientTestContext ctx) {
        final DesktopWindow window = window(ctx);
        return window != null && window.app() instanceof SoundfoundryApp app ? app : null;
    }

    @Nullable
    private static SoundfoundryStandardApp standard(final ClientTestContext ctx) {
        final DesktopWindow window = window(ctx);
        return window != null && window.app() instanceof SoundfoundryStandardApp app ? app : null;
    }

    private static ResourceLocation jsc(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }
}
