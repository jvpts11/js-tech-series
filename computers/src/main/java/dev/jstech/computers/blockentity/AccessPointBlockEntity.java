/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import com.mojang.serialization.Codec;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.AccessPointBlock;
import dev.jstech.computers.block.DataWires;
import dev.jstech.computers.wireless.AccessPoints;
import dev.jstech.computers.wireless.WirelessStandard;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.energy.EnergyStore;
import dev.jstech.core.energy.IEnergyPort;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.moving.ForeignMovers;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * A Wireless Access Point (computers/docs/design/wireless.md): it draws its era's watts through the face its cables
 * come in by, reads the network off the data cable there once a second, and while it has both it gives that network to
 * every computer with a Wi-Fi card that it reaches and that has no nearer one. It keeps no list of them: a computer
 * asks the access points of its level which reaches it ({@link AccessPoints}).
 */
public class AccessPointBlockEntity extends SyncedBlockEntity {

    private final EnergyStore store = new EnergyStore("Energy", JsEnergy.ENERGY, JsEnergy.LOW, 0L, TAKES_A_TICK, 0L,
            this::setChanged);
    private final ValueField<String> node = fields().value("NodeUuid", Codec.STRING, "").save();
    /* Its lamps, which the client draws: its power, and whether it is on a network. */
    private final ValueField<Boolean> on = fields().value("On", Codec.BOOL, false).toClient();
    private final ValueField<Boolean> linked = fields().value("Linked", Codec.BOOL, false).toClient();
    @Nullable
    private NetworkUuid network;
    private final TickDraw draw = new TickDraw();
    /* Ticks until it reads its cable again. */
    private int lookIn;

    /* The most its store takes from the line in a tick: far more than any access point draws. */
    private static final long TAKES_A_TICK = 1_000L;
    private static final int TICKS_A_SECOND = 20;
    private static final String HOST_PREFIX = "AP-";
    /** A signal at full strength, in percent. */
    private static final long WHOLE_SIGNAL = 100L;

    public AccessPointBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.ACCESS_POINT_BE.get(), pos, state);
        fields().part("Energy", store).save();
        fields().whenBroken((level, at) -> AccessPoints.remove(level, at));
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final AccessPointBlockEntity point) {
        if (level instanceof ServerLevel server) {
            point.tick(server);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) {
            AccessPoints.add(server, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel server) {
            AccessPoints.remove(server, worldPosition);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level instanceof ServerLevel server) {
            AccessPoints.remove(server, worldPosition);
        }
    }

    /** The Wi-Fi it gives, its era's. */
    public WirelessStandard standard() {
        return getBlockState().getBlock() instanceof AccessPointBlock block ? block.standard()
                : WirelessStandard.LEGACY;
    }

    /** The network its cable reaches, or null when it is on none. */
    @Nullable
    public NetworkUuid network() {
        return network;
    }

    /** The network it gives the devices round it: its own, while it has its energy; null otherwise. */
    @Nullable
    public NetworkUuid serving() {
        return isOn() ? network : null;
    }

    /** Whether it has its energy, or the server lets devices run free. */
    public boolean isOn() {
        return Boolean.TRUE.equals(on.get());
    }

    /** Whether it is on a network, as the client knows it for its lamps. */
    public boolean isLinked() {
        return Boolean.TRUE.equals(linked.get());
    }

    /**
     * Whether a device at {@code pos} is within its reach, as the crow flies, where each stands in the world: an
     * access point aboard a ship of Create Aeronautics reaches one on the ground from where the ship flies now
     * (computers/docs/design/implementation.md, "Create").
     */
    public boolean reaches(final BlockPos pos) {
        final int reach = standard().reachBlocks();
        return level == null ? worldPosition.distSqr(pos) <= (double) reach * reach
                : inWorld(level, worldPosition).distanceToSqr(inWorld(level, pos)) <= (double) reach * reach;
    }

    /**
     * How strong its signal is at {@code pos}, in whole percent: all of it beside it, falling evenly to none at the
     * edge of its reach, measured as {@link #reaches} measures. It only tells: the link's speed is its standard's
     * anywhere in reach (computers/docs/design/wireless.md).
     */
    public int signalPercent(final BlockPos pos) {
        final double reach = standard().reachBlocks();
        final double distance = level == null ? Math.sqrt(worldPosition.distSqr(pos))
                : inWorld(level, worldPosition).distanceTo(inWorld(level, pos));
        return (int) Math.max(0L, Math.min(WHOLE_SIGNAL, Math.round(WHOLE_SIGNAL * (1.0 - distance / reach))));
    }

    /** Where {@code pos} is in the world: a place aboard a moving ship, where the ship is now; any other, itself. */
    public static Vec3 inWorld(final Level level, final BlockPos pos) {
        return ForeignMovers.inWorld(level, Vec3.atCenterOf(pos));
    }

    /**
     * The name an access point of node {@code node} goes by on its network's screens, as a computer goes by its host
     * name: {@code AP-} and its node's short id.
     */
    public static String hostname(final NodeUuid node) {
        return HOST_PREFIX + ShortId.of(node.asString());
    }

    /** The node it is known by on its network, drawn the first time anyone asks. */
    public NodeUuid nodeUuid() {
        if (node.get() == null || node.get().isEmpty()) {
            node.set(NodeUuid.random().asString());
        }
        return NodeUuid.fromString(node.get());
    }

    /** The data cables it is on: the one at the face its cables come in by, or none. */
    public List<Long> cables(final ServerLevel level) {
        return DataWires.numbersReaching(level, worldPosition, AccessPointBlock.cableFace(getBlockState()),
                DataWires::isNetwork);
    }

    /** Its energy port, on the face its cables come in by and no other. */
    @Nullable
    public IEnergyPort energyPort(@Nullable final Direction side) {
        return side == AccessPointBlock.cableFace(getBlockState()) ? store : null;
    }

    /** What the game's FE capability sees there: the same store, while the series speaks FE. */
    @Nullable
    public IEnergyStorage feEnergy(@Nullable final Direction side) {
        return side == AccessPointBlock.cableFace(getBlockState()) && JsEnergy.speaksFe() ? store.asFe() : null;
    }

    /* A tick: its draw, and once a second the network off its cable; the lamps follow both. */
    private void tick(final ServerLevel level) {
        final boolean powered = draw.draw(store, standard().watts());
        if (--lookIn <= 0) {
            lookIn = TICKS_A_SECOND;
            final List<Long> found = cables(level);
            final Optional<NetworkUuid> read = found.isEmpty() ? Optional.empty()
                    : NetworkSystem.get(level).connectivity().networkOf(found.getFirst());
            network = read.orElse(null);
        }
        if (powered != isOn() || (network != null) != isLinked()) {
            on.set(powered);
            linked.set(network != null);
            fields().syncToClients();
        }
    }
}
