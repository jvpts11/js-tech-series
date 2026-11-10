/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.program.DebugRequestPayload;
import dev.jstech.computers.operation.payload.program.DebugStatePayload;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The debugger of a studio, on the client: the breakpoints set in its files, the names it watches, and what the machine
 * last said of the program it is debugging. Virtual Studio and Virtual Studio Code each keep one and draw it their own
 * way; what it holds and what it asks the machine is the same for both.
 *
 * <p>The program runs on the machine. What comes back is kept here: what the program printed, the events the
 * Diagnostic Tools list, and the state of the last stop, which the editor marks and the panes show.
 */
public final class StudioDebug {

    private final BlockPos host;
    /** The breakpoints, by the file on the disk they are in, each a line counted from one. */
    private final Map<String, TreeSet<Integer>> breakpoints = new LinkedHashMap<>();
    private final List<String> watches = new ArrayList<>();
    private final List<String> output = new ArrayList<>();
    private final List<String> events = new ArrayList<>();
    /** What the machine last said, or null before anything was debugged. */
    private DebugStatePayload state;
    /** The listing being debugged, to start it again, and the name it is shown under. */
    private String listing = "";
    private String name = "";

    /** The most lines of output and of events kept, the oldest going first. */
    private static final int MOST_KEPT = 200;

    /** The debugger the machine's answers go to: the one that started the program being debugged. */
    private static StudioDebug listening;

    public StudioDebug(final BlockPos host) {
        this.host = host.immutable();
    }

    /** Hands a state the machine sent to the debugger it belongs to. */
    public static void deliver(final DebugStatePayload state) {
        if (listening != null && listening.host.equals(state.hostPos())) {
            listening.take(state);
        }
    }

    /** Whether a program is being debugged and has not ended. */
    public boolean active() {
        return this.state != null && this.state.live();
    }

    /** Whether the program being debugged is stopped. */
    public boolean paused() {
        return this.state != null && this.state.status() == DebugStatePayload.PAUSED;
    }

    /** What the machine last said, or null. */
    public DebugStatePayload state() {
        return this.state;
    }

    /** The name the program being debugged is shown under. */
    public String name() {
        return this.name;
    }

    /** What the program printed, oldest first. */
    public List<String> output() {
        return List.copyOf(this.output);
    }

    /** What happened while debugging, oldest first: lines printed, stops, the end. */
    public List<String> events() {
        return List.copyOf(this.events);
    }

    /** The names watched, in the order they were added. */
    public List<String> watches() {
        return List.copyOf(this.watches);
    }

    /**
     * Starts the listing at {@code listing}, a path on the machine's disk, under the debugger, with every breakpoint
     * set in the studio's files.
     */
    public void start(final String listing, final String name) {
        this.listing = listing;
        this.name = name;
        this.output.clear();
        this.events.clear();
        listening = this;
        PacketDistributor.sendToServer(new DebugRequestPayload(this.host, DebugRequestPayload.START,
                "/" + listing.replace('\\', '/'), this.written()));
        if (!this.watches.isEmpty()) {
            this.sendWatches();
        }
    }

    /** Starts the program again from the beginning. */
    public void restart() {
        if (!this.listing.isEmpty()) {
            this.start(this.listing, this.name);
        }
    }

    public void resume() {
        this.ask(DebugRequestPayload.CONTINUE);
    }

    public void stepInto() {
        this.ask(DebugRequestPayload.STEP_INTO);
    }

    public void stepOver() {
        this.ask(DebugRequestPayload.STEP_OVER);
    }

    public void stepOut() {
        this.ask(DebugRequestPayload.STEP_OUT);
    }

    public void pause() {
        this.ask(DebugRequestPayload.PAUSE);
    }

    /** Ends the program being debugged, at once. */
    public void stop() {
        if (this.active()) {
            PacketDistributor.sendToServer(DebugRequestPayload.of(this.host, DebugRequestPayload.STOP));
        }
    }

    /** Hands the program a typed line, when it waits for one. */
    public void input(final String line) {
        if (this.active() && this.state.waiting()) {
            PacketDistributor.sendToServer(new DebugRequestPayload(this.host, DebugRequestPayload.INPUT, "",
                    List.of(line)));
        }
    }

    /** Lets the machine go: the program being debugged ends with the window that debugged it. */
    public void close() {
        this.stop();
        if (listening == this) {
            listening = null;
        }
    }

    /**
     * The keys of the debugger, the same in both studios: F5 starts debugging or goes on, Ctrl+F5 runs without the
     * debugger, Shift+F5 stops and Ctrl+Shift+F5 starts again, F9 sets a breakpoint, F10 steps over, F11 steps into
     * and Shift+F11 steps out. Says whether the key was one of them.
     */
    public boolean keyPressed(final int key, final int modifiers, final Runnable start, final Runnable startWithout,
                              final Runnable toggle) {
        final boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        final boolean control = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        switch (key) {
            case GLFW.GLFW_KEY_F5 -> {
                if (shift && control) {
                    this.restart();
                } else if (shift) {
                    this.stop();
                } else if (control) {
                    startWithout.run();
                } else if (this.active()) {
                    this.resume();
                } else {
                    start.run();
                }
            }
            case GLFW.GLFW_KEY_F9 -> toggle.run();
            case GLFW.GLFW_KEY_F10 -> this.stepOver();
            case GLFW.GLFW_KEY_F11 -> {
                if (shift) {
                    this.stepOut();
                } else {
                    this.stepInto();
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Sets or takes away the breakpoint on a line of a file, and says whether one is there now. */
    public boolean toggle(final String path, final int line) {
        final TreeSet<Integer> lines = this.breakpoints.computeIfAbsent(path, ignored -> new TreeSet<>());
        final boolean set = lines.add(line) || !lines.remove(line);
        if (lines.isEmpty()) {
            this.breakpoints.remove(path);
        }
        if (this.active()) {
            PacketDistributor.sendToServer(new DebugRequestPayload(this.host, DebugRequestPayload.BREAKPOINTS, "",
                    this.written()));
        }
        return set;
    }

    /** Takes every breakpoint away. */
    public void deleteAll() {
        this.breakpoints.clear();
        if (this.active()) {
            PacketDistributor.sendToServer(new DebugRequestPayload(this.host, DebugRequestPayload.BREAKPOINTS, "",
                    List.of()));
        }
    }

    /** The lines of a file a breakpoint stands on. */
    public Set<Integer> breakpointsIn(final String path) {
        final TreeSet<Integer> lines = this.breakpoints.get(path);
        return lines == null ? Set.of() : Set.copyOf(lines);
    }

    /** Every breakpoint, as a file and a line, in the order of the files. */
    public List<Map.Entry<String, Integer>> allBreakpoints() {
        final List<Map.Entry<String, Integer>> all = new ArrayList<>();
        this.breakpoints.forEach((path, lines) -> lines.forEach(line -> all.add(Map.entry(path, line))));
        return all;
    }

    /** The line of a file the program is stopped at, or 0 when it is not stopped there. */
    public int stoppedLineIn(final String path) {
        if (!this.paused() || this.state.stack().isEmpty()) {
            return 0;
        }
        final DebugStatePayload.Frame top = this.state.stack().getFirst();
        return sameFile(path, top.file()) ? top.line() : 0;
    }

    /** Adds a name to watch, which is shown with what it holds whenever the program stops. */
    public void addWatch(final String watched) {
        final String trimmed = watched.trim();
        if (!trimmed.isEmpty() && !this.watches.contains(trimmed)) {
            this.watches.add(trimmed);
            this.sendWatches();
        }
    }

    /** Takes a watched name away. */
    public void removeWatch(final int index) {
        if (index >= 0 && index < this.watches.size()) {
            this.watches.remove(index);
            this.sendWatches();
        }
    }

    /** Whether a file on the disk is the source a listing names: the same, or the disk's path ends with it. */
    public static boolean sameFile(final String path, final String source) {
        if (path == null || source == null || source.isEmpty()) {
            return false;
        }
        final String disk = path.replace('\\', '/');
        final String named = source.replace('\\', '/');
        return disk.equalsIgnoreCase(named)
                || disk.toLowerCase(Locale.ROOT).endsWith("/" + named.toLowerCase(Locale.ROOT));
    }

    private void ask(final int action) {
        if (this.active()) {
            PacketDistributor.sendToServer(DebugRequestPayload.of(this.host, action));
        }
    }

    private void sendWatches() {
        if (this.active()) {
            PacketDistributor.sendToServer(new DebugRequestPayload(this.host, DebugRequestPayload.WATCHES, "",
                    List.copyOf(this.watches)));
        }
    }

    /* The breakpoints as a request carries them: "path:line", the path being the file's on the disk. */
    private List<String> written() {
        final List<String> all = new ArrayList<>();
        this.breakpoints.forEach((path, lines) -> lines.forEach(line -> all.add(path + ":" + line)));
        return all.subList(0, Math.min(all.size(), DebugRequestPayload.MOST_LINES));
    }

    /* Keeps what came: the lines printed and what happened, which the Diagnostic Tools and the consoles list. */
    private void take(final DebugStatePayload next) {
        final boolean wasPaused = this.paused();
        for (final String line : next.output()) {
            keep(this.output, line);
            keep(this.events, GameText.resolve(DebugTexts.EVENT_OUTPUT.with(line)));
        }
        if (next.status() == DebugStatePayload.PAUSED && !wasPaused && !next.stack().isEmpty()) {
            final DebugStatePayload.Frame top = next.stack().getFirst();
            keep(this.events, GameText.resolve(switch (next.reason()) {
                case "breakpoint" -> DebugTexts.EVENT_BREAKPOINT.with(top.line());
                case "interrupted" -> DebugTexts.EVENT_BREAK.with(top.line());
                default -> DebugTexts.EVENT_STEP.with(top.line());
            }));
            // The console says where it stopped when it is a breakpoint, or a call it was not in before.
            final boolean entered = this.state != null && !this.state.stack().isEmpty()
                    && next.stack().size() > this.state.stack().size();
            if ("breakpoint".equals(next.reason())) {
                keep(this.output, GameText.resolve(DebugTexts.HIT.with(leaf(top.file()), top.line())));
            } else if (entered) {
                keep(this.output, GameText.resolve(DebugTexts.STEPPED_INTO.with(top.method())));
            }
        }
        if (next.status() == DebugStatePayload.ENDED && this.state != null && this.state.live()) {
            keep(this.output, GameText.resolve(next.halt().english().isEmpty()
                    ? DebugTexts.EXITED.with(this.name, next.exitCode())
                    : DebugTexts.HALTED.with(this.name, next.halt())));
        } else if (next.status() == DebugStatePayload.ENDED && !next.halt().english().isEmpty()) {
            keep(this.output, GameText.resolve(DebugTexts.NOT_STARTED.with(next.halt())));
        }
        this.state = next;
    }

    private static String leaf(final String file) {
        return file.substring(Math.max(file.lastIndexOf('/'), file.lastIndexOf('\\')) + 1);
    }

    private static void keep(final List<String> lines, final String line) {
        lines.add(line);
        if (lines.size() > MOST_KEPT) {
            lines.removeFirst();
        }
    }
}
