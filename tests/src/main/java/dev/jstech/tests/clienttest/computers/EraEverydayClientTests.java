/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.os.CalculatorApp;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.EditorApp;
import dev.jstech.computers.client.os.FilesApp;
import dev.jstech.computers.client.os.MinesweeperApp;
import dev.jstech.computers.client.os.SnakeApp;
import dev.jstech.computers.client.os.SolitaireApp;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.clienttest.ClientTestFailure;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerLevel;
import org.lwjgl.glfw.GLFW;

/**
 * Each era's everyday programs in a player's hands, at the era's Personal Computer on its own network: a sum on the
 * Calculator; a note written in the Editor and saved under a name; a folder and a file made in the file manager, the
 * file moved into the folder, copied back out, moved over its namesake and deleted into the bin; and a move in each of
 * the three games.
 */
public final class EraEverydayClientTests {

    /** How long a step may take to show on the screen. */
    private static final int WAIT = 80;
    /** What the player writes in the Editor, and finds on the disk. */
    private static final String NOTE = "Notes from the journey";
    /** The name the note is saved under. */
    private static final String NOTE_NAME = "journey";
    private static final String FOLDER = "New Folder";
    private static final String FILE = "New File.txt";
    private static final List<String> GAMES = List.of("minesweeper", "solitaire", "snake");

    private EraEverydayClientTests() {
    }

    @ClientTest(timeoutTicks = 6000)
    public static void legacy_everydayProgramsDoTheirWork(final ClientTestContext ctx) {
        everyday(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = 6000)
    public static void transition_everydayProgramsDoTheirWork(final ClientTestContext ctx) {
        everyday(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = 6000)
    public static void standard_everydayProgramsDoTheirWork(final ClientTestContext ctx) {
        everyday(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = 6000)
    public static void advanced_everydayProgramsDoTheirWork(final ClientTestContext ctx) {
        everyday(ctx, EraKit.ADVANCED);
    }

    private static void everyday(final ClientTestContext ctx, final EraKit kit) {
        // The games are on the machine as the install journey leaves them; this one plays them.
        EraJourney.atTheDesk(ctx, kit)
                .thenServer(0, level -> {
                    for (final String game : GAMES) {
                        computer(ctx, level).console().install("jsc:" + game);
                    }
                })
                .then(EraJourney.SETTLE, () -> {
                    final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
                    for (final String open : List.copyOf(desktop.openWindowLabels())) {
                        close(ctx, open);
                    }
                });
        calculator(ctx, kit);
        editor(ctx, kit);
        files(ctx, kit);
        minesweeper(ctx, kit);
        solitaire(ctx, kit);
        snake(ctx, kit);
    }

    /* A sum typed on the keyboard and Enter: the display answers it. */
    private static void calculator(final ClientTestContext ctx, final EraKit kit) {
        final String label = "Calculator";
        open(ctx, kit, label)
                .then(EraJourney.SETTLE, () -> {
                    ctx.type("12*3+6");
                    ctx.key(GLFW.GLFW_KEY_ENTER);
                })
                .thenWaitUntil(() -> "42".equals(app(ctx, label, CalculatorApp.class).result()), WAIT,
                        "the Calculator to answer 12*3+6 with 42")
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "calculator"));
        shut(ctx, label);
    }

    /* A note written and saved under a name through Save As, and the file on the disk with what was written. */
    private static void editor(final ClientTestContext ctx, final EraKit kit) {
        final String label = "Editor";
        final String[] saved = new String[1];
        open(ctx, kit, label)
                .then(EraJourney.SETTLE, () -> ctx.type(NOTE))
                .thenWaitUntil(() -> app(ctx, label, EditorApp.class).text().contains(NOTE), WAIT,
                        "the note to be in the Editor")
                // Ctrl+S on a page never saved asks where and under what name, as Save As does.
                .then(EraJourney.SETTLE, () -> ctx.key(GLFW.GLFW_KEY_S, GLFW.GLFW_MOD_CONTROL))
                .thenWaitUntil(() -> app(ctx, label, EditorApp.class).fileDialog().isOpen(), WAIT,
                        "Ctrl+S on a new page to open the system's file window")
                // The field offers untitled.txt with the cursor before the extension: the player types the name over.
                .then(EraJourney.SETTLE, () -> {
                    for (int i = 0; i < "untitled".length(); i++) {
                        ctx.key(GLFW.GLFW_KEY_BACKSPACE);
                    }
                    ctx.type(NOTE_NAME);
                })
                .thenWaitUntil(() -> (NOTE_NAME + ".txt").equals(
                        app(ctx, label, EditorApp.class).fileDialog().typedName()), WAIT,
                        "the file window's name field to read " + NOTE_NAME + ".txt")
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(
                        app(ctx, label, EditorApp.class).fileDialog().primaryPoint()))
                .thenWaitUntil(() -> app(ctx, label, EditorApp.class).openFile().endsWith(NOTE_NAME + ".txt"), WAIT,
                        "the Editor to hold the file it saved")
                .then(0, () -> saved[0] = app(ctx, label, EditorApp.class).openFile());
        ctx.thenWaitUntilServer(level -> onDisk(ctx, level, saved[0]).contains(NOTE), WAIT,
                        "the note to be on the disk under its name",
                        level -> "at " + saved[0] + ": " + onDisk(ctx, level, saved[0]))
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "editor"));
        shut(ctx, label);
    }

    /*
     * A folder and a text file made from the folder's own menu; the file cut with Ctrl+X and pasted into the folder
     * with Ctrl+V; copied back out from its menu and pasted from the folder's menu; the one in the folder cut and
     * pasted over its namesake, Yes answering the question that asks; and the file deleted into the bin with the
     * Delete key.
     */
    private static void files(final ClientTestContext ctx, final EraKit kit) {
        final String label = filesLabel(kit);
        final String textDocument = FilesApp.typeLabel(FileType.TXT) + " (." + FileType.TXT.extension() + ")";
        final String[] home = new String[1];
        open(ctx, kit, label)
                .thenWaitUntil(() -> !files(ctx, label).waiting(), WAIT, "the file manager to list its folder")
                .then(0, () -> home[0] = files(ctx, label).currentDir())
                // Maximised, as a player makes it to see the whole folder rather than scroll a small window.
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(ctx.screen(DesktopScreen.class).windowFor(label)
                        .buttonCentre(DesktopWindow.BUTTON_MAXIMIZE)))
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor(label).maximized(), WAIT,
                        "the file manager to fill the screen");
        fromNewMenu(ctx, label, "Folder", FOLDER);
        fromNewMenu(ctx, label, textDocument, FILE);
        // The file cut with the keyboard, the folder opened, and pasted into it with the keyboard: moved.
        ctx.then(EraJourney.SETTLE, () -> ctx.clickDesktop(files(ctx, label).rowPoint(FILE)))
                .then(EraJourney.SETTLE, () -> ctx.key(GLFW.GLFW_KEY_X, GLFW.GLFW_MOD_CONTROL))
                .then(EraJourney.SETTLE, () -> ctx.assertTrue(files(ctx, label).open(FOLDER),
                        "the folder opens as a double click opens it"))
                .thenWaitUntil(() -> files(ctx, label).currentDir().endsWith(FOLDER)
                        && !files(ctx, label).waiting(), WAIT, "the file manager to go into the folder")
                .then(EraJourney.SETTLE, () -> ctx.key(GLFW.GLFW_KEY_V, GLFW.GLFW_MOD_CONTROL))
                .thenWaitUntil(() -> files(ctx, label).names().contains(FILE), WAIT,
                        "Ctrl+V to move the cut file into the folder");
        ctx.thenWaitUntilServer(level -> exists(ctx, level, join(home[0], FOLDER, FILE))
                        && !exists(ctx, level, join(home[0], FILE)), WAIT,
                "the file to have moved into the folder on the disk", level -> "home=" + home[0]);
        // Copied from its own menu, the folder left with Backspace, and pasted from the folder's menu: copied.
        ctx.then(EraJourney.SETTLE, () -> ctx.rightClickDesktop(files(ctx, label).rowPoint(FILE)))
                .thenWaitUntil(() -> files(ctx, label).contextOpen(), WAIT, "the file's menu to open")
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(files(ctx, label).contextPoint("Copy")))
                .then(EraJourney.SETTLE, () -> ctx.key(GLFW.GLFW_KEY_BACKSPACE))
                .thenWaitUntil(() -> home[0].equals(files(ctx, label).currentDir())
                        && !files(ctx, label).waiting(), WAIT, "Backspace to go back up out of the folder")
                .then(EraJourney.SETTLE, () -> files(ctx, label).openBackgroundMenu())
                .thenWaitUntil(() -> files(ctx, label).contextOpen(), WAIT, "the folder's menu to open")
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(files(ctx, label).contextPoint("Paste")))
                .thenWaitUntil(() -> files(ctx, label).names().contains(FILE), WAIT,
                        "Paste to put the copy beside the folder");
        ctx.thenWaitUntilServer(level -> exists(ctx, level, join(home[0], FOLDER, FILE))
                        && exists(ctx, level, join(home[0], FILE)), WAIT,
                "the file to be on the disk both in the folder and beside it", level -> "home=" + home[0]);
        // The one in the folder cut and pasted over its namesake: the desktop asks, and Yes replaces it.
        ctx.then(EraJourney.SETTLE, () -> ctx.assertTrue(files(ctx, label).open(FOLDER),
                        "the folder opens as a double click opens it"))
                .thenWaitUntil(() -> files(ctx, label).currentDir().endsWith(FOLDER)
                        && files(ctx, label).rowPoint(FILE) != null, WAIT, "the file manager to go into the folder")
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(files(ctx, label).rowPoint(FILE)))
                .then(EraJourney.SETTLE, () -> ctx.key(GLFW.GLFW_KEY_X, GLFW.GLFW_MOD_CONTROL))
                .then(EraJourney.SETTLE, () -> ctx.key(GLFW.GLFW_KEY_BACKSPACE))
                .thenWaitUntil(() -> home[0].equals(files(ctx, label).currentDir())
                        && files(ctx, label).rowPoint(FILE) != null, WAIT, "Backspace to go back up out of the folder")
                .then(EraJourney.SETTLE, () -> ctx.key(GLFW.GLFW_KEY_V, GLFW.GLFW_MOD_CONTROL))
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).question() != null
                        && ctx.screen(DesktopScreen.class).question().asks(), WAIT,
                        "pasting onto a name already taken to ask whether to replace it")
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "replace-question"))
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(
                        ctx.screen(DesktopScreen.class).question().firstButtonCentre()));
        ctx.thenWaitUntilServer(level -> !exists(ctx, level, join(home[0], FOLDER, FILE))
                        && exists(ctx, level, join(home[0], FILE)), WAIT,
                "Yes to move the file over its namesake", level -> "home=" + home[0]);
        // The file deleted with the Delete key goes into the bin, not away for good.
        ctx.then(EraJourney.SETTLE, () -> ctx.clickDesktop(files(ctx, label).rowPoint(FILE)))
                .then(EraJourney.SETTLE, () -> ctx.key(GLFW.GLFW_KEY_DELETE))
                .thenWaitUntil(() -> !files(ctx, label).names().contains(FILE)
                        && ctx.screen(DesktopScreen.class).trashFull(), WAIT,
                        "Delete to take the file into the bin")
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "files"));
        shut(ctx, label);
    }

    /*
     * The folder's menu, the pointer rested on New until its own menu opens beside it, then on the entry of that label
     * and a click, until the new item is listed. The pointer goes where the click goes, as a hand on the mouse takes
     * it: a menu opens and closes the one beside it by what is under the pointer.
     */
    private static void fromNewMenu(final ClientTestContext ctx, final String label, final String entry,
                                    final String made) {
        ctx.then(EraJourney.SETTLE, () -> files(ctx, label).openBackgroundMenu())
                .thenWaitUntil(() -> files(ctx, label).contextOpen(), WAIT, "the folder's menu to open")
                .then(EraJourney.SETTLE, () -> ctx.pointAtDesktop(files(ctx, label).contextPoint("New")))
                .thenWaitUntil(() -> files(ctx, label).submenuPoint(entry) != null, WAIT,
                        "New to open its menu with " + entry)
                .then(EraJourney.SETTLE, () -> {
                    final int[] at = files(ctx, label).submenuPoint(entry);
                    ctx.pointAtDesktop(at);
                    ctx.clickDesktop(at);
                })
                .thenWaitUntil(() -> files(ctx, label).names().contains(made), WAIT, made + " to be made")
                // The new item comes up with its name open for typing, as on every desktop; Enter keeps the name.
                .then(EraJourney.SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ENTER));
    }

    /* A cell of the minefield uncovered with the left button. */
    private static void minesweeper(final ClientTestContext ctx, final EraKit kit) {
        final String label = "Minesweeper";
        open(ctx, kit, label)
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(app(ctx, label, MinesweeperApp.class).cellPoint(0, 0)))
                .thenWaitUntil(() -> app(ctx, label, MinesweeperApp.class).game().isRevealed(0, 0), WAIT,
                        "the cell clicked to be uncovered")
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "minesweeper"));
        shut(ctx, label);
    }

    /* A card turned from the stock onto the waste. */
    private static void solitaire(final ClientTestContext ctx, final EraKit kit) {
        final String label = "Solitaire";
        open(ctx, kit, label)
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(app(ctx, label, SolitaireApp.class).stockPoint()))
                .thenWaitUntil(() -> app(ctx, label, SolitaireApp.class).game().wasteSize() > 0, WAIT,
                        "a click on the stock to turn a card onto the waste")
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "solitaire"));
        shut(ctx, label);
    }

    /* The snake sent off with an arrow key, and seen to move. */
    private static void snake(final ClientTestContext ctx, final EraKit kit) {
        final String label = "Snake";
        final int[] start = new int[1];
        open(ctx, kit, label)
                .then(EraJourney.SETTLE, () -> {
                    start[0] = app(ctx, label, SnakeApp.class).game().head();
                    ctx.key(GLFW.GLFW_KEY_UP);
                })
                .thenWaitUntil(() -> app(ctx, label, SnakeApp.class).game().head() != start[0], WAIT,
                        "the snake to move off with the arrow key")
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "snake"));
        shut(ctx, label);
    }

    /* Opens the program from Start and waits for its window. */
    private static ClientTestContext open(final ClientTestContext ctx, final EraKit kit, final String label) {
        return EraJourney.launch(ctx, kit, label)
                .thenWaitUntil(window(ctx, label), WAIT, "the " + label + " window to open from Start");
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

    private static BooleanSupplier window(final ClientTestContext ctx, final String label) {
        return () -> ctx.screen(DesktopScreen.class).windowFor(label) != null;
    }

    private static <T> T app(final ClientTestContext ctx, final String label, final Class<T> type) {
        final DesktopWindow window = ctx.screen(DesktopScreen.class).windowFor(label);
        if (window == null || !type.isInstance(window.app())) {
            throw new ClientTestFailure("no " + type.getSimpleName() + " window named " + label);
        }
        return type.cast(window.app());
    }

    private static FilesApp files(final ClientTestContext ctx, final String label) {
        return app(ctx, label, FilesApp.class);
    }

    /** What the era's desktop calls its file manager: Frames 10 named it File Explorer, and 11 kept the name. */
    private static String filesLabel(final EraKit kit) {
        return kit.era().isAtLeast(EraKit.STANDARD.era()) ? "File Explorer" : "Files";
    }

    private static String join(final String... parts) {
        final StringBuilder path = new StringBuilder();
        for (final String part : parts) {
            if (part == null || part.isEmpty()) {
                continue;
            }
            if (!path.isEmpty()) {
                path.append('/');
            }
            path.append(part);
        }
        return path.toString();
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
