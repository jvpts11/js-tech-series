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
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.cannon.Diagnostic;
import dev.jstech.computers.cannon.DiagnosticBag;
import dev.jstech.computers.cannon.SourceFile;
import java.util.List;
import org.junit.jupiter.api.Test;

class LuaLexerTest {

    private DiagnosticBag bag;

    private List<LuaToken> lex(final String text) {
        this.bag = new DiagnosticBag("test.lua");
        return new LuaLexer(new SourceFile("test.lua", text), this.bag).tokenize();
    }

    private List<LuaTokenKind> kinds(final String text) {
        return this.lex(text).stream().map(LuaToken::kind).toList();
    }

    @Test
    void tokenize_tellsKeywordsFromNames() {
        assertEquals(List.of(LuaTokenKind.LOCAL, LuaTokenKind.NAME, LuaTokenKind.ASSIGN, LuaTokenKind.NIL,
                LuaTokenKind.END_OF_FILE), this.kinds("local endx = nil"));
    }

    @Test
    void tokenize_readsEveryOperatorIncludingTheLongOnes() {
        assertEquals(List.of(LuaTokenKind.ELLIPSIS, LuaTokenKind.CONCAT, LuaTokenKind.DOT, LuaTokenKind.EQUAL,
                LuaTokenKind.NOT_EQUAL, LuaTokenKind.LESS_EQUAL, LuaTokenKind.DOUBLE_COLON, LuaTokenKind.DOUBLE_SLASH,
                LuaTokenKind.HASH, LuaTokenKind.END_OF_FILE), this.kinds("... .. . == ~= <= :: // #"));
    }

    @Test
    void tokenize_readsWholeRealAndHexadecimalNumbers() {
        final List<LuaToken> tokens = this.lex("42 3.5 .5 1e3 0xff 0x10p1");
        assertEquals(42L, tokens.get(0).value());
        assertEquals(3.5, tokens.get(1).value());
        assertEquals(0.5, tokens.get(2).value());
        assertEquals(1000.0, tokens.get(3).value());
        assertEquals(255L, tokens.get(4).value());
        assertEquals(32.0, tokens.get(5).value());
    }

    @Test
    void tokenize_appliesEveryEscape() {
        final List<LuaToken> tokens = this.lex("'a\\tb\\n\\65\\x41\\\\\\'' \"\\z   \n  c\"");
        assertEquals("a\tb\nAA\\'", tokens.get(0).value());
        assertEquals("c", tokens.get(1).value());
        assertFalse(this.bag.hasErrors());
    }

    @Test
    void tokenize_readsLongStringsAndDropsTheirFirstNewline() {
        final List<LuaToken> tokens = this.lex("[[\nline one\nline two]] [==[a]]b]==]");
        assertEquals("line one\nline two", tokens.get(0).value());
        assertEquals("a]]b", tokens.get(1).value());
    }

    @Test
    void tokenize_skipsBothCommentFormsAndRemembersThem() {
        final LuaLexer lexer = new LuaLexer(new SourceFile("test.lua",
                "x -- to the end\n--[[ over\ntwo lines ]] y"), new DiagnosticBag("test.lua"));
        final List<LuaToken> tokens = lexer.tokenize();
        assertEquals(List.of("x", "y", ""), tokens.stream().map(LuaToken::text).toList());
        assertEquals(3, lexer.comments().size(), "one for the line comment, one for each line of the long one");
        assertEquals(new LuaLexer.Comment(1, 3, 13), lexer.comments().getFirst());
    }

    @Test
    void tokenize_skipsAShebangOnTheFirstLine() {
        assertEquals(List.of(LuaTokenKind.NAME, LuaTokenKind.END_OF_FILE), this.kinds("#!/usr/bin/lua\nx"));
    }

    @Test
    void tokenize_complainsAboutAnUnfinishedString() {
        this.lex("x = 'open");
        assertTrue(this.bag.hasErrors());
        assertEquals("L1001", this.bag.sorted().getFirst().code());
    }

    @Test
    void tokenize_complainsAboutACharacterTheLanguageDoesNotHave() {
        this.lex("x = @");
        assertEquals(List.of("L1003"), this.bag.sorted().stream().map(Diagnostic::code).toList());
    }
}
