/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.Cables;
import dev.jstech.core.gametest.ScenarioBuilder;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.Grid;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.GridPlace;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Laying cables in a GameTest's arena: a wire of a cable in the Core's cable block, as a player laying it would, into
 * the cable block already there or into a new one.
 */
public final class TestCables {

    private TestCables() {
    }

    /**
     * Lays a wire of {@code cable} at {@code relative} and gives back the cable block; fails the test when it can't.
     * Whatever stands there that is no cable block is replaced, as setting a block in a test replaces it.
     */
    public static CableBlockEntity lay(final GameTestHelper helper, final BlockPos relative,
                                       final Supplier<CableType> cable) {
        if (!lay(helper.getLevel(), helper.absolutePos(relative), cable.get())) {
            helper.fail("could not lay " + cable.get().id() + " at " + relative.toShortString());
        }
        return cable(helper, relative);
    }

    /** Lays a wire of {@code type} at {@code pos}, replacing what stands there when it is no cable block. */
    public static boolean lay(final Level level, final BlockPos pos, final CableType type) {
        return ScenarioBuilder.layCable(level, pos, type);
    }

    /**
     * The number a place of the data grid in the block at {@code relative} is known by: its first wire of data, or the
     * whole block for a router; empty when the block is no place of the data grid.
     */
    public static OptionalLong dataNumber(final GameTestHelper helper, final BlockPos relative) {
        final ServerLevel level = helper.getLevel();
        final Grid grid = CoreGrids.of(level, GridKind.DATA);
        for (final GridPlace place : CoreGrids.places(level).at(helper.absolutePos(relative).asLong())) {
            final OptionalLong number = CoreGrids.places(level).find(place);
            if (number.isPresent() && grid.contains(number.getAsLong())) {
                return number;
            }
        }
        return OptionalLong.empty();
    }

    /** The network the block at {@code relative} carries, through its data grid place; empty for none. */
    public static Optional<NetworkUuid> network(final GameTestHelper helper, final BlockPos relative) {
        final OptionalLong number = dataNumber(helper, relative);
        return number.isEmpty() ? Optional.empty()
                : NetworkSystem.get(helper.getLevel()).connectivity().networkOf(number.getAsLong());
    }

    /** Whether the blocks at {@code a} and {@code b} are joined by the data grid. */
    public static boolean joined(final GameTestHelper helper, final BlockPos a, final BlockPos b) {
        final OptionalLong first = dataNumber(helper, a);
        final OptionalLong second = dataNumber(helper, b);
        return first.isPresent() && second.isPresent() && NetworkSystem.get(helper.getLevel()).connectivity()
                .inSameNetwork(first.getAsLong(), second.getAsLong());
    }

    /**
     * Whether the wire of {@code first} at {@code a} and the wire of {@code second} at {@code b} are joined by the data
     * grid: for blocks that hold more than one wire.
     */
    public static boolean wiresJoined(final GameTestHelper helper, final BlockPos a, final Supplier<CableType> first,
                                      final BlockPos b, final Supplier<CableType> second) {
        final ServerLevel level = helper.getLevel();
        final OptionalLong one = Cables.number(level, helper.absolutePos(a), first.get());
        final OptionalLong other = Cables.number(level, helper.absolutePos(b), second.get());
        return one.isPresent() && other.isPresent() && NetworkSystem.get(level).connectivity()
                .inSameNetwork(one.getAsLong(), other.getAsLong());
    }

    /** The cable block at {@code relative}; fails the test when there is none. */
    public static CableBlockEntity cable(final GameTestHelper helper, final BlockPos relative) {
        if (!(helper.getBlockEntity(relative) instanceof CableBlockEntity cable)) {
            helper.fail("no cable block at " + relative.toShortString());
            throw new IllegalStateException("unreachable");
        }
        return cable;
    }
}
