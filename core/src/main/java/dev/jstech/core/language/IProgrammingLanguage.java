/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.language;

import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
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
     * @param line   the line it is on, counting from one
     * @param column the column, counting from one
     */
    record Complaint(String file, int line, int column, String code, String message) {

        /** How a person reads it: {@code file(line,col): error CODE: message}. */
        public String format() {
            return file + "(" + line + "," + column + "): error " + code + ": " + message;
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
     * Breaks source into pieces an editor can colour.
     *
     * <p>Text that will not compile still has to come back sensibly, because that is most of what an
     * editor is ever asked to colour.
     */
    List<Token> tokenize(String text);

    /**
     * Starts a program on a machine.
     *
     * <p>The machine is handed over whole: what a language may reach through it is the language's own
     * business, and putting that in this contract would tie every language to what the first one needed. Only
     * asked of a language with binary extensions; one that only compiles leaves this alone.
     *
     * @param binary   the compiled text
     * @param heapBytes how much memory the program may hold at once
     * @return the running program, or null when the text cannot be run at all
     */
    @Nullable
    default ILanguageProcess start(final String binary, final long heapBytes, final BlockEntity machine) {
        return null;
    }

    /**
     * The same, with what the program was started with.
     *
     * <p>A language whose programs take no arguments may leave this alone; the arguments are then
     * simply not handed on.
     */
    @Nullable
    default ILanguageProcess start(final String binary, final long heapBytes, final BlockEntity machine,
                                   final List<String> arguments) {
        return this.start(binary, heapBytes, machine);
    }

    /**
     * Reads a program back out of what {@link ILanguageProcess#save} wrote. Only asked of a language with binary
     * extensions, like {@link #start}.
     */
    @Nullable
    default ILanguageProcess restore(final String binary, final CompoundTag saved, final BlockEntity machine) {
        return null;
    }
}
