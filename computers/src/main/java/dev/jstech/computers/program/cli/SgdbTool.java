/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.machine.DebugSession;
import dev.jstech.computers.program.tty.ITtyProcess;
import dev.jstech.computers.program.tty.ITtySink;
import dev.jstech.computers.program.tty.TtyQuestion;
import dev.jstech.computers.vm.program.Debugger;
import dev.jstech.computers.vm.program.ProgramImage;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

/**
 * sgdb holding the terminal: it asks for a command at its own prompt while the program is stopped or not started, and
 * while the program runs it hands on what the program prints and, when the program asks for a line, what is typed.
 *
 * <p>Breakpoints are numbered here, in the order they were set, and placed on the listing before the program runs, so
 * a breakpoint can be set, said and listed before {@code run}, as in gdb. Ctrl+C stops a running program where it is;
 * at the prompt it leaves sgdb, ending the program, so the terminal is never kept by a debugger nobody can quit.
 */
final class SgdbTool implements ITtyProcess {

    private final ICliComputer computer;
    private final String path;
    private final ProgramImage image;
    private final List<String> arguments;
    /** The breakpoints, by the number each was given, in the order they were set. */
    private final Map<Integer, Debugger.Breakpoint> breakpoints = new LinkedHashMap<>();
    /** The lines of each source file read so far, to show the line the program stopped at. */
    private final Map<String, List<String>> sources = new HashMap<>();
    private int nextNumber = 1;
    /** The program being debugged, or null before it runs and after it ends. */
    private DebugSession session;
    /** Whether the program is running, rather than stopped at a line. */
    private boolean running;
    /** The call the program last stopped in, and how deep it was, which a stop in the same call does not name again. */
    private String lastMethod = "";
    private int lastDepth;
    private boolean over;

    private static final String PROMPT = "(sgdb) ";
    /** How wide the column of a line's number is, before the line itself. */
    private static final int NUMBER_WIDTH = 8;

    SgdbTool(final ICliComputer computer, final String path, final ProgramImage image,
             final List<String> arguments) {
        this.computer = computer;
        this.path = path;
        this.image = image;
        this.arguments = List.copyOf(arguments);
    }

    @Override
    public void begin(final long now) {
        // Nothing runs until it is told to.
    }

    @Override
    public void advance(final long now, @Nullable final ITtySink out) {
        if (this.session == null) {
            return;
        }
        for (final String line : this.session.output()) {
            print(out, CliLine.plain(line));
        }
        if (this.session.ended()) {
            say(out, this.session.halted() ? SgdbTexts.HALTED.with(this.name(), this.session.haltMessage())
                    : SgdbTexts.EXITED.with(this.name(), this.session.exitCode()));
            this.session = null;
            this.running = false;
            return;
        }
        if (this.running && this.session.debugger().paused()) {
            this.running = false;
            this.stopped(out);
        }
    }

    @Nullable
    @Override
    public TtyQuestion asking() {
        if (this.over) {
            return null;
        }
        if (this.running) {
            // A running program has the keyboard when it asks for a line, and nobody has it otherwise.
            return this.session != null && this.session.waitingForInput()
                    ? new TtyQuestion(CliLine.plain(""), false) : null;
        }
        return new TtyQuestion(new CliLine(PROMPT, CliStyle.WARN), false);
    }

    @Override
    public void answer(final String line, final long now, @Nullable final ITtySink out) {
        if (this.over) {
            return;
        }
        if (this.running) {
            if (this.session != null && this.session.waitingForInput()) {
                this.session.offerInput(line);
            }
            return;
        }
        this.command(line.trim(), out);
    }

    @Override
    public void interrupt(final long now, @Nullable final ITtySink out) {
        if (this.running && this.session != null) {
            this.session.debugger().interrupt();
            print(out, CliLine.plain("^C"));
            return;
        }
        this.endProgram();
        this.over = true;
    }

    @Override
    public boolean over() {
        return this.over;
    }

    private void command(final String line, @Nullable final ITtySink out) {
        final String verb = line.split("\\s+", 2)[0];
        final String rest = line.substring(verb.length()).trim();
        switch (verb) {
            case "" -> { }
            case "help", "h" -> {
                say(out, SgdbTexts.HELP_HEAD.text());
                SgdbTexts.HELP.forEach(each -> say(out, each.text()));
            }
            case "break", "b", "br" -> this.setBreakpoint(rest, out);
            case "delete", "d" -> this.delete(rest, out);
            case "info", "i" -> this.info(line, rest, out);
            case "run", "r" -> this.run(out);
            case "continue", "c" -> this.go(out, Debugger::resume);
            case "step", "s" -> this.go(out, Debugger::stepInto);
            case "next", "n" -> this.go(out, Debugger::stepOver);
            case "finish", "fin" -> this.finish(out);
            case "print", "p" -> this.print(rest, out);
            case "backtrace", "bt", "where" -> this.backtrace(out);
            case "kill", "k" -> {
                if (this.session == null) {
                    say(out, SgdbTexts.NOT_RUNNING.text());
                } else {
                    this.endProgram();
                    say(out, SgdbTexts.KILLED.with(this.name()));
                }
            }
            case "quit", "q" -> {
                this.endProgram();
                this.over = true;
            }
            default -> say(out, SgdbTexts.UNDEFINED.with(verb));
        }
    }

    /* What info shows, named by {@code what}; a name it does not know is answered with the whole line typed. */
    private void info(final String typed, final String what, @Nullable final ITtySink out) {
        switch (what) {
            case "breakpoints", "break", "b" -> {
                if (this.breakpoints.isEmpty()) {
                    say(out, SgdbTexts.NO_BREAKPOINTS.text());
                    return;
                }
                say(out, SgdbTexts.BREAKPOINTS_HEAD.text());
                this.breakpoints.forEach((number, at) -> print(out,
                        CliLine.plain(pad(String.valueOf(number), 5) + at.file() + ":" + at.line())));
            }
            case "locals" -> this.variables(out, false);
            case "args" -> this.variables(out, true);
            case "credits" -> {
                if (this.session == null) {
                    say(out, SgdbTexts.NOT_RUNNING.text());
                } else {
                    say(out, SgdbTexts.CREDITS.with(this.session.debugger().spent()));
                }
            }
            default -> say(out, SgdbTexts.UNDEFINED.with(typed));
        }
    }

    /* "11", or "Plant.sgs:11": a line of the file the program starts in, or of the file named. */
    private void setBreakpoint(final String where, @Nullable final ITtySink out) {
        final int colon = where.lastIndexOf(':');
        final String file = colon < 0 ? Debugger.startFile(this.image) : where.substring(0, colon).trim();
        final String number = colon < 0 ? where : where.substring(colon + 1).trim();
        int line;
        try {
            line = Integer.parseInt(number);
        } catch (final NumberFormatException notALine) {
            line = -1;
        }
        final Optional<Debugger.Breakpoint> landed = line < 1 ? Optional.empty()
                : Debugger.landing(this.image, file, line);
        if (landed.isEmpty()) {
            say(out, SgdbTexts.NO_LINE.with(number, file));
            return;
        }
        final Debugger.Breakpoint at = landed.get();
        final int given = this.nextNumber++;
        this.breakpoints.put(given, at);
        if (this.session != null) {
            this.session.debugger().breakAt(at.file(), at.line());
        }
        say(out, SgdbTexts.BREAKPOINT_SET.with(given, at.file(), at.line()));
    }

    private void delete(final String which, @Nullable final ITtySink out) {
        if (which.isEmpty()) {
            this.breakpoints.clear();
            if (this.session != null) {
                this.session.debugger().deleteAll();
            }
            return;
        }
        Debugger.Breakpoint gone = null;
        try {
            gone = this.breakpoints.remove(Integer.parseInt(which));
        } catch (final NumberFormatException notANumber) {
            // Said below, as a number that names no breakpoint.
        }
        if (gone == null) {
            say(out, SgdbTexts.NO_SUCH_BREAKPOINT.with(which));
            return;
        }
        // Another breakpoint on the same line keeps the program stopping there.
        if (this.session != null && !this.breakpoints.containsValue(gone)) {
            this.session.debugger().delete(gone);
        }
    }

    private void run(@Nullable final ITtySink out) {
        if (this.session != null) {
            say(out, SgdbTexts.AGAIN.text());
            this.endProgram();
        }
        final DebugSession.Started started = this.computer.debugSigma(this.path, this.arguments);
        if (started == null || !started.ok()) {
            say(out, started == null ? ICliProcesses.ProcessWords.NOT_INSTALLED.text() : started.refusal());
            return;
        }
        this.session = started.session();
        for (final Debugger.Breakpoint at : this.breakpoints.values()) {
            this.session.debugger().breakAt(at.file(), at.line());
        }
        this.running = true;
        this.lastMethod = "";
        this.lastDepth = 0;
    }

    private void go(@Nullable final ITtySink out, final Consumer<Debugger> how) {
        if (this.session == null) {
            say(out, SgdbTexts.NOT_RUNNING.text());
            return;
        }
        how.accept(this.session.debugger());
        this.running = true;
    }

    private void finish(@Nullable final ITtySink out) {
        if (this.session == null) {
            say(out, SgdbTexts.NOT_RUNNING.text());
            return;
        }
        final List<Debugger.Place> stack = this.session.debugger().stack();
        if (!stack.isEmpty()) {
            final Debugger.Place top = stack.getFirst();
            say(out, SgdbTexts.RUN_TILL.with("#0 " + top.method(), arguments(top), top.file(), top.line()));
        }
        this.go(out, Debugger::stepOut);
    }

    private void print(final String name, @Nullable final ITtySink out) {
        if (this.session == null) {
            say(out, SgdbTexts.NO_STACK.text());
            return;
        }
        final Optional<Debugger.Variable> found = this.session.debugger().find(name, 0);
        if (found.isEmpty()) {
            say(out, SgdbTexts.NO_SYMBOL.with(name));
            return;
        }
        print(out, CliLine.plain(shown(found.get())));
    }

    private void variables(@Nullable final ITtySink out, final boolean argumentsOnly) {
        if (this.session == null) {
            say(out, SgdbTexts.NO_STACK.text());
            return;
        }
        final Debugger debugger = this.session.debugger();
        final List<Debugger.Place> stack = debugger.stack();
        final List<Debugger.Variable> shown = argumentsOnly
                ? (stack.isEmpty() ? List.of() : stack.getFirst().arguments()) : debugger.locals(0);
        if (shown.isEmpty()) {
            say(out, argumentsOnly ? SgdbTexts.NO_ARGUMENTS.text() : SgdbTexts.NO_LOCALS.text());
            return;
        }
        shown.forEach(each -> print(out, CliLine.plain(shown(each))));
    }

    private void backtrace(@Nullable final ITtySink out) {
        final List<Debugger.Place> stack = this.session == null ? List.of() : this.session.debugger().stack();
        if (stack.isEmpty()) {
            say(out, SgdbTexts.NO_STACK.text());
            return;
        }
        for (int i = 0; i < stack.size(); i++) {
            final Debugger.Place place = stack.get(i);
            say(out, SgdbTexts.FRAME.with(pad("#" + i, 4) + place.method(), arguments(place), place.file(),
                    place.line()));
        }
    }

    /* Says where the program stopped: why, when the call is not the one it last stopped in, and the line itself. */
    private void stopped(@Nullable final ITtySink out) {
        final Debugger debugger = this.session.debugger();
        final List<Debugger.Place> stack = debugger.stack();
        if (stack.isEmpty()) {
            say(out, SgdbTexts.STOPPED.text());
            return;
        }
        final Debugger.Place top = stack.getFirst();
        final boolean moved = !top.method().equals(this.lastMethod) || stack.size() != this.lastDepth;
        switch (debugger.stop()) {
            case BREAKPOINT -> say(out, SgdbTexts.HIT.with(this.numberOf(top), top.method(), arguments(top),
                    top.file(), top.line()));
            case INTERRUPTED -> {
                say(out, SgdbTexts.STOPPED.text());
                say(out, SgdbTexts.FRAME.with(top.method(), arguments(top), top.file(), top.line()));
            }
            case STEP -> {
                if (moved) {
                    say(out, SgdbTexts.FRAME.with(top.method(), arguments(top), top.file(), top.line()));
                }
            }
            case null -> { }
        }
        print(out, CliLine.plain(pad(String.valueOf(top.line()), NUMBER_WIDTH) + this.sourceLine(top)));
        this.lastMethod = top.method();
        this.lastDepth = stack.size();
    }

    /* The number of the breakpoint the program stopped at, the first one set on that line. */
    private int numberOf(final Debugger.Place at) {
        for (final Map.Entry<Integer, Debugger.Breakpoint> each : this.breakpoints.entrySet()) {
            if (each.getValue().file().equals(at.file()) && each.getValue().line() == at.line()) {
                return each.getKey();
            }
        }
        return 0;
    }

    /*
     * The text of the line the program stopped at, read from the source beside the listing, which is where a build
     * writes it; nothing when the source is not there.
     */
    private String sourceLine(final Debugger.Place at) {
        final List<String> lines = this.sources.computeIfAbsent(at.file(), this::readSource);
        return at.line() >= 1 && at.line() <= lines.size() ? lines.get(at.line() - 1).stripTrailing() : "";
    }

    private List<String> readSource(final String file) {
        final int slash = Math.max(this.path.lastIndexOf('/'), this.path.lastIndexOf('\\'));
        final int fileSlash = Math.max(file.lastIndexOf('/'), file.lastIndexOf('\\'));
        final String beside = (slash < 0 ? "" : this.path.substring(0, slash + 1)) + file.substring(fileSlash + 1);
        final ICliComputer.FsResult read = this.computer.readFile(beside);
        return read.ok() ? new ArrayList<>(List.of(read.message().english().split("\n", -1))) : List.of();
    }

    private void endProgram() {
        if (this.session != null) {
            this.session.end();
        }
        this.session = null;
        this.running = false;
    }

    /* The listing's own name, which the program's end is said under. */
    private String name() {
        final int slash = Math.max(this.path.lastIndexOf('/'), this.path.lastIndexOf('\\'));
        return this.path.substring(slash + 1);
    }

    /* What a call was handed, as gdb writes it: name=value, one after another. */
    private static String arguments(final Debugger.Place place) {
        final List<String> handed = new ArrayList<>();
        for (final Debugger.Variable each : place.arguments()) {
            handed.add(each.name() + "=" + each.value());
        }
        return String.join(", ", handed);
    }

    private static String shown(final Debugger.Variable variable) {
        return variable.name() + " = " + variable.value() + " (" + variable.type() + ")";
    }

    private static String pad(final String text, final int width) {
        return text.length() >= width ? text + " " : text + " ".repeat(width - text.length());
    }

    private static void say(@Nullable final ITtySink out, final Text text) {
        print(out, CliLine.plain(text));
    }

    private static void print(@Nullable final ITtySink out, final CliLine line) {
        if (out != null) {
            out.line(line);
        }
    }
}
