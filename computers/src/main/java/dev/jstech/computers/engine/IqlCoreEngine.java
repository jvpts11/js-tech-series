/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.iql.IIqlView;

/**
 * An engine that speaks the network's language as it is written, plans crafts the way the Operations core does,
 * and keeps views and procedures when it says it offers them.
 *
 * <p>This is the Midsoft IQL Server, the engine every new Mainframe ships with, and what an engine registered by
 * another mod does until that mod gives it a planner of its own: everything it is asked, it answers the way the
 * network always has.
 */
final class IqlCoreEngine implements INetworkEngine {

    private final EngineDef def;

    IqlCoreEngine(final EngineDef def) {
        this.def = def;
    }

    @Override
    public EngineDef def() {
        return def;
    }

    @Override
    public IqlEngine.Outcome query(final MainframeBlockEntity core, final IIqlView caller, final String statement,
                                   final int rowLimit) {
        return new IqlEngine(core, caller, rowLimit, def.offers(EngineCapability.PROCEDURES_AND_VIEWS))
                .run(statement);
    }
}
