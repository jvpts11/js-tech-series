/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.language;

import dev.jstech.core.text.Text;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A language the computers of this world can be programmed in.
 *
 * <p>One of these is everything the machines need in order to treat a language as their own: what its
 * files are called, how to turn source into something runnable, how to colour it in an editor, and how
 * to start and restore a running program. An addon that registers one gets the prompt, the file
 * explorer, the terminal, the task manager, saving and the tick budget without writing any of them.
 *
 * <p>What it does NOT include is how the language works. Nothing here says anything about types,
 * memory or instructions: a language that compiles to something else entirely, or interprets its source
 * directly, fits this just as well.
 *
 * <p>A language may do nothing but compile. Its binary extensions are then empty, and what it compiles to is a listing
 * the machines run themselves: listings belong to the machines, and no language may claim their extension. A language
 * that runs its own files instead names their extensions and starts and restores its programs.
 *
 * <p>The smallest language compiles and colours, and leaves the rest to the defaults:
 *
 * <pre>{@code
 * public final class Brainstorm implements IProgrammingLanguage {
 *     public ResourceLocation id() { return ResourceLocation.fromNamespaceAndPath("myaddon", "brainstorm"); }
 *     public String displayName() { return "Brainstorm"; }
 *     public Set<String> sourceExtensions() { return Set.of("bs"); }
 *     public Set<String> binaryExtensions() { return Set.of(); } // compiles to the machines' own listing
 *     public CompileResult compile(List<SourceText> sources) { return CompileResult.of(toListing(sources)); }
 *     public List<Token> tokenize(String text) { return List.of(); } // nothing coloured
 * }
 * }</pre>
 *
 * <p>Register it from {@code CoreRegisterEvent#languages()}. Its id names it on every machine and in every saved
 * program, so it never changes once released.
 */
public interface IProgrammingLanguage {

    /** What this language is called, as a machine names it. */
    ResourceLocation id();

    /** What to call it in front of a person. */
    String displayName();

    /** The extensions a person writes in, without the dot. */
    Set<String> sourceExtensions();

    /**
     * The extensions of the files this language runs itself, without the dot: what a machine hands to {@link #start}.
     *
     * <p>Empty for a language that only compiles, whose output is a listing the machine runs.
     */
    Set<String> binaryExtensions();

    /** One file handed to a compiler: what it is called, and what is in it. */
    record SourceText(String name, String text) {
    }

    /**
     * One thing wrong with a program, where it is and what to say about it.
     *
     * <p>The message is a sentence, read in the language of whoever is shown it, as a compiler's messages are on a
     * machine set to another language; the file, the place and the code stay as they are, since those are what a
     * person looks up.
     *
     * @param line      the line it is on, counting from one
     * @param column    the column, counting from one
     * @param arguments the names and values the message was written around, in the order it names them, so a
     *                  tool that acts on a complaint reads them here rather than taking the sentence apart
     */
    @ApiStatus.Experimental
    record Complaint(String file, int line, int column, String code, Text message, List<String> arguments) {

        public Complaint {
            message = message == null ? Text.EMPTY : message;
            arguments = arguments == null ? List.of() : List.copyOf(arguments);
        }

        /** A complaint whose message stands on its own, with nothing a tool would need to read out of it. */
        public Complaint(final String file, final int line, final int column, final String code,
                         final Text message) {
            this(file, line, column, code, message, List.of());
        }

        /** How a person reads it: {@code file(line,col): error CODE: message}. */
        public Text text() {
            return LanguageTexts.COMPLAINT.with(this.file, this.line, this.column, this.code, this.message);
        }

        /** The same line in English, the form it takes as data: in a file, down a pipe, in a log. */
        public String format() {
            return this.text().english();
        }
    }

    /** What came of compiling: what to run, or what is wrong with what was written. */
    record CompileResult(String binary, List<Complaint> complaints) {

        public CompileResult {
            complaints = List.copyOf(complaints);
        }

        /** Whether there is something to run at the end of it. */
        public boolean ok() {
            return complaints.isEmpty() && !binary.isBlank();
        }

        /** A build that worked. */
        public static CompileResult of(final String binary) {
            return new CompileResult(binary, List.of());
        }

        /** One that did not. */
        public static CompileResult failed(final List<Complaint> complaints) {
            return new CompileResult("", complaints);
        }
    }

    /**
     * What a build is asked for besides its sources.
     *
     * @param isa             the instruction set to build for, such as {@code jsc:x86_64}, or empty to leave it to the
     *                        language
     * @param languageVersion the version of the language the sources are held to, or {@link #NEWEST} for the newest
     *                        the compiler knows; a language without versions reads it as nothing
     */
    @ApiStatus.Experimental
    record CompileOptions(String isa, int languageVersion) {

        /** A version of none, which leaves a build to the newest version its compiler knows. */
        public static final int NEWEST = 0;

        public CompileOptions {
            isa = isa == null ? "" : isa;
            languageVersion = Math.max(NEWEST, languageVersion);
        }

        /** A build for that instruction set, at the newest version of the language. */
        public static CompileOptions forIsa(final String isa) {
            return new CompileOptions(isa, NEWEST);
        }
    }

    /** What a piece of source is made of, for an editor to colour it by. */
    enum Kind {
        KEYWORD, NAME, TEXT, NUMBER, COMMENT, SYMBOL
    }

    /**
     * One piece of it: where it starts, how long it is, and what it is.
     *
     * <p>Counted in lines and columns from one, not in characters from the start of the file, because
     * that is how an editor holds text and how a compiler's complaints are already written.
     */
    record Token(int line, int column, int length, Kind kind) {
    }

    /** Turns source into something a machine can be asked to run. */
    CompileResult compile(List<SourceText> sources);

    /**
     * The same, built for a named instruction set architecture (ISA), such as {@code jsc:x86_64}.
     *
     * <p>A language that compiles for a processor overrides this. One that runs its own source on any machine that
     * has it installed has no instruction set to build for, and answers as it does without one, which is why this
     * is not something every language has to implement.
     */
    default CompileResult compile(final List<SourceText> sources, final String isa) {
        return compile(sources);
    }

    /**
     * The same, built as {@code options} asks: for an instruction set, and held to a version of the language.
     *
     * <p>A language with versions overrides this and refuses what came after the version asked for. One without
     * them builds as it does for the instruction set alone, which is what this does unless overridden.
     */
    @ApiStatus.Experimental
    default CompileResult compile(final List<SourceText> sources, final CompileOptions options) {
        return options.isa().isEmpty() ? compile(sources) : compile(sources, options.isa());
    }

    /**
     * Breaks source into pieces an editor can colour.
     *
     * <p>Text that will not compile still has to come back sensibly, because that is most of what an
     * editor is ever asked to colour.
     */
    List<Token> tokenize(String text);

    /**
     * Starts a program.
     *
     * <p>Only asked of a language with binary extensions; one that only compiles leaves this alone. The program reaches
     * the machine through the view it is given, which stays its own for as long as it runs.
     *
     * @param binary    the compiled text
     * @param machine   the program's view of the machine it runs on
     * @param arguments what the program was started with; a language whose programs take none ignores them
     * @return the running program, or null when the text cannot be run at all
     */
    @Nullable
    default ILanguageProcess start(final String binary, final IMachineView machine, final List<String> arguments) {
        return null;
    }

    /**
     * The version of what this language's programs write when they are saved. A machine keeps it beside each saved
     * program and hands it back to {@link #restore}, so a language that changed what it writes can still read, or
     * refuse, what an earlier version of it wrote.
     */
    default int stateVersion() {
        return 1;
    }

    /**
     * Reads a program back out of what {@link ILanguageProcess#save} wrote, with a view of the machine of its own.
     * Only asked of a language with binary extensions, like {@link #start}.
     *
     * @param version the {@link #stateVersion()} of the language that saved the program
     * @return the program carrying on, or null to leave it out; the machine and its other programs load either way
     */
    @Nullable
    default ILanguageProcess restore(final String binary, final CompoundTag saved, final int version,
                                     final IMachineView machine) {
        return null;
    }
}
