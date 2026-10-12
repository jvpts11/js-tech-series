/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.audio.MusicImports;
import dev.jstech.computers.audio.MusicPlayer;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.client.audio.SoundfoundryPages;
import dev.jstech.computers.client.audio.SoundfoundryStates;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.KnotApp;
import dev.jstech.computers.client.os.MessengerApp;
import dev.jstech.computers.client.os.RemoteControlApp;
import dev.jstech.computers.client.os.SoundfoundryApp;
import dev.jstech.computers.client.os.SoundfoundryStandardApp;
import dev.jstech.computers.gui.layout.SoundfoundryLayout;
import dev.jstech.computers.gui.layout.SoundfoundryLayout.Rect;
import dev.jstech.computers.gui.layout.SoundfoundryStandardLayout;
import dev.jstech.computers.operation.payload.SoundfoundryPagePayload;
import dev.jstech.computers.operation.payload.SoundfoundryStatePayload;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.install.SetupTiming;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.core.client.audio.media.MediaPlayer;
import dev.jstech.tests.testkit.EraKit;
import dev.jstech.tests.testkit.EraNetwork;
import dev.jstech.tests.testkit.TestMedia;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.lwjgl.glfw.GLFW;

/**
 * Each era's networked programs in a player's hands. Soundfoundry plays a song: one kept on the computer in the eras
 * before its server, and from the Standard on the catalogue's album streamed by the Soundfoundry Server on the rack.
 * From the Standard on, with a second computer on the access line: a message said in the Messenger is read on the
 * other computer; a file pushed with Knot is a revision on the rack's KnotHub; pckmgr installs a program from the
 * Mainframe's Mirror; and Remote Control takes the monitor over to the other computer.
 */
public final class EraNetworkedProgramsClientTests {

    /** How long a step may take to show on the screen. */
    private static final int WAIT = 80;
    /** What the player says in the Messenger, and reads on the other computer. */
    private static final String SAID = "hello from the journey";
    /** The file Knot offers to push, written on the computer beforehand. */
    private static final String KNOT_FILE = "plant.sgs";
    /** The program the Mirror serves to pckmgr. */
    private static final String FROM_THE_MIRROR = "screenfetch";
    private static final String SOUNDFOUNDRY = "Soundfoundry";
    private static final String MESSENGER = "Midsoft Messenger";
    private static final String KNOT = "Knot";
    private static final String REMOTE_CONTROL = "Remote Control";

    private EraNetworkedProgramsClientTests() {
    }

    @ClientTest(timeoutTicks = 4000)
    public static void legacy_networkedProgramsDoTheirWork(final ClientTestContext ctx) {
        networked(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = 4000)
    public static void transition_networkedProgramsDoTheirWork(final ClientTestContext ctx) {
        networked(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = 9000)
    public static void standard_networkedProgramsDoTheirWork(final ClientTestContext ctx) {
        networked(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = 9000)
    public static void advanced_networkedProgramsDoTheirWork(final ClientTestContext ctx) {
        networked(ctx, EraKit.ADVANCED);
    }

    private static void networked(final ClientTestContext ctx, final EraKit kit) {
        final boolean served = kit.era().isAtLeast(EraKit.STANDARD.era());
        EraJourney.lay(ctx, kit);
        if (served) {
            ctx.thenBuild(0, world -> EraNetwork.secondComputer(world, kit));
        }
        // What the install journey and the rack's own system would leave: the programs, the services and a song.
        ctx.thenServer(EraJourney.SETTLE, level -> {
                    final PersonalComputerBlockEntity computer = computer(ctx, level, EraNetwork.COMPUTER);
                    computer.console().install(Programs.SOUNDFOUNDRY.toString());
                    if (!served) {
                        keep(level, ctx.abs(EraNetwork.COMPUTER), "Copper Rain.wav");
                        return;
                    }
                    for (final String program : List.of("messenger", "knot", "remote_control")) {
                        computer.console().install("jsc:" + program);
                    }
                    computer(ctx, level, EraNetwork.SECOND_COMPUTER).console().install("jsc:messenger");
                    DiskFilesystem.write(computer.systemDisk(), KNOT_FILE, FileType.ofPath(KNOT_FILE),
                            "class Plant { }", Long.MAX_VALUE, FilesystemKind.HIERARCHICAL);
                    final ServerRackBlockEntity rack = rack(ctx, level);
                    rack.unitHost(0).installOs(ResourceLocation.fromNamespaceAndPath("jsc", "debian"));
                    for (final ResourceLocation service : List.of(Programs.SOUNDFOUNDRY_SERVER,
                            Programs.MESSENGER_SERVICE, Programs.KNOT_HUB)) {
                        rack.consoleOf(0).install(service.toString());
                    }
                    mainframe(ctx, level).installMirror();
                })
                .thenTeleport(EraJourney.SETTLE, EraNetwork.AT_THE_DESK, Direction.WEST)
                .thenRightClick(EraJourney.SETTLE, EraNetwork.MONITOR)
                .thenAwaitScreen(EraJourney.systemScreen(kit), EraJourney.BOOT_WAIT)
                .then(EraJourney.SETTLE, () -> {
                    final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
                    for (final String open : List.copyOf(desktop.openWindowLabels())) {
                        close(ctx, open);
                    }
                });
        if (!served) {
            soundfoundryAlone(ctx, kit);
            return;
        }
        soundfoundryStreamed(ctx, kit);
        messenger(ctx, kit);
        knot(ctx, kit);
        mirror(ctx, kit);
        remoteControl(ctx, kit);
    }

    /* The song kept on the computer played with the player's Play button, heard out of the machine. */
    private static void soundfoundryAlone(final ClientTestContext ctx, final EraKit kit) {
        open(ctx, kit, SOUNDFOUNDRY)
                .thenWaitUntil(() -> SoundfoundryStates.of(ctx.abs(EraNetwork.COMPUTER)) != null
                        && !SoundfoundryStates.of(ctx.abs(EraNetwork.COMPUTER)).songs().isEmpty(), WAIT,
                        "Soundfoundry to list the song on the computer")
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(app(ctx, SOUNDFOUNDRY, SoundfoundryApp.class)
                        .playerPoint(SoundfoundryLayout.PLAY)))
                .thenWaitUntil(() -> status(ctx) == SoundfoundryStatePayload.PLAYING && heard(ctx), WAIT,
                        "Play to play the song and the machine to sound it")
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "soundfoundry"))
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(app(ctx, SOUNDFOUNDRY, SoundfoundryApp.class)
                        .playerPoint(SoundfoundryLayout.STOP)))
                .thenWaitUntil(() -> status(ctx) == SoundfoundryStatePayload.STOPPED, WAIT, "Stop to end it");
        shut(ctx, SOUNDFOUNDRY);
    }

    /*
     * The catalogue's album opened from Soundfoundry's home and played with its page's Play button, streamed by the
     * Soundfoundry Server on the rack and heard out of the machine; Space pauses it again.
     */
    private static void soundfoundryStreamed(final ClientTestContext ctx, final EraKit kit) {
        open(ctx, kit, SOUNDFOUNDRY)
                .thenWaitUntil(() -> page(ctx) != null && !page(ctx).albums().isEmpty(), WAIT,
                        "Soundfoundry's home to list the catalogue")
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(standard(ctx).point(SoundfoundryStandardLayout.SIDE_W
                        + SoundfoundryStandardLayout.PAD + 30, SoundfoundryStandardLayout.TITLE_H
                        + SoundfoundryStandardLayout.CARD_Y + 30)))
                .thenWaitUntil(() -> standard(ctx).page() == SoundfoundryPagePayload.ALBUM && page(ctx) != null
                        && !page(ctx).rows().isEmpty(), WAIT, "a click on the album to open its page")
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(standard(ctx).point(inPage(
                        SoundfoundryStandardLayout.PLAY))))
                .thenWaitUntil(() -> status(ctx) == SoundfoundryStatePayload.PLAYING && streaming(ctx) && heard(ctx),
                        WAIT, "the album to play, streamed through the server and heard out of the machine")
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "soundfoundry"))
                .then(EraJourney.SETTLE, () -> ctx.key(GLFW.GLFW_KEY_SPACE))
                .thenWaitUntil(() -> status(ctx) == SoundfoundryStatePayload.PAUSED, WAIT, "Space to pause it");
        shut(ctx, SOUNDFOUNDRY);
    }

    /*
     * A line said in the Messenger, typed into its box and sent with Enter; the player then walks to the other
     * computer, opens the Messenger there and reads the line in the same conversation.
     */
    private static void messenger(final ClientTestContext ctx, final EraKit kit) {
        open(ctx, kit, MESSENGER)
                .thenWaitUntil(() -> app(ctx, MESSENGER, MessengerApp.class).online(), WAIT,
                        "the Messenger to reach the service on the rack")
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(app(ctx, MESSENGER, MessengerApp.class).composePoint()))
                .then(EraJourney.SETTLE, () -> {
                    ctx.type(SAID);
                    ctx.key(GLFW.GLFW_KEY_ENTER);
                })
                .thenWaitUntil(() -> app(ctx, MESSENGER, MessengerApp.class).conversationText().contains(SAID), WAIT,
                        "the line to be in the conversation")
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "messenger-said"));
        shut(ctx, MESSENGER);
        leaveTheDesk(ctx)
                .thenTeleport(EraJourney.SETTLE, EraNetwork.AT_THE_SECOND_DESK, Direction.WEST)
                .thenRightClick(EraJourney.SETTLE, EraNetwork.SECOND_MONITOR)
                .thenAwaitScreen(EraJourney.systemScreen(kit), EraJourney.BOOT_WAIT);
        open(ctx, kit, MESSENGER)
                .thenWaitUntil(() -> app(ctx, MESSENGER, MessengerApp.class).conversationText().contains(SAID), WAIT,
                        "the other computer's Messenger to show the line")
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "messenger-read"));
        shut(ctx, MESSENGER);
        leaveTheDesk(ctx)
                .thenTeleport(EraJourney.SETTLE, EraNetwork.AT_THE_DESK, Direction.WEST)
                .thenRightClick(EraJourney.SETTLE, EraNetwork.MONITOR)
                .thenAwaitScreen(EraJourney.systemScreen(kit), WAIT);
    }

    /* The file Knot offers pushed with a message: KnotHub on the rack keeps it as its first revision. */
    private static void knot(final ClientTestContext ctx, final EraKit kit) {
        open(ctx, kit, KNOT)
                .thenWaitUntil(() -> app(ctx, KNOT, KnotApp.class).online(), WAIT, "Knot to reach KnotHub on the rack")
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(app(ctx, KNOT, KnotApp.class).messagePoint()))
                .then(EraJourney.SETTLE, () -> ctx.type("the first plant"))
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(app(ctx, KNOT, KnotApp.class).pushPoint()));
        ctx.thenWaitUntilServer(level -> rack(ctx, level).knotAt(0).revisionsOf(KNOT_FILE).size() == 1, WAIT,
                        "KnotHub to keep the pushed file as a revision",
                        level -> "revisions=" + rack(ctx, level).knotAt(0).revisionsOf(KNOT_FILE).size())
                .thenWaitUntil(() -> app(ctx, KNOT, KnotApp.class).revisionCount() == 1, WAIT,
                        "Knot's history to list the revision")
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "knot"));
        shut(ctx, KNOT);
    }

    /* pckmgr at the system's terminal installs a program over the network from the Mainframe's Mirror. */
    private static void mirror(final ClientTestContext ctx, final EraKit kit) {
        final String terminal = EraJourney.terminal(kit);
        open(ctx, kit, terminal)
                .then(EraJourney.SETTLE, () -> {
                    ctx.type("pckmgr install " + FROM_THE_MIRROR);
                    ctx.key(GLFW.GLFW_KEY_ENTER);
                });
        ctx.thenWaitUntilServer(level -> computer(ctx, level, EraNetwork.COMPUTER).console()
                        .isInstalled("jsc:" + FROM_THE_MIRROR),
                SetupTiming.networkTicks(Programs.get(ResourceLocation.fromNamespaceAndPath("jsc", FROM_THE_MIRROR))
                        .minDiskMb(), false) + WAIT,
                "pckmgr to install " + FROM_THE_MIRROR + " from the Mirror",
                level -> "installed=" + computer(ctx, level, EraNetwork.COMPUTER).console().installed())
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "mirror"));
        shut(ctx, terminal);
    }

    /* The other computer picked in Remote Control and taken over: the monitor now shows that machine. */
    private static void remoteControl(final ClientTestContext ctx, final EraKit kit) {
        final long other = ctx.abs(EraNetwork.SECOND_COMPUTER).asLong();
        open(ctx, kit, REMOTE_CONTROL)
                .thenWaitUntil(() -> app(ctx, REMOTE_CONTROL, RemoteControlApp.class).rowPoint(other) != null, WAIT,
                        "Remote Control to list the other computer")
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(
                        app(ctx, REMOTE_CONTROL, RemoteControlApp.class).rowPoint(other)))
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(
                        app(ctx, REMOTE_CONTROL, RemoteControlApp.class).takeOverPoint()));
        ctx.thenWaitUntilServer(level -> ctx.abs(EraNetwork.SECOND_COMPUTER).equals(
                        monitor(ctx, level).remoteSession()), WAIT,
                "Take over to put the other computer on the monitor",
                level -> "session=" + monitor(ctx, level).remoteSession())
                .thenScreenshot(EraJourney.BOOT_WAIT / 20, shot(kit, "remote-control"));
    }

    /* Opens the program from Start and waits for its window. */
    private static ClientTestContext open(final ClientTestContext ctx, final EraKit kit, final String label) {
        return EraJourney.launch(ctx, kit, label)
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor(label) != null, WAIT,
                        "the " + label + " window to open from Start");
    }

    /* Closes the program's window with its close box and waits for it to go. */
    private static void shut(final ClientTestContext ctx, final String label) {
        ctx.then(EraJourney.SETTLE, () -> close(ctx, label))
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor(label) == null, WAIT,
                        "the " + label + " window to close from its close box");
    }

    private static void close(final ClientTestContext ctx, final String label) {
        final DesktopWindow window = ctx.screen(DesktopScreen.class).windowFor(label);
        if (window != null) {
            ctx.clickDesktop(window.buttonCentre(DesktopWindow.BUTTON_CLOSE));
        }
    }

    /* Closes the screen at the desk, so the player can walk to another. */
    private static ClientTestContext leaveTheDesk(final ClientTestContext ctx) {
        return ctx.then(0, () -> {
            if (ctx.mc().screen != null) {
                ctx.key(GLFW.GLFW_KEY_ESCAPE);
            }
        }).thenAwaitNoScreen(WAIT);
    }

    private static <T> T app(final ClientTestContext ctx, final String label, final Class<T> type) {
        final DesktopWindow window = ctx.screen(DesktopScreen.class).windowFor(label);
        if (window == null || !type.isInstance(window.app())) {
            throw new ClientTestFailure("no " + type.getSimpleName() + " window named " + label);
        }
        return type.cast(window.app());
    }

    private static SoundfoundryStandardApp standard(final ClientTestContext ctx) {
        return app(ctx, SOUNDFOUNDRY, SoundfoundryStandardApp.class);
    }

    /* The page Soundfoundry is showing, once the machine has sent it. */
    private static SoundfoundryPagePayload page(final ClientTestContext ctx) {
        final DesktopWindow window = ctx.screen(DesktopScreen.class).windowFor(SOUNDFOUNDRY);
        if (window == null || !(window.app() instanceof SoundfoundryStandardApp app)) {
            return null;
        }
        final SoundfoundryPages.Known known = SoundfoundryPages.of(ctx.abs(EraNetwork.COMPUTER));
        return known == null || !known.is(app.page(), app.pageArg()) ? null : known.page();
    }

    /* A part of a page, measured from the page's corner, as a part of the window. */
    private static Rect inPage(final Rect part) {
        return new Rect(SoundfoundryStandardLayout.SIDE_W + part.x(), SoundfoundryStandardLayout.TITLE_H + part.y(),
                part.w(), part.h());
    }

    private static int status(final ClientTestContext ctx) {
        final SoundfoundryStates.Known known = SoundfoundryStates.of(ctx.abs(EraNetwork.COMPUTER));
        return known == null ? -1 : known.state().status();
    }

    private static boolean streaming(final ClientTestContext ctx) {
        final SoundfoundryStates.Known known = SoundfoundryStates.of(ctx.abs(EraNetwork.COMPUTER));
        return known != null && known.state().stream();
    }

    private static boolean heard(final ClientTestContext ctx) {
        return MediaPlayer.heard(MusicPlayer.keyOf(ctx.abs(EraNetwork.COMPUTER)));
    }

    /* A song kept on the machine and put on its playlist, as the player's own import would leave it. */
    private static void keep(final ServerLevel level, final BlockPos machine, final String name) {
        try {
            final MediaStore store = MediaStore.current().orElseThrow();
            final MediaId song = store.put(TestMedia.tone(60_000, name + " " + System.nanoTime()), "wav");
            MusicImports.keep(level, machine, "", true, name, song, store.info(song));
        } catch (final IOException unexpected) {
            throw new IllegalStateException(unexpected);
        }
    }

    private static String shot(final EraKit kit, final String what) {
        return kit.name().toLowerCase(Locale.ROOT) + "-" + what;
    }

    private static PersonalComputerBlockEntity computer(final ClientTestContext ctx, final ServerLevel level,
                                                        final BlockPos at) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(at, PersonalComputerBlockEntity.class);
    }

    private static ServerRackBlockEntity rack(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.RACK, ServerRackBlockEntity.class);
    }

    private static MainframeBlockEntity mainframe(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.MAINFRAME, MainframeBlockEntity.class);
    }

    private static MonitorBlockEntity monitor(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.MONITOR, MonitorBlockEntity.class);
    }
}
