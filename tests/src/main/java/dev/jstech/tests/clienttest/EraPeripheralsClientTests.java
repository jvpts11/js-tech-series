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
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.PrinterBlockEntity;
import dev.jstech.computers.blockentity.SpeakerBlockEntity;
import dev.jstech.computers.client.SpeakerScreen;
import dev.jstech.computers.client.audio.SoundfoundryStates;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.EditorApp;
import dev.jstech.computers.client.os.PaintApp;
import dev.jstech.computers.client.os.SoundfoundryApp;
import dev.jstech.computers.gui.layout.SoundfoundryLayout;
import dev.jstech.computers.gui.layout.SpeakerLayout;
import dev.jstech.computers.operation.payload.SoundfoundryStatePayload;
import dev.jstech.computers.os.media.DockStationBlockEntity;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.core.client.audio.media.MediaPlayer;
import dev.jstech.tests.testkit.EraKit;
import dev.jstech.tests.testkit.EraNetwork;
import dev.jstech.tests.testkit.TestMedia;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.io.IOException;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import org.lwjgl.glfw.GLFW;

/**
 * Each era's peripherals in a player's hands, around the era's computer on its network. A page written in the Editor
 * and a picture from Paint are printed through the system's Print window, and come out of the era's printer; on the
 * Vintage a file is printed with print at MC-DOS's prompt. From the Legacy the era's speaker beside the computer is
 * named in its own window, and a song kept on the computer plays out of the machine. From the Transition, whose
 * monitors are flat panels, a second monitor put down from the hand beside the first joins it into one screen, which
 * the player looks at from close by and from across the world. From the Standard a stick goes into a Dock Station
 * beside the computer by hand, and the computer gives it a letter.
 */
public final class EraPeripheralsClientTests {

    private static final int SETTLE = EraJourney.SETTLE;
    private static final int WAIT = 200;
    private static final int PRINT_WAIT = 900;
    private static final int TIMEOUT = 9_000;
    private static final BlockPos PRINTER = new BlockPos(9, 2, 1);
    private static final BlockPos SPEAKER = new BlockPos(9, 2, 3);
    private static final BlockPos BY_THE_SPEAKER = new BlockPos(9, 2, 5);
    private static final BlockPos DOCK = new BlockPos(9, 2, 3);
    private static final BlockPos BY_THE_DOCK = new BlockPos(9, 2, 5);
    /** The second monitor of the big screen, south of the first and facing east as it does. */
    private static final BlockPos SECOND_MONITOR = new BlockPos(10, 2, 3);
    private static final BlockPos BEFORE_THE_SECOND_MONITOR = new BlockPos(12, 2, 3);
    /** Where the big screen is looked at from across the world. */
    private static final BlockPos FAR_FROM_THE_SCREEN = new BlockPos(40, 2, 3);
    private static final String SPEAKER_NAME = "Desk";
    private static final String SOUNDFOUNDRY = "Soundfoundry";

    private EraPeripheralsClientTests() {
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void vintage_aFileIsPrintedAtThePrompt(final ClientTestContext ctx) {
        final EraKit kit = EraKit.VINTAGE;
        bench(ctx, kit)
                .then(SETTLE, () -> {
                    ctx.type("write note.txt A page for the printer");
                    ctx.key(GLFW.GLFW_KEY_ENTER);
                })
                .then(SETTLE, () -> {
                    ctx.type("print note.txt");
                    ctx.key(GLFW.GLFW_KEY_ENTER);
                })
                .thenWaitUntilServer(level -> printed(ctx, level) >= 1, PRINT_WAIT,
                        "the file to come out of the printer", level -> "jobs=" + printer(ctx, level).jobs().size())
                .thenScreenshot(SETTLE, "vintage-printed");
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_peripheralsDoTheirWork(final ClientTestContext ctx) {
        peripherals(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void transition_peripheralsDoTheirWork(final ClientTestContext ctx) {
        peripherals(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void standard_peripheralsDoTheirWork(final ClientTestContext ctx) {
        peripherals(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void advanced_peripheralsDoTheirWork(final ClientTestContext ctx) {
        peripherals(ctx, EraKit.ADVANCED);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void transition_monitorsJoinIntoOneScreen(final ClientTestContext ctx) {
        bigScreen(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void standard_monitorsJoinAndAStickIsDocked(final ClientTestContext ctx) {
        bigScreen(ctx, EraKit.STANDARD);
        docked(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void advanced_monitorsJoinAndAStickIsDocked(final ClientTestContext ctx) {
        bigScreen(ctx, EraKit.ADVANCED);
        docked(ctx, EraKit.ADVANCED);
    }

    /* Printing a page and a picture, then the speaker named and a song heard. */
    private static void peripherals(final ClientTestContext ctx, final EraKit kit) {
        bench(ctx, kit);
        EraJourney.launch(ctx, kit, "Editor")
                .thenWaitUntil(() -> app(ctx, "Editor", EditorApp.class) != null, WAIT, "the Editor to open")
                .then(SETTLE, () -> ctx.type("A page for the printer"))
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_P, GLFW.GLFW_MOD_CONTROL))
                .thenWaitUntil(() -> app(ctx, "Editor", EditorApp.class).printDialog().isOpen()
                                && !app(ctx, "Editor", EditorApp.class).printDialog().printerNames().isEmpty(), WAIT,
                        "Ctrl+P to open the Print window listing the printer")
                .thenScreenshot(SETTLE, shot(kit, "print-window"))
                .then(SETTLE, () -> app(ctx, "Editor", EditorApp.class).printDialog().confirm())
                .thenWaitUntilServer(level -> printed(ctx, level) >= 1, PRINT_WAIT,
                        "the page to come out of the printer", level -> "jobs=" + printer(ctx, level).jobs().size());
        shut(ctx, "Editor");
        EraJourney.launch(ctx, kit, "Paint")
                .thenWaitUntil(() -> app(ctx, "Paint", PaintApp.class) != null, WAIT, "Paint to open")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_P, GLFW.GLFW_MOD_CONTROL))
                .thenWaitUntil(() -> app(ctx, "Paint", PaintApp.class).printDialog().isOpen()
                                && !app(ctx, "Paint", PaintApp.class).printDialog().printerNames().isEmpty(), WAIT,
                        "Paint's Print window to open")
                .then(SETTLE, () -> app(ctx, "Paint", PaintApp.class).printDialog().confirm())
                .thenWaitUntilServer(level -> printed(ctx, level) >= 2, PRINT_WAIT,
                        "the picture to come out of the printer after the page",
                        level -> "jobs=" + printer(ctx, level).jobs().size())
                .thenScreenshot(SETTLE, shot(kit, "printed"));
        shut(ctx, "Paint");
        // The speaker named in its window, and a song kept on the computer played out of the machine.
        ctx.thenWaitUntil(EraJourney.away(ctx), WAIT, "the desktop to be left")
                .thenTeleport(SETTLE, BY_THE_SPEAKER, Direction.NORTH)
                .then(SETTLE, () -> ctx.selectHotbar(8))
                .thenRightClick(SETTLE, SPEAKER)
                .thenAwaitScreen(SpeakerScreen.class, WAIT)
                .then(2, () -> ctx.clickGui(SpeakerLayout.NAME_X + 20, SpeakerLayout.NAME_Y + 6))
                .then(2, () -> ctx.type(SPEAKER_NAME))
                .then(2, () -> ctx.player().closeContainer())
                .thenWaitUntilServer(level -> SPEAKER_NAME.equals(speaker(ctx, level).name())
                                && ctx.abs(EraNetwork.COMPUTER).equals(speaker(ctx, level).ownerPos()), WAIT,
                        "the speaker to take the name typed in its window, linked to the computer",
                        level -> "name=" + speaker(ctx, level).name() + " owner=" + speaker(ctx, level).ownerPos());
        if (kit.era().isAtLeast(EraKit.STANDARD.era())) {
            return;
        }
        ctx.thenServer(SETTLE, level -> keep(level, ctx.abs(EraNetwork.COMPUTER)))
                .thenTeleport(SETTLE, EraNetwork.AT_THE_DESK, Direction.WEST)
                .thenRightClick(SETTLE, EraNetwork.MONITOR)
                .thenAwaitScreen(DesktopScreen.class, EraJourney.BOOT_WAIT);
        EraJourney.launch(ctx, kit, SOUNDFOUNDRY)
                .thenWaitUntil(() -> SoundfoundryStates.of(ctx.abs(EraNetwork.COMPUTER)) != null
                                && !SoundfoundryStates.of(ctx.abs(EraNetwork.COMPUTER)).songs().isEmpty(), WAIT,
                        "Soundfoundry to list the song kept on the computer")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx, SOUNDFOUNDRY, SoundfoundryApp.class)
                        .playerPoint(SoundfoundryLayout.PLAY)))
                .thenWaitUntil(() -> {
                    final SoundfoundryStates.Known known = SoundfoundryStates.of(ctx.abs(EraNetwork.COMPUTER));
                    return known != null && known.state().status() == SoundfoundryStatePayload.PLAYING
                            && MediaPlayer.heard(MusicPlayer.keyOf(ctx.abs(EraNetwork.COMPUTER)));
                }, WAIT, "the song to be heard out of the machine and its speaker")
                .thenScreenshot(SETTLE, shot(kit, "song"));
    }

    /*
     * A second monitor put down from the hand beside the first: the two flat panels join into one screen showing the
     * computer, looked at from close by and from across the world.
     */
    private static void bigScreen(final ClientTestContext ctx, final EraKit kit) {
        ctx.assertTrue(((MonitorBlock) kit.monitor()).kind().flat(), "the " + kit + " monitor is a flat panel");
        bench(ctx, kit)
                .thenWaitUntil(EraJourney.away(ctx), WAIT, "the desktop to be left")
                .thenGive(0, new ItemStack(kit.monitor()))
                .thenTeleport(SETTLE, BEFORE_THE_SECOND_MONITOR, Direction.WEST)
                .then(SETTLE, () -> ctx.selectHotbar(0))
                .thenPlace(1, SECOND_MONITOR)
                .thenWaitUntil(() -> joined(ctx, EraNetwork.MONITOR) || joined(ctx, SECOND_MONITOR), WAIT,
                        "the two panels to join into one screen", () -> "first " + panelOf(ctx, EraNetwork.MONITOR)
                                + ", second " + panelOf(ctx, SECOND_MONITOR) + " "
                                + ctx.mc().level.getBlockState(ctx.abs(SECOND_MONITOR)))
                .thenScreenshot(SETTLE * 5, shot(kit, "big-screen-near"))
                .thenTeleport(SETTLE, FAR_FROM_THE_SCREEN, Direction.WEST)
                .thenScreenshot(SETTLE * 10, shot(kit, "big-screen-far"));
    }

    /* A stick put into a Dock Station beside the computer by hand: the computer gives it a letter. */
    private static void docked(final ClientTestContext ctx, final EraKit kit) {
        ctx.thenBuild(0, world -> world.setBlock(DOCK, ComputingModule.DOCK_STATION.get().defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH)))
                .thenTeleport(SETTLE, BY_THE_DOCK, Direction.NORTH);
        DiscSteps.swap(ctx, DOCK, new ItemStack(ComputingModule.USB_FLASH_DRIVE.get()));
        ctx.thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(DOCK)) instanceof DockStationBlockEntity dock
                        && dock.letter(DockStationBlockEntity.USB) != ' ', WAIT,
                        "the computer to give the docked stick a letter")
                .thenScreenshot(SETTLE, shot(kit, "docked"));
    }

    /*
     * The era's network with its printer, with paper, north of the computer, and from the Legacy its speaker south of
     * it; the player sits at the computer.
     */
    private static ClientTestContext bench(final ClientTestContext ctx, final EraKit kit) {
        EraJourney.lay(ctx, kit).thenBuild(0, world -> {
            world.setBlock(PRINTER, kit.printer());
            final Block speaker = speakerOf(kit);
            if (speaker != null) {
                world.setBlock(SPEAKER, speaker);
            }
        });
        return ctx.thenServer(SETTLE, level -> {
                    printer(ctx, level).paper().setStackInSlot(0, new ItemStack(Items.PAPER, 16));
                    if (kit != EraKit.VINTAGE) {
                        computerAt(ctx, level).console().install(Programs.SOUNDFOUNDRY.toString());
                        computerAt(ctx, level).console().install("jsc:paint");
                    }
                })
                .thenTeleport(SETTLE, EraNetwork.AT_THE_DESK, Direction.WEST)
                .thenRightClick(SETTLE, EraNetwork.MONITOR)
                .thenAwaitScreen(EraJourney.systemScreen(kit), EraJourney.BOOT_WAIT);
    }

    /* Closes the program's window with its close box and waits for it to go. */
    private static void shut(final ClientTestContext ctx, final String label) {
        ctx.then(SETTLE, () -> {
            final DesktopWindow window = ctx.screen(DesktopScreen.class).windowFor(label);
            if (window != null) {
                ctx.clickDesktop(window.buttonCentre(DesktopWindow.BUTTON_CLOSE));
            }
        }).thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor(label) == null, WAIT,
                "the " + label + " window to close from its close box");
    }

    /* A song kept on the machine and put on its playlist, as the player's own import would leave it. */
    private static void keep(final ServerLevel level, final BlockPos machine) {
        try {
            final MediaStore store = MediaStore.current().orElseThrow();
            final MediaId song = store.put(TestMedia.tone(60_000, "Desk song " + System.nanoTime()), "wav");
            MusicImports.keep(level, machine, "", true, "Desk song.wav", song, store.info(song));
        } catch (final IOException unexpected) {
            throw new IllegalStateException(unexpected);
        }
    }

    private static Block speakerOf(final EraKit kit) {
        return switch (kit) {
            case VINTAGE -> null;
            case LEGACY -> ComputingModule.LEGACY_SPEAKER.get();
            case TRANSITION -> ComputingModule.TRANSITION_SPEAKER.get();
            case STANDARD -> ComputingModule.SPEAKER.get();
            case ADVANCED -> ComputingModule.ADVANCED_SPEAKER.get();
        };
    }

    /* Every sheet in the printer's tray: a page and a picture are not one stack, so each lies in a slot of its own. */
    private static int printed(final ClientTestContext ctx, final ServerLevel level) {
        int sheets = 0;
        for (int slot = 0; slot < PrinterBlockEntity.OUTPUT_SLOTS; slot++) {
            sheets += printer(ctx, level).output().getStackInSlot(slot).getCount();
        }
        return sheets;
    }

    private static <T> T app(final ClientTestContext ctx, final String label, final Class<T> type) {
        return DesktopSteps.app(ctx, label, type);
    }

    /* Whether the monitor at {@code at}, as the player's game sees it, belongs to a screen of two. */
    private static boolean joined(final ClientTestContext ctx, final BlockPos at) {
        final MonitorBlockEntity monitor = clientMonitor(ctx, at);
        return monitor != null && monitor.panel() != null && monitor.panel().width() * monitor.panel().height() == 2;
    }

    private static String panelOf(final ClientTestContext ctx, final BlockPos at) {
        final MonitorBlockEntity monitor = clientMonitor(ctx, at);
        return monitor == null ? "none" : monitor.panel() == null ? "alone"
                : monitor.panel().width() + "x" + monitor.panel().height();
    }

    private static MonitorBlockEntity clientMonitor(final ClientTestContext ctx, final BlockPos at) {
        return ctx.mc().level.getBlockEntity(ctx.abs(at)) instanceof MonitorBlockEntity monitor ? monitor : null;
    }

    private static PrinterBlockEntity printer(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(PRINTER, PrinterBlockEntity.class);
    }

    private static SpeakerBlockEntity speaker(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(SPEAKER, SpeakerBlockEntity.class);
    }

    private static PersonalComputerBlockEntity computerAt(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.COMPUTER,
                PersonalComputerBlockEntity.class);
    }

    private static String shot(final EraKit kit, final String what) {
        return kit.name().toLowerCase(Locale.ROOT) + "-" + what;
    }
}
