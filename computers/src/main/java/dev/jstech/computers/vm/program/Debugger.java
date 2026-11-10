/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.listing.AsmMethod;
import dev.jstech.computers.vm.listing.IOperand;
import dev.jstech.computers.vm.listing.Instruction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A debugger attached to one process: where it stops, why it stopped there, and what it shows while it is stopped.
 *
 * <p>It stops at a breakpoint, which is a line of a source file, the first time the program comes to the start of that
 * line; after a step, at the next line the program comes to (into a call, over it, or out of the one it is in); and
 * where it is when it is interrupted. While it is stopped the whole process is: no thread of it runs and nothing is
 * spent. Stepping spends what running spends, no more and no less, since a step is the program running until it
 * reaches the next line (computers/docs/design/sigma.md).
 *
 * <p>Lines and variables come from a build for the debugger, which writes the line and the column of every statement
 * and where each variable the source named lives. A listing built without them runs under the debugger all the same,
 * but has no lines to stop at and no names to show.
 *
 * <p>Nothing here costs a program that is not being debugged anything: the runtime asks this only when one is attached.
 */
public final class Debugger {

    private final Process process;
    /** The lines of each file a breakpoint stands on. */
    private final Map<String, Set<Integer>> lines = new HashMap<>();
    /** What the calls the program made cost beyond the instruction that made them, by the call's name. */
    private final Map<String, Long> calls = new LinkedHashMap<>();
    /** Why the program is stopped, or null while it runs. */
    private Reason stop;
    /** The thread it stopped in, whose calls are the ones shown. */
    private ProgramThread stopped;
    /** The step under way, or null. */
    private Step step;
    /** The thread that goes on from where it stopped, which does not stop again before it has moved. */
    private ProgramThread leaving;

    Debugger(final Process process) {
        this.process = process;
    }

    /** Whether the program is stopped. */
    public boolean paused() {
        return this.stop != null;
    }

    /** Why the program is stopped, or null while it runs. */
    public Reason stop() {
        return this.stop;
    }

    /**
     * Where a breakpoint asked for on a line of a file of a program lands: on that line, or on the first line after it
     * that has code, since a blank line or a comment is nothing a program comes to. A file named without its folders
     * is any file of that name. A debugger front end asks this before the program starts, to say where it will stop.
     *
     * @return the breakpoint, at the file as the listing names it and the line it landed on, or empty when the file has
     *         no code at or after that line
     */
    public static Optional<Breakpoint> landing(final ProgramImage program, final String file, final int line) {
        final Set<Integer> coded = new HashSet<>();
        String found = null;
        for (final TypeImage type : program.types()) {
            if (!sameFile(type.source(), file)) {
                continue;
            }
            found = found == null ? type.source() : found;
            for (final MethodImage method : type.methods().values()) {
                for (int i = 0; i < method.length(); i++) {
                    coded.add(method.positionOf(i));
                }
            }
        }
        int landed = -1;
        for (final int each : coded) {
            if (each >= line && (landed < 0 || each < landed)) {
                landed = each;
            }
        }
        return found == null || landed < 0 ? Optional.empty() : Optional.of(new Breakpoint(found, landed));
    }

    /** The file a program starts in, which a breakpoint named by its line alone is set in; empty with no lines. */
    public static String startFile(final ProgramImage program) {
        final String start = program.entryPoint();
        final TypeImage entry = start == null ? null : program.type(start);
        if (entry != null && !entry.source().isEmpty()) {
            return entry.source();
        }
        for (final TypeImage type : program.types()) {
            if (!type.source().isEmpty()) {
                return type.source();
            }
        }
        return "";
    }

    /** Whether a listing was built for the debugger, with the columns and the names it shows. */
    public static boolean hasLines(final ProgramImage program) {
        for (final TypeImage type : program.types()) {
            for (final MethodImage method : type.methods().values()) {
                if (!method.locals().isEmpty()) {
                    return true;
                }
                for (int i = 0; i < method.length(); i++) {
                    if (method.columnOf(i) > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Sets a breakpoint, as {@link #landing} places it, and says where it landed. */
    public Optional<Breakpoint> breakAt(final String file, final int line) {
        final Optional<Breakpoint> landed = landing(this.process.program(), file, line);
        landed.ifPresent(at -> this.lines.computeIfAbsent(at.file(), ignored -> new HashSet<>()).add(at.line()));
        return landed;
    }

    /** Takes a breakpoint away, and says whether there was one there. */
    public boolean delete(final Breakpoint breakpoint) {
        final Set<Integer> here = this.lines.get(breakpoint.file());
        return here != null && here.remove(breakpoint.line());
    }

    /** Takes every breakpoint away. */
    public void deleteAll() {
        this.lines.clear();
    }

    /** Goes on from where the program stopped, until a breakpoint or its end. */
    public void resume() {
        this.leave(null);
    }

    /** Goes on to the next line the program comes to, stepping into a call. */
    public void stepInto() {
        this.leave(StepKind.INTO);
    }

    /** Goes on to the next line of this call, running a call made on the way all the way through. */
    public void stepOver() {
        this.leave(StepKind.OVER);
    }

    /** Goes on until the call the program stopped in has returned. */
    public void stepOut() {
        this.leave(StepKind.OUT);
    }

    /**
     * Stops the program where it is, at once: in the middle of a line, or in a call it is waiting in, the way a real
     * debugger stops a program it is told to.
     */
    public void interrupt() {
        if (this.stop != null) {
            return;
        }
        ProgramThread at = this.process.mainThread();
        if (at.frames.isEmpty()) {
            for (final ProgramThread each : this.process.threads0()) {
                if (!each.frames.isEmpty()) {
                    at = each;
                    break;
                }
            }
        }
        this.halt(at, Reason.INTERRUPTED);
    }

    /**
     * The calls of the thread the program stopped in, the one it stopped in first: what each is, the file and the
     * line it is at, and what it was handed. Empty while the program runs.
     */
    public List<Place> stack() {
        if (this.stopped == null) {
            return List.of();
        }
        final List<Place> places = new ArrayList<>();
        boolean top = true;
        for (final Frame frame : this.stopped.frames) {
            // The call it stopped in is at the line it is about to run; a caller, at the call it made.
            final int at = top ? frame.at : Math.max(0, frame.at - 1);
            places.add(new Place(display(frame.method), this.fileOf(frame.method), frame.method.positionOf(at),
                    frame.method.columnOf(at), this.variables(frame, true)));
            top = false;
        }
        return places;
    }

    /** The variables of call {@code depth} of the stack, 0 the one it stopped in: its parameters, then its locals. */
    public List<Variable> locals(final int depth) {
        final Frame frame = this.frameAt(depth);
        return frame == null ? List.of() : this.variables(frame, false);
    }

    /**
     * What a name means in call {@code depth} of the stack, or in the first of its callers that has it: a parameter or
     * a local, then a field of the object the call runs on, then a static field of its type. A name may go on into an
     * object's fields with dots ({@code gate.Name}).
     */
    public Optional<Variable> find(final String path, final int depth) {
        final String[] parts = path.trim().split("\\.");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return Optional.empty();
        }
        for (int d = Math.max(0, depth); this.frameAt(d) != null; d++) {
            final Variable found = this.lookUp(this.frameAt(d), parts);
            if (found != null) {
                return Optional.of(found);
            }
        }
        return Optional.empty();
    }

    /** Every credit the program has spent since it started. */
    public long spent() {
        return this.process.spent();
    }

    /** What the program's calls cost beyond the instructions that made them, by the call's name. */
    public Map<String, Long> calls() {
        return Map.copyOf(this.calls);
    }

    /** What its instructions themselves cost: everything it spent that no call did. */
    public long steps() {
        long byCalls = 0;
        for (final long each : this.calls.values()) {
            byCalls += each;
        }
        return Math.max(0, this.spent() - byCalls);
    }

    /**
     * Asked before the next instruction of a thread runs: whether the program stops there. A thread that was just let
     * go from where it stopped runs one instruction before it can stop again, so it moves at all.
     */
    boolean stops(final ProgramThread thread) {
        if (this.stop != null) {
            return true;
        }
        final Frame top = thread.frames.peek();
        if (top == null) {
            return false;
        }
        if (this.leaving == thread) {
            this.leaving = null;
            return false;
        }
        if (this.lines.isEmpty() && this.step == null) {
            return false;
        }
        final int line = top.method.positionOf(top.at);
        if (line <= 0) {
            return false;
        }
        final boolean starts = top.at == 0 || top.method.positionOf(top.at - 1) != line;
        if (starts && !this.lines.isEmpty()) {
            final Set<Integer> here = this.lines.get(this.fileOf(top.method));
            if (here != null && here.contains(line)) {
                this.halt(thread, Reason.BREAKPOINT);
                return true;
            }
        }
        if (this.step != null && this.step.thread() == thread && this.stepDone(thread, top, line)) {
            this.halt(thread, Reason.STEP);
            return true;
        }
        return false;
    }

    /** Puts what an instruction cost beyond itself down to the call it made, when it made one. */
    void charged(final Instruction instruction, final int credits) {
        if (credits <= 0) {
            return;
        }
        final String name = instruction != null && instruction.operand() instanceof IOperand.Method method
                ? DebugValues.shortType(method.owner().value()) + "." + method.name() : "";
        if (!name.isEmpty()) {
            this.calls.merge(name, (long) credits, Long::sum);
        }
    }

    private void leave(final StepKind kind) {
        if (this.stop == null) {
            return;
        }
        final ProgramThread from = this.stopped;
        final Frame top = from.frames.peek();
        this.step = kind == null || top == null ? null
                : new Step(kind, from, top, from.frames.size(), top.method.positionOf(top.at));
        this.leaving = from;
        this.stop = null;
        this.stopped = null;
    }

    private void halt(final ProgramThread thread, final Reason reason) {
        this.stop = reason;
        this.stopped = thread;
        this.step = null;
        this.leaving = null;
    }

    private boolean stepDone(final ProgramThread thread, final Frame top, final int line) {
        final int depth = thread.frames.size();
        final boolean moved = top != this.step.frame() || line != this.step.line();
        return switch (this.step.kind()) {
            case INTO -> moved;
            case OVER -> depth < this.step.depth() || (depth == this.step.depth() && moved);
            case OUT -> depth < this.step.depth();
        };
    }

    private Frame frameAt(final int depth) {
        if (this.stopped == null || depth < 0) {
            return null;
        }
        int at = 0;
        for (final Frame frame : this.stopped.frames) {
            if (at++ == depth) {
                return frame;
            }
        }
        return null;
    }

    private String fileOf(final MethodImage method) {
        final TypeImage type = this.process.program().type(method.owner());
        return type == null ? "" : type.source();
    }

    /* The named variables of a call, or only its parameters; with the object it runs on first, as "this". */
    private List<Variable> variables(final Frame frame, final boolean parametersOnly) {
        final List<Variable> found = new ArrayList<>();
        if (!parametersOnly && frame.self != null && !frame.method.isStatic()) {
            found.add(new Variable("this", DebugValues.shortType(frame.method.owner()),
                    DebugValues.describe(frame.self, frame.method.owner())));
        }
        for (final AsmMethod.Local local : frame.method.locals()) {
            if (!parametersOnly || local.parameter()) {
                found.add(new Variable(local.name(), DebugValues.shortType(local.type()),
                        DebugValues.describe(this.read(frame, local), local.type())));
            }
        }
        return found;
    }

    /* What a variable holds now, wherever it lives; one in raw memory out of reach is shown as its address. */
    private Object read(final Frame frame, final AsmMethod.Local local) {
        return switch (local.where()) {
            case SLOT -> local.slot() < frame.slots.length ? frame.slots[local.slot()] : null;
            case KEPT -> {
                final Object holder = local.slot() < 0 ? frame.self
                        : local.slot() < frame.slots.length ? frame.slots[local.slot()] : null;
                yield holder instanceof Values.Obj shared ? shared.get(local.field()) : null;
            }
            case MEMORY -> this.readMemory(frame, local);
        };
    }

    private Object readMemory(final Frame frame, final AsmMethod.Local local) {
        final Object address = local.slot() < frame.slots.length ? frame.slots[local.slot()] : null;
        if (!(address instanceof Number at)) {
            return null;
        }
        final RawMemory.Width width = widthOf(local.type());
        if (width == null) {
            return at.longValue();
        }
        try {
            return this.process.memory().read(at.longValue(), width, 0);
        } catch (final Halt unreachable) {
            return at.longValue();
        }
    }

    private Variable lookUp(final Frame frame, final String[] parts) {
        Object value = null;
        String type = null;
        boolean known = false;
        if ("this".equals(parts[0]) && frame.self != null) {
            value = frame.self;
            type = frame.method.owner();
            known = true;
        }
        for (final AsmMethod.Local local : frame.method.locals()) {
            if (!known && local.name().equals(parts[0])) {
                value = this.read(frame, local);
                type = local.type();
                known = true;
            }
        }
        if (!known && frame.self instanceof Values.Obj self && self.all().containsKey(parts[0])) {
            value = self.get(parts[0]);
            known = true;
        }
        if (!known) {
            final Values.Obj statics = this.process.staticsOf(frame.method.owner());
            if (statics != null && statics.all().containsKey(parts[0])) {
                value = statics.get(parts[0]);
                known = true;
            }
        }
        if (!known) {
            return null;
        }
        for (int i = 1; i < parts.length; i++) {
            if (!(value instanceof Values.Obj object) || !object.all().containsKey(parts[i])) {
                return null;
            }
            value = object.get(parts[i]);
            type = null;
        }
        final String shownType = type != null ? type : typeOf(value);
        return new Variable(String.join(".", parts), DebugValues.shortType(shownType),
                DebugValues.describe(value, shownType));
    }

    /* The name of a method as a stack shows it: its type's own name, then its name. */
    private static String display(final MethodImage method) {
        return DebugValues.shortType(method.owner()) + "." + method.name();
    }

    /* The width a value of a type takes in raw memory, or null for a type read as its address. */
    private static RawMemory.Width widthOf(final String type) {
        if (type.endsWith("*")) {
            return RawMemory.Width.LONG;
        }
        return switch (type) {
            case "byte", "bool" -> RawMemory.Width.BYTE;
            case "char" -> RawMemory.Width.CHAR;
            case "int" -> RawMemory.Width.INT;
            case "long" -> RawMemory.Width.LONG;
            case "float" -> RawMemory.Width.FLOAT;
            case "double" -> RawMemory.Width.DOUBLE;
            default -> null;
        };
    }

    /* The type a value has, for one reached through an object's field, where no declaration says it. */
    private static String typeOf(final Object value) {
        if (value instanceof Values.Obj object) {
            return object.type();
        }
        if (value instanceof String) {
            return "string";
        }
        if (value instanceof Boolean) {
            return "bool";
        }
        if (value instanceof Double || value instanceof Float) {
            return "double";
        }
        if (value instanceof Long) {
            return "long";
        }
        if (value instanceof Number) {
            return "int";
        }
        return "";
    }

    /* Whether a type's source file is the file asked for: the same name, or the same name without the folders. */
    private static boolean sameFile(final String source, final String asked) {
        if (source.isEmpty() || asked.isEmpty()) {
            return false;
        }
        final String one = source.toLowerCase(Locale.ROOT).replace('\\', '/');
        final String other = asked.toLowerCase(Locale.ROOT).replace('\\', '/');
        return one.equals(other) || one.endsWith("/" + other) || other.endsWith("/" + one);
    }

    /** A breakpoint: the file, as the listing names it, and the line it is on. */
    public record Breakpoint(String file, int line) {
    }

    /**
     * A call on the stack: the method as a stack shows it, the file and the line it is at, the column of the statement
     * there (0 when the build did not say), and what it was handed.
     */
    public record Place(String method, String file, int line, int column, List<Variable> arguments) {
    }

    /** A variable as the debugger shows it: its name, its type, and its value written out. */
    public record Variable(String name, String type, String value) {
    }

    /** What stops a program. */
    public enum Reason {
        BREAKPOINT, STEP, INTERRUPTED
    }

    private enum StepKind {
        INTO, OVER, OUT
    }

    /* A step under way: the thread, the call and the line it started from, and how deep that call was. */
    private record Step(StepKind kind, ProgramThread thread, Frame frame, int depth, int line) {
    }
}
