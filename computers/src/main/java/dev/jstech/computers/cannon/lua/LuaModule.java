/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua;

import dev.jstech.computers.cannon.DiagnosticBag;
import dev.jstech.computers.cannon.SourceFile;
import dev.jstech.computers.cannon.asm.AsmMethod;
import dev.jstech.computers.cannon.asm.AsmProgram;
import dev.jstech.computers.cannon.asm.AsmType;
import dev.jstech.computers.cannon.asm.IOperand;
import dev.jstech.computers.cannon.asm.Instruction;
import dev.jstech.computers.cannon.asm.Opcode;
import dev.jstech.computers.cannon.lex.TokenKind;
import dev.jstech.computers.cannon.lua.ast.ILuaExpr;
import dev.jstech.computers.cannon.lua.ast.ILuaStmt;
import dev.jstech.computers.cannon.lua.ast.LuaChunk;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A Lua file a Cannon program includes, as the Cannon side sees it.
 *
 * <p>The file becomes a type named after it, with a static method for every function it declares at
 * its top level and a static field for every other global it sets there. The methods take any
 * values and give back an {@code object}; each one asks the runtime to call the Lua function of that
 * name, after running the file's top level once, the first time anything of it is used. What the
 * file does elsewhere (a global set inside a function) is still reachable from Lua, only not by name
 * from Cannon, since nothing short of running it could say what it is.
 */
public final class LuaModule {

    /** The type every included file's facade stands on, which is how the runtime knows one. */
    public static final String MARKER = LuaEmitter.RUNTIME + ".0module";

    /** The most arguments a facade method takes. */
    public static final int MOST_ARGUMENTS = 8;

    /** How many more arguments than it names a function taking {@code ...} can be handed. */
    private static final int SPARE_FOR_VARARGS = 4;

    /** A function the file declares at its top level: how many parameters it names, and whether it takes more. */
    public record Function(String name, int arity, boolean varargs) {

        /** The most arguments the facade lets a call hand it. */
        public int mostArguments() {
            return Math.min(MOST_ARGUMENTS, this.arity + (this.varargs ? SPARE_FOR_VARARGS : 0));
        }
    }

    private final String file;
    private final String name;
    private final LuaChunk chunk;
    private final LuaScopes scopes;
    private final List<Function> functions;
    private final List<String> globals;

    private LuaModule(final String file, final LuaChunk chunk, final LuaScopes scopes) {
        this.file = file;
        this.name = facadeNameFor(file);
        this.chunk = chunk;
        this.scopes = scopes;
        final Map<String, Function> declared = new LinkedHashMap<>();
        final List<String> set = new ArrayList<>();
        for (final ILuaStmt statement : chunk.body().body().statements()) {
            if (statement instanceof ILuaStmt.FunctionDecl declaration && declaration.path().size() == 1
                    && declaration.method() == null && scopes.variableOf(scopes.headOf(declaration)) == null) {
                declared.putIfAbsent(declaration.path().getFirst(), new Function(declaration.path().getFirst(),
                        declaration.body().parameters().size(), declaration.body().varargs()));
            } else if (statement instanceof ILuaStmt.Assign assign) {
                for (int i = 0; i < assign.targets().size(); i++) {
                    if (!(assign.targets().get(i) instanceof ILuaExpr.Name target) || scopes.variableOf(target) != null) {
                        continue;
                    }
                    final ILuaExpr value = i < assign.values().size() ? assign.values().get(i) : null;
                    if (value instanceof ILuaExpr.Function function) {
                        declared.putIfAbsent(target.identifier(), new Function(target.identifier(),
                                function.body().parameters().size(), function.body().varargs()));
                    } else if (!set.contains(target.identifier())) {
                        set.add(target.identifier());
                    }
                }
            }
        }
        this.functions = new ArrayList<>();
        for (final Function function : declared.values()) {
            if (usable(function.name())) {
                this.functions.add(function);
            }
        }
        this.globals = new ArrayList<>();
        for (final String global : set) {
            if (usable(global) && !declared.containsKey(global)) {
                this.globals.add(global);
            }
        }
    }

    /**
     * Reads an included file; null when it does not read, with what was wrong in the diagnostics
     * under the file's own name.
     */
    public static LuaModule read(final SourceFile source, final DiagnosticBag diagnostics) {
        diagnostics.setFile(source.name());
        final List<LuaToken> tokens = new LuaLexer(source, diagnostics).tokenize();
        if (diagnostics.hasErrors()) {
            return null;
        }
        final LuaChunk chunk = new LuaParser(tokens, diagnostics, source.name()).parse();
        if (chunk == null) {
            return null;
        }
        final LuaScopes scopes = new LuaScopes(diagnostics);
        scopes.resolve(chunk);
        return diagnostics.hasErrors() ? null : new LuaModule(source.name(), chunk, scopes);
    }

    /** The name a file is known by in Cannon: its name without folders or extension, made a name. */
    public static String facadeNameFor(final String fileName) {
        return LuaEmitter.typeNameFor(fileName).substring(LuaEmitter.RUNTIME.length() + 1);
    }

    /** The type the file's own code is compiled to, from the name of its facade. */
    public static String chunkTypeOf(final String facade) {
        return LuaEmitter.RUNTIME + "." + facade;
    }

    /* A name Cannon code can write: not one of its keywords. */
    private static boolean usable(final String name) {
        return TokenKind.keyword(name) == null;
    }

    public String file() {
        return this.file;
    }

    /** What Cannon calls the file: {@code reactor} for {@code reactor.lua}. */
    public String name() {
        return this.name;
    }

    public List<Function> functions() {
        return List.copyOf(this.functions);
    }

    /** The other globals the file sets at its top level, which Cannon reads and writes as static fields. */
    public List<String> globals() {
        return List.copyOf(this.globals);
    }

    /**
     * What the file adds to the program: its own compiled code, the facade Cannon calls, and the type
     * the facade stands on.
     */
    public List<AsmType> types() {
        final String chunkType = chunkTypeOf(this.name);
        final AsmProgram compiled = new LuaEmitter(this.chunk, this.scopes, this.file, chunkType).emit();
        final List<AsmType> out = new ArrayList<>(compiled.types());
        out.add(new AsmType(AsmType.Kind.INTERFACE, MARKER));
        final AsmType facade = new AsmType(AsmType.Kind.CLASS, this.name);
        facade.addBase(MARKER);
        for (final Function function : this.functions) {
            for (int count = 0; count <= function.mostArguments(); count++) {
                facade.addMethod(facadeMethod(chunkType, function.name(), count));
            }
        }
        out.add(facade);
        return out;
    }

    /* One way of calling a function: that many arguments, gathered into a run and handed to the runtime. */
    private static AsmMethod facadeMethod(final String chunkType, final String function, final int count) {
        final List<Instruction> body = new ArrayList<>();
        body.add(Instruction.of(Opcode.LDSTR, new IOperand.Text(chunkType)));
        body.add(Instruction.of(Opcode.LDSTR, new IOperand.Text(function)));
        body.add(Instruction.of(Opcode.LDC_I4, new IOperand.I4(count)));
        body.add(Instruction.of(Opcode.NEWARR, new IOperand.Type("object")));
        for (int i = 0; i < count; i++) {
            body.add(Instruction.of(Opcode.DUP));
            body.add(Instruction.of(Opcode.LDC_I4, new IOperand.I4(i)));
            body.add(Instruction.of(Opcode.LDLOC, new IOperand.Slot(i)));
            body.add(Instruction.of(Opcode.STELEM));
        }
        body.add(Instruction.of(Opcode.CALL, new IOperand.Method(LuaEmitter.RUNTIME, "ModuleCall",
                List.of("object", "object", "object"), "object")));
        body.add(Instruction.of(Opcode.RET));
        final List<String> parameters = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            parameters.add("object");
        }
        return new AsmMethod(function, "object", parameters, true, count, body);
    }
}
