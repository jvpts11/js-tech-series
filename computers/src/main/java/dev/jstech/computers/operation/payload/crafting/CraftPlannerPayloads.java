/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.crafting;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.os.CraftPlannerApp;
import dev.jstech.computers.crafting.CraftPlanner;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.CraftPlanPayload;
import dev.jstech.computers.operation.payload.CraftPlannerPayload;
import dev.jstech.computers.operation.payload.RequestCraftPlannerPayload;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.util.Sizes;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.jstech.computers.operation.payload.crafting.CraftingPayloads.dispatchCraftCatalog;
import static dev.jstech.computers.operation.payload.network.NetworkLookup.resolveMainframe;
import static dev.jstech.computers.operation.payload.terminal.TerminalHosts.niHost;

/**
 * The Craft Planner's payloads: the tree of what a craft needs, step by step.
 */
public final class CraftPlannerPayloads {

    private CraftPlannerPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, RequestCraftPlannerPayload.TYPE, RequestCraftPlannerPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestCraftPlannerPayload::host), CraftPlannerPayloads::handleRequestCraftPlanner);
        registrar.playToClient(CraftPlannerPayload.TYPE, CraftPlannerPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(CraftPlannerPayloads::handleCraftPlanner));
    }

    private static void handleRequestCraftPlanner(final RequestCraftPlannerPayload payload, final ServerPlayer player,
                                                  final ServerLevel level) {
        final var host = niHost(player, level, payload.host(), payload.monitorPos());
        if (host == null || host.networkUuid() == null) {
            return;
        }
        if (payload.target().isEmpty()) {
            dispatchCraftCatalog(player, host.networkUuid(), level);
        } else {
            PacketDistributor.sendToPlayer(player, collectCraftPlanner(level, host.networkUuid(),
                    StorageKey.of(payload.target()), Math.max(1, payload.quantity())));
        }
    }

    private static void handleCraftPlanner(final CraftPlannerPayload payload, final Player player) {
        CraftPlannerApp.accept(payload);
    }

    /** Runs the recursive planner for the Craft Planner: feasibility, the ordered stages, and the raw bill. */
    private static CraftPlannerPayload collectCraftPlanner(final ServerLevel level, final NetworkUuid net,
                                                           final StorageKey key, final long quantity) {
        final MainframeBlockEntity mf = resolveMainframe(level, net);
        if (mf == null) {
            return new CraftPlannerPayload(key.stack(1), quantity, false, false, 0L, 0L,
                    List.of(), List.of(), List.of());
        }
        final var patterns = mf.networkPatterns();
        final var machines = mf.networkProcessingPatterns();
        final Map<StorageKey, Long> stock = mf.networkIndex().snapshot();
        final var plan = CraftPlanner.plan(
                key, quantity, patterns, machines, stock);
        if (plan.steps().isEmpty()) {
            return new CraftPlannerPayload(key.stack(1), quantity, false, false, 0L, 0L,
                    List.of(), List.of(), List.of());
        }
        final long maxFeasible = CraftPlanner.maxFeasible(
                key, quantity, patterns, machines, stock);
        final List<CraftPlannerPayload.Stage> stages = new ArrayList<>();
        for (final var step : plan.steps()) {
            if (stages.size() >= CraftPlannerPayload.MAX_STAGES) {
                break;
            }
            stages.add(new CraftPlannerPayload.Stage(step.resultName(), step.isMachine(), step.runs(),
                    step.produced()));
        }
        final List<CraftPlanPayload.Row> ingredients = new ArrayList<>();
        for (final Map.Entry<StorageKey, Long> e : plan.rawConsumption().entrySet()) {
            if (ingredients.size() >= CraftPlannerPayload.MAX_INGREDIENTS) {
                break;
            }
            ingredients.add(new CraftPlanPayload.Row(e.getKey().stack(1), e.getValue(),
                    stock.getOrDefault(e.getKey(), 0L)));
        }
        final List<CraftPlannerPayload.TreeNode> tree = new ArrayList<>();
        treeWalk(key, Math.max(1, quantity), 0, patterns, machines, tree, new HashSet<>());
        return new CraftPlannerPayload(key.stack(1), quantity, true, plan.feasible(), plan.produced(),
                maxFeasible, stages, ingredients, tree);
    }

    /** Recursively expands one recipe path (crafting preferred, then a machine) into a pre-order tree. */
    private static void treeWalk(final StorageKey key, final long need, final int depth,
            final List<CraftingPattern> patterns,
            final List<ProcessingPattern> machines,
            final List<CraftPlannerPayload.TreeNode> out, final Set<StorageKey> visiting) {
        if (out.size() >= CraftPlannerPayload.MAX_TREE || depth > 6) {
            return;
        }
        Map<StorageKey, Long> inputs = null;
        long perRun = 1;
        for (final var cp : patterns) {
            if (StorageKey.of(cp.result()).equals(key)) {
                inputs = cp.ingredientTotals();
                perRun = Math.max(1, cp.result().getCount());
                break;
            }
        }
        if (inputs == null) {
            for (final var pp : machines) {
                final var o = pp.primaryOutput();
                if (o != null && o.key().equals(key)) {
                    final Map<StorageKey, Long> merged = new LinkedHashMap<>();
                    for (final var pi : pp.inputs()) {
                        merged.merge(pi.key(), pi.amount(), Long::sum);
                    }
                    inputs = merged;
                    perRun = Math.max(1, o.amount());
                    break;
                }
            }
        }
        final boolean craftable = inputs != null;
        out.add(new CraftPlannerPayload.TreeNode(depth, key.stack(1), need, craftable));
        if (!craftable || !visiting.add(key)) {
            return;
        }
        final long runs = Math.max(1, Sizes.ceilDiv(need, perRun));
        for (final Map.Entry<StorageKey, Long> e : inputs.entrySet()) {
            treeWalk(e.getKey(), e.getValue() * runs, depth + 1, patterns, machines, out, visiting);
        }
        visiting.remove(key);
    }
}
