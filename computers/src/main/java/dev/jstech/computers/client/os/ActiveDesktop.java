/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.DesktopWindowsPayload;
import dev.jstech.computers.operation.payload.DiskFilesPayload;
import dev.jstech.computers.operation.payload.SettingsSnapshotPayload;
import dev.jstech.computers.operation.payload.SetupProgressPayload;
import dev.jstech.computers.operation.payload.UiWindowPayload;
import dev.jstech.computers.os.DesktopEffects;
import dev.jstech.core.text.GameText;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * The desktop in front of the player, as the rest of the client reaches it: what programs, other screens and the
 * handlers of the machine's replies ask of whichever desktop is up. Each call does nothing, or answers empty, while no
 * desktop is up, except the requests to open or close a window, which wait for the next desktop to come up.
 */
public final class ActiveDesktop {

    private ActiveDesktop() {
    }

    /** Takes a window a Σ# program has open on the machine being looked at. */
    public static void acceptWindow(final UiWindowPayload payload) {
        DesktopRequests.window(payload);
    }

    /** Lets a running app end another program's window, the way a task manager does. */
    public static void requestClose(final String key) {
        DesktopRequests.close(key);
    }

    /**
     * Opens this desktop's terminal and has it run that program, which is what double-clicking one does.
     *
     * <p>A program of the console kind needs a terminal to print into, so it is given one; the window is whatever
     * this desktop calls its terminal, because that is the one the machine has.
     */
    public static void requestRunAtTerminal(final String path) {
        DesktopRequests.open(new OpenRequest.RunAtTerminal(path));
    }

    /** Lets a running app open a file in a program the player picked. */
    public static void requestOpenFileWith(final String programId, final String path) {
        DesktopRequests.open(new OpenRequest.OpenFile(programId, path));
    }

    /** Lets a running app ask the player which program opens a file, as Choose another program does. */
    public static void requestChooseOpener(final String path) {
        DesktopRequests.open(new OpenRequest.ChooseOpener(path));
    }

    /**
     * Lets a running app hand the shell a job: lines typed at the terminal, one after the other. A line with a
     * newline in it is typed as the lines it holds.
     */
    public static void requestTypeAtTerminal(final List<String> lines) {
        DesktopRequests.open(new OpenRequest.TypeAtTerminal(List.of(String.join("\n", lines).split("\n"))));
    }

    /** Asks for the Properties window of a file on the desktop, which the explorer knows how to show. */
    public static void requestFileProperties(final String path) {
        DesktopRequests.open(new OpenRequest.Properties(path));
    }

    /**
     * Opens {@code dialog} as a window of its own over the window running {@code owner}: it is listed with the owner
     * on the panel, sits in front of it, and holds it until it is put away. The way an Open or Save window belongs to
     * the program that asked. Nothing happens when no desktop is up or the owner has no window on it.
     */
    public static void openDialogFor(final IDesktopApp owner, final IDesktopApp dialog) {
        final DesktopState desktop = DesktopScreen.current();
        if (desktop != null) {
            desktop.wm().openDialog(owner, dialog);
        }
    }

    /** Closes the window running {@code app}, when it is up, the way its own Close button does. */
    public static void closeWindowFor(final IDesktopApp app) {
        final DesktopState desktop = DesktopScreen.current();
        if (desktop != null) {
            desktop.wm().closeOf(app);
        }
    }

    /** Puts away the window running {@code dialog}, when it is up. */
    public static void closeDialog(final IDesktopApp dialog) {
        closeWindowFor(dialog);
    }

    /**
     * Starts one of this machine's programs by its id, the way a shortcut to it does.
     *
     * <p>For a program that offers another as a way out of itself: a welcome pointing at This PC, and whatever comes
     * to want the same. Nothing happens when this machine has no such program, which is the honest answer on a
     * computer where it was never installed.
     */
    public static void openProgramById(final String path) {
        final DesktopState desktop = DesktopScreen.current();
        if (desktop != null) {
            desktop.opener().startProgram(path);
        }
    }

    /**
     * Brings the window of that key forward, or opens it the way a session coming back would, for a program that has
     * a second window of its own: Soundfoundry's sharing window, say. The key is the id its window factory is
     * registered under ({@link ProgramClient#register}), so the window comes back with the session like any other.
     */
    public static void openOrFocus(final String key) {
        final DesktopState desktop = DesktopScreen.current();
        if (desktop != null) {
            desktop.opener().openOrFocus(key);
        }
    }

    /**
     * The id of the wallpaper actually hanging on the desktop right now: the player's own choice when they made one,
     * else whatever the desktop ships with on the platform it runs on; empty while no desktop is up.
     */
    public static String currentWallpaperId() {
        final DesktopState desktop = DesktopScreen.current();
        return desktop == null ? "" : desktop.wallpaperId();
    }

    /** Whether a window of that key is up on the desktop in front of the player. */
    public static boolean windowOpen(final String key) {
        final DesktopState desktop = DesktopScreen.current();
        return desktop != null && desktop.windowOpen(key);
    }

    /**
     * The name this desktop gives that program, or empty when this machine has no such program.
     *
     * <p>The name is the desktop's, not the program's: the same prompt is called one thing on one edition and
     * something else on another, and a button that offers it should say what this machine calls it.
     */
    public static String programLabel(final String path) {
        final DesktopState desktop = DesktopScreen.current();
        final Launcher launcher = desktop == null ? null : desktop.catalogue().byPath(path);
        return launcher == null ? "" : launcher.label();
    }

    /** The ids of the programs the open desktop's machine has, for a window offering what can open a file. */
    public static List<String> installedProgramIds() {
        final DesktopState desktop = DesktopScreen.current();
        return desktop == null ? List.of() : List.copyOf(desktop.catalogue().installed());
    }

    /**
     * The version the open desktop's machine has that package installed at, or empty when there is no desktop or
     * the machine keeps no record of one.
     */
    public static String installedVersion(final String programId) {
        final DesktopState desktop = DesktopScreen.current();
        return desktop == null ? "" : desktop.packageVersion(programId);
    }

    /** What a program is called, for a menu that offers it by id. */
    public static String openerName(final String programId) {
        return ProgramOpener.openerName(programId);
    }

    /**
     * What a window key reads as on the desktop that is up, for a program listing the windows a machine has open (a
     * task manager) by the names the player knows them by; the key itself while no desktop is up.
     */
    public static String windowName(final String key) {
        final DesktopState desktop = DesktopScreen.current();
        return desktop == null ? key : desktop.nameOf(key);
    }

    /**
     * Forgets every per-machine client cache: the programs' insides kept for the machines of the world the player is
     * leaving, and any open request that never found a desktop. Called on logout, so nothing of one world lingers
     * into the next.
     */
    public static void forgetClientState() {
        WindowLayouts.forgetAll();
        DesktopRequests.forgetAll();
        ClientDeviceMaps.forgetAll();
    }

    /**
     * Applies an accent/brightness change to the live desktop immediately, so a change in the Settings app shows on
     * the chrome without waiting for the next desktop refresh.
     *
     * @param accent     the accent override ({@code 0} = skin default)
     * @param brightness the screen brightness 0..100
     */
    public static void applyLivePrefs(final int accent, final int brightness, final boolean clock12h,
                                      final String wallpaper, final boolean taskbarCentered,
                                      final boolean darkMode, final int scale) {
        final DesktopState desktop = DesktopScreen.current();
        if (desktop != null) {
            desktop.applyLivePrefs(accent, brightness, clock12h, wallpaper, taskbarCentered, darkMode, scale);
        }
    }

    /** Applies the system's visual effects, switched on or off on its settings page, to the live desktop at once. */
    public static void applyLiveEffects(final DesktopEffects effects) {
        final DesktopState desktop = DesktopScreen.current();
        if (desktop != null) {
            desktop.prefs().takeEffects(effects);
        }
    }

    /**
     * Raises the modal {@code .dat}-locked error dialog on the desktop that is up, for a program (the Files explorer)
     * that detects a refused {@code .dat} action and has to say so.
     */
    public static void showDatLockedError() {
        final DesktopState desktop = DesktopScreen.current();
        if (desktop != null) {
            desktop.notices().datLocked();
        }
    }

    /**
     * Raises the error for a refused action on an installer's own files: they are generated from the medium's stamp,
     * so there is nothing to rename, copy off, delete or overwrite.
     */
    public static void showInstallerLockedError() {
        final DesktopState desktop = DesktopScreen.current();
        if (desktop != null) {
            desktop.notices().showError(GameText.resolve(DesktopTexts.ERROR),
                    GameText.resolve(DesktopTexts.INSTALLER_LOCKED));
        }
    }

    /** Hangs a picture on the wall of the desktop that is up, which is what the paint program's last button does. */
    public static void setWallpaperToPicture(final String path) {
        final DesktopState desktop = DesktopScreen.current();
        if (desktop != null && path != null && !path.isEmpty()) {
            desktop.hangPicture(path);
        }
    }

    /** Raises that balloon on whichever desktop is looking at that machine, if one is. */
    public static void raise(final BlockPos host, final String title, final String body, final String opens) {
        final DesktopState desktop = DesktopScreen.current();
        if (desktop != null && desktop.host().equals(host)) {
            desktop.notices().showBalloon(title, body, opens);
        }
    }

    /**
     * Refreshes the open desktop's server-derived state (installed programs included), so a program installed or
     * removed while the desktop is up gets its launcher without closing the monitor. Called after any action that can
     * change the installed set (a shell command, an install disc, Settings).
     */
    public static void refreshActive() {
        final DesktopState desktop = DesktopScreen.current();
        if (desktop != null) {
            desktop.requestDesktop();
        }
    }

    /** Takes the removable media a listing names, which are the machine's whichever folder was listed. */
    public static void acceptVolumes(final DiskFilesPayload payload) {
        final DesktopState desktop = DesktopScreen.current();
        if (desktop != null) {
            desktop.takeVolumes(payload);
        }
    }

    /** Routes the machine's settings to the open desktop, whose panel shows its sound. */
    public static void acceptSettings(final SettingsSnapshotPayload payload) {
        final DesktopState desktop = DesktopScreen.current();
        if (desktop != null && desktop.host().equals(payload.hostPos())) {
            desktop.volume().accept(payload.sound());
        }
    }

    /** Restores the windows the machine has open, when the server hands them over. */
    public static void applyWindows(final DesktopWindowsPayload payload) {
        final DesktopState desktop = DesktopScreen.current();
        if (desktop != null && desktop.host().equals(payload.host())) {
            desktop.takeWindows(payload);
        }
    }

    /** A machine saying how the program it is setting up is going, which its Setup window shows. */
    public static void acceptSetup(final SetupProgressPayload payload) {
        final DesktopState desktop = DesktopScreen.current();
        if (desktop != null && desktop.host().equals(payload.hostPos())) {
            desktop.takeSetup(payload);
        }
    }

    /**
     * Asks the player a question over the whole desktop, running {@code yes} only when the answer is Yes: what
     * deletes a thing for good asks this first.
     */
    static void ask(final String title, final String message, final Runnable yes) {
        final DesktopState desktop = DesktopScreen.current();
        if (desktop != null) {
            desktop.notices().ask(title, message, yes);
        }
    }

    /** Tells the player something over the whole desktop, in a note they close with OK. */
    static void tell(final String title, final String message) {
        ask(title, message, null);
    }
}
