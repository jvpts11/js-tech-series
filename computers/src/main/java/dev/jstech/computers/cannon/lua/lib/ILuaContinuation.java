/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.lib;

import dev.jstech.computers.cannon.run.Values;

/**
 * What a library function does once the Lua function it asked for has returned.
 *
 * <p>It gets the state it left behind and what the call gave back, and answers the way a function
 * does: a value, a run of values, or another {@link LuaCall} when there is more to call.
 */
@FunctionalInterface
public interface ILuaContinuation {

    Object resume(ILuaContext context, Values.Obj state, Object result, int line);
}
