/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.sem.BuiltIns;
import dev.jstech.computers.sigma.sem.IMemberSymbol;
import dev.jstech.computers.sigma.sem.NamedType;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.listing.ListingProblem;
import dev.jstech.computers.vm.program.ProgramImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The listings the compiler writes for a corpus of programs that between them use every member of the language's
 * library and machine types, held against the listings it wrote before.
 *
 * <p>Where those types are declared can move without changing what any program compiles to, and this is what proves
 * it. A listing that differs, or is not kept yet, is written under {@code build/sigma-corpus} and the test fails, so a
 * changed listing is only ever taken in on purpose.
 */
class SigmaCorpusTest {

    /** The programs of the corpus, kept under {@code sigma/corpus} as source and as the listing each compiles to. */
    private static final List<String> PROGRAMS =
            List.of("Library", "Programs", "Machine", "Network", "Gateway", "Ui", "Steps");

    /**
     * The types the compiler declares as the language's own core, which the corpus is not about.
     *
     * <p>Both shapes of entry point are here for the same reason: their three methods are what a program that
     * stays up is made of, not something a program reaches for, and the corpus programs run at a terminal.
     */
    private static final Set<String> CORE =
            Set.of("object", "string", "List", "Map", "Action", "Func", "IScript", "Script");

    @Test
    void compile_writesTheListingsItWroteBefore() throws IOException {
        final List<String> changed = new ArrayList<>();
        for (final String name : PROGRAMS) {
            final SigmaCompiler.Result built =
                    SigmaCompiler.compile(List.of(new SourceFile(name + ".sgs", source(name + ".sgs"))));
            assertTrue(built.ok(), () -> name + " does not compile: " + built.diagnostics());
            final String kept = resource(name + ".asm");
            if (kept == null || !kept.equals(built.assembly())) {
                final Path written = Path.of("build", "sigma-corpus", name + ".asm");
                Files.createDirectories(written.getParent());
                Files.writeString(written, built.assembly(), StandardCharsets.UTF_8);
                changed.add(name + (kept == null ? " has no listing kept yet" : " compiles to a different listing")
                        + "; what it compiles to now is in " + written.toAbsolutePath());
            }
        }
        assertTrue(changed.isEmpty(), () -> String.join("\n", changed));
    }

    @Test
    void corpus_usesEveryMemberOfTheLibraryAndMachineTypes() {
        final StringBuilder all = new StringBuilder();
        for (final String name : PROGRAMS) {
            all.append(source(name + ".sgs")).append('\n');
        }
        final String text = all.toString();
        final List<String> missing = new ArrayList<>();
        for (final NamedType type : new BuiltIns().all()) {
            if (CORE.contains(type.name())) {
                continue;
            }
            for (final IMemberSymbol member : type.members()) {
                final String used = member instanceof IMemberSymbol.ConstructorSymbol
                        ? "new " + type.name() + "(" : "." + member.name();
                if (!text.contains(used)) {
                    missing.add(type.name() + "." + member.name());
                }
            }
        }
        assertTrue(missing.isEmpty(), () -> "the corpus never uses " + missing);
    }

    /*
     * A machine refuses a listing that reaches for something nothing answers, so everything the compiler writes has
     * to be answered: every call, new and value of the corpus loads with no problem at all.
     */
    @Test
    void corpus_loadsWithNothingAMachineWouldRefuse() {
        final List<String> refused = new ArrayList<>();
        for (final String name : PROGRAMS) {
            final AsmReader reader = new AsmReader(source(name + ".asm"));
            final AsmProgram program = reader.read();
            final List<ListingProblem> problems = new ArrayList<>(reader.problems());
            if (!reader.hasProblems()) {
                problems.addAll(ProgramImage.of(program).problems());
            }
            for (final ListingProblem problem : problems) {
                refused.add(name + ".asm " + problem.format());
            }
        }
        assertTrue(refused.isEmpty(), () -> String.join("\n", refused));
    }

    private static String source(final String file) {
        final String text = resource(file);
        assertNotNull(text, () -> "the corpus has no " + file);
        return text;
    }

    /** A file of the corpus with its line endings as the compiler writes them, or null when there is none. */
    private static String resource(final String file) {
        try (InputStream in = SigmaCorpusTest.class.getResourceAsStream("/sigma/corpus/" + file)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (final IOException unreadable) {
            throw new IllegalStateException("the corpus file " + file + " could not be read", unreadable);
        }
    }
}
