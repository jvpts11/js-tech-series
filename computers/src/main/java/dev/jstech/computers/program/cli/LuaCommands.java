/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import java.util.List;

/**
 * The verb the Lua runtime brings to the prompt: {@code lrt run reactor.lua}, {@code lrt stop},
 * {@code lrt ps}.
 *
 * <p>Lua has a runtime and a verb of its own rather than riding on Cannon's, because the two are
 * different languages: a player running a ComputerCraft program is running Lua, and nothing at the
 * prompt should suggest Cannon is what does it. A Lua file runs as it is; there is nothing to compile
 * first.
 */
public final class LuaCommands {

    /** The id of the Lua runtime package, as the Mirror serves it. */
    public static final String RUNTIME = "jsc:lrt";

    private LuaCommands() {
    }

    /** Every verb, for the shell to register. */
    public static List<ICliCommand> all() {
        return List.of(new CannonCommands.Run(CannonCommands.LUA));
    }
}
