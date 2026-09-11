/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua;

import dev.jstech.computers.cannon.CannonError;
import dev.jstech.computers.cannon.DiagnosticBag;
import dev.jstech.computers.cannon.lua.ast.ILuaExpr;
import dev.jstech.computers.cannon.lua.ast.ILuaStmt;
import dev.jstech.computers.cannon.lua.ast.LuaBlock;
import dev.jstech.computers.cannon.lua.ast.LuaChunk;
import dev.jstech.computers.cannon.lua.ast.LuaFunctionBody;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Which name means which variable, and which variables outlive the function that declares them.
 *
 * <p>A local a nested function reaches for is kept in a record made for the block that declares it,
 * fresh each time the block is entered, so a closure made inside a loop keeps the variable of its
 * own turn. A function that reaches into a block of an outer function holds that block's record in
 * a field of its own, as does every function between the two, so the record can be handed down at
 * the moment each closure is made. Everything else is a slot of its own function.
 */
final class LuaScopes {

    /** One local variable. */
    static final class Variable {
        final String name;
        final Function owner;
        final Block block;
        boolean captured;
        int slot = -1;
        /** The field of the block's record this lives in, when captured. */
        String field;

        Variable(final String name, final Function owner, final Block block) {
            this.name = name;
            this.owner = owner;
            this.block = block;
        }
    }

    /** One block: the variables it declares, and the record they share when any is captured. */
    static final class Block {
        final Function function;
        final List<Variable> declared = new ArrayList<>();
        final List<Variable> captured = new ArrayList<>();
        /** The type of the record, or null when nothing here is captured. */
        String recordType;
        int recordSlot = -1;

        Block(final Function function) {
            this.function = function;
        }
    }

    /** One function: its parameters, its blocks, and the outer records it has to hold. */
    static final class Function {
        final Function parent;
        final LuaFunctionBody body;
        final List<Variable> parameters = new ArrayList<>();
        final Set<Block> needs = new LinkedHashSet<>();
        final Map<Block, String> fieldFor = new HashMap<>();
        boolean usesVarargs;
        String typeName;
        Block top;

        Function(final Function parent, final LuaFunctionBody body) {
            this.parent = parent;
            this.body = body;
        }
    }

    /** One level of names, as a block opens. */
    private static final class Scope {
        final Block block;
        final Map<String, Variable> names = new HashMap<>();
        final Scope outer;

        Scope(final Block block, final Scope outer) {
            this.block = block;
            this.outer = outer;
        }
    }

    private final DiagnosticBag diagnostics;
    private final Map<ILuaExpr.Name, Variable> uses = new IdentityHashMap<>();
    private final Map<LuaBlock, Block> blocks = new IdentityHashMap<>();
    private final Map<LuaFunctionBody, Function> functions = new IdentityHashMap<>();
    private final Map<Object, List<Variable>> declarations = new IdentityHashMap<>();
    private final Map<ILuaStmt.FunctionDecl, ILuaExpr.Name> headOf = new IdentityHashMap<>();
    private final List<Function> allFunctions = new ArrayList<>();
    private final List<Block> allBlocks = new ArrayList<>();
    private Scope scope;
    private Function function;
    private int loops;

    LuaScopes(final DiagnosticBag diagnostics) {
        this.diagnostics = diagnostics;
    }

    /** The variable a name reads or writes, or null for a global. */
    Variable variableOf(final ILuaExpr.Name name) {
        return this.uses.get(name);
    }

    Block blockOf(final LuaBlock block) {
        return this.blocks.get(block);
    }

    Function functionOf(final LuaFunctionBody body) {
        return this.functions.get(body);
    }

    /** The variables a statement declares: a local, a local function, or a loop's names. */
    List<Variable> declaredBy(final Object statement) {
        return this.declarations.getOrDefault(statement, List.of());
    }

    List<Function> functions() {
        return this.allFunctions;
    }

    List<Block> blocks() {
        return this.allBlocks;
    }

    void resolve(final LuaChunk chunk) {
        this.function(chunk.body());
    }

    // walking

    private void function(final LuaFunctionBody body) {
        final Function made = new Function(this.function, body);
        this.functions.put(body, made);
        this.allFunctions.add(made);
        final Function outerFunction = this.function;
        final Scope outerScope = this.scope;
        final int outerLoops = this.loops;
        this.function = made;
        this.loops = 0;
        final Block top = this.openBlock(body.body());
        made.top = top;
        for (final String parameter : body.parameters()) {
            made.parameters.add(this.declare(parameter));
        }
        this.statements(body.body());
        this.closeBlock();
        this.function = outerFunction;
        this.scope = outerScope;
        this.loops = outerLoops;
    }

    private Block openBlock(final LuaBlock block) {
        final Block made = new Block(this.function);
        this.blocks.put(block, made);
        this.allBlocks.add(made);
        this.scope = new Scope(made, this.scope);
        return made;
    }

    private void closeBlock() {
        this.scope = this.scope.outer;
    }

    private Variable declare(final String name) {
        final Variable made = new Variable(name, this.function, this.scope.block);
        this.scope.block.declared.add(made);
        this.scope.names.put(name, made);
        return made;
    }

    private void block(final LuaBlock block) {
        this.openBlock(block);
        this.statements(block);
        this.closeBlock();
    }

    private void statements(final LuaBlock block) {
        for (final ILuaStmt statement : block.statements()) {
            this.statement(statement);
        }
    }

    private void statement(final ILuaStmt statement) {
        switch (statement) {
            case ILuaStmt.Local local -> {
                for (final ILuaExpr value : local.values()) {
                    this.expression(value);
                }
                final List<Variable> declared = new ArrayList<>();
                for (final String name : local.names()) {
                    declared.add(this.declare(name));
                }
                this.declarations.put(local, declared);
            }
            case ILuaStmt.Assign assign -> {
                for (final ILuaExpr target : assign.targets()) {
                    this.expression(target);
                }
                for (final ILuaExpr value : assign.values()) {
                    this.expression(value);
                }
            }
            case ILuaStmt.CallStmt call -> this.expression(call.call());
            case ILuaStmt.Do inner -> this.block(inner.body());
            case ILuaStmt.While loop -> {
                this.expression(loop.condition());
                this.loops++;
                this.block(loop.body());
                this.loops--;
            }
            case ILuaStmt.Repeat loop -> {
                this.loops++;
                this.openBlock(loop.body());
                this.statements(loop.body());
                // The condition sees the body's names, which is why the block is still open here.
                this.expression(loop.condition());
                this.closeBlock();
                this.loops--;
            }
            case ILuaStmt.If branch -> {
                for (final ILuaStmt.Clause clause : branch.clauses()) {
                    this.expression(clause.condition());
                    this.block(clause.body());
                }
                if (branch.otherwise() != null) {
                    this.block(branch.otherwise());
                }
            }
            case ILuaStmt.NumericFor loop -> {
                this.expression(loop.start());
                this.expression(loop.limit());
                if (loop.step() != null) {
                    this.expression(loop.step());
                }
                this.loops++;
                this.openBlock(loop.body());
                this.declarations.put(loop, List.of(this.declare(loop.name())));
                this.statements(loop.body());
                this.closeBlock();
                this.loops--;
            }
            case ILuaStmt.GenericFor loop -> {
                for (final ILuaExpr value : loop.values()) {
                    this.expression(value);
                }
                this.loops++;
                this.openBlock(loop.body());
                final List<Variable> declared = new ArrayList<>();
                for (final String name : loop.names()) {
                    declared.add(this.declare(name));
                }
                this.declarations.put(loop, declared);
                this.statements(loop.body());
                this.closeBlock();
                this.loops--;
            }
            case ILuaStmt.FunctionDecl declaration -> {
                final ILuaExpr.Name head = new ILuaExpr.Name(declaration.path().getFirst(), declaration.line(),
                        declaration.column());
                this.declarations.put(declaration, List.of());
                this.uses.put(head, this.lookup(head.identifier()));
                this.headOf.put(declaration, head);
                this.function(declaration.body());
            }
            case ILuaStmt.LocalFunction declaration -> {
                this.declarations.put(declaration, List.of(this.declare(declaration.name())));
                this.function(declaration.body());
            }
            case ILuaStmt.Return give -> {
                for (final ILuaExpr value : give.values()) {
                    this.expression(value);
                }
            }
            case ILuaStmt.Break stop -> {
                if (this.loops == 0) {
                    this.diagnostics.error(stop.line(), stop.column(), CannonError.LUA_BREAK_OUTSIDE_LOOP);
                }
            }
        }
    }

    /** The name expression standing for the first part of a function declaration's path. */
    ILuaExpr.Name headOf(final ILuaStmt.FunctionDecl declaration) {
        return this.headOf.get(declaration);
    }

    private void expression(final ILuaExpr expression) {
        switch (expression) {
            case ILuaExpr.Name name -> this.uses.put(name, this.lookup(name.identifier()));
            case ILuaExpr.Index index -> {
                this.expression(index.target());
                this.expression(index.key());
            }
            case ILuaExpr.Call call -> {
                this.expression(call.callee());
                for (final ILuaExpr argument : call.arguments()) {
                    this.expression(argument);
                }
            }
            case ILuaExpr.MethodCall call -> {
                this.expression(call.target());
                for (final ILuaExpr argument : call.arguments()) {
                    this.expression(argument);
                }
            }
            case ILuaExpr.Function function -> this.function(function.body());
            case ILuaExpr.Binary binary -> {
                this.expression(binary.left());
                this.expression(binary.right());
            }
            case ILuaExpr.Logical logical -> {
                this.expression(logical.left());
                this.expression(logical.right());
            }
            case ILuaExpr.Unary unary -> this.expression(unary.operand());
            case ILuaExpr.Paren paren -> this.expression(paren.inner());
            case ILuaExpr.Table table -> {
                for (final ILuaExpr.Field field : table.fields()) {
                    if (field.key() != null) {
                        this.expression(field.key());
                    }
                    this.expression(field.value());
                }
            }
            case ILuaExpr.Vararg vararg -> {
                if (!this.function.body.varargs()) {
                    this.diagnostics.error(vararg.line(), vararg.column(), CannonError.LUA_VARARG_OUTSIDE);
                }
                this.function.usesVarargs = true;
            }
            case ILuaExpr.Nil ignored -> { }
            case ILuaExpr.Bool ignored -> { }
            case ILuaExpr.Number ignored -> { }
            case ILuaExpr.Text ignored -> { }
        }
    }

    /**
     * The variable a name means here, or null for a global. Finding it in an outer function marks it
     * captured and makes every function from there down to this one hold its block's record.
     */
    private Variable lookup(final String name) {
        for (Scope at = this.scope; at != null; at = at.outer) {
            final Variable found = at.names.get(name);
            if (found == null) {
                continue;
            }
            if (found.owner != this.function) {
                found.captured = true;
                for (Function holder = this.function; holder != null && holder != found.owner; holder = holder.parent) {
                    holder.needs.add(found.block);
                }
            }
            return found;
        }
        return null;
    }
}
