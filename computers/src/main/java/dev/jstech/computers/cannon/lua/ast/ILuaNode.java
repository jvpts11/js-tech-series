/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.ast;

/** Anything in a Lua syntax tree: it knows where in the source it began. */
public interface ILuaNode {

    int line();

    int column();
}
