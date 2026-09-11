/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.machine;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.cannon.Diagnostic;
import dev.jstech.computers.cannon.DiagnosticBag;
import dev.jstech.computers.cannon.SourceFile;
import dev.jstech.computers.cannon.lua.LuaCompiler;
import dev.jstech.computers.cannon.lua.LuaLexer;
import dev.jstech.computers.cannon.lua.LuaToken;
import dev.jstech.computers.cannon.lua.LuaTokenKind;
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

/**
 * Lua, the language ComputerCraft computers speak, as a language this mod's machines run.
 *
 * <p>A Lua file is compiled to the same assembly as a Cannon program and run by the same runtime,
 * so it counts against the same budget, keeps its memory on the same heap and is saved with the
 * machine like any other program. There is no step between writing a Lua program and running it: a
 * {@code .lua} is both what this language reads and what it runs, and the compiling happens on the
 * way in.
 */
public final class LuaLanguage implements IProgrammingLanguage {

    public static final LuaLanguage INSTANCE = new LuaLanguage();

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "lua");
    private static final String EXTENSION = "lua";
    private static final String DEFAULT_NAME = "program.lua";
    /** How every listing begins, which is how one is told from source. */
    private static final String LISTING = ".asm ";

    private LuaLanguage() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Lua";
    }

    @Override
    public Set<String> sourceExtensions() {
        return Set.of(EXTENSION);
    }

    @Override
    public Set<String> binaryExtensions() {
        return Set.of(EXTENSION);
    }

    /**
     * Compiles the first source. A Lua program is one file and needs nothing beside it, so the others
     * an editor hands over with it (the files in the same folder) are not part of it.
     */
    @Override
    public CompileResult compile(final List<SourceText> sources) {
        if (sources.isEmpty()) {
            return CompileResult.failed(List.of());
        }
        final SourceText source = sources.getFirst();
        final LuaCompiler.Result built = LuaCompiler.compile(new SourceFile(source.name(), source.text()));
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
        final LuaLexer lexer = new LuaLexer(new SourceFile("editor", text), new DiagnosticBag("editor"));
        final List<IProgrammingLanguage.Token> out = new ArrayList<>();
        for (final LuaToken token : lexer.tokenize()) {
            if (token.is(LuaTokenKind.END_OF_FILE)) {
                break;
            }
            final Kind kind = kindOf(token);
            // A long string can span lines, and an editor colours a line at a time.
            final String[] lines = token.text().split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                if (!lines[i].isEmpty()) {
                    out.add(new IProgrammingLanguage.Token(token.line() + i, i == 0 ? token.column() : 1,
                            lines[i].length(), kind));
                }
            }
        }
        for (final LuaLexer.Comment comment : lexer.comments()) {
            out.add(new IProgrammingLanguage.Token(comment.line(), comment.column(), comment.length(), Kind.COMMENT));
        }
        out.sort(Comparator.comparingInt(IProgrammingLanguage.Token::line)
                .thenComparingInt(IProgrammingLanguage.Token::column));
        return out;
    }

    private static Kind kindOf(final LuaToken token) {
        if (token.kind().isKeyword()) {
            return Kind.KEYWORD;
        }
        return switch (token.kind()) {
            case NAME -> Kind.NAME;
            case STRING -> Kind.TEXT;
            case NUMBER -> Kind.NUMBER;
            default -> Kind.SYMBOL;
        };
    }

    @Override
    @Nullable
    public ILanguageProcess start(final String binary, final long heapBytes, final BlockEntity machine) {
        return this.start(binary, heapBytes, machine, List.of());
    }

    @Override
    @Nullable
    public ILanguageProcess start(final String binary, final long heapBytes, final BlockEntity machine,
                                  final List<String> arguments) {
        final String assembly = assemble(binary);
        return assembly == null ? null : CannonLanguage.INSTANCE.start(assembly, heapBytes, machine, arguments);
    }

    @Override
    @Nullable
    public ILanguageProcess restore(final String binary, final CompoundTag saved, final BlockEntity machine) {
        final String assembly = assemble(binary);
        return assembly == null ? null : CannonLanguage.INSTANCE.restore(assembly, saved, machine);
    }

    /*
     * A machine hands over the listing it compiled when it started the program (it knows the file's
     * name, which the program's errors quote), and that is what it keeps; handed source instead, this
     * compiles it under a name of its own.
     */
    @Nullable
    private static String assemble(final String binary) {
        if (binary.startsWith(LISTING)) {
            return binary;
        }
        final LuaCompiler.Result built = LuaCompiler.compile(new SourceFile(DEFAULT_NAME, binary));
        return built.ok() ? built.assembly() : null;
    }
}
