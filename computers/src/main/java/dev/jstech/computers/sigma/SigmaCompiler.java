/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma;

import dev.jstech.computers.hardware.ArchitectureSpec;
import dev.jstech.computers.hardware.Architectures;
import dev.jstech.computers.sigma.emit.Emitter;
import dev.jstech.computers.vm.listing.AsmMethod;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmType;
import dev.jstech.computers.vm.listing.AsmWriter;
import dev.jstech.computers.vm.listing.Instruction;
import dev.jstech.computers.vm.listing.Opcode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
                analysis.declarations(), bag, analysis.lowered()).emit();
        if (bag.hasErrors()) {
            return new Result(null, bag.sorted(), bag.wasCapped());
        }
        program.setArchitecture(targetOf(program, architecture), 1);
        return new Result(AsmWriter.write(program), bag.sorted(), bag.wasCapped());
    }

    /**
     * The architecture the listing is written for: the one asked for, or the oldest that has what the program
     * turned out to need.
     *
     * <p>Asking for one is a decision somebody made and is left alone, even where an older one would have done.
     * Left alone, the program is read back for the instructions it actually uses and given the oldest machine
     * that has all of them, so it runs on everything it could have run on rather than on everything the newest
     * chip can.
     *
     * <p>An architecture nothing knows is handed back untouched. It is not this stage's to refuse: a listing for
     * a machine nobody has is refused where a machine reads it, with the name of the one it was built for.
     */
    private static String targetOf(final AsmProgram program, final String architecture) {
        final Optional<ArchitectureSpec> baseline = Architectures.byId(architecture);
        return baseline.map(spec -> Architectures.oldestWith(spec, instructionsOf(program)).id())
                .orElse(architecture);
    }

    /** Every kind of instruction the program turned out to be made of. */
    private static Set<Opcode> instructionsOf(final AsmProgram program) {
        final Set<Opcode> used = EnumSet.noneOf(Opcode.class);
        for (final AsmType type : program.types()) {
            for (final AsmMethod method : type.methods()) {
                for (final Instruction instruction : method.body()) {
                    used.add(instruction.opcode());
                }
            }
        }
        return used;
    }
}
