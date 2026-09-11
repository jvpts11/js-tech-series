/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon;

import dev.jstech.computers.cannon.ast.CompilationUnit;
import dev.jstech.computers.cannon.lua.LuaModule;
import dev.jstech.computers.cannon.sem.BodyChecker;
import dev.jstech.computers.cannon.sem.BuiltIns;
import dev.jstech.computers.cannon.sem.Declarations;
import dev.jstech.computers.cannon.sem.SemanticModel;
import dev.jstech.computers.cannon.sem.TypeRules;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads a set of files and works out what they mean: the door into the middle of the compiler.
 *
 * <p>There are two ways in because there are two things a player might be compiling. A program is a
 * set of files with exactly one class the runtime can start, and that is what the compiler builds.
 * A set of classes on their own is still worth checking, which is what an editor does while the
 * program is half written, and asking it for an entry point it does not have yet would be noise.
 */
public final class CannonSemantics {

    private CannonSemantics() {
    }

    /**
     * What checking produced: the model, everything the compiler had to say about the files, and the
     * tree read from each file, by the file's name.
     */
    public record Result(SemanticModel model, List<Diagnostic> diagnostics, boolean truncated,
                         Map<String, CompilationUnit> units) {

        public Result {
            diagnostics = List.copyOf(diagnostics);
            units = Map.copyOf(units);
        }

        /** Whether the files can go on to the next stage. */
        public boolean ok() {
            return this.diagnostics.stream().noneMatch(Diagnostic::isError);
        }

        /** The tree read from the file called {@code name}, or null when no file had that name. */
        public CompilationUnit unit(final String name) {
            return this.units.get(name);
        }

        /** The messages as the console prints them, one per line, plus a note if any were dropped. */
        public List<String> lines() {
            final List<String> lines = new ArrayList<>();
            for (final Diagnostic diagnostic : this.diagnostics) {
                lines.add(diagnostic.format());
            }
            if (this.truncated) {
                lines.add("too many errors; the rest were not reported");
            }
            return lines;
        }
    }

    /** Checks a set of classes: every type, every member and every body, but no entry point. */
    public static Result check(final List<SourceFile> sources) {
        return analyse(sources, false);
    }

    /** Checks a whole program, which also means it has exactly one class the runtime can start. */
    public static Result checkProgram(final List<SourceFile> sources) {
        return analyse(sources, true);
    }

    /**
     * Checks what it can of sources that may not parse, for an editor.
     *
     * <p>A file being typed is broken most of the time, and an editor still has to say what the
     * program's types are and what their members take. The parser leaves out what it could not read
     * and keeps the rest, so the checker runs over that; what it says about the mistakes is less
     * certain than after a clean parse, which is why the compiler proper stops at the parser.
     */
    public static Result checkTolerant(final List<SourceFile> sources) {
        final DiagnosticBag bag = new DiagnosticBag(sources.isEmpty() ? "" : sources.getFirst().name());
        return result(sources, bag, analyse(sources, bag, false, true));
    }

    private static Result analyse(final List<SourceFile> sources, final boolean wholeProgram) {
        final DiagnosticBag bag = new DiagnosticBag(sources.isEmpty() ? "" : sources.getFirst().name());
        return result(sources, bag, analyse(sources, bag, wholeProgram));
    }

    private static Result result(final List<SourceFile> sources, final DiagnosticBag bag, final Analysis analysis) {
        final Map<String, CompilationUnit> units = new LinkedHashMap<>();
        for (final CompilationUnit unit : analysis.units()) {
            units.putIfAbsent(unit.file(), unit);
        }
        return new Result(analysis.model(), bag.sorted(), bag.wasCapped(), units);
    }

    /**
     * Everything the middle of the compiler built, for the stage that writes the assembly.
     *
     * <p>The stage after this one needs more than the model: it has to resolve a type it meets in a
     * cast, and ask what two numbers meet in, which is what these carry. The trees come along, one per
     * Cannon source in the order the sources were given, for an editor asking where in a file a caret
     * is, and so do the Lua files the program includes, whose code goes into the same program.
     */
    record Analysis(SemanticModel model, BuiltIns builtIns, TypeRules rules, Declarations declarations,
                    List<CompilationUnit> units, List<LuaModule> modules) {

        Analysis(final SemanticModel model, final BuiltIns builtIns, final TypeRules rules,
                 final Declarations declarations, final List<CompilationUnit> units) {
            this(model, builtIns, rules, declarations, units, List.of());
        }
    }

    /** Whether a source is a Lua file, which the program only takes in where something includes it. */
    static boolean isLua(final String name) {
        return name.toLowerCase(Locale.ROOT).endsWith(".lua");
    }

    private static String leaf(final String path) {
        final int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return (slash < 0 ? path : path.substring(slash + 1)).toLowerCase(Locale.ROOT);
    }

    /*
     * Every Lua file a Cannon file includes, read once however many include it, and declared as a type
     * named after it. An include names a file by its path; it is found among the sources by the file's
     * own name, since whoever compiles gathers them from beside the file that includes them.
     */
    private static List<LuaModule> includes(final List<CompilationUnit> units, final Map<String, SourceFile> lua,
                                            final Declarations declarations, final DiagnosticBag bag) {
        final List<LuaModule> modules = new ArrayList<>();
        final Map<String, LuaModule> read = new LinkedHashMap<>();
        for (final CompilationUnit unit : units) {
            for (final CompilationUnit.Include include : unit.includes()) {
                final String key = leaf(include.path());
                if (read.containsKey(key)) {
                    continue;
                }
                final SourceFile file = lua.get(key);
                if (file == null) {
                    bag.setFile(unit.file());
                    bag.error(include.line(), include.column(), CannonError.INCLUDE_NOT_FOUND, include.path());
                    continue;
                }
                final LuaModule module = LuaModule.read(file, bag);
                read.put(key, module);
                if (module == null) {
                    continue;
                }
                if (!declarations.declareModule(module)) {
                    bag.setFile(unit.file());
                    bag.error(include.line(), include.column(), CannonError.INCLUDE_NAME_TAKEN, module.name(),
                            include.path());
                    continue;
                }
                modules.add(module);
            }
        }
        return modules;
    }

    /** Reads and checks into a bag the caller owns, and hands back what the next stage needs. */
    static Analysis analyse(final List<SourceFile> sources, final DiagnosticBag bag,
                            final boolean wholeProgram) {
        return analyse(sources, bag, wholeProgram, false);
    }

    private static Analysis analyse(final List<SourceFile> sources, final DiagnosticBag bag,
                                    final boolean wholeProgram, final boolean tolerant) {
        final List<CompilationUnit> units = new ArrayList<>();
        final Map<String, SourceFile> lua = new LinkedHashMap<>();
        for (final SourceFile source : sources) {
            if (isLua(source.name())) {
                lua.putIfAbsent(leaf(source.name()), source);
                continue;
            }
            bag.setFile(source.name());
            units.add(CannonFrontEnd.parse(source, bag));
        }

        final SemanticModel model = new SemanticModel();
        final BuiltIns builtIns = new BuiltIns();
        final TypeRules rules = new TypeRules(builtIns);
        final Declarations declarations = new Declarations(builtIns, rules, bag, model);
        /*
         * A tree the parser had to guess its way through says nothing reliable about types, so the
         * player gets the mistakes that are certainly there rather than the ones that follow from them.
         * An editor asks anyway, since a file being typed is that tree most of the time.
         */
        if (bag.hasErrors() && !tolerant) {
            return new Analysis(model, builtIns, rules, declarations, units);
        }
        declarations.declare(units);
        final List<LuaModule> modules = includes(units, lua, declarations, bag);
        /*
         * A Lua file that does not read leaves nothing for the Cannon code to call, and every use of it
         * would be a second complaint about the first one, so its own mistakes are what is reported.
         */
        if (bag.hasErrors() && !tolerant) {
            return new Analysis(model, builtIns, rules, declarations, units, modules);
        }
        declarations.fill();
        declarations.checkInterfaces();
        new BodyChecker(builtIns, rules, declarations, bag, model).check(model.declaredTypes());
        if (wholeProgram) {
            bag.setFile(sources.isEmpty() ? "" : sources.getFirst().name());
            declarations.checkEntryPoint(1, 1);
        }
        return new Analysis(model, builtIns, rules, declarations, units, modules);
    }
}
