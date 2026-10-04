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
import dev.jstech.computers.operation.NetworkUpdateOperation;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.iql.IIqlView;
import dev.jstech.computers.storage.IDataSink;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.workshop.UpdateRequest;
import dev.jstech.core.uuid.NodeUuid;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * What a Network Operations Engine does with the work its network's door hands it.
 *
 * <p>Each verb takes the Mainframe the engine runs on, whose Operations core carries the plan out: the engine
 * decides what to do, the core does it, and what the core is doing belongs to nobody's engine. Where an engine has
 * nothing of its own to add to a verb, the default hands it to the core as it came, which is also what a network
 * with no engine does for the verbs that only move what is there.
 *
 * <p>Each answers with the Operation it started, or {@code null} when it started none (the network refused it, or
 * there was nothing to do).
 */
public interface INetworkEngine {

    /** Which engine this is: its package, its dialect, its versions and its extras. */
    EngineDef def();

    /**
     * Takes {@code demand} of {@code key} into {@code to}; from the servers in {@code from} only, when given.
     */
    @Nullable
    default NetworkSelectOperation pull(final MainframeBlockEntity core, final StorageKey key, final long demand,
                                        final IDataSink to, final String label, @Nullable final Set<NodeUuid> from) {
        return from == null ? core.submitNetworkSelect(key, demand, to, label)
                : core.submitNetworkSelect(key, demand, to, label, from);
    }

    /** Puts {@code amount} of {@code key} into the network's storage. */
    @Nullable
    default NetworkInsertOperation push(final MainframeBlockEntity core, final StorageKey key, final long amount,
                                        final String label) {
        return core.submitNetworkInsert(key, amount, label);
    }

    /** Moves {@code demand} of {@code key} from the servers in {@code from} into {@code to}. */
    @Nullable
    default NetworkSelectOperation move(final MainframeBlockEntity core, final StorageKey key, final long demand,
                                        final IDataSink to, final String label, final Set<NodeUuid> from) {
        return core.submitNetworkMove(key, demand, to, label, from);
    }

    /** Sends {@code demand} of {@code key} out of the network, into {@code to}. */
    @Nullable
    default NetworkSelectOperation export(final MainframeBlockEntity core, final StorageKey key, final long demand,
                                          final IDataSink to, final String label) {
        return core.submitNetworkDelete(key, demand, to, label);
    }

    /** Fills a held container, through {@code to}, with {@code demand} of {@code key}. */
    @Nullable
    default NetworkSelectOperation fill(final MainframeBlockEntity core, final StorageKey key, final long demand,
                                        final IDataSink to, final String label) {
        return core.submitNetworkSelect(key, demand, to, label);
    }

    /** Makes what {@code request} asks for, planning how. */
    @Nullable
    default INetworkOperation craft(final MainframeBlockEntity core, final CraftRequest request) {
        return request.recipe() == CraftRequest.ANY_RECIPE
                ? core.submitCraftRequest(request.key(), request.demand(), request.partial(), request.label(),
                        request.onSettle(), request.preferMultiStage())
                : core.submitCraftRequest(request.key(), request.demand(), request.partial(), request.label(),
                        request.onSettle(), request.recipe());
    }

    /** Runs one machine recipe until {@code demand} of its output is made. */
    @Nullable
    default NetworkProcessingOperation process(final MainframeBlockEntity core, final ProcessingPattern pattern,
                                               final long demand, final String label) {
        return core.submitNetworkProcessing(pattern, demand, label);
    }

    /** Runs one multi-stage recipe until {@code demand} of its output is made. */
    @Nullable
    default NetworkMultiStageOperation pipeline(final MainframeBlockEntity core, final MultiStagePattern pattern,
                                                final long demand, final String label) {
        return core.submitNetworkMultiStage(pattern, demand, label);
    }

    /** Changes an item the network holds with a card of the computer {@code request} names. */
    @Nullable
    default NetworkUpdateOperation update(final MainframeBlockEntity core, final UpdateRequest request) {
        return core.submitNetworkUpdate(request);
    }

    /** How the engine works out a craft's plan before anything is made. */
    default ICraftPlanning planner() {
        return ICraftPlanning.REFERENCE;
    }

    /**
     * Runs a statement in the engine's dialect, for {@code caller}: what it reads comes from there and what it asks
     * to be done is done there.
     *
     * @param rowLimit how many rows a read may bring back, when the statement sets no limit of its own
     */
    IqlEngine.Outcome query(MainframeBlockEntity core, IIqlView caller, String statement, int rowLimit);
}
