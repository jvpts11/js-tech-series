/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.program.cli.ICliKnot;
import net.minecraft.server.level.ServerLevel;

/**
 * The KnotHub a machine's prompt reached, as {@code knot} works it: the repository of the hub {@code reach} found,
 * what the machine may do with it, and who what it pushes is signed by.
 */
record KnotAtPrompt(KnotHubs.Reach reach, KnotRepository repository, String author, ServerLevel level)
        implements ICliKnot {

    @Override
    public String host() {
        return reach.host().hostname();
    }

    @Override
    public String network() {
        return reach.network();
    }

    @Override
    public boolean maySee() {
        return reach.see();
    }

    @Override
    public boolean maySend() {
        return reach.send();
    }

    @Override
    public long now() {
        return level.getGameTime();
    }

    @Override
    public void changed() {
        reach.host().changed();
    }
}
