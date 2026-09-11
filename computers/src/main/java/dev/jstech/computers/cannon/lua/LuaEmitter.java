/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua;

import dev.jstech.computers.cannon.Shape;
import dev.jstech.computers.cannon.asm.AsmMethod;
import dev.jstech.computers.cannon.asm.AsmProgram;
import dev.jstech.computers.cannon.asm.AsmType;
import dev.jstech.computers.cannon.asm.IOperand;
import dev.jstech.computers.cannon.asm.Instruction;
import dev.jstech.computers.cannon.asm.Opcode;
import dev.jstech.computers.cannon.lua.ast.ILuaExpr;
import dev.jstech.computers.cannon.lua.ast.ILuaStmt;
import dev.jstech.computers.cannon.lua.ast.LuaBlock;
import dev.jstech.computers.cannon.lua.ast.LuaChunk;
import dev.jstech.computers.cannon.lua.ast.LuaFunctionBody;
import dev.jstech.computers.cannon.lua.lib.LuaNumbers;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes a Lua chunk as the same assembly Cannon compiles to.
 *
 * <p>Every Lua value is an {@code object}, so the emitter never needs a type: each operator, index
 * and call is a call on {@code Lua}, which the runtime answers with the language's rules and its
 * metatables. Every function is a class of its own with one method, {@code object Invoke(object[])},
 * that takes its arguments as a run and gives back one value or a run of them; a function value is a
 * delegate bound to an instance of that class.
 *
 * <p>A local that a nested function reaches for lives in a record made for the block that declares
 * it, fresh each time the block is entered, and the function holds the record in a field; every other
 * local is a slot. Text constants are made once, in the chunk's static fields, so a program that
 * reads {@code t.name} in a loop does not make a new {@code "name"} every turn.
 *
 * <p>The first instruction of every source line carries the file and the line as its comment, which
 * is how an error names its place; a call or an index also carries what it reaches for, which is how
 * an error says {@code (global 'foo')}.
 */
public final class LuaEmitter {

    /** The owner of every call the emitter writes. */
    public static final String RUNTIME = "Lua";

    /** The type every Lua program carries, with the carrying methods the runtime runs in. */
    public static final String RUNTIME_TYPE = "Lua.0runtime";

    private static final String OBJECT = "object";
    private static final String VOID = "void";
    private static final String INVOKE = "Invoke";
    private static final List<String> INVOKE_PARAMETERS = List.of("object[]");
    private static final List<List<String>> ARITIES = List.of(List.of(), List.of(OBJECT),
            List.of(OBJECT, OBJECT), List.of(OBJECT, OBJECT, OBJECT));

    private final LuaChunk chunk;
    private final LuaScopes scopes;
    private final String source;
    private final String chunkType;
    private final AsmProgram program = new AsmProgram();
    private final AsmType chunkClass;
    private final Map<String, String> constants = new LinkedHashMap<>();
    private final Map<LuaScopes.Block, Integer> blockIds = new IdentityHashMap<>();

    LuaEmitter(final LuaChunk chunk, final LuaScopes scopes, final String source, final String chunkType) {
        this.chunk = chunk;
        this.scopes = scopes;
        this.source = source;
        this.chunkType = chunkType;
        this.chunkClass = new AsmType(AsmType.Kind.CLASS, this.chunkType);
    }

    /** The text constants the chunk's static fields hold, each by the field it is kept in. */
    Map<String, String> constants() {
        return Map.copyOf(this.constants);
    }

    /** The type of the function a chunk compiles to, which is what running the chunk calls. */
    public static String mainFunctionOf(final String chunkType) {
        return chunkType + ".0f0";
    }

    /** The chunk a function's type belongs to, or null for a type that is not a Lua function's. */
    public static String chunkOf(final String functionType) {
        final int at = functionType.lastIndexOf(".0f");
        return at < 0 ? null : functionType.substring(0, at);
    }

    /**
     * The type a file's chunk is compiled to: {@code Lua.} and the file's name without its extension,
     * with anything that is not a letter, a digit or an underscore made one, and never starting with a
     * digit, so it can never be taken for one of the runtime's own types.
     */
    public static String typeNameFor(final String fileName) {
        String bare = fileName;
        final int slash = Math.max(bare.lastIndexOf('/'), bare.lastIndexOf('\\'));
        if (slash >= 0) {
            bare = bare.substring(slash + 1);
        }
        final int dot = bare.lastIndexOf('.');
        if (dot > 0) {
            bare = bare.substring(0, dot);
        }
        final StringBuilder name = new StringBuilder();
        for (int i = 0; i < bare.length(); i++) {
            final char c = bare.charAt(i);
            name.append(Character.isLetterOrDigit(c) && c < 128 || c == '_' ? c : '_');
        }
        if (name.isEmpty() || Character.isDigit(name.charAt(0))) {
            name.insert(0, '_');
        }
        return RUNTIME + "." + name;
    }

    AsmProgram emit() {
        int count = 0;
        for (final LuaScopes.Function function : this.scopes.functions()) {
            function.typeName = this.chunkType + ".0f" + count++;
        }
        int block = 0;
        int variable = 0;
        for (final LuaScopes.Block each : this.scopes.blocks()) {
            this.blockIds.put(each, block);
            for (final LuaScopes.Variable declared : each.declared) {
                if (declared.captured) {
                    declared.field = declared.name + "_" + variable++;
                    each.captured.add(declared);
                }
            }
            if (!each.captured.isEmpty()) {
                each.recordType = this.chunkType + ".0b" + block;
                final AsmType record = new AsmType(AsmType.Kind.CLASS, each.recordType);
                for (final LuaScopes.Variable held : each.captured) {
                    record.addField(new AsmType.Field(held.field, OBJECT, false));
                }
                this.program.addType(record);
            }
            block++;
        }
        this.program.addType(runtimeType());
        this.program.addType(this.chunkClass);
        final LuaScopes.Function top = this.scopes.functionOf(this.chunk.body());
        this.emitFunction(top);
        this.chunkClass.addMethod(new AsmMethod("Main", VOID, List.of(), true, 0, List.of(
                Instruction.of(Opcode.NEWOBJ, new IOperand.Constructor(top.typeName, List.of())),
                Instruction.of(Opcode.LDFN, new IOperand.Method(top.typeName, INVOKE, INVOKE_PARAMETERS, OBJECT)),
                runtimeCall("ScriptArgs", 0, OBJECT),
                runtimeCall("Call", 2, OBJECT),
                Instruction.of(Opcode.POP),
                runtimeCall("Flush", 0, VOID),
                Instruction.of(Opcode.RET))));
        final List<Instruction> setUp = new ArrayList<>();
        for (final Map.Entry<String, String> constant : this.constants.entrySet()) {
            this.chunkClass.addField(new AsmType.Field(constant.getValue(), OBJECT, true));
            setUp.add(Instruction.of(Opcode.LDSTR, new IOperand.Text(constant.getKey())));
            setUp.add(Instruction.of(Opcode.STSFLD, new IOperand.Field(this.chunkType, constant.getValue())));
        }
        setUp.add(Instruction.of(Opcode.RET));
        this.chunkClass.addMethod(new AsmMethod(this.chunkType, VOID, List.of(), true, 0, setUp));
        this.program.setEntryPoint(this.chunkType, Shape.CONSOLE);
        return this.program;
    }

    /* The methods the runtime runs its carrying frames in: after a call, around a wait, as a coroutine. */
    private static AsmType runtimeType() {
        final AsmType runtime = new AsmType(AsmType.Kind.CLASS, RUNTIME_TYPE);
        for (final String field : List.of("G", "StringMeta", "Out", "Empty")) {
            runtime.addField(new AsmType.Field(field, OBJECT, true));
        }
        runtime.addMethod(new AsmMethod("0resume", OBJECT, List.of(OBJECT), true, 1, List.of(
                Instruction.of(Opcode.LDLOC, new IOperand.Slot(0)),
                runtimeCall("Continue", 2, OBJECT),
                Instruction.of(Opcode.RET))));
        runtime.addMethod(new AsmMethod("0park", OBJECT, List.of(OBJECT, OBJECT), true, 2, List.of(
                Instruction.of(Opcode.LDLOC, new IOperand.Slot(0)),
                Instruction.of(Opcode.LDLOC, new IOperand.Slot(1)),
                runtimeCall("Call", 2, OBJECT),
                Instruction.of(Opcode.RET))));
        runtime.addMethod(new AsmMethod("0coroutine", VOID, List.of(OBJECT, OBJECT), true, 2, List.of(
                Instruction.of(Opcode.LDLOC, new IOperand.Slot(0)),
                Instruction.of(Opcode.LDLOC, new IOperand.Slot(1)),
                runtimeCall("Call", 2, OBJECT),
                runtimeCall("CoReturn", 1, OBJECT),
                Instruction.of(Opcode.POP),
                Instruction.of(Opcode.RET))));
        return runtime;
    }

    private static Instruction runtimeCall(final String name, final int arity, final String returns) {
        return Instruction.of(Opcode.CALL, new IOperand.Method(RUNTIME, name, ARITIES.get(arity), returns));
    }

    private String recordField(final LuaScopes.Block block) {
        return "r" + this.blockIds.get(block);
    }

    private String constantField(final String value) {
        return this.constants.computeIfAbsent(value, ignored -> "K" + this.constants.size());
    }

    private void emitFunction(final LuaScopes.Function function) {
        final AsmType type = new AsmType(AsmType.Kind.CLASS, function.typeName);
        for (final LuaScopes.Block needed : function.needs) {
            type.addField(new AsmType.Field(this.recordField(needed), OBJECT, false));
        }
        this.program.addType(type);
        final Body body = new Body(function);
        body.prologue();
        body.statements(function.body.body());
        body.call("None", 0);
        body.emit(Opcode.RET);
        final List<Instruction> code = body.finish();
        type.addMethod(new AsmMethod(INVOKE, OBJECT, INVOKE_PARAMETERS, false, body.slots, code));
    }

    /** The code of one function, as it is written. */
    private final class Body {

        private final LuaScopes.Function function;
        private final List<Instruction> code = new ArrayList<>();
        private final Map<String, String> aliases = new HashMap<>();
        private final Deque<String> breaks = new ArrayDeque<>();
        private final Deque<Integer> free = new ArrayDeque<>();
        private String pending;
        private String description;
        private int slots = 1;
        private int labels;
        private int varargSlot = -1;
        private int line;
        private int placed = -1;

        Body(final LuaScopes.Function function) {
            this.function = function;
        }

        // the shape of the method

        void prologue() {
            final LuaBlock top = this.function.body.body();
            this.line = this.function.body.line();
            this.enter(top);
            final List<LuaScopes.Variable> parameters = this.function.parameters;
            for (int i = 0; i < parameters.size(); i++) {
                final int index = i;
                this.assign(parameters.get(i), () -> {
                    this.emit(Opcode.LDLOC, new IOperand.Slot(0));
                    this.emit(Opcode.LDC_I4, new IOperand.I4(index));
                    this.call("Arg", 2);
                });
            }
            if (this.function.usesVarargs) {
                if (parameters.isEmpty()) {
                    // With nothing named, what the function was given is all of it, as it came.
                    this.varargSlot = 0;
                } else {
                    this.varargSlot = this.slots++;
                    this.emit(Opcode.LDLOC, new IOperand.Slot(0));
                    this.emit(Opcode.LDC_I4, new IOperand.I4(parameters.size()));
                    this.call("Rest", 2);
                    this.emit(Opcode.STLOC, new IOperand.Slot(this.varargSlot));
                }
            }
        }

        List<Instruction> finish() {
            if (this.pending != null) {
                this.emit(Opcode.RET);
            }
            final List<Instruction> resolved = new ArrayList<>(this.code.size());
            for (final Instruction instruction : this.code) {
                if (instruction.operand() instanceof IOperand.Label label) {
                    resolved.add(new Instruction(instruction.label(), instruction.opcode(),
                            new IOperand.Label(this.resolve(label.name())), instruction.comment()));
                } else {
                    resolved.add(instruction);
                }
            }
            return resolved;
        }

        private String resolve(final String label) {
            String at = label;
            while (this.aliases.containsKey(at)) {
                at = this.aliases.get(at);
            }
            return at;
        }

        // instructions

        void emit(final Opcode opcode) {
            this.add(Instruction.of(opcode));
        }

        void emit(final Opcode opcode, final IOperand operand) {
            this.add(Instruction.of(opcode, operand));
        }

        private void add(final Instruction instruction) {
            Instruction written = instruction;
            if (this.pending != null) {
                written = written.labelled(this.pending);
                this.pending = null;
            }
            String comment = null;
            if (this.line > 0 && this.line != this.placed) {
                comment = LuaEmitter.this.source + ":" + this.line;
                this.placed = this.line;
            }
            if (this.description != null) {
                comment = comment == null ? this.description : comment + "; " + this.description;
                this.description = null;
            }
            if (comment != null) {
                written = written.saying(comment);
            }
            this.code.add(written);
        }

        void call(final String name, final int arity) {
            this.emit(Opcode.CALL, new IOperand.Method(RUNTIME, name, ARITIES.get(arity), OBJECT));
        }

        void callVoid(final String name, final int arity) {
            this.emit(Opcode.CALL, new IOperand.Method(RUNTIME, name, ARITIES.get(arity), VOID));
        }

        /** A call that carries what it reaches for, so an error can say which. */
        void callDescribed(final String name, final int arity, final String what) {
            this.description = what;
            this.call(name, arity);
        }

        String label() {
            return "L" + this.labels++;
        }

        void mark(final String label) {
            if (this.pending == null) {
                this.pending = label;
            } else {
                this.aliases.put(label, this.pending);
            }
        }

        void branch(final Opcode opcode, final String label) {
            this.emit(opcode, new IOperand.Label(label));
        }

        int temp() {
            return this.free.isEmpty() ? this.slots++ : this.free.pop();
        }

        void release(final int slot) {
            this.free.push(slot);
        }

        private int slot(final LuaScopes.Variable variable) {
            if (variable.slot < 0) {
                variable.slot = this.slots++;
            }
            return variable.slot;
        }

        private int recordSlot(final LuaScopes.Block block) {
            if (block.recordSlot < 0) {
                block.recordSlot = this.slots++;
            }
            return block.recordSlot;
        }

        void constant(final String value) {
            this.emit(Opcode.LDSFLD, new IOperand.Field(LuaEmitter.this.chunkType,
                    LuaEmitter.this.constantField(value)));
        }

        // variables

        /** Makes the record of a block whose locals a nested function keeps, each time it is entered. */
        void enter(final LuaBlock block) {
            final LuaScopes.Block scope = LuaEmitter.this.scopes.blockOf(block);
            if (scope != null && scope.recordType != null) {
                this.emit(Opcode.NEWOBJ, new IOperand.Constructor(scope.recordType, List.of()));
                this.emit(Opcode.STLOC, new IOperand.Slot(this.recordSlot(scope)));
            }
        }

        private void pushRecord(final LuaScopes.Block block) {
            if (block.function == this.function) {
                this.emit(Opcode.LDLOC, new IOperand.Slot(this.recordSlot(block)));
            } else {
                this.emit(Opcode.LDTHIS);
                this.emit(Opcode.LDFLD, new IOperand.Field(this.function.typeName,
                        LuaEmitter.this.recordField(block)));
            }
        }

        void load(final LuaScopes.Variable variable) {
            if (variable.captured) {
                this.pushRecord(variable.block);
                this.emit(Opcode.LDFLD, new IOperand.Field(variable.block.recordType, variable.field));
            } else {
                this.emit(Opcode.LDLOC, new IOperand.Slot(this.slot(variable)));
            }
        }

        void assign(final LuaScopes.Variable variable, final Runnable value) {
            if (variable.captured) {
                this.pushRecord(variable.block);
                value.run();
                this.emit(Opcode.STFLD, new IOperand.Field(variable.block.recordType, variable.field));
            } else {
                value.run();
                this.emit(Opcode.STLOC, new IOperand.Slot(this.slot(variable)));
            }
        }

        // statements

        void statements(final LuaBlock block) {
            for (final ILuaStmt statement : block.statements()) {
                this.statement(statement);
            }
        }

        void block(final LuaBlock block) {
            this.enter(block);
            this.statements(block);
        }

        private void statement(final ILuaStmt statement) {
            this.line = statement.line();
            switch (statement) {
                case ILuaStmt.Local local -> this.local(local);
                case ILuaStmt.Assign assign -> this.assignment(assign);
                case ILuaStmt.CallStmt call -> {
                    this.multi(call.call());
                    this.emit(Opcode.POP);
                }
                case ILuaStmt.Do inner -> this.block(inner.body());
                case ILuaStmt.While loop -> {
                    final String top = this.label();
                    final String end = this.label();
                    this.mark(top);
                    this.jump(loop.condition(), end, false);
                    this.breaks.push(end);
                    this.block(loop.body());
                    this.breaks.pop();
                    this.branch(Opcode.BR, top);
                    this.mark(end);
                }
                case ILuaStmt.Repeat loop -> {
                    final String top = this.label();
                    final String end = this.label();
                    this.mark(top);
                    this.breaks.push(end);
                    this.enter(loop.body());
                    this.statements(loop.body());
                    this.breaks.pop();
                    this.line = loop.condition().line();
                    this.jump(loop.condition(), top, false);
                    this.mark(end);
                }
                case ILuaStmt.If branch -> {
                    final String end = this.label();
                    for (final ILuaStmt.Clause clause : branch.clauses()) {
                        final String next = this.label();
                        this.line = clause.line();
                        this.jump(clause.condition(), next, false);
                        this.block(clause.body());
                        this.branch(Opcode.BR, end);
                        this.mark(next);
                    }
                    if (branch.otherwise() != null) {
                        this.block(branch.otherwise());
                    }
                    this.mark(end);
                }
                case ILuaStmt.NumericFor loop -> this.numericFor(loop);
                case ILuaStmt.GenericFor loop -> this.genericFor(loop);
                case ILuaStmt.FunctionDecl declaration -> this.functionDecl(declaration);
                case ILuaStmt.LocalFunction declaration -> {
                    final LuaScopes.Variable variable =
                            LuaEmitter.this.scopes.declaredBy(declaration).getFirst();
                    this.assign(variable, () -> this.closure(declaration.body()));
                }
                case ILuaStmt.Return give -> this.give(give);
                case ILuaStmt.Break ignored -> {
                    if (!this.breaks.isEmpty()) {
                        this.branch(Opcode.BR, this.breaks.peek());
                    }
                }
            }
        }

        private void local(final ILuaStmt.Local local) {
            final List<LuaScopes.Variable> variables = LuaEmitter.this.scopes.declaredBy(local);
            this.spread(local.values(), variables.size(), (index, value) -> this.assign(variables.get(index), value));
        }

        /**
         * Hands out a list of values to {@code wanted} places: the last value, when it can give several,
         * fills every place left; a place with nothing for it gets nil; a value with no place is still
         * worked out, for what it does, and dropped.
         */
        private void spread(final List<ILuaExpr> values, final int wanted, final IPlace place) {
            final int count = values.size();
            for (int i = 0; i < count; i++) {
                final ILuaExpr value = values.get(i);
                final boolean last = i == count - 1;
                if (last && isMulti(value)) {
                    final int remaining = wanted - i;
                    if (remaining <= 0) {
                        this.multi(value);
                        this.emit(Opcode.POP);
                    } else if (remaining == 1) {
                        place.put(i, () -> {
                            this.multi(value);
                            this.call("First", 1);
                        });
                    } else {
                        this.multi(value);
                        this.emit(Opcode.LDC_I4, new IOperand.I4(remaining));
                        this.call("Take", 2);
                        final int held = this.temp();
                        this.emit(Opcode.STLOC, new IOperand.Slot(held));
                        for (int k = 0; k < remaining; k++) {
                            final int index = k;
                            place.put(i + k, () -> {
                                this.emit(Opcode.LDLOC, new IOperand.Slot(held));
                                this.emit(Opcode.LDC_I4, new IOperand.I4(index));
                                this.emit(Opcode.LDELEM);
                            });
                        }
                        this.release(held);
                    }
                    return;
                }
                if (i < wanted) {
                    place.put(i, () -> this.single(value));
                } else {
                    this.single(value);
                    this.emit(Opcode.POP);
                }
            }
            for (int i = count; i < wanted; i++) {
                place.put(i, () -> this.emit(Opcode.LDNULL));
            }
        }

        private void assignment(final ILuaStmt.Assign assign) {
            final List<ILuaExpr> targets = assign.targets();
            if (targets.size() == 1 && assign.values().size() == 1) {
                this.store(targets.getFirst(), () -> this.single(assign.values().getFirst()));
                return;
            }
            /*
             * Everything on the right is worked out before anything on the left is written, so that
             * a, b = b, a swaps; the tables and keys on the left are worked out first of all.
             */
            final int[][] places = new int[targets.size()][];
            for (int i = 0; i < targets.size(); i++) {
                if (targets.get(i) instanceof ILuaExpr.Index index) {
                    final int table = this.temp();
                    this.single(index.target());
                    this.emit(Opcode.STLOC, new IOperand.Slot(table));
                    final int key = this.temp();
                    this.single(index.key());
                    this.emit(Opcode.STLOC, new IOperand.Slot(key));
                    places[i] = new int[] {table, key};
                }
            }
            final int[] values = new int[targets.size()];
            for (int i = 0; i < values.length; i++) {
                values[i] = this.temp();
            }
            this.spread(assign.values(), targets.size(), (index, value) -> {
                value.run();
                this.emit(Opcode.STLOC, new IOperand.Slot(values[index]));
            });
            for (int i = 0; i < targets.size(); i++) {
                final int held = values[i];
                final Runnable value = () -> this.emit(Opcode.LDLOC, new IOperand.Slot(held));
                if (places[i] != null) {
                    this.emit(Opcode.LDLOC, new IOperand.Slot(places[i][0]));
                    this.emit(Opcode.LDLOC, new IOperand.Slot(places[i][1]));
                    value.run();
                    this.call("SetIndex", 3);
                    this.emit(Opcode.POP);
                    this.release(places[i][0]);
                    this.release(places[i][1]);
                } else {
                    this.store(targets.get(i), value);
                }
                this.release(held);
            }
        }

        /** Writes a value into a name or a field. */
        private void store(final ILuaExpr target, final Runnable value) {
            if (target instanceof ILuaExpr.Name name) {
                final LuaScopes.Variable variable = LuaEmitter.this.scopes.variableOf(name);
                if (variable != null) {
                    this.assign(variable, value);
                    return;
                }
                this.constant(name.identifier());
                value.run();
                this.call("SetGlobal", 2);
                this.emit(Opcode.POP);
                return;
            }
            final ILuaExpr.Index index = (ILuaExpr.Index) target;
            this.single(index.target());
            this.single(index.key());
            value.run();
            this.callDescribed("SetIndex", 3, this.describe(index.target()));
            this.emit(Opcode.POP);
        }

        private void numericFor(final ILuaStmt.NumericFor loop) {
            this.single(loop.start());
            this.single(loop.limit());
            if (loop.step() == null) {
                this.emit(Opcode.LDC_I8, new IOperand.I8(1L));
            } else {
                this.single(loop.step());
            }
            this.call("ForPrep", 3);
            final int prepared = this.temp();
            this.emit(Opcode.STLOC, new IOperand.Slot(prepared));
            final int index = this.temp();
            final int limit = this.temp();
            final int step = this.temp();
            final int[] parts = {index, limit, step};
            for (int i = 0; i < parts.length; i++) {
                this.emit(Opcode.LDLOC, new IOperand.Slot(prepared));
                this.emit(Opcode.LDC_I4, new IOperand.I4(i));
                this.emit(Opcode.LDELEM);
                this.emit(Opcode.STLOC, new IOperand.Slot(parts[i]));
            }
            this.release(prepared);
            final String top = this.label();
            final String end = this.label();
            this.mark(top);
            this.emit(Opcode.LDLOC, new IOperand.Slot(index));
            this.emit(Opcode.LDLOC, new IOperand.Slot(limit));
            this.emit(Opcode.LDLOC, new IOperand.Slot(step));
            this.call("ForOn", 3);
            this.branch(Opcode.BRFALSE, end);
            this.enter(loop.body());
            final LuaScopes.Variable variable = LuaEmitter.this.scopes.declaredBy(loop).getFirst();
            this.assign(variable, () -> this.emit(Opcode.LDLOC, new IOperand.Slot(index)));
            this.breaks.push(end);
            this.statements(loop.body());
            this.breaks.pop();
            this.emit(Opcode.LDLOC, new IOperand.Slot(index));
            this.emit(Opcode.LDLOC, new IOperand.Slot(step));
            this.call("Add", 2);
            this.emit(Opcode.STLOC, new IOperand.Slot(index));
            this.branch(Opcode.BR, top);
            this.mark(end);
            this.release(step);
            this.release(limit);
            this.release(index);
        }

        private void genericFor(final ILuaStmt.GenericFor loop) {
            this.list(loop.values());
            this.emit(Opcode.LDC_I4, new IOperand.I4(3));
            this.call("Take", 2);
            final int three = this.temp();
            this.emit(Opcode.STLOC, new IOperand.Slot(three));
            final int function = this.temp();
            final int state = this.temp();
            final int control = this.temp();
            final int[] parts = {function, state, control};
            for (int i = 0; i < parts.length; i++) {
                this.emit(Opcode.LDLOC, new IOperand.Slot(three));
                this.emit(Opcode.LDC_I4, new IOperand.I4(i));
                this.emit(Opcode.LDELEM);
                this.emit(Opcode.STLOC, new IOperand.Slot(parts[i]));
            }
            this.release(three);
            final String top = this.label();
            final String end = this.label();
            this.mark(top);
            this.emit(Opcode.LDLOC, new IOperand.Slot(function));
            this.emit(Opcode.LDC_I4, new IOperand.I4(2));
            this.emit(Opcode.NEWARR, new IOperand.Type(OBJECT));
            for (int i = 0; i < 2; i++) {
                this.emit(Opcode.DUP);
                this.emit(Opcode.LDC_I4, new IOperand.I4(i));
                this.emit(Opcode.LDLOC, new IOperand.Slot(i == 0 ? state : control));
                this.emit(Opcode.STELEM);
            }
            this.callDescribed("Call", 2, "for iterator 'for iterator'");
            final List<LuaScopes.Variable> variables = LuaEmitter.this.scopes.declaredBy(loop);
            final int results = variables.size() > 1 ? this.temp() : -1;
            if (results < 0) {
                this.call("First", 1);
                this.emit(Opcode.STLOC, new IOperand.Slot(control));
            } else {
                this.emit(Opcode.LDC_I4, new IOperand.I4(variables.size()));
                this.call("Take", 2);
                this.emit(Opcode.STLOC, new IOperand.Slot(results));
                this.emit(Opcode.LDLOC, new IOperand.Slot(results));
                this.emit(Opcode.LDC_I4, new IOperand.I4(0));
                this.emit(Opcode.LDELEM);
                this.emit(Opcode.STLOC, new IOperand.Slot(control));
            }
            // The loop ends when the first value is nil, and only nil: false goes round like anything else.
            this.emit(Opcode.LDLOC, new IOperand.Slot(control));
            this.emit(Opcode.LDNULL);
            this.branch(Opcode.BEQ, end);
            this.enter(loop.body());
            for (int k = 0; k < variables.size(); k++) {
                final int index = k;
                this.assign(variables.get(k), () -> {
                    if (index == 0) {
                        this.emit(Opcode.LDLOC, new IOperand.Slot(control));
                    } else {
                        this.emit(Opcode.LDLOC, new IOperand.Slot(results));
                        this.emit(Opcode.LDC_I4, new IOperand.I4(index));
                        this.emit(Opcode.LDELEM);
                    }
                });
            }
            if (results >= 0) {
                this.release(results);
            }
            this.breaks.push(end);
            this.statements(loop.body());
            this.breaks.pop();
            this.branch(Opcode.BR, top);
            this.mark(end);
            this.release(control);
            this.release(state);
            this.release(function);
        }

        private void functionDecl(final ILuaStmt.FunctionDecl declaration) {
            final List<String> path = declaration.path();
            final ILuaExpr.Name head = LuaEmitter.this.scopes.headOf(declaration);
            if (path.size() == 1 && declaration.method() == null) {
                this.store(head, () -> this.closure(declaration.body()));
                return;
            }
            this.single(head);
            final int last = declaration.method() != null ? path.size() : path.size() - 1;
            for (int i = 1; i < last; i++) {
                this.constant(path.get(i));
                this.callDescribed("Index", 2, i == 1 ? this.describe(head) : field(path.get(i - 1)));
            }
            this.constant(declaration.method() != null ? declaration.method() : path.getLast());
            this.closure(declaration.body());
            this.callDescribed("SetIndex", 3, last == 1 ? this.describe(head) : field(path.get(last - 1)));
            this.emit(Opcode.POP);
        }

        private void give(final ILuaStmt.Return give) {
            final List<ILuaExpr> values = give.values();
            if (values.isEmpty()) {
                this.call("None", 0);
            } else if (values.size() == 1) {
                if (isMulti(values.getFirst())) {
                    this.multi(values.getFirst());
                } else {
                    this.single(values.getFirst());
                }
            } else {
                this.list(values);
            }
            this.emit(Opcode.RET);
        }

        // expressions

        /** Leaves exactly one value. */
        void single(final ILuaExpr expression) {
            if (expression.line() > 0) {
                this.line = expression.line();
            }
            switch (expression) {
                case ILuaExpr.Nil ignored -> this.emit(Opcode.LDNULL);
                case ILuaExpr.Bool flag -> this.call(flag.value() ? "True" : "False", 0);
                case ILuaExpr.Number number -> this.number(number.value());
                case ILuaExpr.Text text -> this.constant(text.value());
                case ILuaExpr.Vararg ignored -> {
                    this.emit(Opcode.LDLOC, new IOperand.Slot(this.varargSlot));
                    this.call("First", 1);
                }
                case ILuaExpr.Name name -> {
                    final LuaScopes.Variable variable = LuaEmitter.this.scopes.variableOf(name);
                    if (variable != null) {
                        this.load(variable);
                    } else {
                        this.constant(name.identifier());
                        this.call("GetGlobal", 1);
                    }
                }
                case ILuaExpr.Index index -> {
                    this.single(index.target());
                    this.single(index.key());
                    this.callDescribed("Index", 2, this.describe(index.target()));
                }
                case ILuaExpr.Call ignored -> {
                    this.multi(expression);
                    this.call("First", 1);
                }
                case ILuaExpr.MethodCall ignored -> {
                    this.multi(expression);
                    this.call("First", 1);
                }
                case ILuaExpr.Function function -> this.closure(function.body());
                case ILuaExpr.Binary binary -> {
                    this.single(binary.left());
                    this.single(binary.right());
                    this.call(operatorName(binary.operator()), 2);
                }
                case ILuaExpr.Logical logical -> {
                    final String end = this.label();
                    this.single(logical.left());
                    this.emit(Opcode.DUP);
                    this.call("Truth", 1);
                    this.branch(logical.and() ? Opcode.BRFALSE : Opcode.BRTRUE, end);
                    this.emit(Opcode.POP);
                    this.single(logical.right());
                    this.mark(end);
                }
                case ILuaExpr.Unary unary -> this.unary(unary);
                case ILuaExpr.Paren paren -> this.single(paren.inner());
                case ILuaExpr.Table table -> this.table(table);
            }
        }

        private void number(final Object value) {
            if (value instanceof Long whole) {
                this.emit(Opcode.LDC_I8, new IOperand.I8(whole));
            } else {
                this.emit(Opcode.LDC_R8, new IOperand.R8((Double) value));
            }
        }

        private void unary(final ILuaExpr.Unary unary) {
            if (unary.operator() == LuaTokenKind.MINUS && unary.operand() instanceof ILuaExpr.Number number) {
                this.number(LuaNumbers.negate(number.value()));
                return;
            }
            this.single(unary.operand());
            this.call(switch (unary.operator()) {
                case NOT -> "Not";
                case MINUS -> "Unm";
                default -> "Len";
            }, 1);
        }

        /** Leaves what the expression gives: several values when it is a call or {@code ...}, else one. */
        void multi(final ILuaExpr expression) {
            if (expression.line() > 0) {
                this.line = expression.line();
            }
            switch (expression) {
                case ILuaExpr.Call call -> {
                    this.single(call.callee());
                    this.list(call.arguments());
                    this.callDescribed("Call", 2, this.describe(call.callee()));
                }
                case ILuaExpr.MethodCall call -> this.methodCall(call);
                case ILuaExpr.Vararg ignored -> this.emit(Opcode.LDLOC, new IOperand.Slot(this.varargSlot));
                default -> this.single(expression);
            }
        }

        private void methodCall(final ILuaExpr.MethodCall call) {
            this.single(call.target());
            final int self = this.temp();
            this.emit(Opcode.STLOC, new IOperand.Slot(self));
            this.emit(Opcode.LDLOC, new IOperand.Slot(self));
            this.constant(call.name());
            this.callDescribed("Index", 2, this.describe(call.target()));
            final List<ILuaExpr> arguments = call.arguments();
            final boolean spreads = !arguments.isEmpty() && isMulti(arguments.getLast());
            final int fixed = 1 + (spreads ? arguments.size() - 1 : arguments.size());
            this.emit(Opcode.LDC_I4, new IOperand.I4(fixed));
            this.emit(Opcode.NEWARR, new IOperand.Type(OBJECT));
            this.emit(Opcode.DUP);
            this.emit(Opcode.LDC_I4, new IOperand.I4(0));
            this.emit(Opcode.LDLOC, new IOperand.Slot(self));
            this.emit(Opcode.STELEM);
            this.release(self);
            for (int i = 0; i < fixed - 1; i++) {
                this.emit(Opcode.DUP);
                this.emit(Opcode.LDC_I4, new IOperand.I4(i + 1));
                this.single(arguments.get(i));
                this.emit(Opcode.STELEM);
            }
            if (spreads) {
                this.multi(arguments.getLast());
                this.call("Append", 2);
            }
            this.callDescribed("Call", 2, isName(call.name()) ? "method '" + call.name() + "'" : null);
        }

        /**
         * Leaves the values as one run, the last one spread: a call or {@code ...} at the end gives all
         * it has. A single call is handed on as it came, since it already is what a run would be.
         */
        void list(final List<ILuaExpr> values) {
            if (values.isEmpty()) {
                this.call("None", 0);
                return;
            }
            final ILuaExpr last = values.getLast();
            final boolean spreads = isMulti(last);
            if (values.size() == 1 && spreads) {
                this.multi(last);
                return;
            }
            final int fixed = spreads ? values.size() - 1 : values.size();
            this.emit(Opcode.LDC_I4, new IOperand.I4(fixed));
            this.emit(Opcode.NEWARR, new IOperand.Type(OBJECT));
            for (int i = 0; i < fixed; i++) {
                this.emit(Opcode.DUP);
                this.emit(Opcode.LDC_I4, new IOperand.I4(i));
                this.single(values.get(i));
                this.emit(Opcode.STELEM);
            }
            if (spreads) {
                this.multi(last);
                this.call("Append", 2);
            }
        }

        private void table(final ILuaExpr.Table table) {
            this.call("NewTable", 0);
            long position = 1;
            final List<ILuaExpr.Field> fields = table.fields();
            for (int i = 0; i < fields.size(); i++) {
                final ILuaExpr.Field field = fields.get(i);
                this.emit(Opcode.DUP);
                if (field.key() != null) {
                    this.single(field.key());
                    this.single(field.value());
                    this.callVoid("RawSet", 3);
                } else if (i == fields.size() - 1 && isMulti(field.value())) {
                    this.emit(Opcode.LDC_I8, new IOperand.I8(position));
                    this.multi(field.value());
                    this.callVoid("RawSetAll", 3);
                } else {
                    this.emit(Opcode.LDC_I8, new IOperand.I8(position++));
                    this.single(field.value());
                    this.callVoid("RawSet", 3);
                }
            }
        }

        /** Makes a function value: an instance of the function's class holding the records it reaches into. */
        void closure(final LuaFunctionBody body) {
            final LuaScopes.Function made = LuaEmitter.this.scopes.functionOf(body);
            LuaEmitter.this.emitFunction(made);
            this.emit(Opcode.NEWOBJ, new IOperand.Constructor(made.typeName, List.of()));
            for (final LuaScopes.Block needed : made.needs) {
                this.emit(Opcode.DUP);
                this.pushRecord(needed);
                this.emit(Opcode.STFLD, new IOperand.Field(made.typeName, LuaEmitter.this.recordField(needed)));
            }
            this.emit(Opcode.LDFN, new IOperand.Method(made.typeName, INVOKE, INVOKE_PARAMETERS, OBJECT));
        }

        /** Jumps to the label when the expression's truth is {@code when}, and falls through otherwise. */
        void jump(final ILuaExpr expression, final String label, final boolean when) {
            this.line = expression.line() > 0 ? expression.line() : this.line;
            switch (expression) {
                case ILuaExpr.Logical logical -> {
                    if (logical.and() != when) {
                        // (a and b) false: either is false. (a or b) true: either is true.
                        this.jump(logical.left(), label, when);
                        this.jump(logical.right(), label, when);
                    } else {
                        final String past = this.label();
                        this.jump(logical.left(), past, !when);
                        this.jump(logical.right(), label, when);
                        this.mark(past);
                    }
                }
                case ILuaExpr.Unary unary when unary.operator() == LuaTokenKind.NOT ->
                        this.jump(unary.operand(), label, !when);
                case ILuaExpr.Paren paren -> this.jump(paren.inner(), label, when);
                case ILuaExpr.Nil ignored -> {
                    if (!when) {
                        this.branch(Opcode.BR, label);
                    }
                }
                case ILuaExpr.Bool flag -> {
                    if (flag.value() == when) {
                        this.branch(Opcode.BR, label);
                    }
                }
                case ILuaExpr.Number ignored -> {
                    if (when) {
                        this.branch(Opcode.BR, label);
                    }
                }
                case ILuaExpr.Text ignored -> {
                    if (when) {
                        this.branch(Opcode.BR, label);
                    }
                }
                case ILuaExpr.Binary binary when comparison(binary.operator()) -> {
                    this.single(binary);
                    this.branch(when ? Opcode.BRTRUE : Opcode.BRFALSE, label);
                }
                default -> {
                    this.single(expression);
                    this.call("Truth", 1);
                    this.branch(when ? Opcode.BRTRUE : Opcode.BRFALSE, label);
                }
            }
        }

        /** What an expression is, as an error names it: a global, a local, an upvalue or a field. */
        private String describe(final ILuaExpr expression) {
            if (expression instanceof ILuaExpr.Name name) {
                final LuaScopes.Variable variable = LuaEmitter.this.scopes.variableOf(name);
                if (variable == null) {
                    return "global '" + name.identifier() + "'";
                }
                return (variable.owner == this.function ? "local '" : "upvalue '") + name.identifier() + "'";
            }
            if (expression instanceof ILuaExpr.Index index && index.key() instanceof ILuaExpr.Text key) {
                return field(key.value());
            }
            if (expression instanceof ILuaExpr.MethodCall call) {
                return isName(call.name()) ? "method '" + call.name() + "'" : null;
            }
            return null;
        }
    }

    private static String field(final String name) {
        return isName(name) ? "field '" + name + "'" : null;
    }

    /* Only a plain name is written into a comment, so nothing a program writes can break a listing's line. */
    private static boolean isName(final String text) {
        if (text.isEmpty() || !(Character.isLetter(text.charAt(0)) || text.charAt(0) == '_')) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (!(c < 128 && (Character.isLetterOrDigit(c) || c == '_'))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isMulti(final ILuaExpr expression) {
        return expression instanceof ILuaExpr.Call || expression instanceof ILuaExpr.MethodCall
                || expression instanceof ILuaExpr.Vararg;
    }

    private static boolean comparison(final LuaTokenKind operator) {
        return switch (operator) {
            case EQUAL, NOT_EQUAL, LESS, LESS_EQUAL, GREATER, GREATER_EQUAL -> true;
            default -> false;
        };
    }

    private static String operatorName(final LuaTokenKind operator) {
        return switch (operator) {
            case PLUS -> "Add";
            case MINUS -> "Sub";
            case STAR -> "Mul";
            case SLASH -> "Div";
            case DOUBLE_SLASH -> "IDiv";
            case PERCENT -> "Mod";
            case CARET -> "Pow";
            case CONCAT -> "Concat";
            case EQUAL -> "Eq";
            case NOT_EQUAL -> "Ne";
            case LESS -> "Lt";
            case LESS_EQUAL -> "Le";
            case GREATER -> "Gt";
            default -> "Ge";
        };
    }

    /** Puts one value into one of a list of places. */
    @FunctionalInterface
    private interface IPlace {

        void put(int index, Runnable value);
    }
}
