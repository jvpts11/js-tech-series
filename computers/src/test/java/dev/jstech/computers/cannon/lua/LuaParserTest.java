/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.jstech.computers.cannon.Diagnostic;
import dev.jstech.computers.cannon.DiagnosticBag;
import dev.jstech.computers.cannon.SourceFile;
import dev.jstech.computers.cannon.lua.ast.ILuaExpr;
import dev.jstech.computers.cannon.lua.ast.ILuaStmt;
import dev.jstech.computers.cannon.lua.ast.LuaChunk;
import java.util.List;
import org.junit.jupiter.api.Test;

class LuaParserTest {

    private DiagnosticBag bag;

    private LuaChunk parse(final String text) {
        this.bag = new DiagnosticBag("test.lua");
        final SourceFile source = new SourceFile("test.lua", text);
        return new LuaParser(new LuaLexer(source, this.bag).tokenize(), this.bag, "test.lua").parse();
    }

    private ILuaExpr valueOf(final String expression) {
        final LuaChunk chunk = this.parse("return " + expression);
        assertNotNull(chunk, () -> this.bag.sorted().toString());
        return ((ILuaStmt.Return) chunk.body().body().statements().getFirst()).values().getFirst();
    }

    private List<String> codes() {
        return this.bag.sorted().stream().map(Diagnostic::code).toList();
    }

    @Test
    void parse_givesMultiplicationPrecedenceOverAddition() {
        final ILuaExpr.Binary sum = assertInstanceOf(ILuaExpr.Binary.class, this.valueOf("1 + 2 * 3"));
        assertEquals(LuaTokenKind.PLUS, sum.operator());
        assertInstanceOf(ILuaExpr.Binary.class, sum.right());
    }

    @Test
    void parse_bindsPowersAndConcatenationToTheRight() {
        final ILuaExpr.Binary power = assertInstanceOf(ILuaExpr.Binary.class, this.valueOf("2 ^ 3 ^ 2"));
        assertInstanceOf(ILuaExpr.Number.class, power.left());
        final ILuaExpr.Binary joined = assertInstanceOf(ILuaExpr.Binary.class, this.valueOf("'a' .. 'b' .. 'c'"));
        assertInstanceOf(ILuaExpr.Text.class, joined.left());
    }

    @Test
    void parse_letsAMinusBindTighterThanAPowerOnlyOnTheRight() {
        // -2 ^ 2 is -(2 ^ 2): the power binds tighter than the minus in front of it.
        final ILuaExpr.Unary negated = assertInstanceOf(ILuaExpr.Unary.class, this.valueOf("-2 ^ 2"));
        assertInstanceOf(ILuaExpr.Binary.class, negated.operand());
    }

    @Test
    void parse_readsCallsWithoutBracketsAndMethodCalls() {
        final LuaChunk chunk = this.parse("print 'hi' f{1} obj:go(1, 2)");
        assertNotNull(chunk);
        assertEquals(3, chunk.body().body().statements().size());
        final ILuaStmt.CallStmt last = (ILuaStmt.CallStmt) chunk.body().body().statements().get(2);
        final ILuaExpr.MethodCall call = assertInstanceOf(ILuaExpr.MethodCall.class, last.call());
        assertEquals("go", call.name());
        assertEquals(2, call.arguments().size());
    }

    @Test
    void parse_readsATableWithEveryKindOfField() {
        final ILuaExpr.Table table = assertInstanceOf(ILuaExpr.Table.class, this.valueOf("{1, x = 2, [3] = 4; 5}"));
        assertEquals(4, table.fields().size());
        assertNull(table.fields().get(0).key());
        assertInstanceOf(ILuaExpr.Text.class, table.fields().get(1).key());
        assertInstanceOf(ILuaExpr.Number.class, table.fields().get(2).key());
    }

    @Test
    void parse_readsFunctionDeclarationsWithAPathAndAMethod() {
        final LuaChunk chunk = this.parse("function a.b.c:m(x) end");
        final ILuaStmt.FunctionDecl declaration =
                (ILuaStmt.FunctionDecl) chunk.body().body().statements().getFirst();
        assertEquals(List.of("a", "b", "c"), declaration.path());
        assertEquals("m", declaration.method());
        assertEquals(List.of("self", "x"), declaration.body().parameters());
    }

    @Test
    void parse_complainsLikeLuaAboutAMissingEnd() {
        assertNull(this.parse("if x then\n  y()\n"));
        assertEquals(List.of("L2002"), this.codes());
        assertEquals("'end' expected (to close 'if' at line 1) near <eof>", this.bag.sorted().getFirst().message());
    }

    @Test
    void parse_refusesAnExpressionThatIsNotAStatement() {
        assertNull(this.parse("x + 1"));
        assertFalse(this.codes().isEmpty());
    }

    @Test
    void parse_refusesGotoAsNotSupported() {
        assertNull(this.parse("goto done"));
        assertEquals(List.of("L2007"), this.codes());
    }

    @Test
    void resolve_refusesABreakOutsideALoopAndVarargsOutsideAVarargFunction() {
        final LuaCompiler.Result built = LuaCompiler.compile(new SourceFile("test.lua",
                "break\nlocal function f() return ... end"));
        assertFalse(built.ok());
        assertEquals(List.of("L2005", "L2006"), built.diagnostics().stream().map(Diagnostic::code).toList());
    }
}
