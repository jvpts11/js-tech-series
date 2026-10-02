/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.crafting.MultiStagePattern;
import dev.jstech.computers.crafting.NetworkMultiStageOperation;
import dev.jstech.computers.crafting.NetworkProcessingOperation;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.operation.INetworkOperation;
import dev.jstech.computers.operation.NetworkInsertOperation;
import dev.jstech.computers.operation.NetworkSelectOperation;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.iql.IIqlView;
import dev.jstech.computers.storage.IDataSink;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.uuid.NodeUuid;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * The network's Network Operations Service: the one door through which anything asks the network to do work.
 *
 * <p>The Network Interactor, the terminals, the buses, the gateways, the Crafting Manager, the programs and the
 * network's language all come in here, and never to the Mainframe's Operations core directly. The door hands each
 * request to the engine running on the Mainframe, which decides how to do it; the core then does it. Computers bind
 * to the network, not to an engine, so changing the engine changes nothing for them.
 *
 * <p>The contract has three levels. The network level is always there, engine or not: what the network holds and
 * where, its computers, its recipes, its Operations, its Mainframe, its watches, its locks and its maintenance; it
 * is read where it lives, not through here. The engine level is the verbs of {@link EngineVerb}, which every engine
 * answers. The extras are the {@link EngineCapability capabilities} an engine may offer on top. The mod's own
 * programs use the first two levels only, so they work the same with any engine.
 *
 * <p>With no engine running, the verbs that only move what is there still work, straight through the core;
 * crafting, a craft's plan and the language are refused with {@link #UNAVAILABLE}.
 */
@TextHolder
public final class NetworkOperationsService {

    private final MainframeBlockEntity core;

    /** What a network with no engine running answers to a request only an engine can plan. */
    public static final TextKey UNAVAILABLE =
            TextKey.of("jsc.engine.unavailable", "Network Operations Service unavailable");

    public NetworkOperationsService(final MainframeBlockEntity core) {
        this.core = core;
    }

    /** The engine running on the Mainframe, or {@code null} when none is and the network has no engine. */
    @Nullable
    public INetworkEngine engine() {
        return core.runningEngine();
    }

    /** Whether the network does {@code verb} now. */
    public boolean serves(final EngineVerb verb) {
        return verb.transfer() || engine() != null;
    }

    /** Why the network refuses {@code verb} now, or {@code null} when it does it. */
    @Nullable
    public Text refusal(final EngineVerb verb) {
        return serves(verb) ? null : UNAVAILABLE.text();
    }

    /** Takes {@code demand} of {@code key} from anywhere in the network's storage into {@code to}. */
    @Nullable
    public NetworkSelectOperation pull(final StorageKey key, final long demand, final IDataSink to,
                                       final String label) {
        return pull(key, demand, to, label, null);
    }

    /** Takes {@code demand} of {@code key} into {@code to}; from the servers in {@code from} only, when given. */
    @Nullable
    public NetworkSelectOperation pull(final StorageKey key, final long demand, final IDataSink to,
                                       final String label, @Nullable final Set<NodeUuid> from) {
        final INetworkEngine engine = engine();
        if (engine != null) {
            return planned(engine.pull(core, key, demand, to, label, from));
        }
        return from == null ? core.submitNetworkSelect(key, demand, to, label)
                : core.submitNetworkSelect(key, demand, to, label, from);
    }

    /** Puts {@code amount} of {@code key} into the network's storage. */
    @Nullable
    public NetworkInsertOperation push(final StorageKey key, final long amount, final String label) {
        final INetworkEngine engine = engine();
        return engine != null ? planned(engine.push(core, key, amount, label))
                : core.submitNetworkInsert(key, amount, label);
    }

    /** Moves {@code demand} of {@code key} from the servers in {@code from} into {@code to}. */
    @Nullable
    public NetworkSelectOperation move(final StorageKey key, final long demand, final IDataSink to,
                                       final String label, final Set<NodeUuid> from) {
        final INetworkEngine engine = engine();
        return engine != null ? planned(engine.move(core, key, demand, to, label, from))
                : core.submitNetworkMove(key, demand, to, label, from);
    }

    /** Sends {@code demand} of {@code key} out of the network into {@code to}: an outside inventory, or nothing. */
    @Nullable
    public NetworkSelectOperation export(final StorageKey key, final long demand, final IDataSink to,
                                         final String label) {
        final INetworkEngine engine = engine();
        return engine != null ? planned(engine.export(core, key, demand, to, label))
                : core.submitNetworkDelete(key, demand, to, label);
    }

    /** Fills a held container, through {@code to}, with {@code demand} of {@code key}. */
    @Nullable
    public NetworkSelectOperation fill(final StorageKey key, final long demand, final IDataSink to,
                                       final String label) {
        final INetworkEngine engine = engine();
        return engine != null ? planned(engine.fill(core, key, demand, to, label))
                : core.submitNetworkSelect(key, demand, to, label);
    }

    /** Makes what {@code request} asks for; refused, with nothing started, when no engine is running. */
    @Nullable
    public INetworkOperation craft(final CraftRequest request) {
        final INetworkEngine engine = engine();
        return engine == null ? null : planned(engine.craft(core, request));
    }

    /** Runs one machine recipe; refused when no engine is running. */
    @Nullable
    public NetworkProcessingOperation process(final ProcessingPattern pattern, final long demand,
                                              final String label) {
        final INetworkEngine engine = engine();
        return engine == null ? null : planned(engine.process(core, pattern, demand, label));
    }

    /** Runs one multi-stage recipe; refused when no engine is running. */
    @Nullable
    public NetworkMultiStageOperation pipeline(final MultiStagePattern pattern, final long demand,
                                               final String label) {
        final INetworkEngine engine = engine();
        return engine == null ? null : planned(engine.pipeline(core, pattern, demand, label));
    }

    /** How the running engine plans a craft before anything is made, or {@code null} when no engine is running. */
    @Nullable
    public ICraftPlanning planner() {
        final INetworkEngine engine = engine();
        return engine == null ? null : engine.planner();
    }

    /** Runs a statement in the running engine's dialect for {@code caller}; refused when no engine is running. */
    public IqlEngine.Outcome query(final IIqlView caller, final String statement, final int rowLimit) {
        final INetworkEngine engine = engine();
        if (engine == null) {
            return new IqlEngine.Outcome(false, UNAVAILABLE.text(), List.of());
        }
        core.notePlanned();
        return engine.query(core, caller, statement, rowLimit);
    }

    /** Counts a request the engine took on, for the plans the network's services show for today. */
    @Nullable
    private <T> T planned(@Nullable final T operation) {
        if (operation != null) {
            core.notePlanned();
        }
        return operation;
    }
}
