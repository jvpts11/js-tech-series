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
 * A call the library asks the runtime to make on its behalf.
 *
 * <p>{@code state} names the continuation to carry on with once {@code function} returns, and the
 * one to fall back on if it raises instead; a call with a fallback protects what runs under it, which
 * is what {@code pcall} is. The library never runs a Lua function itself, because the runtime is a
 * machine that steps one instruction at a time and nothing may hold it up.
 */
public record LuaCall(Object function, Object[] arguments, Values.Obj state) {

    /** The field of the state that names the continuation. */
    public static final String NEXT = "Next";

    /** The field of the state that names the continuation taken when the call raises. */
    public static final String FAILED = "Failed";

    /** Whether this call catches what is raised under it. */
    public boolean protects() {
        return this.state.get(FAILED) != null;
    }
}
