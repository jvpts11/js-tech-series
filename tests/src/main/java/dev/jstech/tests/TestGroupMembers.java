/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.LineGroup;
import dev.jstech.core.grid.LineGroups;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * The machines the tests' group line joins: a lodestone is a member, and hears of the group it is in, kept by its
 * position; and every group the line works out, or dissolves, is written down. Only the test mod gives lodestones the
 * capability, and only in development.
 */
@EventBusSubscriber(modid = JsTests.MODID)
public final class TestGroupMembers {

    private static final Map<Member, Optional<LineGroup>> HEARD = new ConcurrentHashMap<>();
    private static final List<LineGroup> DISSOLVED = new CopyOnWriteArrayList<>();

    static {
        LineGroups.listen(TestCableTypes.GROUPS, new LineGroups.IGroupListener() {
            @Override
            public void changed(final ServerLevel level, final LineGroup group) {
                // What a member heard says the same: the listener only keeps what was dissolved.
            }

            @Override
            public void dissolved(final ServerLevel level, final LineGroup group) {
                DISSOLVED.add(group);
            }
        });
    }

    private TestGroupMembers() {
    }

    /** The group the lodestone at {@code pos} last heard it is in; empty when it heard it is in none, or nothing. */
    public static Optional<LineGroup> heard(final Level level, final BlockPos pos) {
        return HEARD.getOrDefault(new Member(level.dimension(), pos.immutable()), Optional.empty());
    }

    /** Whether the lodestone at {@code pos} heard anything at all. */
    public static boolean heardAnything(final Level level, final BlockPos pos) {
        return HEARD.containsKey(new Member(level.dimension(), pos.immutable()));
    }

    /** Whether a group that joined the machine at {@code pos} was dissolved. */
    public static boolean dissolvedWith(final BlockPos pos) {
        return DISSOLVED.stream().anyMatch(group -> group.has(pos));
    }

    /** Forgets what the lodestone at {@code pos} heard. */
    public static void clear(final Level level, final BlockPos pos) {
        HEARD.remove(new Member(level.dimension(), pos.immutable()));
        DISSOLVED.removeIf(group -> group.has(pos));
    }

    /** Forgets everything when a world closes. */
    @SubscribeEvent
    public static void onServerStopped(final ServerStoppedEvent event) {
        HEARD.clear();
        DISSOLVED.clear();
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlock(LineGroups.MEMBER, (level, pos, state, entity, side) -> new Heard(level.dimension(),
                pos.immutable()), Blocks.LODESTONE);
    }

    /* A lodestone in a dimension. */
    private record Member(ResourceKey<Level> dimension, BlockPos pos) {
    }

    /* A lodestone as a member: it writes down what it hears. */
    private record Heard(ResourceKey<Level> dimension, BlockPos pos) implements LineGroups.IGroupMember {

        @Override
        public void joined(final LineGroup group) {
            HEARD.put(new Member(this.dimension, this.pos), Optional.of(group));
        }

        @Override
        public void left(final GridKind kind) {
            HEARD.put(new Member(this.dimension, this.pos), Optional.empty());
        }
    }
}
