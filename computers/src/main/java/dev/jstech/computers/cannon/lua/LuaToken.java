/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua;

import java.util.Objects;

/**
 * One token of Lua source: its kind, the text it was read from, the value it stands for (a number
 * or the contents of a string), and where it began.
 */
public record LuaToken(LuaTokenKind kind, String text, Object value, int line, int column) {

    public LuaToken {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(text, "text");
    }

    public static LuaToken of(final LuaTokenKind kind, final String text, final int line, final int column) {
        return new LuaToken(kind, text, null, line, column);
    }

    public boolean is(final LuaTokenKind other) {
        return this.kind == other;
    }

    /** How the token reads in a message: quoted, or {@code <eof>} at the end. */
    public String describe() {
        return this.kind == LuaTokenKind.END_OF_FILE ? this.kind.describe() : "'" + this.text + "'";
    }
}
