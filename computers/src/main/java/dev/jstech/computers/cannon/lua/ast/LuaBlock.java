/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.ast;

import java.util.List;

/** A run of statements with a scope of its own. */
public record LuaBlock(List<ILuaStmt> statements, int line, int column) implements ILuaNode {

    public LuaBlock {
        statements = List.copyOf(statements);
    }
}
