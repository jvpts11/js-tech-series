/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import dev.jstech.core.JsCore;
import dev.jstech.core.util.DeclarationError;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.ResourceLocation;

/**
 * What a grid carries, open to any mod: each kind has one grid in each dimension, which every line of that kind shares
 * without the lines ever joining, and a policy, how what it carries is shared along a run. Only data carries the
 * identity of a network along its grid; the others carry what they carry and nothing else.
 *
 * <p>The peripheral grid carries the short cables between a machine and its own screens, speakers and devices: it
 * joins no network, and a peripheral finds its machine by following it.
 *
 * <pre>{@code
 * public static final GridKind MEDIUM_VOLTAGE = GridKind.declare(rl("medium_voltage"), GridPolicies.ENERGY);
 * }</pre>
 */
public final class GridKind {

    private final ResourceLocation id;
    private final IGridPolicy policy;

    private static final Map<ResourceLocation, GridKind> BY_ID = new ConcurrentHashMap<>();
    private static final List<GridKind> ALL = new CopyOnWriteArrayList<>();

    public static final GridKind POWER = declare(core("power"), GridPolicies.ENERGY);
    public static final GridKind FLUID = declare(core("fluid"), GridPolicies.FLUID);
    public static final GridKind HEAT = declare(core("heat"), GridPolicies.JOINS_ONLY);
    public static final GridKind GAS = declare(core("gas"), GridPolicies.JOINS_ONLY);
    public static final GridKind MOTION = declare(core("motion"), GridPolicies.JOINS_ONLY);
    public static final GridKind DATA = declare(core("data"), GridPolicies.NETWORK);
    public static final GridKind PERIPHERAL = declare(core("peripheral"), GridPolicies.JOINS_ONLY);

    private GridKind(final ResourceLocation id, final IGridPolicy policy) {
        this.id = id;
        this.policy = policy;
    }

    /**
     * Declares the grid {@code id}, sharing what it carries by {@code policy}.
     *
     * @throws DeclarationError when a grid of that id is declared already
     */
    public static GridKind declare(final ResourceLocation id, final IGridPolicy policy) {
        final GridKind kind = new GridKind(Objects.requireNonNull(id, "id"), Objects.requireNonNull(policy, "policy"));
        if (BY_ID.putIfAbsent(id, kind) != null) {
            throw DeclarationError.of(id.getNamespace(), "the grid " + id, "is declared twice",
                    "declare it once, as a static final");
        }
        ALL.add(kind);
        return kind;
    }

    /** Every grid declared, in the order they were. */
    public static List<GridKind> all() {
        return List.copyOf(ALL);
    }

    /** The grid declared as {@code id}, or none. */
    public static Optional<GridKind> byId(final ResourceLocation id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public ResourceLocation id() {
        return this.id;
    }

    /** How what it carries is shared along a run, and what its wires plug into. */
    public IGridPolicy policy() {
        return this.policy;
    }

    /** Whether the parts of this kind's grid belong to networks, each with its identity. */
    public boolean carriesNetwork() {
        return this.policy == GridPolicies.NETWORK;
    }

    @Override
    public String toString() {
        return this.id.toString();
    }

    private static ResourceLocation core(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }
}
