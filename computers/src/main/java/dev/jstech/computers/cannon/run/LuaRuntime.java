/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.run;

import dev.jstech.computers.cannon.Diagnostic;
import dev.jstech.computers.cannon.SourceFile;
import dev.jstech.computers.cannon.asm.Instruction;
import dev.jstech.computers.cannon.asm.IOperand;
import dev.jstech.computers.cannon.lua.LuaCompiler;
import dev.jstech.computers.cannon.lua.LuaEmitter;
import dev.jstech.computers.cannon.lua.lib.ILuaContext;
import dev.jstech.computers.cannon.lua.lib.ILuaContinuation;
import dev.jstech.computers.cannon.lua.lib.ILuaFunction;
import dev.jstech.computers.cannon.lua.lib.LuaCall;
import dev.jstech.computers.cannon.lua.lib.LuaLib;
import dev.jstech.computers.cannon.lua.lib.LuaNumbers;
import dev.jstech.computers.cannon.lua.lib.LuaValues;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * The Lua side of the runtime: what a compiled Lua program calls for everything its values do.
 *
 * <p>The Lua front end compiles a program to the same assembly as Cannon, but every Lua value is an
 * {@code object} and every operator, index and call is dynamic, so each of those is a call on
 * {@code Lua} that the process answers here. A metamethod that turns out to be a Lua function is not
 * run on the spot: a frame that carries the operation on is pushed, then the function's own frame,
 * and the interpreter steps through both as it steps through anything, one instruction at a time.
 * That is what lets a metamethod, a sort with a comparator or a protected call cost the budget it
 * uses, and be saved and brought back in the middle like any other call.
 *
 * <p>Coroutines are threads of the process parked until resumed; a protected call is a carrying
 * frame that also catches what is raised under it.
 */
final class LuaRuntime implements ILuaContext {

    /** The owner every call the front end emits names. */
    static final String OWNER = "Lua";

    /** The type the front end emits into every program, with the globals and the carrying methods. */
    static final String RUNTIME_TYPE = "Lua.0runtime";

    static final String GLOBALS = "G";
    static final String STRING_META = "StringMeta";
    static final String OUT = "Out";
    static final String EMPTY = "Empty";
    static final String LOADED = "Loaded";
    static final String ENVIRONMENT = "0env";
    static final String RESUME = "0resume";
    static final String COROUTINE_BODY = "0coroutine";
    static final String PARK = "0park";
    static final String INVOKE = "Invoke";
    static final List<String> INVOKE_PARAMETERS = List.of("object[]");
    static final String FUNCTION_TYPE = "Lua.Function";

    private static final List<String> ONE_OBJECT = List.of("object");
    private static final List<String> TWO_OBJECTS = List.of("object", "object");
    private static final int CHAIN_LIMIT = 100;
    private static final int COROUTINE_COST = 49;

    private static final long SUSPENDED = 0;
    private static final long RUNNING = 1;
    private static final long NORMAL = 2;
    private static final long DEAD = 3;

    /** What a library call answers when it put the call back to be asked again once its wait is over. */
    private static final Object REWIND = new Object();

    private final Process process;
    private final Heap heap;
    /** The chunk each function type belongs to, worked out once, for reading globals. */
    private final java.util.Map<String, String> chunks = new java.util.HashMap<>();
    private Random random = new Random();

    LuaRuntime(final Process process) {
        this.process = process;
        this.heap = process.heap0();
    }

    // the calls the front end emits

    void call(final Process.Frame frame, final IOperand.Method named, final int line) {
        final List<Object> arguments = this.process.takeArguments(frame, named.parameters());
        switch (named.name()) {
            case "Call" -> this.callWith(frame, arguments.get(0), arguments.get(1), described(frame), line);
            case "Continue" -> this.carryOn(frame, (Values.Obj) arguments.get(1), arguments.get(0), line);
            case "CoReturn" -> {
                this.coroutineReturned(arguments.get(0), line);
                frame.push(null);
            }
            case "Globals" -> frame.push(this.globals(line));
            case "GetGlobal" -> this.index(frame, this.environment(frame, line), arguments.get(0), null, line);
            case "SetGlobal" -> this.setIndex(frame, this.environment(frame, line), arguments.get(0),
                    arguments.get(1), null, line);
            case "ScriptArgs" -> frame.push(this.scriptArgs(line));
            case "Flush" -> this.flush();
            case "True" -> frame.push(Boolean.TRUE);
            case "False" -> frame.push(Boolean.FALSE);
            case "None" -> frame.push(this.none(line));
            case "Index" -> this.index(frame, arguments.get(0), arguments.get(1), described(frame), line);
            case "SetIndex" -> this.setIndex(frame, arguments.get(0), arguments.get(1), arguments.get(2),
                    described(frame), line);
            case "Gt" -> this.compare(frame, "__lt", arguments.get(1), arguments.get(0), line);
            case "Ge" -> this.compare(frame, "__le", arguments.get(1), arguments.get(0), line);
            case "Add" -> this.arithmetic(frame, "__add", arguments.get(0), arguments.get(1), line);
            case "Sub" -> this.arithmetic(frame, "__sub", arguments.get(0), arguments.get(1), line);
            case "Mul" -> this.arithmetic(frame, "__mul", arguments.get(0), arguments.get(1), line);
            case "Div" -> this.arithmetic(frame, "__div", arguments.get(0), arguments.get(1), line);
            case "Mod" -> this.arithmetic(frame, "__mod", arguments.get(0), arguments.get(1), line);
            case "Pow" -> this.arithmetic(frame, "__pow", arguments.get(0), arguments.get(1), line);
            case "IDiv" -> this.arithmetic(frame, "__idiv", arguments.get(0), arguments.get(1), line);
            case "Unm" -> this.arithmetic(frame, "__unm", arguments.get(0), arguments.get(0), line);
            case "Concat" -> this.concat(frame, arguments.get(0), arguments.get(1), line);
            case "Len" -> this.length(frame, arguments.get(0), line);
            case "Eq" -> this.equals(frame, arguments.get(0), arguments.get(1), false, line);
            case "Ne" -> this.equals(frame, arguments.get(0), arguments.get(1), true, line);
            case "Lt" -> this.compare(frame, "__lt", arguments.get(0), arguments.get(1), line);
            case "Le" -> this.compare(frame, "__le", arguments.get(0), arguments.get(1), line);
            case "Not" -> frame.push(!LuaValues.truth(arguments.get(0)));
            case "Truth" -> frame.push(LuaValues.truth(arguments.get(0)));
            case "First" -> frame.push(first(arguments.get(0)));
            case "Take" -> frame.push(this.take(arguments.get(0), Numbers.toInt(arguments.get(1)), line));
            case "Append" -> frame.push(this.append((Values.Arr) arguments.get(0), arguments.get(1), line));
            case "Rest" -> frame.push(this.rest((Values.Arr) arguments.get(0), Numbers.toInt(arguments.get(1)),
                    line));
            case "Arg" -> frame.push(at((Values.Arr) arguments.get(0), Numbers.toInt(arguments.get(1))));
            case "NewTable" -> frame.push(this.table(line));
            case "RawSet" -> this.rawSet((Values.Table) arguments.get(0), arguments.get(1), arguments.get(2),
                    line);
            case "RawSetAll" -> this.rawSetAll((Values.Table) arguments.get(0),
                    Numbers.toLong(arguments.get(1)), arguments.get(2), line);
            case "ForPrep" -> frame.push(this.forPrep(arguments.get(0), arguments.get(1), arguments.get(2),
                    line));
            case "ForOn" -> frame.push(forOn(arguments.get(0), arguments.get(1), arguments.get(2)));
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, "Lua has no " + named.name());
        }
    }

    // values with several parts

    /** The first of several values, the value itself when it is one, or nil for none. */
    static Object first(final Object result) {
        if (result instanceof Values.Arr several) {
            return several.length() == 0 ? null : several.get(0, 0);
        }
        return result;
    }

    private static Object at(final Values.Arr values, final int index) {
        return index >= 0 && index < values.length() ? values.get(index, 0) : null;
    }

    @Override
    public List<Object> expand(final Object result) {
        if (result instanceof Values.Arr several) {
            return several.all();
        }
        final List<Object> one = new ArrayList<>(1);
        one.add(result);
        return one;
    }

    private Object[] expandArguments(final Object arguments) {
        if (arguments instanceof Values.Arr several) {
            return several.all().toArray();
        }
        // A single nil is one argument that is nil; no arguments at all come as an empty run.
        return new Object[] {arguments};
    }

    /** The empty run, made once: what a function that returns nothing gives back. */
    private Values.Arr none(final int line) {
        final Values.Obj statics = this.runtimeStatics();
        if (statics.get(EMPTY) instanceof Values.Arr empty) {
            return empty;
        }
        final Values.Arr made = this.values(List.of(), line);
        statics.set(EMPTY, made);
        return made;
    }

    /** What the program was started with, as the chunk's {@code ...}. */
    private Values.Arr scriptArgs(final int line) {
        final List<Object> given = new ArrayList<>();
        for (final String argument : this.process.args()) {
            given.add(this.text(argument, line));
        }
        return this.values(given, line);
    }

    private Values.Arr take(final Object result, final int count, final int line) {
        final List<Object> all = this.expand(result);
        final List<Object> taken = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            taken.add(i < all.size() ? all.get(i) : null);
        }
        return this.values(taken, line);
    }

    private Values.Arr append(final Values.Arr fixed, final Object tail, final int line) {
        final List<Object> all = new ArrayList<>(fixed.all());
        all.addAll(this.expand(tail));
        return this.values(all, line);
    }

    private Values.Arr rest(final Values.Arr arguments, final int from, final int line) {
        final List<Object> all = arguments.all();
        return this.values(from >= all.size() ? List.of() : all.subList(from, all.size()), line);
    }

    @Override
    public Values.Arr values(final List<Object> items, final int line) {
        final Values.Arr made = new Values.Arr("object", items.size());
        this.heap.allocate(made, Heap.HEADER + (long) Heap.REFERENCE * items.size(), line);
        for (int i = 0; i < items.size(); i++) {
            made.set(i, items.get(i), line);
        }
        return made;
    }

    // tables

    @Override
    public Values.Table table(final int line) {
        final Values.Table made = new Values.Table();
        return this.heap.allocate(made, made.bytes(), line);
    }

    @Override
    public void resized(final Values.Table table, final int line) {
        this.heap.resize(table, table.bytes(), line);
    }

    private void rawSet(final Values.Table table, final Object key, final Object value, final int line) {
        if (key == null) {
            throw this.error("table index is nil", line);
        }
        if (key instanceof Double real && real.isNaN()) {
            throw this.error("table index is NaN", line);
        }
        table.put(key, value);
        this.resized(table, line);
    }

    private void rawSetAll(final Values.Table table, final long from, final Object values, final int line) {
        long index = from;
        for (final Object value : this.expand(values)) {
            table.put(index++, value);
        }
        this.resized(table, line);
    }

    @Override
    public Values.Table metatableOf(final Object value) {
        if (value instanceof Values.Table table) {
            return table.metatable();
        }
        if (value instanceof String) {
            return this.runtimeStatics().get(STRING_META) instanceof Values.Table meta ? meta : null;
        }
        return null;
    }

    private Object metamethod(final Object value, final String event) {
        final Values.Table meta = this.metatableOf(value);
        return meta == null ? null : meta.get(event);
    }

    private void index(final Process.Frame frame, final Object target, final Object key, final String what,
                       final int line) {
        Object at = target;
        for (int depth = 0; depth < CHAIN_LIMIT; depth++) {
            if (at instanceof Values.Table table) {
                final Object found = table.get(key);
                if (found != null) {
                    frame.push(found);
                    return;
                }
            }
            final Object handler = at instanceof Values.Table || at instanceof String
                    ? this.metamethod(at, "__index") : null;
            if (handler == null) {
                if (at instanceof Values.Table) {
                    frame.push(null);
                    return;
                }
                throw this.error("attempt to index a " + LuaValues.typeName(at) + " value"
                        + (depth == 0 ? naming(what) : ""), line);
            }
            if (handler instanceof Values.DelegateValue) {
                this.metaCall(frame, handler, List.of(at, key), "first", line);
                return;
            }
            at = handler;
        }
        throw this.error("'__index' chain too long; possible loop", line);
    }

    private static String naming(final String what) {
        return what == null ? "" : " (" + what + ")";
    }

    private void setIndex(final Process.Frame frame, final Object target, final Object key,
                          final Object value, final String what, final int line) {
        Object at = target;
        for (int depth = 0; depth < CHAIN_LIMIT; depth++) {
            if (at instanceof Values.Table table) {
                final Object handler = table.get(key) != null ? null : this.metamethod(table, "__newindex");
                if (handler == null) {
                    this.rawSet(table, key, value, line);
                    frame.push(null);
                    return;
                }
                if (handler instanceof Values.DelegateValue) {
                    this.metaCall(frame, handler, List.of(at, key, value), "none", line);
                    return;
                }
                at = handler;
                continue;
            }
            throw this.error("attempt to index a " + LuaValues.typeName(at) + " value"
                    + (depth == 0 ? naming(what) : ""), line);
        }
        throw this.error("'__newindex' chain too long; possible loop", line);
    }

    // operators

    private void arithmetic(final Process.Frame frame, final String event, final Object left,
                            final Object right, final int line) {
        final Object a = LuaNumbers.toNumber(left);
        final Object b = LuaNumbers.toNumber(right);
        if (a != null && b != null) {
            final Object result = switch (event) {
                case "__add" -> LuaNumbers.add(a, b);
                case "__sub" -> LuaNumbers.subtract(a, b);
                case "__mul" -> LuaNumbers.multiply(a, b);
                case "__div" -> LuaNumbers.divide(a, b);
                case "__mod" -> LuaNumbers.modulo(a, b);
                case "__pow" -> LuaNumbers.power(a, b);
                case "__idiv" -> LuaNumbers.floorDivide(a, b);
                default -> LuaNumbers.negate(a);
            };
            if (result == null) {
                throw this.error("attempt to perform 'n" + ("__mod".equals(event) ? "%%" : "//") + "0'", line);
            }
            frame.push(result);
            return;
        }
        Object handler = this.metamethod(left, event);
        if (handler == null) {
            handler = this.metamethod(right, event);
        }
        if (handler == null) {
            final Object bad = a == null ? left : right;
            throw this.error("attempt to perform arithmetic on a " + LuaValues.typeName(bad) + " value", line);
        }
        this.metaCall(frame, handler, List.of(left, right), "first", line);
    }

    private void concat(final Process.Frame frame, final Object left, final Object right, final int line) {
        final String a = LuaValues.asText(left);
        final String b = LuaValues.asText(right);
        if (a != null && b != null) {
            this.process.charge((a.length() + b.length()) / 64);
            frame.push(this.text(a + b, line));
            return;
        }
        Object handler = this.metamethod(left, "__concat");
        if (handler == null) {
            handler = this.metamethod(right, "__concat");
        }
        if (handler == null) {
            final Object bad = a == null ? left : right;
            throw this.error("attempt to concatenate a " + LuaValues.typeName(bad) + " value", line);
        }
        this.metaCall(frame, handler, List.of(left, right), "first", line);
    }

    private void length(final Process.Frame frame, final Object value, final int line) {
        if (value instanceof String text) {
            frame.push((long) text.length());
            return;
        }
        final Object handler = this.metamethod(value, "__len");
        if (handler != null) {
            this.metaCall(frame, handler, List.of(value), "first", line);
            return;
        }
        if (value instanceof Values.Table table) {
            frame.push(table.length());
            return;
        }
        throw this.error("attempt to get length of a " + LuaValues.typeName(value) + " value", line);
    }

    private void equals(final Process.Frame frame, final Object left, final Object right,
                        final boolean negated, final int line) {
        if (LuaValues.rawEqual(left, right)) {
            frame.push(!negated);
            return;
        }
        if (left instanceof Values.Table && right instanceof Values.Table) {
            Object handler = this.metamethod(left, "__eq");
            if (handler == null) {
                handler = this.metamethod(right, "__eq");
            }
            if (handler != null) {
                this.metaCall(frame, handler, List.of(left, right), negated ? "not" : "truth", line);
                return;
            }
        }
        frame.push(negated);
    }

    private void compare(final Process.Frame frame, final String event, final Object left,
                         final Object right, final int line) {
        if (LuaNumbers.isNumber(left) && LuaNumbers.isNumber(right)) {
            frame.push("__lt".equals(event) ? LuaNumbers.less(left, right)
                    : LuaNumbers.lessOrEqual(left, right));
            return;
        }
        if (left instanceof String a && right instanceof String b) {
            frame.push("__lt".equals(event) ? a.compareTo(b) < 0 : a.compareTo(b) <= 0);
            return;
        }
        Object handler = this.metamethod(left, event);
        if (handler == null) {
            handler = this.metamethod(right, event);
        }
        if (handler != null) {
            this.metaCall(frame, handler, List.of(left, right), "truth", line);
            return;
        }
        if ("__le".equals(event)) {
            // Without a __le, a <= b is taken as not (b < a), as the language allows.
            Object less = this.metamethod(right, "__lt");
            if (less == null) {
                less = this.metamethod(left, "__lt");
            }
            if (less != null) {
                this.metaCall(frame, less, List.of(right, left), "not", line);
                return;
            }
        }
        final String one = LuaValues.typeName(left);
        final String other = LuaValues.typeName(right);
        throw this.error(one.equals(other) ? "attempt to compare two " + one + " values"
                : "attempt to compare " + one + " with " + other, line);
    }

    private Values.Arr forPrep(final Object start, final Object limit, final Object step, final int line) {
        final Object from = LuaNumbers.toNumber(start);
        final Object to = LuaNumbers.toNumber(limit);
        final Object by = LuaNumbers.toNumber(step);
        if (from == null) {
            throw this.error("'for' initial value must be a number", line);
        }
        if (to == null) {
            throw this.error("'for' limit must be a number", line);
        }
        if (by == null) {
            throw this.error("'for' step must be a number", line);
        }
        if (by instanceof Long whole ? whole == 0 : (Double) by == 0) {
            throw this.error("'for' step is zero", line);
        }
        if (from instanceof Long && to instanceof Long && by instanceof Long) {
            return this.values(List.of(from, to, by), line);
        }
        if (from instanceof Long && by instanceof Long) {
            /*
             * A whole start and step with a real limit still count in whole numbers, as the language
             * does, with the limit brought to the nearest whole number the loop can reach.
             */
            final double real = (Double) to;
            final long clipped = (Long) by > 0 ? (long) Math.floor(real) : (long) Math.ceil(real);
            return this.values(List.of(from, clipped, by), line);
        }
        return this.values(List.of(LuaNumbers.toDouble(from), LuaNumbers.toDouble(to), LuaNumbers.toDouble(by)),
                line);
    }

    private static boolean forOn(final Object index, final Object limit, final Object step) {
        if (step instanceof Long whole) {
            return whole > 0 ? LuaNumbers.lessOrEqual(index, limit) : LuaNumbers.lessOrEqual(limit, index);
        }
        return (Double) step > 0 ? LuaNumbers.lessOrEqual(index, limit) : LuaNumbers.lessOrEqual(limit, index);
    }

    // calls

    /**
     * Calls a value with what a call site built: a run of arguments, or one value standing for itself.
     * A run is handed to a function of the program as it is, since nothing changes a run once made.
     */
    private void callWith(final Process.Frame frame, final Object function, final Object arguments,
                          final String what, final int line) {
        if (arguments instanceof Values.Arr run && function instanceof Values.DelegateValue delegate
                && !delegate.chain().isEmpty() && !OWNER.equals(delegate.chain().getFirst().owner())) {
            this.enterFunction(delegate.chain().getFirst(), run, line);
            return;
        }
        this.callValue(frame, function, this.expandArguments(arguments), what, line);
    }

    private void enterFunction(final Values.Bound bound, final Values.Arr arguments, final int line) {
        final Loaded.Method method =
                this.process.program0().method(bound.owner(), bound.method(), bound.parameters());
        if (method == null || method.code().isEmpty()) {
            throw this.error("attempt to call a function that has no body", line);
        }
        final List<Object> passed = new ArrayList<>(1);
        passed.add(arguments);
        this.process.enterFrame(method, bound.target(), passed, line);
    }

    /** Calls a value with those arguments, the answer going to that frame's stack in its turn. */
    private void callValue(final Process.Frame frame, final Object function, final Object[] arguments,
                           final String what, final int line) {
        if (function instanceof Values.DelegateValue delegate && !delegate.chain().isEmpty()) {
            final Values.Bound bound = delegate.chain().getFirst();
            if (OWNER.equals(bound.owner())) {
                this.library(frame, function, bound.method(), bound.target(), arguments, line);
                return;
            }
            this.enterFunction(bound, this.values(Arrays.asList(arguments), line), line);
            return;
        }
        final Object handler = this.metamethod(function, "__call");
        if (handler != null) {
            final Object[] withSelf = new Object[arguments.length + 1];
            withSelf[0] = function;
            System.arraycopy(arguments, 0, withSelf, 1, arguments.length);
            this.callValue(frame, handler, withSelf, null, line);
            return;
        }
        throw this.error("attempt to call a " + LuaValues.typeName(function) + " value" + naming(what), line);
    }

    /**
     * Calls a metamethod and carries the operation on with what it gives: the first of its values,
     * whether that counts as true, the opposite, or nothing.
     */
    private void metaCall(final Process.Frame frame, final Object handler, final List<Object> arguments,
                          final String next, final int line) {
        final Values.Obj state = this.state(next, null, line);
        this.startCall(frame, new LuaCall(handler, arguments.toArray(), state), line);
    }

    /**
     * Makes a call the library asked for: a frame that carries on with the continuation goes under
     * the function's own, so that when the function returns, its answer lands there and the
     * continuation runs with it.
     */
    private void startCall(final Process.Frame frame, final LuaCall call, final int line) {
        final Loaded.Method resume = this.process.program0().method(RUNTIME_TYPE, RESUME, ONE_OBJECT);
        if (resume == null) {
            throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, "this program has no Lua runtime in it");
        }
        final Process.Frame carrying = new Process.Frame(resume, null);
        carrying.slots[0] = call.state();
        carrying.role = call.protects() ? Process.Role.PROTECT : Process.Role.RESUME;
        this.process.current().frames.push(carrying);
        this.callValue(carrying, call.function(), call.arguments(), null, line);
    }

    /** What the carrying frame does once the call under it has returned. */
    private void carryOn(final Process.Frame frame, final Values.Obj state, final Object result, final int line) {
        final String next = String.valueOf(state.get(LuaCall.NEXT));
        switch (next) {
            case "first" -> frame.push(first(result));
            case "truth" -> frame.push(LuaValues.truth(first(result)));
            case "not" -> frame.push(!LuaValues.truth(first(result)));
            case "none" -> frame.push(null);
            default -> {
                final ILuaContinuation continuation = LuaLib.continuation(next);
                if (continuation == null) {
                    throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, "nothing carries on with " + next);
                }
                final Object answer = continuation.resume(this, state, result, line);
                if (answer instanceof LuaCall again) {
                    /*
                     * Another call to make before this one is done (a sort's next comparison): the
                     * frame that carries it on is used again rather than stacked, so a sort of a
                     * thousand things is one frame deep however many comparisons it asks for.
                     */
                    frame.slots[0] = again.state();
                    frame.role = again.protects() ? Process.Role.PROTECT : Process.Role.RESUME;
                    frame.at = 0;
                    this.callValue(frame, again.function(), again.arguments(), null, line);
                    return;
                }
                this.deliver(frame, answer, line);
            }
        }
    }

    /** Puts what a library call answered where it goes: the stack, or another call. */
    private void deliver(final Process.Frame frame, final Object result, final int line) {
        if (result == REWIND) {
            return;
        }
        if (result instanceof LuaCall call) {
            this.startCall(frame, call, line);
            return;
        }
        frame.push(result);
    }

    /**
     * Runs a function of the library.
     *
     * <p>The few that wait (resuming a coroutine, yielding, reading a line) run inside a frame of
     * their own whose one instruction is the call, so that putting the call back to be asked again
     * is putting one instruction back, wherever the call came from.
     */
    private void library(final Process.Frame frame, final Object function, final String name,
                         final Object target, final Object[] arguments, final int line) {
        final boolean waits = switch (name) {
            case "coroutine.resume", "coroutine.yield", "coroutine.wrapped", "io.read", "read" -> true;
            default -> false;
        };
        if (waits && !this.inPark(frame)) {
            final Loaded.Method park = this.process.program0().method(RUNTIME_TYPE, PARK, TWO_OBJECTS);
            if (park == null) {
                throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, "this program has no Lua runtime in it");
            }
            final List<Object> passed = new ArrayList<>(2);
            passed.add(function);
            passed.add(this.values(Arrays.asList(arguments), line));
            this.process.enterFrame(park, null, passed, line);
            return;
        }
        final Object result = switch (name) {
            case "load", "loadstring" -> this.load(arguments, line);
            case "coroutine.create" -> this.coroutineCreate(argument(arguments, 0), line);
            case "coroutine.resume" -> this.coroutineResume(frame, function, argument(arguments, 0),
                    Arrays.copyOfRange(arguments, Math.min(1, arguments.length), arguments.length), false, line);
            case "coroutine.wrapped" -> this.coroutineResume(frame, function, target, arguments, true, line);
            case "coroutine.yield" -> this.coroutineYield(frame, function, arguments, line);
            case "coroutine.wrap" -> this.function("coroutine.wrapped",
                    this.coroutineCreate(argument(arguments, 0), line), line);
            case "coroutine.status" -> this.coroutineStatus(argument(arguments, 0), line);
            case "coroutine.running" -> this.coroutineRunning(line);
            case "coroutine.isyieldable" -> this.current().token instanceof Values.Obj token
                    && LuaValues.COROUTINE.equals(token.type());
            case "io.read", "read" -> this.readLine(frame, function, arguments, line);
            default -> {
                final ILuaFunction found = LuaLib.function(name);
                if (found == null) {
                    throw this.error("'" + name + "' is not a function of this library", line);
                }
                yield found.call(this, target, arguments, line);
            }
        };
        this.deliver(frame, result, line);
    }

    private static Object argument(final Object[] arguments, final int index) {
        return index < arguments.length ? arguments[index] : null;
    }

    private boolean inPark(final Process.Frame frame) {
        return RUNTIME_TYPE.equals(frame.method.owner()) && PARK.equals(frame.method.name());
    }

    /** Puts a waiting call back on the parking frame, to be asked again when the wait is over. */
    private Object rewind(final Process.Frame frame, final Object function, final Object[] arguments,
                          final int line) {
        frame.push(function);
        frame.push(this.values(Arrays.asList(arguments), line));
        frame.at--;
        return REWIND;
    }

    // errors

    /**
     * Catches what a halt raised, if anything under the thread's top frames does.
     *
     * <p>Frames come off until one that protects is on top; that one comes off too, and its state's
     * fallback continuation is given the error and answers into the frame beneath, where the
     * protected call was made. A coroutine with nothing protecting dies and hands the error to
     * whoever resumed it. Anything else is a halt like any other.
     */
    boolean recover(final Process.Thread thread, final Halt halt) {
        boolean caught = false;
        for (final Process.Frame frame : thread.frames) {
            if (frame.role == Process.Role.PROTECT) {
                caught = true;
                break;
            }
        }
        final Values.Obj coroutine = thread.token instanceof Values.Obj token
                && LuaValues.COROUTINE.equals(token.type()) ? token : null;
        if (!caught && coroutine == null) {
            this.flush();
            return false;
        }
        final Object raised = halt.reason() == Halt.Reason.RAISED ? halt.value() : this.message(halt);
        if (coroutine != null && !caught) {
            thread.frames.clear();
            this.coroutineFailed(thread, coroutine, raised, halt.line());
            return true;
        }
        while (true) {
            final Process.Frame top = thread.frames.pop();
            if (top.role != Process.Role.PROTECT) {
                continue;
            }
            final Values.Obj state = (Values.Obj) top.slots[0];
            final Process.Frame below = thread.frames.peek();
            final ILuaContinuation failed = LuaLib.continuation(String.valueOf(state.get(LuaCall.FAILED)));
            if (below == null || failed == null) {
                return false;
            }
            this.deliver(below, failed.resume(this, state, raised, 0), 0);
            return true;
        }
    }

    /* The words of a halt the runtime itself raised, as a value; kept off the heap only if the heap is full. */
    private Object message(final Halt halt) {
        try {
            return this.text(halt.getMessage(), halt.line());
        } catch (final Halt full) {
            return halt.getMessage();
        }
    }

    @Override
    public Halt raise(final Object value, final int line) {
        return new Halt(Halt.Reason.RAISED, line, LuaValues.plainString(value), value);
    }

    @Override
    public Halt error(final String message, final int line) {
        final String placed = this.where(1, line) + message;
        return new Halt(Halt.Reason.RAISED, line, placed, this.text(placed, line));
    }

    @Override
    public String where(final int level, final int line) {
        if (level <= 0) {
            return "";
        }
        int remaining = level;
        for (final Process.Frame frame : this.current().frames) {
            if (!frame.method.owner().startsWith(OWNER + ".") || RUNTIME_TYPE.equals(frame.method.owner())) {
                continue;
            }
            if (--remaining == 0) {
                final String place = sourcePlace(frame);
                return place == null ? "" : place + ": ";
            }
        }
        return "";
    }

    /**
     * Where a frame is in its source, as {@code file:line}, read off the comment the front end left
     * on the first instruction of every line, or null when it left none.
     */
    private static String sourcePlace(final Process.Frame frame) {
        final List<Instruction> code = frame.method.code();
        for (int i = Math.min(frame.at, code.size()) - 1; i >= 0; i--) {
            final String comment = code.get(i).comment();
            if (comment == null) {
                continue;
            }
            final String first = comment.split("; ", 2)[0];
            if (isPlace(first)) {
                return first;
            }
        }
        return null;
    }

    /**
     * What the instruction running in that frame reaches for ({@code global 'foo'}), read off the part
     * of its comment that is not a place, or null.
     */
    private static String described(final Process.Frame frame) {
        if (!frame.method.owner().startsWith(OWNER + ".") || frame.at <= 0
                || frame.at > frame.method.code().size()) {
            return null;
        }
        final String comment = frame.method.code().get(frame.at - 1).comment();
        if (comment == null) {
            return null;
        }
        for (final String part : comment.split("; ")) {
            if (!isPlace(part)) {
                return part;
            }
        }
        return null;
    }

    /* A place ends in a colon and the digits of a line. */
    private static boolean isPlace(final String comment) {
        final int colon = comment.lastIndexOf(':');
        if (colon <= 0 || colon == comment.length() - 1) {
            return false;
        }
        for (int i = colon + 1; i < comment.length(); i++) {
            if (!Character.isDigit(comment.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    // coroutines

    private Process.Thread current() {
        return this.process.current();
    }

    private Values.Obj coroutineCreate(final Object function, final int line) {
        if (!(function instanceof Values.DelegateValue)) {
            throw this.error("bad argument #1 to 'create' (function expected)", line);
        }
        final Loaded.Method body = this.process.program0().method(RUNTIME_TYPE, COROUTINE_BODY, TWO_OBJECTS);
        if (body == null) {
            throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, "this program has no Lua runtime in it");
        }
        final Values.Obj token = new Values.Obj(LuaValues.COROUTINE);
        this.heap.allocate(token, Heap.HEADER + 8L * Heap.REFERENCE, line);
        final Process.Thread made = new Process.Thread(this.process.nextThreadId());
        made.token = token;
        token.set("Thread", made.id);
        token.set("Status", SUSPENDED);
        token.set("Fn", function);
        token.set("Started", false);
        token.set("Failed", false);
        token.set("Yielding", false);
        final Process.Frame frame = new Process.Frame(body, null);
        frame.slots[0] = function;
        made.frames.push(frame);
        made.parked = Process.Parked.COROUTINE;
        this.process.threads0().add(made);
        this.process.charge(COROUTINE_COST);
        return token;
    }

    private Values.Obj coroutineToken(final Object value, final String what, final int line) {
        if (value instanceof Values.Obj token && LuaValues.COROUTINE.equals(token.type())) {
            return token;
        }
        throw this.error("bad argument #1 to '" + what + "' (coroutine expected)", line);
    }

    private Object coroutineResume(final Process.Frame frame, final Object function, final Object which,
                                   final Object[] arguments, final boolean wrapped, final int line) {
        final Values.Obj token = this.coroutineToken(which, wrapped ? "wrap" : "resume", line);
        final Process.Thread me = this.current();
        if (me.on == token) {
            // Woken: the coroutine yielded, returned or died, and what it handed over is the answer.
            me.on = null;
            final List<Object> handed = this.expand(token.get("Transfer"));
            final boolean failed = Boolean.TRUE.equals(token.get("Failed"));
            token.set("Failed", false);
            if (wrapped) {
                if (failed) {
                    throw this.raise(handed.isEmpty() ? null : handed.getFirst(), line);
                }
                return this.values(handed, line);
            }
            final List<Object> answer = new ArrayList<>(handed.size() + 1);
            answer.add(!failed);
            answer.addAll(handed);
            return this.values(answer, line);
        }
        final long status = Numbers.toLong(token.get("Status"));
        if (status != SUSPENDED) {
            final String why = "cannot resume " + (status == DEAD ? "dead" : "non-suspended") + " coroutine";
            if (wrapped) {
                throw this.error(why, line);
            }
            return this.values(List.of(false, this.text(why, line)), line);
        }
        final Process.Thread coroutine = this.process.threadById(token.get("Thread"));
        if (coroutine == null) {
            token.set("Status", DEAD);
            return this.coroutineResume(frame, function, which, arguments, wrapped, line);
        }
        final Values.Arr passed = this.values(Arrays.asList(arguments), line);
        token.set("Transfer", passed);
        if (!Boolean.TRUE.equals(token.get("Started"))) {
            token.set("Started", true);
            final Process.Frame body = coroutine.frames.peekLast();
            if (body != null) {
                body.slots[1] = passed;
            }
        }
        token.set("Status", RUNNING);
        token.set("Resumer", me.id);
        if (me.token instanceof Values.Obj mine && LuaValues.COROUTINE.equals(mine.type())) {
            mine.set("Status", NORMAL);
        }
        coroutine.parked = Process.Parked.NONE;
        me.parked = Process.Parked.COROUTINE;
        me.on = token;
        return this.rewind(frame, function, wrapped ? arguments : withFirst(which, arguments), line);
    }

    private static Object[] withFirst(final Object first, final Object[] rest) {
        final Object[] all = new Object[rest.length + 1];
        all[0] = first;
        System.arraycopy(rest, 0, all, 1, rest.length);
        return all;
    }

    private Object coroutineYield(final Process.Frame frame, final Object function, final Object[] arguments,
                                  final int line) {
        final Process.Thread me = this.current();
        if (!(me.token instanceof Values.Obj token) || !LuaValues.COROUTINE.equals(token.type())) {
            throw this.error("attempt to yield from outside a coroutine", line);
        }
        if (Boolean.TRUE.equals(token.get("Yielding"))) {
            // Resumed again: what the resume passed is what the yield gives back.
            token.set("Yielding", false);
            return token.get("Transfer");
        }
        token.set("Transfer", this.values(Arrays.asList(arguments), line));
        token.set("Status", SUSPENDED);
        token.set("Yielding", true);
        this.wakeResumer(token);
        me.parked = Process.Parked.COROUTINE;
        return this.rewind(frame, function, arguments, line);
    }

    private void wakeResumer(final Values.Obj token) {
        final Process.Thread resumer = this.process.threadById(token.get("Resumer"));
        if (resumer == null) {
            return;
        }
        resumer.parked = Process.Parked.NONE;
        if (resumer.token instanceof Values.Obj theirs && LuaValues.COROUTINE.equals(theirs.type())) {
            theirs.set("Status", RUNNING);
        }
    }

    private void coroutineReturned(final Object result, final int line) {
        final Process.Thread me = this.current();
        if (!(me.token instanceof Values.Obj token) || !LuaValues.COROUTINE.equals(token.type())) {
            return;
        }
        token.set("Transfer", this.values(this.expand(result), line));
        token.set("Status", DEAD);
        this.wakeResumer(token);
    }

    private void coroutineFailed(final Process.Thread thread, final Values.Obj token, final Object raised,
                                 final int line) {
        final List<Object> one = new ArrayList<>(1);
        one.add(raised);
        token.set("Transfer", this.values(one, line));
        token.set("Status", DEAD);
        token.set("Failed", true);
        this.wakeResumer(token);
        this.process.endThread(thread);
    }

    private Object coroutineStatus(final Object which, final int line) {
        final Values.Obj token = this.coroutineToken(which, "status", line);
        final long status = Numbers.toLong(token.get("Status"));
        final String word;
        if (token == this.current().token) {
            word = "running";
        } else if (status == SUSPENDED) {
            word = "suspended";
        } else if (status == DEAD) {
            word = "dead";
        } else if (status == NORMAL) {
            word = "normal";
        } else {
            word = "running";
        }
        return this.text(word, line);
    }

    private Object coroutineRunning(final int line) {
        final Process.Thread me = this.current();
        if (me.token instanceof Values.Obj token && LuaValues.COROUTINE.equals(token.type())) {
            return this.values(List.of(token, false), line);
        }
        final List<Object> answer = new ArrayList<>(2);
        answer.add(null);
        answer.add(true);
        return this.values(answer, line);
    }

    // the console

    private Object readLine(final Process.Frame frame, final Object function, final Object[] arguments,
                            final int line) {
        if (!this.process.hasInput()) {
            // Whatever was written as a prompt is shown before the wait, or nobody would know to type.
            this.flush();
            this.process.park();
            return this.rewind(frame, function, arguments, line);
        }
        return this.text(this.process.takeInput(), line);
    }

    @Override
    public void print(final String line) {
        this.write(line + "\n");
    }

    /** Ends the line being written, if one was started, so what is on it is shown. */
    void flush() {
        final Values.Obj statics = this.runtimeStatics();
        if (statics.get(OUT) instanceof String pending) {
            statics.set(OUT, null);
            this.process.library().write(pending);
        }
    }

    @Override
    public void write(final String text) {
        final Values.Obj statics = this.runtimeStatics();
        final String pending = statics.get(OUT) instanceof String head ? head : "";
        final String joined = pending + text;
        final int newline = joined.lastIndexOf('\n');
        if (newline < 0) {
            statics.set(OUT, this.text(joined, 0));
            return;
        }
        for (final String whole : joined.substring(0, newline).split("\n", -1)) {
            this.process.library().write(whole);
        }
        final String rest = joined.substring(newline + 1);
        statics.set(OUT, rest.isEmpty() ? null : this.text(rest, 0));
    }

    // the globals

    private Values.Obj runtimeStatics() {
        return this.process.staticsOf(RUNTIME_TYPE);
    }

    /**
     * Where the chunk running in that frame keeps its globals: the table it was loaded with, or the
     * program's own globals. Known by the chunk the frame's function belongs to, so reading a global
     * costs no more instructions for having a choice of where from.
     */
    private Object environment(final Process.Frame frame, final int line) {
        final String chunk = this.chunks.computeIfAbsent(frame.method.owner(), LuaEmitter::chunkOf);
        if (chunk != null) {
            final Object own = this.process.staticsOf(chunk).get(ENVIRONMENT);
            if (own != null) {
                return own;
            }
        }
        return this.globals(line);
    }

    /**
     * Compiles text into a function, as {@code load} does: nil and the complaint when it does not
     * read, the chunk as a function otherwise.
     *
     * <p>The chunk's types join the running program's, and its text is kept with the program so a
     * reload can compile it again before anything that was running in it is brought back.
     */
    private Object load(final Object[] arguments, final int line) {
        final Object given = argument(arguments, 0);
        if (!(given instanceof String text)) {
            if (given instanceof Values.DelegateValue) {
                return this.values(Arrays.asList(null,
                        this.text("reading a chunk from a function is not supported on this runtime", line)), line);
            }
            throw this.error("bad argument #1 to 'load' (string expected, got " + LuaValues.typeName(given) + ")",
                    line);
        }
        final Object named = argument(arguments, 1);
        final String name = named instanceof String chosen ? chunkName(chosen) : chunkName(text);
        final Object mode = argument(arguments, 2);
        if (mode instanceof String only && only.indexOf('t') < 0) {
            return this.values(Arrays.asList(null,
                    this.text("attempt to load a text chunk (mode is '" + only + "')", line)), line);
        }
        final Values.Obj statics = this.runtimeStatics();
        if (!(statics.get(LOADED) instanceof Values.Table loaded)) {
            final Values.Table made = this.table(line);
            statics.set(LOADED, made);
            return this.load(arguments, line);
        }
        final String chunkType = OWNER + ".0load" + (loaded.length() + 1);
        final LuaCompiler.Result built = LuaCompiler.compile(new SourceFile(name, text), chunkType);
        if (!built.ok()) {
            final Diagnostic first = built.diagnostics().getFirst();
            return this.values(Arrays.asList(null,
                    this.text(first.file() + ":" + first.line() + ": " + first.message(), line)), line);
        }
        this.process.program0().add(built.program());
        final Values.Obj chunkStatics = this.process.staticsOf(chunkType);
        for (final java.util.Map.Entry<String, String> constant : built.constants().entrySet()) {
            chunkStatics.set(constant.getValue(), this.text(constant.getKey(), line));
        }
        if (arguments.length > 3) {
            chunkStatics.set(ENVIRONMENT, arguments[3]);
        }
        final Values.Table remembered = this.table(line);
        remembered.put(1L, this.text(chunkType, line));
        remembered.put(2L, this.text(name, line));
        remembered.put(3L, this.text(text, line));
        this.resized(remembered, line);
        loaded.put(loaded.length() + 1, remembered);
        this.resized(loaded, line);
        final String main = LuaEmitter.mainFunctionOf(chunkType);
        final Values.Obj closure = new Values.Obj(main);
        this.heap.allocate(closure, Heap.HEADER, line);
        final Values.DelegateValue made = new Values.DelegateValue(main,
                List.of(new Values.Bound(closure, main, INVOKE, INVOKE_PARAMETERS, "object")));
        return this.heap.allocate(made, made.bytes(), line);
    }

    /* How a loaded chunk names itself in its errors: its name as given, or its text's first line. */
    private static String chunkName(final String given) {
        if (given.startsWith("=") || given.startsWith("@")) {
            return given.substring(1);
        }
        final int newline = given.indexOf('\n');
        final String first = newline < 0 ? given : given.substring(0, newline) + "...";
        return "[string \"" + (first.length() > 40 ? first.substring(0, 37) + "..." : first) + "\"]";
    }

    /**
     * Compiles again what a program had loaded, before the rest of it is brought back after a
     * reload: its frames may be in the middle of those chunks.
     */
    static void reload(final Loaded program, final Object loaded) {
        if (!(loaded instanceof Values.Table chunks)) {
            return;
        }
        for (long i = 1; i <= chunks.length(); i++) {
            if (chunks.get(i) instanceof Values.Table one && one.get(1L) instanceof String chunkType
                    && one.get(2L) instanceof String name && one.get(3L) instanceof String text) {
                final LuaCompiler.Result built = LuaCompiler.compile(new SourceFile(name, text), chunkType);
                if (built.ok()) {
                    program.add(built.program());
                }
            }
        }
    }

    /** Makes the table of globals with the whole library in it, and remembers it for the program. */
    private Values.Table globals(final int line) {
        final Values.Obj statics = this.runtimeStatics();
        if (statics.get(GLOBALS) instanceof Values.Table existing) {
            return existing;
        }
        final Values.Table globals = this.table(line);
        statics.set(GLOBALS, globals);
        LuaLib.install(this, globals, line);
        final Values.Table forText = this.table(line);
        forText.put(this.text("__index", line), globals.get("string"));
        this.resized(forText, line);
        statics.set(STRING_META, forText);
        this.resized(globals, line);
        return globals;
    }

    @Override
    public Values.DelegateValue function(final String name, final Object target, final int line) {
        final Values.Bound bound = new Values.Bound(target, OWNER, name, INVOKE_PARAMETERS, "object");
        final Values.DelegateValue made = new Values.DelegateValue(FUNCTION_TYPE, List.of(bound));
        return this.heap.allocate(made, made.bytes(), line);
    }

    @Override
    public Values.Obj state(final String next, final String failed, final int line) {
        final Values.Obj made = new Values.Obj("Lua.0state");
        made.set(LuaCall.NEXT, this.text(next, line));
        if (failed != null) {
            made.set(LuaCall.FAILED, this.text(failed, line));
        }
        this.heap.allocate(made, Heap.HEADER + 6L * Heap.REFERENCE, line);
        return made;
    }

    @Override
    public String text(final String value, final int line) {
        return this.process.textOnHeap(value, line);
    }

    @Override
    public long tick() {
        return this.process.library().hostTick();
    }

    @Override
    public long dayTime() {
        return this.process.library().hostDayTime();
    }

    @Override
    public long day() {
        return this.process.library().hostDay();
    }

    @Override
    public long spent() {
        return this.process.spent();
    }

    @Override
    public Random random() {
        return this.random;
    }

    @Override
    public void seed(final long seed) {
        this.random = new Random(seed);
    }

    @Override
    public long heldBytes() {
        return this.heap.used();
    }

    @Override
    public long collect() {
        return this.heap.collectNow();
    }
}
