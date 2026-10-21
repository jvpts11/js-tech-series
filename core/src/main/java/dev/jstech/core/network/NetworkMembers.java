/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network;

import dev.jstech.core.JsCore;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.moving.StructureBlocksMovedEvent;
import dev.jstech.core.operation.OperationType;
import dev.jstech.core.util.DeclarationError;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * Machines of any mod on the data network, through the Core ({@link INetworkMember}). A member is kept by its block
 * entity's constructor; from then on it is on the network of the data cable it touches as it loads, as the blocks
 * beside it change, and once a second besides, as networks form and part around it; and off it once broken or
 * unloaded. What it declares is checked as it is made, so a member that could never be what it says stops at once.
 */
public final class NetworkMembers {

    /* How often every kept member looks again for the network it touches, in ticks. */
    private static final int RECHECK = 20;

    private NetworkMembers() {
    }

    /**
     * Hands the game's bus the second-by-second look, once, from the Core's constructor; and forgets, where blocks
     * moved away with a structure, the cables an owner of a network touched there.
     */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % RECHECK == 0) {
                recheck(level);
            }
        });
        NeoForge.EVENT_BUS.addListener(StructureBlocksMovedEvent.class, event -> {
            if (event.move().from() instanceof ServerLevel level) {
                final ConnectivityIndex index = NetworkSystem.get(level).connectivity();
                for (final BlockPos was : event.move().moved().keySet()) {
                    index.unanchor(was.asLong());
                }
            }
        });
    }

    /**
     * Keeps {@code blockEntity}, which implements {@link INetworkMember}, on the network it touches. Called from its
     * constructor.
     *
     * @return its membership: its identity on the network and the network it is on
     * @throws DeclarationError when what it declares cannot stand: a block that only carries the network, kinds of
     *                          Operation for one only read, or a kind not registered or not open to its category
     */
    public static NetworkMembership keep(final SyncedBlockEntity blockEntity) {
        if (!(blockEntity instanceof INetworkMember member)) {
            throw new IllegalArgumentException("a network member's block entity implements INetworkMember");
        }
        check(blockEntity, member);
        final NetworkMembership membership = new NetworkMembership(blockEntity, member);
        blockEntity.fields().whenLoaded(level -> {
            NetworkSystem.get(level).keptMembers().put(blockEntity.getBlockPos().asLong(), membership);
            membership.resolve(level);
        });
        blockEntity.fields().whenNeighbourChanges((level, face) -> membership.resolve(level));
        blockEntity.fields().whenBroken((level, pos) -> membership.forget(NetworkSystem.get(level), level));
        return membership;
    }

    /* Every kept member looks again; one that is gone, unloaded or broken, leaves. */
    private static void recheck(final ServerLevel level) {
        final NetworkSystem system = NetworkSystem.get(level);
        if (system.keptMembers().isEmpty()) {
            return;
        }
        for (final NetworkMembership membership : List.copyOf(system.keptMembers().values())) {
            membership.resolve(level);
        }
    }

    private static void check(final SyncedBlockEntity blockEntity, final INetworkMember member) {
        final ResourceLocation entity = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
        final String mod = entity == null ? null : entity.getNamespace();
        final String name = blockEntity.getClass().getSimpleName();
        final NetworkCategory category = member.category();
        if (category == null || !category.hasUuid()) {
            throw DeclarationError.of(mod, "the network member " + name,
                    "says it only carries the network, and such a block is no member",
                    "answer NetworkCategory.B for a machine the network reads, or C for one it gives work to");
        }
        final List<String> wrong = new ArrayList<>();
        for (final OperationType<?> type : member.carriesOut()) {
            if (!category.canReceiveOperations()) {
                throw DeclarationError.of(mod, "the network member " + name,
                        "carries out kinds of Operation, and is only read",
                        "answer NetworkCategory.C, or carry out none");
            }
            if (!JsCore.operations().contains(type.id()) || !type.requiredCategories().contains(category)) {
                wrong.add(type.id());
            }
        }
        if (!wrong.isEmpty()) {
            throw DeclarationError.of(mod, "the network member " + name,
                    "carries out " + wrong + ", not registered or not open to it",
                    "register each kind from CoreRegisterEvent, open to the member's category");
        }
    }
}
