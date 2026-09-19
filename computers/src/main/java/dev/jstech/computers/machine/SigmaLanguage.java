/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.hardware.Architectures;
import dev.jstech.computers.sigma.Diagnostic;
import dev.jstech.computers.sigma.DiagnosticBag;
import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SigmaError;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.sigma.edit.CommentSpans;
import dev.jstech.computers.sigma.lex.Lexer;
import dev.jstech.computers.sigma.lex.TokenKind;
import dev.jstech.core.language.IProgrammingLanguage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Σ and Σ#, as the machines of the series know them.
 *
 * <p>Two languages and one compiler: the smaller is a true subset of the full one, so each is the same compiler
 * told how much of the language a source may be. They are two entries in the registry all the same, because an
 * editor finds a language by what a file ends in, and a {@code .sg} file deserves its colours, its complaints and
 * its builds as much as a {@code .sgs} one does. Writing for an old machine is no reason to write without them.
 *
 * <p>A language compiles and nothing else. What it compiles to is a listing, and a listing belongs to the machine
 * ({@link MachineListing}): the machine runs it, saves it and brings it back, so a machine knows nothing of scripts,
 * heaps or instructions through this. A pack that would rather its computers were written in something else takes
 * these out of the registry and puts its own in, and every part of the machines carries on working.
 */
public final class SigmaLanguage implements IProgrammingLanguage {

    private final LanguageLevel level;
    private final ResourceLocation id;

    /*
     * The oldest machine a program of this language starts out built for, before what it turned out to use is
     * allowed to push it up. The smaller language exists for the oldest machines of all.
     */
    private final String baseline;

    /** The full language; the registry holds it and everything else asks the registry. */
    public static final SigmaLanguage SIGMA_SHARP = new SigmaLanguage(LanguageLevel.SIGMA_SHARP);

    /** The smaller language, which builds for the earliest machines unless a project says otherwise. */
    public static final SigmaLanguage SIGMA = new SigmaLanguage(LanguageLevel.SIGMA);

    private SigmaLanguage(final LanguageLevel level) {
        this.level = level;
        this.id = ResourceLocation.parse(level.id());
        this.baseline = Architectures.oldestFor(level).id();
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public String displayName() {
        return this.level.mark();
    }

    @Override
    public Set<String> sourceExtensions() {
        return Set.of(this.level.sourceExtension());
    }

    @Override
    public Set<String> binaryExtensions() {
        // It runs nothing itself: a listing is the machine's, and the machine compiles a source file on the way in.
        return Set.of();
    }

    /** How much of the language this entry takes. */
    public LanguageLevel level() {
        return this.level;
    }

    @Override
    public CompileResult compile(final List<SourceText> sources) {
        return compile(sources, this.baseline);
    }

    @Override
    public CompileResult compile(final List<SourceText> sources, final String architecture) {
        /*
         * The oldest machines run the smaller language and nothing else, and that is kept true where a listing is
         * made and not at the machine: one they could load can only have come from a source they could have held.
         * The prompt's compiler says the same thing, so a studio pointed at those machines is told as plainly.
         */
        if (this.level.full() && Architectures.X86_16.id().equals(architecture)) {
            final String first = sources.isEmpty() ? "" : sources.getFirst().name();
            return CompileResult.failed(List.of(new Complaint(first, 1, 1,
                    SigmaError.OLDEST_MACHINES_TAKE_SIGMA.code(),
                    SigmaError.OLDEST_MACHINES_TAKE_SIGMA.message(Architectures.X86_16.name()))));
        }
        final List<SourceFile> files = new ArrayList<>();
        for (final SourceText source : sources) {
            files.add(new SourceFile(source.name(), source.text()));
        }
        final SigmaCompiler.Result built = SigmaCompiler.compile(files, architecture, this.level);
        if (built.ok()) {
            return CompileResult.of(built.assembly());
        }
        final List<Complaint> complaints = new ArrayList<>();
        for (final Diagnostic one : built.diagnostics()) {
            complaints.add(new Complaint(one.file(), one.line(), one.column(), one.code(), one.message()));
        }
        return CompileResult.failed(complaints);
    }

    @Override
    public List<IProgrammingLanguage.Token> tokenize(final String text) {
        /*
         * An editor mostly colours text that does not compile, so whatever the lexer complains about is
         * thrown away and the pieces it did make sense of are handed back.
         */
        final DiagnosticBag bag = new DiagnosticBag("editor");
        final List<IProgrammingLanguage.Token> out = new ArrayList<>();
        /*
         * Both Token and Kind are names this interface itself declares, so the language's own are
         * written out in full rather than imported into a fight with them.
         */
        for (final dev.jstech.computers.sigma.lex.Token token
                : new Lexer(new SourceFile("editor", text), bag).tokenize()) {
            if (token.kind() == TokenKind.END_OF_FILE) {
                break;
            }
            out.add(new IProgrammingLanguage.Token(token.line(), token.column(),
                    token.text().length(), kindOf(token.kind())));
        }
        /*
         * The lexer drops comments the way it drops spaces, because nothing that runs a program cares
         * where they were. An editor is the one caller that does, so they are found over the raw text
         * and added here, leaving the compiler's own reading exactly as it was.
         */
        for (final CommentSpans.Span span : CommentSpans.find(text)) {
            out.add(new IProgrammingLanguage.Token(span.line(), span.column(), span.length(), Kind.COMMENT));
        }
        out.sort(Comparator.comparingInt(IProgrammingLanguage.Token::line)
                .thenComparingInt(IProgrammingLanguage.Token::column));
        return out;
    }

    /** What an editor should paint that piece of text as. */
    private static Kind kindOf(final TokenKind kind) {
        if (kind.isKeyword()) {
            return Kind.KEYWORD;
        }
        return switch (kind) {
            case IDENTIFIER -> Kind.NAME;
            case STRING_LITERAL, CHAR_LITERAL -> Kind.TEXT;
            case INT_LITERAL, LONG_LITERAL, FLOAT_LITERAL, DOUBLE_LITERAL -> Kind.NUMBER;
            default -> Kind.SYMBOL;
        };
    }
}
