/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.ExceedApp;
import dev.jstech.computers.client.os.ExposureApp;
import dev.jstech.computers.client.os.FileDialog;
import dev.jstech.computers.client.os.FilesApp;
import dev.jstech.computers.client.os.PaintApp;
import dev.jstech.computers.client.os.ShellApp;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.testkit.client.ClientTestFailure;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.lwjgl.glfw.GLFW;

/**
 * Each era's installed programs in a player's hands, at the era's Personal Computer on its own network: a stroke drawn
 * in Paint, saved under a name, cleared and opened again; that picture packed into an archive and unpacked from it in
 * the file manager; a Σ program written in Exposure and saved with nothing wrong in it; screenfetch at the system's
 * terminal; and, from the Standard on, a sum worked out by a formula in Exceed and saved.
 */
public final class EraInstalledProgramsClientTests {

    /** How long a step may take to show on the screen. */
    private static final int WAIT = 80;
    /** The name every file the journey saves is given. */
    private static final String NAME = "journey";
    private static final String PICTURE = NAME + ".pix";
    private static final String ARCHIVE = NAME + ".ark";
    private static final String SOURCE = "hello.sgs";
    private static final String SHEET = NAME + ".csv";
    /** Where on the picture the stroke runs, along one row. */
    private static final int ROW = 10;
    private static final int FROM = 10;
    private static final int TO = 20;

    private EraInstalledProgramsClientTests() {
    }

    @ClientTest(timeoutTicks = 6000)
    public static void legacy_installedProgramsDoTheirWork(final ClientTestContext ctx) {
        installed(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = 6000)
    public static void transition_installedProgramsDoTheirWork(final ClientTestContext ctx) {
        installed(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = 6000)
    public static void standard_installedProgramsDoTheirWork(final ClientTestContext ctx) {
        installed(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = 6000)
    public static void advanced_installedProgramsDoTheirWork(final ClientTestContext ctx) {
        installed(ctx, EraKit.ADVANCED);
    }

    private static void installed(final ClientTestContext ctx, final EraKit kit) {
        final boolean exceed = kit.era().isAtLeast(EraKit.STANDARD.era());
        final List<String> programs = new ArrayList<>(List.of("paint", "ark", "exposure", "sgsc", "sigma",
                "screenfetch"));
        if (exceed) {
            programs.add("exceed");
        }
        // The programs are on the machine as the install journey leaves them, before the player sits down.
        EraJourney.lay(ctx, kit)
                .thenServer(EraJourney.SETTLE, level -> {
                    for (final String program : programs) {
                        computer(ctx, level).console().install("jsc:" + program);
                    }
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
        paint(ctx, kit);
        ark(ctx, kit);
        exposure(ctx, kit);
        screenfetch(ctx, kit);
        if (exceed) {
            exceed(ctx, kit);
        }
    }

    /*
     * A stroke dragged across the canvas with the pencil; Save names the picture in the system's file window; New
     * clears the canvas, and Open brings the saved picture back with the stroke on it.
     */
    private static void paint(final ClientTestContext ctx, final EraKit kit) {
        final String label = "Paint";
        final String[] folder = new String[1];
        open(ctx, kit, label);
        stroke(ctx, () -> app(ctx, label, PaintApp.class).canvasPoint(FROM, ROW),
                () -> app(ctx, label, PaintApp.class).canvasPoint(TO, ROW));
        ctx.thenWaitUntil(() -> stroked(app(ctx, label, PaintApp.class)), WAIT,
                        "the pencil to draw where it was dragged")
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(app(ctx, label, PaintApp.class).savePoint()));
        named(ctx, () -> app(ctx, label, PaintApp.class).fileDialog(), "picture", folder);
        ctx.thenWaitUntilServer(level -> exists(ctx, level, join(folder[0], PICTURE)), WAIT,
                        "the picture to be saved under its name", level -> "in " + folder[0])
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(app(ctx, label, PaintApp.class).newPoint()))
                .thenWaitUntil(() -> !stroked(app(ctx, label, PaintApp.class)), WAIT, "New to clear the canvas")
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(app(ctx, label, PaintApp.class).openPoint()))
                .thenWaitUntil(() -> app(ctx, label, PaintApp.class).fileDialog().isOpen()
                        && app(ctx, label, PaintApp.class).fileDialog().rowPoint(PICTURE) != null, WAIT,
                        "Open to list the saved picture")
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(
                        app(ctx, label, PaintApp.class).fileDialog().rowPoint(PICTURE)))
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(
                        app(ctx, label, PaintApp.class).fileDialog().primaryPoint()))
                .thenWaitUntil(() -> stroked(app(ctx, label, PaintApp.class)), WAIT,
                        "the picture opened to have its stroke")
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "paint"));
        shut(ctx, label);
    }

    /*
     * A stroke as a hand on the mouse makes it: the pointer brought to the start, the button pressed there, the
     * pointer dragged to the end and the button let go, a frame between each, since Paint draws where the pointer was
     * last drawn.
     */
    private static void stroke(final ClientTestContext ctx, final Supplier<int[]> from, final Supplier<int[]> to) {
        ctx.then(EraJourney.SETTLE, () -> ctx.pointAtDesktop(from.get()))
                .then(1, () -> {
                    final double[] at = onScreen(ctx, from.get());
                    ctx.screen(DesktopScreen.class).mouseClicked(at[0], at[1], 0);
                })
                .then(1, () -> ctx.pointAtDesktop(to.get()))
                .then(1, () -> {
                    final double[] at = onScreen(ctx, to.get());
                    ctx.screen(DesktopScreen.class).mouseDragged(at[0], at[1], 0, 0, 0);
                })
                .then(1, () -> {
                    final double[] at = onScreen(ctx, to.get());
                    ctx.screen(DesktopScreen.class).mouseReleased(at[0], at[1], 0);
                });
    }

    /* A desktop point where the screen has it, as clickDesktop places a click. */
    private static double[] onScreen(final ClientTestContext ctx, final int[] point) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        return new double[] {desktop.desktopX() + point[0] * desktop.desktopScale() + 0.5,
                desktop.desktopY() + point[1] * desktop.desktopScale() + 0.5};
    }

    /* Whether the stroke is on the canvas, at both of its ends. */
    private static boolean stroked(final PaintApp paint) {
        return paint.image().get(FROM, ROW) != 0 && paint.image().get(TO, ROW) != 0;
    }

    /*
     * In the file manager, the picture compressed from its own menu into an archive beside it, the picture deleted
     * into the bin, and the archive extracted from its menu: the picture is back.
     */
    private static void ark(final ClientTestContext ctx, final EraKit kit) {
        final String label = kit.era().isAtLeast(EraKit.STANDARD.era()) ? "File Explorer" : "Files";
        open(ctx, kit, label)
                .thenWaitUntil(() -> !app(ctx, label, FilesApp.class).waiting(), WAIT, "the folder to be listed")
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(ctx.screen(DesktopScreen.class).windowFor(label)
                        .buttonCentre(DesktopWindow.BUTTON_MAXIMIZE)))
                .thenWaitUntil(() -> app(ctx, label, FilesApp.class).rowPoint(PICTURE) != null, WAIT,
                        "the picture to be listed")
                .then(EraJourney.SETTLE, () -> ctx.rightClickDesktop(app(ctx, label, FilesApp.class).rowPoint(PICTURE)))
                .thenWaitUntil(() -> app(ctx, label, FilesApp.class).contextLabels().contains("Compress to " + ARCHIVE),
                        WAIT, "the picture's menu to offer to compress it")
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(
                        app(ctx, label, FilesApp.class).contextPoint("Compress to " + ARCHIVE)))
                .thenWaitUntil(() -> app(ctx, label, FilesApp.class).rowPoint(ARCHIVE) != null, WAIT,
                        "the archive to be made beside the picture")
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(app(ctx, label, FilesApp.class).rowPoint(PICTURE)))
                .then(EraJourney.SETTLE, () -> ctx.key(GLFW.GLFW_KEY_DELETE))
                .thenWaitUntil(() -> !app(ctx, label, FilesApp.class).names().contains(PICTURE), WAIT,
                        "the picture to go into the bin")
                .then(EraJourney.SETTLE, () -> ctx.rightClickDesktop(app(ctx, label, FilesApp.class).rowPoint(ARCHIVE)))
                .thenWaitUntil(() -> app(ctx, label, FilesApp.class).contextLabels().contains("Extract here"), WAIT,
                        "the archive's menu to offer to extract it")
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(
                        app(ctx, label, FilesApp.class).contextPoint("Extract here")))
                .thenWaitUntil(() -> app(ctx, label, FilesApp.class).names().contains(PICTURE), WAIT,
                        "Extract here to bring the picture back out of the archive")
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "ark"));
        shut(ctx, label);
    }

    /*
     * A new source made from Exposure's File menu, named in the small window it asks with; a Σ program typed into it
     * and saved with Ctrl+S; the folder lists it and the table of problems is empty.
     */
    private static void exposure(final ClientTestContext ctx, final EraKit kit) {
        final String label = "Exposure";
        open(ctx, kit, label)
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(
                        app(ctx, label, ExposureApp.class).menuTitlePoint("File")))
                .thenWaitUntil(() -> app(ctx, label, ExposureApp.class).menuOpen(), WAIT, "the File menu to drop down")
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(app(ctx, label, ExposureApp.class).menuItemPoint(0)))
                .thenWaitUntil(() -> app(ctx, label, ExposureApp.class).asking(), WAIT, "New File to ask for a name")
                // It offers untitled.sgs with the cursor before the extension: the player types the name over.
                .then(EraJourney.SETTLE, () -> {
                    for (int i = 0; i < "untitled".length(); i++) {
                        ctx.key(GLFW.GLFW_KEY_BACKSPACE);
                    }
                    ctx.type(SOURCE.substring(0, SOURCE.indexOf('.')));
                    ctx.key(GLFW.GLFW_KEY_ENTER);
                })
                .thenWaitUntil(() -> app(ctx, label, ExposureApp.class).openFile().endsWith(SOURCE), WAIT,
                        "the new source to be open")
                .then(EraJourney.SETTLE, () -> ctx.type("using System.IO.*; namespace Hello; "
                        + "class Hello { static void Main() { Console.PrintLine(\"hi\"); } }"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_S, GLFW.GLFW_MOD_CONTROL))
                .thenWaitUntil(() -> app(ctx, label, ExposureApp.class).fileNames().contains(SOURCE)
                        && app(ctx, label, ExposureApp.class).problemCount() == 0, WAIT,
                        "the folder to list the source with nothing wrong in it")
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "exposure"));
        shut(ctx, label);
    }

    /* screenfetch typed at the system's own terminal answers with the system's name beside its logo. */
    private static void screenfetch(final ClientTestContext ctx, final EraKit kit) {
        final String label = EraJourney.terminal(kit);
        final String system = OsRegistry.getOs(kit.desktopOs()).displayName();
        open(ctx, kit, label)
                .then(EraJourney.SETTLE, () -> {
                    ctx.type("screenfetch");
                    ctx.key(GLFW.GLFW_KEY_ENTER);
                })
                .thenWaitUntil(() -> app(ctx, label, ShellApp.class).scrollbackText().contains(system), WAIT,
                        "screenfetch to name " + system)
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "screenfetch"));
        shut(ctx, label);
    }

    /*
     * Two figures typed into the first two cells and a formula adding them into the third, each cell left with Enter;
     * the formula shows the sum, and Ctrl+S saves the sheet under a name.
     */
    private static void exceed(final ClientTestContext ctx, final EraKit kit) {
        final String label = "Exceed";
        final String[] folder = new String[1];
        open(ctx, kit, label)
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(app(ctx, label, ExceedApp.class).cellPoint(0, 0)))
                .then(EraJourney.SETTLE, () -> {
                    for (final String cell : List.of("12", "30", "=A1+A2")) {
                        ctx.type(cell);
                        ctx.key(GLFW.GLFW_KEY_ENTER);
                    }
                })
                .thenWaitUntil(() -> "42".equals(app(ctx, label, ExceedApp.class).shown(2, 0)), WAIT,
                        "the formula to show the sum of the two cells above it")
                .then(EraJourney.SETTLE, () -> ctx.key(GLFW.GLFW_KEY_S, GLFW.GLFW_MOD_CONTROL));
        named(ctx, () -> app(ctx, label, ExceedApp.class).fileDialog(), "sheet", folder);
        ctx.thenWaitUntilServer(level -> onDisk(ctx, level, join(folder[0], SHEET)).contains("A1+A2"), WAIT,
                        "the sheet to be saved with its formula", level -> onDisk(ctx, level, join(folder[0], SHEET)))
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "exceed"));
        shut(ctx, label);
    }

    /*
     * The system's file window, open on a save, has the offered name typed over with the journey's and Save pressed:
     * the offered name's stem is erased with Backspace, the caret standing before its extension. The folder it saves
     * into is kept for the look at the disk.
     */
    private static void named(final ClientTestContext ctx, final Supplier<FileDialog> dialog, final String offered,
                              final String[] folder) {
        ctx.thenWaitUntil(() -> dialog.get().isOpen(), WAIT, "the system's file window to open to save")
                .then(EraJourney.SETTLE, () -> {
                    for (int i = 0; i < offered.length(); i++) {
                        ctx.key(GLFW.GLFW_KEY_BACKSPACE);
                    }
                    ctx.type(NAME);
                })
                .thenWaitUntil(() -> dialog.get().typedName().startsWith(NAME + "."), WAIT,
                        "the name field to read " + NAME)
                .then(EraJourney.SETTLE, () -> {
                    folder[0] = dialog.get().folder();
                    ctx.clickDesktop(dialog.get().primaryPoint());
                })
                .thenWaitUntil(() -> !dialog.get().isOpen(), WAIT, "Save to close the file window");
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

    private static <T> T app(final ClientTestContext ctx, final String label, final Class<T> type) {
        final DesktopWindow window = ctx.screen(DesktopScreen.class).windowFor(label);
        if (window == null || !type.isInstance(window.app())) {
            throw new ClientTestFailure("no " + type.getSimpleName() + " window named " + label);
        }
        return type.cast(window.app());
    }

    private static String join(final String folder, final String name) {
        return folder == null || folder.isEmpty() ? name : folder + "/" + name;
    }

    private static String onDisk(final ClientTestContext ctx, final ServerLevel level, final String path) {
        return DiskFilesystem.read(computer(ctx, level).systemDisk(), path).orElse("");
    }

    private static boolean exists(final ClientTestContext ctx, final ServerLevel level, final String path) {
        return DiskFilesystem.exists(computer(ctx, level).systemDisk(), path);
    }

    private static String shot(final EraKit kit, final String what) {
        return kit.name().toLowerCase(Locale.ROOT) + "-" + what;
    }

    private static PersonalComputerBlockEntity computer(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.COMPUTER,
                PersonalComputerBlockEntity.class);
    }
}
