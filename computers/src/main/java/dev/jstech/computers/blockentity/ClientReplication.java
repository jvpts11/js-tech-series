/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.menu.DesktopMenu;
import dev.jstech.computers.operation.payload.CommandOutputPayload;
import dev.jstech.computers.operation.payload.DesktopShellOutputPayload;
import dev.jstech.computers.operation.payload.UiWindowPayload;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.computers.vm.program.Numbers;
import dev.jstech.computers.vm.program.UiWidgets;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.core.language.ILanguageProcess;
import dev.jstech.core.text.Text;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What a machine sends to the players watching it: the windows its programs have open, and what the
 * program in front has printed.
 */
final class ClientReplication {

    private final AbstractComputerBlockEntity machine;
    /*
     * What each player watching has already been sent: under the player, the revision each window stood at when
     * it went to them. Per player and not per machine, because two people at one desktop are not sent the same
     * windows at the same moments, and whoever opens second must be given what the first already has.
     */
    private final Map<UUID, Map<WindowId, Long>> sentByViewer = new HashMap<>();
    /*
     * What each player has of each canvas: which drawing it was on, and how many strokes of that drawing have
     * gone to them. A canvas only grows between clears, so what a screen is missing is always the tail.
     */
    private final Map<UUID, Map<CanvasId, CanvasSent>> canvasByViewer = new HashMap<>();
    /*
     * Where each player's turn begins: the window the budget stopped at last tick, so that what waited goes
     * before the rest when the next one comes and no window is left behind while others are served again.
     */
    private final Map<UUID, WindowId> resumeAt = new HashMap<>();

    /**
     * What one machine sends one player in one tick before the rest of it waits for the next.
     *
     * <p>A single canvas may hold four thousand strokes, which is ten times this on its own, so a machine that
     * has just been opened, or one whose programs all drew at once, would otherwise put the lot on the wire in a
     * single tick. The first window of a turn always goes, however big, or one too large for the budget would
     * wait for ever.
     */
    private static final int MOST_BYTES_A_TICK = 8192;

    ClientReplication(final AbstractComputerBlockEntity machine) {
        this.machine = machine;
    }

    /**
     * Sends the windows the programs on this machine have open to whoever is at its desktop.
     *
     * <p>A window goes over whole whenever anything in it changes, and once more, empty, when it closes.
     * A machine nobody is looking at sends nothing and forgets what it sent, so whoever opens the desktop
     * next is sent everything as it stands.
     */
    void pushWindows(final ServerLevel level) {
        final List<ServerPlayer> viewers = this.machine.consoleViewers(level);
        if (viewers.isEmpty()) {
            this.sentByViewer.clear();
            this.canvasByViewer.clear();
            this.resumeAt.clear();
            return;
        }
        forgetWhoLeft(viewers);
        final Map<WindowId, Values.Obj> open = openNow();
        final Map<WindowId, UiWindowPayload> made = new HashMap<>();
        for (final ServerPlayer viewer : viewers) {
            if (!(viewer.containerMenu instanceof DesktopMenu)) {
                continue;
            }
            for (final UiWindowPayload payload : takeOwed(viewer, open, made)) {
                PacketDistributor.sendToPlayer(viewer, payload);
            }
        }
    }

    /**
     * The windows this machine owes that player, taking them as sent in the same breath: everything its
     * programs have open that the player has not been given already, and the empty word for one that has
     * closed since.
     *
     * <p>A player who has just opened the desktop is owed every window there is, which is how they arrive
     * whole on opening rather than at whatever changes next.
     */
    List<UiWindowPayload> takeOwed(final ServerPlayer viewer) {
        return takeOwed(viewer, openNow(), new HashMap<>());
    }

    /**
     * A window is written out for the wire only when its revision has moved since that player was sent it, and
     * once written it serves every player owed it this tick.
     *
     * <p>The revision is what makes this cheap: every change to a window or to a widget it shows goes through
     * the one door that moves it, so a machine whose desktop is sitting still costs two lookups per window a
     * tick instead of flattening its whole tree of widgets and comparing that with what went last time.
     */
    private List<UiWindowPayload> takeOwed(final ServerPlayer viewer, final Map<WindowId, Values.Obj> open,
                                           final Map<WindowId, UiWindowPayload> made) {
        final UUID who = viewer.getUUID();
        final Map<WindowId, Long> sent = this.sentByViewer.computeIfAbsent(who, id -> new HashMap<>());
        final Map<CanvasId, CanvasSent> canvases = this.canvasByViewer.computeIfAbsent(who, id -> new HashMap<>());
        final BlockPos pos = this.machine.getBlockPos();
        final List<UiWindowPayload> owed = new ArrayList<>();

        /* A window that has closed always goes, budget or no: it is a few bytes, and one left on a screen is a lie. */
        for (final WindowId id : sent.keySet()) {
            if (!open.containsKey(id)) {
                owed.add(UiWindowPayload.gone(pos, id.program(), id.window()));
                canvases.keySet().removeIf(canvas -> canvas.window().equals(id));
            }
        }
        sent.keySet().removeIf(id -> !open.containsKey(id));

        final List<WindowId> moved = new ArrayList<>();
        for (final Map.Entry<WindowId, Values.Obj> entry : open.entrySet()) {
            if (!Long.valueOf(revisionOf(entry.getValue())).equals(sent.get(entry.getKey()))) {
                moved.add(entry.getKey());
            }
        }
        if (moved.isEmpty()) {
            this.resumeAt.remove(who);
            return owed;
        }
        moved.sort(Comparator.naturalOrder());
        final int start = startFor(moved, this.resumeAt.get(who));
        int spent = 0;
        int given = 0;
        WindowId waits = null;
        for (int n = 0; n < moved.size(); n++) {
            final WindowId id = moved.get((start + n) % moved.size());
            final Values.Obj window = open.get(id);
            final UiWindowPayload whole =
                    made.computeIfAbsent(id, k -> UiWindowPayload.of(pos, k.program(), window));
            if (whole == null) {
                continue;
            }
            final UiWindowPayload cut = cutCanvases(id, whole, canvases);
            if (given > 0 && spent + cut.weight() > MOST_BYTES_A_TICK) {
                waits = id; // this one and whatever follows it wait, and begin the next turn
                break;
            }
            owed.add(cut);
            spent += cut.weight();
            given++;
            noteSent(id, whole, window, sent, canvases);
        }
        if (waits == null) {
            this.resumeAt.remove(who);
        } else {
            this.resumeAt.put(who, waits);
        }
        return owed;
    }

    /** Where a player's turn begins: at the window the budget stopped at, or at the first one owed after it. */
    private static int startFor(final List<WindowId> moved, final WindowId from) {
        if (from == null) {
            return 0;
        }
        for (int i = 0; i < moved.size(); i++) {
            if (moved.get(i).compareTo(from) >= 0) {
                return i;
            }
        }
        return 0;
    }

    /** Writes down what that player now holds of a window: where it stood, and how much of each canvas in it. */
    private void noteSent(final WindowId id, final UiWindowPayload whole, final Values.Obj window,
                          final Map<WindowId, Long> sent, final Map<CanvasId, CanvasSent> canvases) {
        sent.put(id, revisionOf(window));
        for (final UiWindowPayload.Widget widget : whole.widgets()) {
            if (UiWidgets.CANVAS.equals(widget.kind())) {
                canvases.put(new CanvasId(id, widget.id()),
                        new CanvasSent(widget.epoch(), widget.drawing().size()));
            }
        }
    }

    /** The windows the programs on this machine have open, as the programs hold them. */
    private Map<WindowId, Values.Obj> openNow() {
        final Map<WindowId, Values.Obj> open = new HashMap<>();
        this.machine.programs().eachWindow((window, program) -> {
            if (window != null && UiWidgets.WINDOW.equals(window.type())) {
                open.put(new WindowId(program, Numbers.toLong(window.get(UiWidgets.ID))), window);
            }
        });
        return open;
    }

    /**
     * The window as that player is owed it: a canvas they are already up to date on carries only the strokes
     * drawn since it last went to them.
     *
     * <p>This is what keeps a program that draws every tick from sending its whole picture over and over. A
     * canvas the player has never seen, or one that has been cleared since (which moves its drawing on), goes
     * whole, and the player's screen replaces what it had.
     */
    private UiWindowPayload cutCanvases(final WindowId window, final UiWindowPayload whole,
                                        final Map<CanvasId, CanvasSent> canvases) {
        final List<UiWindowPayload.Widget> widgets = whole.widgets();
        List<UiWindowPayload.Widget> cut = null;
        for (int i = 0; i < widgets.size(); i++) {
            final UiWindowPayload.Widget widget = widgets.get(i);
            if (!UiWidgets.CANVAS.equals(widget.kind())) {
                continue;
            }
            final CanvasSent had = canvases.get(new CanvasId(window, widget.id()));
            if (had == null || had.epoch() != widget.epoch() || had.strokes() <= 0) {
                continue;
            }
            if (cut == null) {
                cut = new ArrayList<>(widgets);
            }
            cut.set(i, widget.strokesFrom(had.strokes()));
        }
        return cut == null ? whole : whole.withWidgets(cut);
    }

    /** Where a window stands: a number its one door moves whenever anything the window shows changes. */
    private static long revisionOf(final Values.Obj window) {
        return Numbers.toLong(window.get(UiWidgets.REVISION));
    }

    /** What was sent to somebody who is no longer looking is dropped, rather than kept for a return. */
    private void forgetWhoLeft(final List<ServerPlayer> viewers) {
        if (this.sentByViewer.isEmpty()) {
            return;
        }
        this.sentByViewer.keySet().removeIf(id -> !watching(viewers, id));
        this.canvasByViewer.keySet().removeIf(id -> !watching(viewers, id));
        this.resumeAt.keySet().removeIf(id -> !watching(viewers, id));
    }

    private static boolean watching(final List<ServerPlayer> viewers, final UUID id) {
        for (final ServerPlayer viewer : viewers) {
            if (viewer.getUUID().equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sends what the program in front has printed to whoever is at this machine's terminal.
     *
     * <p>This is what makes a program at a terminal behave like one anywhere else: its lines appear as
     * it prints them rather than all at once when it is over, and the prompt comes back the moment it
     * returns. A program nobody is watching still runs; there is simply nowhere for its lines to go.
     */
    void pushOutput(final ServerLevel level) {
        final MachinePrograms programs = this.machine.programs();
        if (programs.held() == 0) {
            return;
        }
        final var one = programs.byId(programs.held());
        if (one == null) {
            programs.release();
            return;
        }
        final var state = one.process().state();
        final boolean over = !MachinePrograms.running(one.process());
        final List<Text> fresh = programs.unseen();
        final Text halt = over && state == ILanguageProcess.State.HALTED ? one.process().message() : null;
        if (over) {
            programs.release();
            this.machine.setChanged();
        }
        if (fresh.isEmpty() && halt == null && !over) {
            return;
        }
        final List<ServerPlayer> viewers = this.machine.consoleViewers(level);
        if (viewers.isEmpty()) {
            return;
        }
        final List<WireLine> wire = new ArrayList<>();
        for (final Text line : fresh) {
            wire.add(new WireLine(line, CliStyle.PLAIN.id()));
        }
        if (halt != null) {
            wire.add(new WireLine(halt, CliStyle.ERROR.id()));
        }
        say(viewers, wire, over ? this.machine.shellPrompt() : "", !over);
    }

    /**
     * The same said twice, once in each terminal's own words: a window on a desktop, and the prompt that is
     * the whole glass of a machine that has none. Both are watching this one console, so each viewer is sent
     * the one its own screen speaks.
     */
    private void say(final List<ServerPlayer> viewers, final List<WireLine> wire,
                     final String prompt, final boolean holdsTerminal) {
        final var desktop = new DesktopShellOutputPayload(false, holdsTerminal, prompt, wire);
        final var terminal = new CommandOutputPayload(false, prompt, wire);
        for (final ServerPlayer viewer : viewers) {
            PacketDistributor.sendToPlayer(viewer,
                    viewer.containerMenu instanceof DesktopMenu ? desktop : terminal);
        }
    }

    /**
     * One window of one program on this machine.
     *
     * <p>The two were packed into a single long before, which threw away the top half of a window's number:
     * a machine that had opened more than four thousand million windows would have had two of them answer to
     * the same name and shown one in place of the other.
     */
    private record WindowId(int program, long window) implements Comparable<WindowId> {

        /** A steady order, so that a turn cut short by the budget carries on where it left off. */
        @Override
        public int compareTo(final WindowId other) {
            final int byProgram = Integer.compare(this.program, other.program);
            return byProgram != 0 ? byProgram : Long.compare(this.window, other.window);
        }
    }

    /** One canvas in one window of one program on this machine. */
    private record CanvasId(WindowId window, long widget) {
    }

    /** What a player has of a canvas: the drawing it was on, and how many of its strokes have gone to them. */
    private record CanvasSent(int epoch, int strokes) {
    }
}
