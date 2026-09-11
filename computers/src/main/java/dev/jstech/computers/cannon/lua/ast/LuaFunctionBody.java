/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.ast;

import java.util.List;

/** The parameters and body of a function, whichever way it was written. */
public record LuaFunctionBody(List<String> parameters, boolean varargs, LuaBlock body, int line, int column)
        implements ILuaNode {

    public LuaFunctionBody {
        parameters = List.copyOf(parameters);
    }
}
