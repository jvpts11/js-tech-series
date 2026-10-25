/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.network;

import com.mojang.serialization.Codec;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.cable.Cables;
import dev.jstech.core.cable.Wire;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.jstechseries.operation.OperationType;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * A machine's membership of the data network it touches, kept by the Core ({@link NetworkMembers#keep}): its identity
 * on the network, saved with it, and the network it is on now, if any. The machine reads it; the Core keeps it.
 */
public final class NetworkMembership {

    private final SyncedBlockEntity blockEntity;
    private final INetworkMember member;
    private final ValueField<NodeUuid> node;
    private final List<String> operations;
    private final List<ResourceLocation> tables;
    private @Nullable NetworkUuid network;

    private static final Codec<NodeUuid> NODE_CODEC = UUIDUtil.CODEC.xmap(NodeUuid::new, NodeUuid::value);
    private static final String NODE = "NetworkMember";

    NetworkMembership(final SyncedBlockEntity blockEntity, final INetworkMember member) {
        this.blockEntity = blockEntity;
        this.member = member;
        this.node = blockEntity.fields().nullable(NODE, NODE_CODEC).save();
        final List<String> ids = new ArrayList<>();
        for (final OperationType<?> type : member.carriesOut()) {
            ids.add(type.id());
        }
        this.operations = List.copyOf(ids);
        final List<ResourceLocation> names = new ArrayList<>();
        for (final StateTable table : member.tables()) {
            names.add(table.id());
        }
        this.tables = List.copyOf(names);
    }

    /** Its identity on the network, made the first time it is asked on the server and kept from then on. */
    public NodeUuid node() {
        NodeUuid id = this.node.get();
        if (id == null) {
            id = NodeUuid.random();
            this.node.set(id);
        }
        return id;
    }

    /** The network it is on now; none while no data cable of a network touches it. On the server. */
    public Optional<NetworkUuid> network() {
        return Optional.ofNullable(this.network);
    }

    /** The machine whose membership it is. */
    public INetworkMember member() {
        return this.member;
    }

    /* Works out again which network it is on, and puts it on that one, or off any. */
    void resolve(final ServerLevel level) {
        final NetworkSystem system = NetworkSystem.get(level);
        final long pos = this.blockEntity.getBlockPos().asLong();
        if (this.blockEntity.isRemoved()) {
            forget(system, level);
            return;
        }
        final @Nullable NetworkUuid before = this.network;
        this.network = beside(level, system);
        tellOfChange(level, before);
        if (this.network == null) {
            system.unregisterMemberAt(pos);
            return;
        }
        system.registerMember(new NetworkSystem.MemberNode(node(), this.network, pos, this.member.category(),
                this.operations, this.tables));
    }

    /* It is gone: off its network, and no longer kept. */
    void forget(final NetworkSystem system, final ServerLevel level) {
        final long pos = this.blockEntity.getBlockPos().asLong();
        system.unregisterMemberAt(pos);
        if (system.keptMembers().get(pos) == this) {
            system.keptMembers().remove(pos);
        }
        final @Nullable NetworkUuid before = this.network;
        this.network = null;
        tellOfChange(level, before);
    }

    /* Tells the game's bus that it left the network it was on and joined the one it is on, where they differ. */
    private void tellOfChange(final ServerLevel level, final @Nullable NetworkUuid before) {
        if (Objects.equals(before, this.network)) {
            return;
        }
        final BlockPos at = this.blockEntity.getBlockPos();
        if (before != null) {
            NeoForge.EVENT_BUS.post(new NetworkMemberEvent.Left(level, at, node(), before));
        }
        if (this.network != null) {
            NeoForge.EVENT_BUS.post(new NetworkMemberEvent.Joined(level, at, node(), this.network));
        }
    }

    /*
     * The network of a data wire crossing into it from a cable block beside it, through a face that takes the wire,
     * for a machine that says which of its faces take which lines; any face, for one that does not. The crafting
     * line runs apart from the network and joins no member to it.
     */
    private @Nullable NetworkUuid beside(final ServerLevel level, final NetworkSystem system) {
        final BlockPos at = this.blockEntity.getBlockPos();
        final ConnectivityIndex connectivity = system.connectivity();
        for (final Direction face : Direction.values()) {
            final BlockPos cable = at.relative(face);
            // The cable block says which of its wires cross into the machine, from what the machine takes there.
            for (final Wire wire : Cables.reaching(level, at, face)) {
                final DataLink link = wire.type().grid().carriesNetwork() ? DataLines.linkOf(wire.type().line())
                        : null;
                if (link == null || link.line() == DataLine.CRAFTING) {
                    continue;
                }
                // The network is kept by the number the wire is known by in its grid, never by where it stands.
                final OptionalLong number = Cables.number(level, cable, wire.type());
                final Optional<NetworkUuid> there = number.isEmpty() ? Optional.empty()
                        : connectivity.networkOf(number.getAsLong());
                if (there.isPresent()) {
                    return there.get();
                }
            }
            final Optional<NetworkUuid> scene = standIn(connectivity, cable.asLong(), face);
            if (scene.isPresent()) {
                return scene.get();
            }
        }
        return null;
    }

    /* The network of a test scene's cable standing at {@code pos}, when the machine's face there takes its line. */
    private Optional<NetworkUuid> standIn(final ConnectivityIndex connectivity, final long pos, final Direction face) {
        final OptionalLong number = connectivity.standInAt(pos);
        if (number.isEmpty()) {
            return Optional.empty();
        }
        final Optional<DataLink> link = connectivity.linkOf(number.getAsLong());
        final BlockState state = this.blockEntity.getBlockState();
        if (state.getBlock() instanceof IFaceConnector connector && link.isPresent()
                && !connector.accepts(state, face, DataLines.of(link.get()))) {
            return Optional.empty();
        }
        return connectivity.networkOf(number.getAsLong());
    }
}
