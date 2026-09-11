/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.ast;

/** A whole file: a function body that takes what it was run with as {@code ...}. */
public record LuaChunk(String name, LuaFunctionBody body) {
}
