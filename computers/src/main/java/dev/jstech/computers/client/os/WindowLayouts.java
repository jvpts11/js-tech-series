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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;

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
    private static final Map<BlockPos, Map<String, IDesktopApp>> SAVED_APPS =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(final Map.Entry<BlockPos, Map<String, IDesktopApp>> eldest) {
                    return size() > MAX_SAVED_DESKTOPS;
                }
            };

    WindowLayouts(final DesktopState desktop, final BlockPos host) {
        this.desktop = desktop;
        this.host = host;
    }

    /** Lets go of every machine's kept programs, as the player leaves a world. */
    static void forgetAll() {
        SAVED_APPS.clear();
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
        final DesktopWindows wm = desktop.wm();
        if (!wm.all().isEmpty()) {
            return;
        }
        // Only a desktop that has workspaces comes back on another than the first.
        wm.setWorkspace(desktop.hasWorkspaces() ? payload.workspace() : 0);
        final Map<String, IDesktopApp> savedApps = SAVED_APPS.get(host);
        final DesktopViewport view = desktop.view();
        for (final OpenWindow ow : payload.toOpenWindows()) {
            IDesktopApp app = savedApps != null ? savedApps.get(ow.key()) : null;
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
    }

    /**
     * Shows the windows the machine has open on a desktop drawn for a monitor's face: each layout the server describes
     * comes in whole, and nothing is ever told back. The windows are drawn with no program behind them
     * ({@link MirroredWindowApp}), so a face seen across the room asks the machine nothing and takes no reply meant
     * for a program open on the player's own screen.
     */
    void mirror(final List<OpenWindow> windows, final int workspace) {
        mirroring = true;
        restored = true;
        final DesktopWindows wm = desktop.wm();
        wm.all().clear();
        wm.setWorkspace(desktop.hasWorkspaces() ? workspace : 0);
        final DesktopViewport view = desktop.view();
        for (final OpenWindow ow : windows) {
            final IDesktopApp app = new MirroredWindowApp(desktop.nameOf(ow.key()), ow.w(), ow.h());
            app.applySkin(desktop.prefs().skin());
            final DesktopWindow w = new DesktopWindow(app, ow.key(), ow.x(), ow.y(), ow.w(), ow.h());
            w.moveTo(ow.x(), ow.y(), view.workAreaTop(), view.width(), view.workAreaBottom());
            w.setMinimized(ow.minimized());
            w.setMaximized(ow.maximized());
            w.setWorkspaces(desktop.hasWorkspaces() ? ow.workspaces() : WorkspaceSet.only(0));
            wm.all().add(w);
        }
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
            if (!w.dialog()) {
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

    /** Lets go of the programs kept for this machine, whose session a reboot has just ended. */
    void forgetSession() {
        SAVED_APPS.remove(host);
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
        final Map<String, IDesktopApp> apps = new LinkedHashMap<>();
        for (final DesktopWindow w : wm.all()) {
            if (w.dialog()) {
                w.app().onClosed();
            } else if (!(w.app() instanceof SigmaWindowApp)) {
                apps.put(w.appKey(), w.app());
            }
        }
        if (apps.isEmpty()) {
            SAVED_APPS.remove(host);
        } else {
            SAVED_APPS.put(host, apps);
        }
    }

    /**
     * The windows as the machine should remember them: their floating bounds and their state. A dialog is a question
     * in flight, not something a machine has open, and a window of the machine's own program is the program's.
     */
    private List<OpenWindow> snapshot() {
        final List<DesktopWindow> windows = desktop.wm().all();
        final List<OpenWindow> out = new ArrayList<>(windows.size());
        for (final DesktopWindow w : windows) {
            if (!w.dialog() && !(w.app() instanceof SigmaWindowApp)) {
                out.add(new OpenWindow(w.appKey(), w.floatX(), w.floatY(), w.floatW(), w.floatH(), w.minimized(),
                        w.maximized(), w.app().saveState(), w.workspaces()));
            }
        }
        return out;
    }
}
