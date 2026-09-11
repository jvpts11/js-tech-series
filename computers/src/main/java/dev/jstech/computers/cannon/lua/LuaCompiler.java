/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua;

import dev.jstech.computers.cannon.Diagnostic;
import dev.jstech.computers.cannon.DiagnosticBag;
import dev.jstech.computers.cannon.SourceFile;
import dev.jstech.computers.cannon.asm.AsmProgram;
import dev.jstech.computers.cannon.asm.AsmWriter;
import dev.jstech.computers.cannon.lua.ast.LuaChunk;
import java.util.List;
import java.util.Map;

/**
 * Compiles a Lua file to the assembly, as {@code cannonc} does a Cannon one.
 *
 * <p>Reading, resolving and writing are separate passes, and the first that finds a mistake stops
 * the rest, as the language's own compiler does: a Lua program is told about its first error, not a
 * cascade of guesses after it.
 */
public final class LuaCompiler {

    /**
     * What a compilation gave: the listing and the program it was written from, the text constants
     * the chunk's static fields hold (each value by its field), or what was wrong.
     */
    public record Result(String assembly, AsmProgram program, Map<String, String> constants,
                         List<Diagnostic> diagnostics) {

        public Result {
            constants = constants == null ? Map.of() : Map.copyOf(constants);
            diagnostics = List.copyOf(diagnostics);
        }

        public boolean ok() {
            return this.assembly != null;
        }

        /** The diagnostics, one formatted line each. */
        public List<String> lines() {
            return this.diagnostics.stream().map(Diagnostic::format).toList();
        }
    }

    private LuaCompiler() {
    }

    /** Compiles a program file, its chunk named after the file. */
    public static Result compile(final SourceFile source) {
        return compile(source, LuaEmitter.typeNameFor(source.name()));
    }

    /** Compiles a chunk under the type name given, as {@code load} does for text a program hands it. */
    public static Result compile(final SourceFile source, final String chunkType) {
        final DiagnosticBag bag = new DiagnosticBag(source.name());
        final List<LuaToken> tokens = new LuaLexer(source, bag).tokenize();
        if (bag.hasErrors()) {
            return failed(bag);
        }
        final LuaChunk chunk = new LuaParser(tokens, bag, source.name()).parse();
        if (chunk == null || bag.hasErrors()) {
            return failed(bag);
        }
        final LuaScopes scopes = new LuaScopes(bag);
        scopes.resolve(chunk);
        if (bag.hasErrors()) {
            return failed(bag);
        }
        final LuaEmitter emitter = new LuaEmitter(chunk, scopes, source.name(), chunkType);
        final AsmProgram program = emitter.emit();
        return new Result(AsmWriter.write(program), program, emitter.constants(), bag.sorted());
    }

    private static Result failed(final DiagnosticBag bag) {
        return new Result(null, null, null, bag.sorted());
    }
}
