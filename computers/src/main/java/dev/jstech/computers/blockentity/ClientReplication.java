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
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.computers.program.install.LiveInstallState;
import dev.jstech.computers.vm.program.Numbers;
import dev.jstech.computers.vm.program.UiWidgets;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.core.language.ILanguageProcess;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
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
     * Which progress quarter (25/50/75%) each running build last reported, so the console gets a handful
     * of emerge-style progress lines instead of one per second. Transient by design.
     */
    private final Map<String, Integer> buildQuarterReported = new HashMap<>();
    private int liveKernelQuarterReported;

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
        final Map<WindowId, Long> sent =
                this.sentByViewer.computeIfAbsent(viewer.getUUID(), id -> new HashMap<>());
        final Map<CanvasId, CanvasSent> canvases =
                this.canvasByViewer.computeIfAbsent(viewer.getUUID(), id -> new HashMap<>());
        final BlockPos pos = this.machine.getBlockPos();
        final List<UiWindowPayload> owed = new ArrayList<>();
        for (final Map.Entry<WindowId, Values.Obj> entry : open.entrySet()) {
            if (Long.valueOf(revisionOf(entry.getValue())).equals(sent.get(entry.getKey()))) {
                continue;
            }
            final UiWindowPayload payload = made.computeIfAbsent(entry.getKey(),
                    id -> UiWindowPayload.of(pos, id.program(), entry.getValue()));
            if (payload != null) {
                owed.add(cutCanvases(entry.getKey(), payload, canvases));
            }
        }
        for (final Map.Entry<WindowId, Long> entry : sent.entrySet()) {
            if (!open.containsKey(entry.getKey())) {
                owed.add(UiWindowPayload.gone(pos, entry.getKey().program(), entry.getKey().window()));
                canvases.keySet().removeIf(canvas -> canvas.window().equals(entry.getKey()));
            }
        }
        sent.clear();
        for (final Map.Entry<WindowId, Values.Obj> entry : open.entrySet()) {
            sent.put(entry.getKey(), revisionOf(entry.getValue()));
        }
        return owed;
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
            final CanvasSent had = canvases.put(new CanvasId(window, widget.id()),
                    new CanvasSent(widget.epoch(), widget.drawing().size()));
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
        final List<String> fresh = programs.unseen();
        final String halt = over && state == ILanguageProcess.State.HALTED ? one.process().message() : null;
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
        final List<DesktopShellOutputPayload.WireLine> wire = new ArrayList<>();
        for (final String line : fresh) {
            wire.add(new DesktopShellOutputPayload.WireLine(line, CliStyle.PLAIN.id()));
        }
        if (halt != null) {
            wire.add(new DesktopShellOutputPayload.WireLine(halt, CliStyle.ERROR.id()));
        }
        say(viewers, wire, over ? this.machine.shellPrompt() : "", !over);
    }

    /**
     * Tells every console open on this machine how a source build is coming along, the full-screen prompt
     * and the desktop's terminal window alike. Looked at once a second, and it only speaks on a 25% step or
     * on a build finishing, the way emerge itself does.
     */
    void pushBuildProgress(final ServerLevel level) {
        final ComputerConsoleState console = this.machine.console();
        if (console == null || level.getGameTime() % 20 != 0) {
            return;
        }
        final long now = level.getGameTime();
        final List<DesktopShellOutputPayload.WireLine> wire = new ArrayList<>();
        final int dim = CliStyle.DIM.id();
        final int ok = CliStyle.OK.id();

        // Package builds (emerge): progress quarters while compiling.
        for (final Map.Entry<String, Long> entry : console.pendingBuilds().entrySet()) {
            final long total = console.buildTotal(entry.getKey());
            if (total <= 0 || entry.getValue() <= now) {
                continue; // completions are handled below
            }
            final long left = entry.getValue() - now;
            final int pct = (int) Math.max(0, Math.min(99, 100 - left * 100 / total));
            final int quarter = pct / 25;
            if (quarter >= 1 && quarter > this.buildQuarterReported.getOrDefault(entry.getKey(), 0)) {
                this.buildQuarterReported.put(entry.getKey(), quarter);
                wire.add(new DesktopShellOutputPayload.WireLine(">>> " + buildDisplayName(entry.getKey())
                        + ": compiling ... " + pct + "% (" + (left / 20) + "s left)", dim));
            }
        }

        // The Gentoo live install's kernel compile gets the same treatment.
        final LiveInstallState live = console.liveInstall();
        if (live != null && live.kernelCompiling(now)) {
            final long kernelTotal =
                    Math.max(5L, Math.min(1800L, 64_000L / Math.max(100, this.machine.maxCpuMhz()))) * 20L;
            final long left = live.kernelReadyAt() - now;
            final int pct = (int) Math.max(0, Math.min(99, 100 - left * 100 / Math.max(1L, kernelTotal)));
            final int quarter = pct / 25;
            if (quarter >= 1 && quarter > this.liveKernelQuarterReported) {
                this.liveKernelQuarterReported = quarter;
                wire.add(new DesktopShellOutputPayload.WireLine(
                        ">>> sys-kernel/gentoo-sources: compiling ... " + pct + "% ("
                                + (left / 20) + "s left)", dim));
            }
        } else if (live != null && live.kernelReadyAt() >= 0 && !live.kernelCompiling(now)
                && this.liveKernelQuarterReported > 0 && this.liveKernelQuarterReported < 4) {
            this.liveKernelQuarterReported = 4;
            wire.add(new DesktopShellOutputPayload.WireLine(
                    ">>> sys-kernel/gentoo-sources: compiled. Run 'genkernel all' to build the kernel.", ok));
        }

        /*
         * Completions: announced live to whoever is looking; with no console open the notice stays queued
         * for the shell to print ahead of the next command instead.
         */
        final List<ServerPlayer> viewers = this.machine.consoleViewers(level);
        if (!console.settleBuilds(now).isEmpty()) {
            this.machine.setChanged();
            if (!viewers.isEmpty()) {
                for (final String id : console.drainFinishedBuilds()) {
                    this.buildQuarterReported.remove(id);
                    wire.add(new DesktopShellOutputPayload.WireLine(
                            ">>> " + buildDisplayName(id) + ": build finished, package installed", ok));
                }
            }
        }
        if (wire.isEmpty() || viewers.isEmpty()) {
            return;
        }
        /*
         * Every reply says whether a program has the terminal, notices included: one that said otherwise
         * would hand the keyboard back while a program was still using it.
         */
        say(viewers, wire, "", this.machine.programs().held() != 0);
    }

    /**
     * The same said twice, once in each terminal's own words: a window on a desktop, and the prompt that is
     * the whole glass of a machine that has none. Both are watching this one console, so each viewer is sent
     * the one its own screen speaks.
     */
    private void say(final List<ServerPlayer> viewers, final List<DesktopShellOutputPayload.WireLine> wire,
                     final String prompt, final boolean holdsTerminal) {
        final var desktop = new DesktopShellOutputPayload(false, holdsTerminal, prompt, wire);
        final List<CommandOutputPayload.WireLine> promptWire = new ArrayList<>(wire.size());
        for (final var line : wire) {
            promptWire.add(new CommandOutputPayload.WireLine(line.text(), line.style()));
        }
        final var terminal = new CommandOutputPayload(false, prompt, promptWire);
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
    private record WindowId(int program, long window) {
    }

    /** One canvas in one window of one program on this machine. */
    private record CanvasId(WindowId window, long widget) {
    }

    /** What a player has of a canvas: the drawing it was on, and how many of its strokes have gone to them. */
    private record CanvasSent(int epoch, int strokes) {
    }

    /** What a program is called on a console line: its command name, or its id without the namespace. */
    private static String buildDisplayName(final String programId) {
        final ResourceLocation rl = ResourceLocation.tryParse(programId);
        final ProgramSpec spec = rl == null ? null : OsRegistry.getProgram(rl);
        return spec != null ? spec.commandName()
                : (programId.contains(":") ? programId.substring(programId.indexOf(':') + 1) : programId);
    }
}
