/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import com.mojang.logging.LogUtils;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.Diagnostic;
import dev.jstech.computers.sigma.DiagnosticBag;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.sigma.edit.CommentSpans;
import dev.jstech.computers.sigma.lex.Lexer;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.listing.Shape;
import dev.jstech.computers.vm.program.Process;
import dev.jstech.computers.vm.program.ProgramImage;
import dev.jstech.computers.vm.program.SnapshotException;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.core.language.ILanguageProcess;
import dev.jstech.core.language.IProgrammingLanguage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Σ#, as the machines of the series know it.
 *
 * <p>Everything specific to the language stays behind this: the machine asks for a program and gets
 * something it can give a share of the tick to, and knows nothing of scripts, heaps or instructions. A
 * pack that would rather its computers spoke something else takes this out of the registry and puts its
 * own in, and every part of the machines carries on working.
 */
public final class SigmaLanguage implements IProgrammingLanguage {

    /** The one instance; the registry holds it and everything else asks the registry. */
    public static final SigmaLanguage INSTANCE = new SigmaLanguage();

    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "sigma");

    private static final Logger LOGGER = LogUtils.getLogger();

    private SigmaLanguage() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Σ#";
    }

    @Override
    public Set<String> sourceExtensions() {
        return Set.of("sgs");
    }

    @Override
    public Set<String> binaryExtensions() {
        // A source file runs too: the machine compiles it on the way in.
        return Set.of("asm", "sgs");
    }

    @Override
    public CompileResult compile(final List<SourceText> sources) {
        final List<SourceFile> files = new ArrayList<>();
        for (final SourceText source : sources) {
            files.add(new SourceFile(source.name(), source.text()));
        }
        final SigmaCompiler.Result built = SigmaCompiler.compile(files);
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
            if (token.kind() == dev.jstech.computers.sigma.lex.TokenKind.END_OF_FILE) {
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
    private static Kind kindOf(final dev.jstech.computers.sigma.lex.TokenKind kind) {
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

    @Override
    @Nullable
    public ILanguageProcess start(final String binary, final long heapBytes, final BlockEntity machine) {
        return this.start(binary, heapBytes, machine, java.util.List.of());
    }

    @Override
    @Nullable
    public ILanguageProcess start(final String binary, final long heapBytes, final BlockEntity machine,
                                  final java.util.List<String> arguments) {
        final ProgramImage program = read(binary);
        if (program == null || program.entryPoint() == null) {
            return null;
        }
        final Process process = new Process(program, heapBytes, new MachineHost(machine));
        process.setArgs(arguments);
        if (program.shape() == Shape.CONSOLE) {
            process.beginStatic(program.entryPoint(), "Main");
        } else {
            final Values.Obj script = process.create(program.entryPoint());
            if (script == null) {
                return null;
            }
            process.begin(script, "OnInit");
        }
        return new SigmaProgram(process);
    }

    @Override
    @Nullable
    public ILanguageProcess restore(final String binary, final CompoundTag saved, final BlockEntity machine) {
        final ProgramImage program = read(binary);
        if (program == null) {
            return null;
        }
        try {
            return new SigmaProgram(Process.restore(program,
                    SnapshotTag.read(SigmaProgram.snapshotOf(saved)), new MachineHost(machine)));
        } catch (final SnapshotException damaged) {
            // One program that cannot come back is left out; the machine and the rest of its programs load.
            LOGGER.warn("A saved program on the machine at {} was left out: {}", machine.getBlockPos(),
                    damaged.getMessage());
            return null;
        } catch (final RuntimeException fault) {
            LOGGER.error("A saved program on the machine at {} could not be brought back and was left out",
                    machine.getBlockPos(), fault);
            return null;
        }
    }

    /** Reads a listing, or null when it is not one. */
    @Nullable
    private static ProgramImage read(final String binary) {
        final AsmReader reader = new AsmReader(binary);
        final AsmProgram program = reader.read();
        return reader.hasProblems() ? null : ProgramImage.of(program);
    }
}
