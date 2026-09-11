/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.lib;

/**
 * One function of the standard library.
 *
 * <p>It answers with a single value, with a {@link dev.jstech.computers.cannon.run.Values.Arr} for
 * several or none, or with a {@link LuaCall} when it needs a Lua function called before it can go on.
 * {@code target} is what the function was bound to when it was made, for the ones that keep state,
 * such as the iterator {@code string.gmatch} hands back.
 */
@FunctionalInterface
public interface ILuaFunction {

    Object call(ILuaContext context, Object target, Object[] arguments, int line);
}
