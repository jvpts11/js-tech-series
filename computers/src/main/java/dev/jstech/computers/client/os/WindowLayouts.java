/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.DesktopWindowsPayload;
import dev.jstech.computers.os.OpenWindow;
import dev.jstech.computers.os.WorkspaceSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The windows a machine has open, as the machine remembers them. They are the machine's, not this client's: they
 * arrive from the server with the desktop listing and are restored once, every change to them is told back, and they
 * go back to the machine when the desktop closes, for whoever looks next and after the game is closed. What stays in
 * this client is only the programs' insides (scrollback, an unsaved query), kept per machine so a restored window
 * picks its session back up when it is still here.
 */
final class WindowLayouts {

    private final DesktopState desktop;
    private final BlockPos host;
    /** Whether the machine's layout has been applied to this desktop. */
    private boolean restored;
    /** The layout the machine was last told about, so only a real change is pushed to it. */
    private String pushed = "";
    /**
     * Set when this desktop is closing because the player shut the machine down or restarted it, so the layout is
     * not handed back to the machine on the way out: the server has just cleared it, and a late arrival would bring
     * windows back on a machine that is off or rebooting.
     */
    private boolean powerCycling;
    /**
     * Set on a desktop drawn for a monitor's face in the world: it shows the machine's layout as the server describes
     * it and tells the machine nothing back, since nobody is at it.
     */
    private boolean mirroring;

    /** How many machines' programs are kept at once, past which the least recently seen is let go. */
    private static final int MAX_SAVED_DESKTOPS = 16;
    /*
     * The live programs kept per machine while its monitor is left, so going back restores each program's session
     * instead of a fresh window. Access-ordered and bounded, so a long session that visits many machines does not
     * grow it without limit.
     */
    private static final Map<BlockPos, Map<String, List<IDesktopApp>>> SAVED_APPS =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(final Map.Entry<BlockPos, Map<String, List<IDesktopApp>>> eldest) {
                    return size() > MAX_SAVED_DESKTOPS;
                }
            };
    /*
     * The welcome a machine's face in the world is drawn with while this client has no program of its own for it: one
     * per machine, so a face shown again as its layout changes keeps the facts the machine already sent.
     */
    private static final Map<BlockPos, WelcomeApp> FACE_WELCOMES = new HashMap<>();
    /*
     * The programs a machine's face in the world is drawn with while this client has none of the machine's own, by
     * window key: made as the machine's desktop makes them when it is opened, each handed what the machine remembers
     * of it, and kept per machine so a face shown again as its layout changes keeps them. Access-ordered and bounded
     * as the kept programs are; a program let go is closed, so it waits on no answer.
     */
    private static final Map<BlockPos, Map<String, IDesktopApp>> FACE_PROGRAMS =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(final Map.Entry<BlockPos, Map<String, IDesktopApp>> eldest) {
                    if (size() <= MAX_SAVED_DESKTOPS) {
                        return false;
                    }
                    eldest.getValue().values().forEach(IDesktopApp::onClosed);
                    return true;
                }
            };
    /** Moves on each time the programs this client has for any machine change hands. */
    private static int generation;

    WindowLayouts(final DesktopState desktop, final BlockPos host) {
        this.desktop = desktop;
        this.host = host;
    }

    /** Lets go of every machine's kept programs, as the player leaves a world. */
    static void forgetAll() {
        SAVED_APPS.clear();
        FACE_WELCOMES.values().forEach(WelcomeApp::onClosed);
        FACE_WELCOMES.clear();
        FACE_PROGRAMS.values().forEach(programs -> programs.values().forEach(IDesktopApp::onClosed));
        FACE_PROGRAMS.clear();
        generation++;
    }

    /**
     * How many times the programs this client has for any machine have changed hands: a desktop opened or left, a
     * session ended. A monitor's face drawn with them looks again when it moves on.
     */
    static int generation() {
        return generation;
    }

    /**
     * The programs this client has for the machine at {@code host}, by window key: those of the desktop open on the
     * player's screen when it is that machine's, otherwise those kept since its desktop was left; none at all for a
     * machine this client never sat at.
     */
    static Map<String, IDesktopApp> programsOf(final BlockPos host) {
        final DesktopState open = DesktopScreen.current();
        if (open != null && open.hostPos().equals(host)) {
            final Map<String, IDesktopApp> apps = new LinkedHashMap<>();
            for (final DesktopWindow w : open.wm().all()) {
                if (!w.dialog()) {
                    apps.put(w.appKey(), w.app());
                }
            }
            return apps;
        }
        final Map<String, List<IDesktopApp>> kept = SAVED_APPS.get(host);
        if (kept == null) {
            return Map.of();
        }
        final Map<String, IDesktopApp> apps = new LinkedHashMap<>();
        kept.forEach((key, instances) -> apps.put(key, instances.getFirst()));
        return apps;
    }

    /**
     * Restores the windows the machine has open, once, when the server hands them over. Later refreshes of the
     * desktop listing send the same layout again and must not open everything a second time; and a player who
     * already opened something before the layout arrived keeps what they opened.
     */
    void apply(final DesktopWindowsPayload payload) {
        if (restored) {
            return;
        }
        restored = true;
        // The machine's programs are this desktop's now, which a face of it in the world draws from.
        generation++;
        final DesktopWindows wm = desktop.wm();
        if (!wm.all().isEmpty()) {
            return;
        }
        // Only a desktop that has workspaces comes back on another than the first.
        wm.setWorkspace(desktop.hasWorkspaces() ? payload.workspace() : 0);
        // Each window takes a kept instance of its own, in order: a program open twice comes back as two.
        final Map<String, ArrayDeque<IDesktopApp>> savedApps = new HashMap<>();
        SAVED_APPS.getOrDefault(host, Map.of()).forEach((key, instances) ->
                savedApps.put(key, new ArrayDeque<>(instances)));
        final DesktopViewport view = desktop.view();
        for (final OpenWindow ow : payload.toOpenWindows()) {
            final ArrayDeque<IDesktopApp> waiting = savedApps.get(ow.key());
            IDesktopApp app = waiting != null ? waiting.pollFirst() : null;
            final boolean kept = app != null;
            if (app == null) {
                app = desktop.opener().factoryFor(ow.key());
            }
            if (app == null) {
                continue; // a program that is no longer installed simply does not come back
            }
            app.applySkin(desktop.prefs().skin());
            if (kept) {
                app.onRestored(); // a kept instance asks the server again for what may have changed meanwhile
            }
            final DesktopWindow w = new DesktopWindow(app, ow.key(), ow.x(), ow.y(), ow.w(), ow.h());
            /*
             * Clamped into the current work area: the monitor may be a different size from the one the layout was
             * left on, and a title bar off-screen is a window nobody can reach.
             */
            w.moveTo(ow.x(), ow.y(), view.workAreaTop(), view.width(), view.workAreaBottom());
            w.setMinimized(ow.minimized());
            w.setMaximized(ow.maximized());
            w.setWorkspaces(desktop.hasWorkspaces() ? ow.workspaces() : WorkspaceSet.only(0));
            wm.all().add(w);
            /*
             * A kept instance still has everything it had; a fresh one, made because the game itself was closed in
             * between, is handed what the machine remembered it having open.
             */
            if (!kept && !ow.state().isEmpty()) {
                app.restoreState(ow.state());
            }
        }
        // A kept instance no window came back for is closed, as its window would have closed it.
        savedApps.values().forEach(left -> left.forEach(IDesktopApp::onClosed));
    }

    /**
     * Shows the windows the machine has open on a desktop drawn for a monitor's face: each layout the server describes
     * comes in whole, and nothing is ever told back.
     *
     * <p>A window whose program this client has, on the desktop open on its screen or kept since that desktop was
     * left, is drawn by that very program, so the face shows what the player saw at the machine and not a stand-in.
     * A window this client has no program for, as after the game was left and joined again, is drawn by a program of
     * the face's own, made as the machine's desktop makes it and handed what the machine remembers of it; it is let go
     * once a program of the player's draws that window. Only while a desktop is open on the player's screen is none
     * made, since a program made then would take the replies meant for that desktop's: the window is drawn with no
     * program behind it ({@link MirroredWindowApp}) until the screen is closed and the face looks again.
     *
     * <p>The machine's own welcome draws a welcome of its own on the face in the same way, kept apart since it only
     * reads facts the machine hands every welcome open at it.
     */
    void mirror(final List<OpenWindow> windows, final int workspace) {
        mirroring = true;
        restored = true;
        final DesktopWindows wm = desktop.wm();
        wm.all().clear();
        wm.setWorkspace(desktop.hasWorkspaces() ? workspace : 0);
        final DesktopViewport view = desktop.view();
        final Map<String, IDesktopApp> programs = programsOf(host);
        final Map<String, IDesktopApp> facedBefore = FACE_PROGRAMS.getOrDefault(host, Map.of());
        final Map<String, IDesktopApp> faced = new LinkedHashMap<>();
        boolean welcomeOnFace = false;
        for (final OpenWindow ow : windows) {
            // A window no launcher or program names (the machine's own welcome) shows no title rather than its key.
            final String name = desktop.nameOf(ow.key());
            final IDesktopApp live = programs.get(ow.key());
            final IDesktopApp own = live != null || WelcomeApp.KEY.equals(ow.key()) ? null
                    : facedBefore.containsKey(ow.key()) ? facedBefore.get(ow.key()) : faceProgram(ow);
            final IDesktopApp app;
            if (live != null) {
                app = live;
            } else if (WelcomeApp.KEY.equals(ow.key())) {
                app = FACE_WELCOMES.computeIfAbsent(host, WelcomeApp::new);
                welcomeOnFace = true;
            } else if (own != null) {
                app = own;
                faced.put(ow.key(), own);
            } else {
                app = new MirroredWindowApp(name.equals(ow.key()) ? "" : name, ow.w(), ow.h());
            }
            app.applySkin(desktop.prefs().skin());
            final DesktopWindow w = new DesktopWindow(app, ow.key(), ow.x(), ow.y(), ow.w(), ow.h());
            w.moveTo(ow.x(), ow.y(), view.workAreaTop(), view.width(), view.workAreaBottom());
            w.setMinimized(ow.minimized());
            w.setMaximized(ow.maximized());
            w.setWorkspaces(desktop.hasWorkspaces() ? ow.workspaces() : WorkspaceSet.only(0));
            wm.all().add(w);
        }
        if (!welcomeOnFace) {
            final WelcomeApp gone = FACE_WELCOMES.remove(host);
            if (gone != null) {
                gone.onClosed();
            }
        }
        for (final Map.Entry<String, IDesktopApp> before : facedBefore.entrySet()) {
            if (faced.get(before.getKey()) != before.getValue()) {
                before.getValue().onClosed();
            }
        }
        if (faced.isEmpty()) {
            FACE_PROGRAMS.remove(host);
        } else {
            FACE_PROGRAMS.put(host, faced);
        }
    }

    /*
     * The program a face draws the window {@code ow} with when this client has none of the machine's own, made as
     * the machine's desktop makes it when it is opened and handed what the machine remembers of it; null while a
     * desktop is open on the player's screen, or for a program no longer installed.
     */
    @Nullable
    private IDesktopApp faceProgram(final OpenWindow ow) {
        if (DesktopScreen.current() != null) {
            return null;
        }
        final IDesktopApp app = desktop.opener().factoryFor(ow.key());
        if (app == null) {
            return null;
        }
        app.applySkin(desktop.prefs().skin());
        if (!ow.state().isEmpty()) {
            app.restoreState(ow.state());
        }
        return app;
    }

    /**
     * Tells the machine which programs it has open, whenever that changes. Without this the machine only learned its
     * layout when the desktop closed, so anything reading its memory ledger (the Task Manager above all) saw a
     * computer running nothing while the player had five windows in front of them.
     */
    void pushIfChanged() {
        if (!restored || powerCycling || mirroring) {
            return;
        }
        final DesktopWindows wm = desktop.wm();
        final StringBuilder signature = new StringBuilder().append(wm.workspace()).append('|');
        for (final DesktopWindow w : wm.all()) {
            if (!w.dialog() && !(w.app() instanceof CopyProgressApp)) {
                signature.append(w.appKey()).append(w.minimized() ? '-' : '+').append(w.workspaces()).append(';');
            }
        }
        final String now = signature.toString();
        if (now.equals(pushed)) {
            return;
        }
        pushed = now;
        PacketDistributor.sendToServer(DesktopWindowsPayload.of(host, snapshot(), wm.workspace()));
    }

    /** Marks this desktop as closing because the machine is going down or restarting. */
    void powerCycling() {
        powerCycling = true;
    }

    /**
     * Hands the layout back to the machine as the desktop closes, unless the machine is going down, and keeps the
     * programs' insides in this client. A question left unanswered is not kept, and neither is a window of one of
     * the machine's own programs: the machine sends it again, as it stands, the moment anyone looks.
     */
    void keep() {
        final DesktopWindows wm = desktop.wm();
        if (!powerCycling && !mirroring) {
            PacketDistributor.sendToServer(DesktopWindowsPayload.of(host, snapshot(), wm.workspace()));
        }
        final Map<String, List<IDesktopApp>> apps = new LinkedHashMap<>();
        for (final DesktopWindow w : wm.all()) {
            if (w.dialog()) {
                w.app().onClosed();
            } else if (!(w.app() instanceof SigmaWindowApp) && !(w.app() instanceof CopyProgressApp)) {
                apps.computeIfAbsent(w.appKey(), key -> new ArrayList<>()).add(w.app());
            }
        }
        if (apps.isEmpty()) {
            SAVED_APPS.remove(host);
        } else {
            SAVED_APPS.put(host, apps);
        }
        generation++;
    }

    /**
     * The windows as the machine should remember them: their floating bounds and their state. A dialog is a question
     * in flight, not something a machine has open, and a window of the machine's own program is the program's.
     */
    private List<OpenWindow> snapshot() {
        final List<DesktopWindow> windows = desktop.wm().all();
        final List<OpenWindow> out = new ArrayList<>(windows.size());
        for (final DesktopWindow w : windows) {
            // A copy window is the desktop's own, shown while a copy runs, and never part of the machine's layout.
            if (!w.dialog() && !(w.app() instanceof SigmaWindowApp) && !(w.app() instanceof CopyProgressApp)) {
                out.add(new OpenWindow(w.appKey(), w.floatX(), w.floatY(), w.floatW(), w.floatH(), w.minimized(),
                        w.maximized(), w.app().saveState(), w.workspaces()));
            }
        }
        return out;
    }
}
