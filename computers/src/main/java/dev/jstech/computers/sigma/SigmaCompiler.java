/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma;

import dev.jstech.computers.sigma.emit.Emitter;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * The compiler, end to end: source files in, an assembly listing out.
 *
 * <p>This is what the shell command runs. Each stage only starts when the one before it found
 * nothing wrong, because a program that does not parse says nothing reliable about its types, and a
 * program whose types do not add up cannot be written down as instructions.
 */
public final class SigmaCompiler {

    private SigmaCompiler() {
    }

    /** What compiling produced: the listing when it worked, and everything the compiler had to say. */
    public record Result(String assembly, List<Diagnostic> diagnostics, boolean truncated) {

        public Result {
            diagnostics = List.copyOf(diagnostics);
        }

        /** Whether there is a listing to run. */
        public boolean ok() {
            return this.assembly != null && this.diagnostics.stream().noneMatch(Diagnostic::isError);
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

    /** Compiles a whole program, which is a set of files with one class the runtime can start. */
    public static Result compile(final List<SourceFile> sources) {
        return compile(sources, AsmProgram.DEFAULT_ARCHITECTURE);
    }

    /**
     * The same, built for that architecture.
     *
     * <p>Left alone, a program is built for the oldest architecture that runs it, so that it runs on every machine
     * it could have run on. Building for a newer one is a decision somebody makes, which is why it is asked for.
     */
    public static Result compile(final List<SourceFile> sources, final String architecture) {
        return compile(sources, architecture, LanguageLevel.SIGMA_SHARP);
    }

    /**
     * The same, holding the sources to as much of the language as {@code level} allows.
     *
     * <p>What comes out is the same either way. The smaller language is a subset, so a source it accepts means
     * exactly what it means to the bigger one and is written out as exactly the same listing; the level decides
     * what a source may be, never what it compiles to.
     */
    public static Result compile(final List<SourceFile> sources, final String architecture,
                                 final LanguageLevel level) {
        final DiagnosticBag bag = new DiagnosticBag(sources.isEmpty() ? "" : sources.getFirst().name());
        final SigmaSemantics.Analysis analysis = SigmaSemantics.analyse(sources, bag, true, false, level);
        if (bag.hasErrors()) {
            return new Result(null, bag.sorted(), bag.wasCapped());
        }
        final AsmProgram program = new Emitter(analysis.model(), analysis.rules(), analysis.builtIns(),
                analysis.declarations(), bag).emit();
        if (bag.hasErrors()) {
            return new Result(null, bag.sorted(), bag.wasCapped());
        }
        program.setArchitecture(architecture, 1);
        return new Result(AsmWriter.write(program), bag.sorted(), bag.wasCapped());
    }
}
