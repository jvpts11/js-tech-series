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
import dev.jstech.computers.block.AsicBlock;
import dev.jstech.computers.teracoin.AsicModel;
import dev.jstech.computers.teracoin.MinerKinds;
import dev.jstech.computers.teracoin.MinerRunner;
import dev.jstech.computers.teracoin.MinerSettings;
import dev.jstech.computers.teracoin.MiningPools;
import dev.jstech.computers.teracoin.TeracoinAddress;
import dev.jstech.computers.teracoin.TeracoinNbt;
import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.LongField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.energy.EnergyStore;
import dev.jstech.core.energy.IEnergyPort;
import dev.jstech.core.id.StableIds;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.network.INetworkMember;
import dev.jstech.core.jstechseries.network.NetworkCategory;
import dev.jstech.core.jstechseries.network.NetworkMembers;
import dev.jstech.core.jstechseries.network.NetworkMembership;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * An ASIC (computers/docs/design/teracoin.md): it draws its watts on any face, is on the data network it touches as a
 * machine the network reads, and while it has its energy mines once a second, paying the address its window set,
 * for the pool it chose or alone. It has no system: nothing of it runs but the mining. Its window reads what it shows
 * off the fields here, which the players who see it are sent.
 */
public class AsicBlockEntity extends SyncedBlockEntity implements INetworkMember {

    private final EnergyStore store = new EnergyStore("Energy", JsEnergy.ENERGY, JsEnergy.LOW, 0L, TAKES_A_TICK, 0L,
            this::setChanged);
    private final TickDraw draw = new TickDraw();
    private final NetworkMembership membership;
    /* Where it pays and for which pool, as its window set them; it always mines while it has its energy. */
    private final MinerSettings miner = new MinerSettings();
    /* How many blocks its work found, and how long it has mined without a stop, in ticks. */
    private final IntField found = fields().integer("Found", 0).save().toClient();
    private final LongField uptime = fields().longInteger("Uptime", 0L).save().toClient();
    /* What its window shows, worked out once a second on the server. */
    private final ValueField<String> address = fields().value("Address", Codec.STRING, "").toClient();
    private final ValueField<String> pool = fields().value("PoolShown", Codec.STRING, "").toClient();
    private final ValueField<String> poolHost = fields().value("PoolHost", Codec.STRING, "").toClient();
    private final ValueField<Boolean> alone = fields().value("Alone", Codec.BOOL, false).toClient();
    private final IntField status = fields().integer("Status", 0).toClient();
    private final ValueField<Boolean> powered = fields().value("Powered", Codec.BOOL, false).toClient();

    /* The most its store takes from the line in a tick: more than any ASIC draws in one. */
    private static final long TAKES_A_TICK = 1_000L;
    private static final int TICKS_A_SECOND = 20;
    private static final StableIds<MinerSettings.Status> STATUSES = StableIds.of(MinerSettings.Status.class);

    public AsicBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.ASIC_BE.get(), pos, state);
        fields().part("Energy", store).save();
        fields().part("Miner", IFieldPart.of((tag, registries) -> TeracoinNbt.save(miner, tag),
                (tag, registries) -> TeracoinNbt.load(miner, tag))).save();
        this.membership = NetworkMembers.keep(this);
        // What its window shows starts as its saved settings, before its first second of work.
        fields().whenLoaded(level -> shown());
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final AsicBlockEntity asic) {
        if (level instanceof ServerLevel server) {
            asic.tick(server);
        }
    }

    /** The model it is: its hashrate, its draw, its hashboards. */
    public AsicModel model() {
        return getBlockState().getBlock() instanceof AsicBlock block ? block.model() : AsicModel.FORGE_S1;
    }

    @Override
    public NetworkCategory category() {
        return NetworkCategory.B;
    }

    /** The network it is on, or null. */
    @Nullable
    public NetworkUuid network() {
        return membership.network().orElse(null);
    }

    /** Sets the address it pays, on the server; a malformed one is refused. Whether it took it. */
    public boolean setAddress(final String typed) {
        final String trimmed = typed.trim();
        if (!TeracoinAddress.valid(trimmed)) {
            return false;
        }
        miner.setAddress(trimmed);
        address.set(trimmed);
        setChanged();
        return true;
    }

    /** Mines alone from now on, on the server. */
    public void setAlone() {
        miner.setAlone();
        alone.set(true);
        setChanged();
    }

    /**
     * Mines for the network's next pool after the one it chose, on the server: the pools of its network in their
     * order, round to the first; alone again when the network has none.
     */
    public void nextPool(final ServerLevel level) {
        final List<String> names = new ArrayList<>();
        MiningPools.on(level, network()).forEach(host -> names.add(MiningPools.name(host)));
        if (names.isEmpty()) {
            setAlone();
            return;
        }
        final int at = miner.alone() ? -1 : names.indexOf(miner.pool().isEmpty() ? pool.get() : miner.pool());
        miner.setPool(names.get((at + 1) % names.size()));
        alone.set(false);
        setChanged();
    }

    /** Its energy port, on every face. */
    public IEnergyPort energyPort() {
        return store;
    }

    /** What the game's FE capability sees: the same store, while the series speaks FE. */
    @Nullable
    public IEnergyStorage feEnergy() {
        return JsEnergy.speaksFe() ? store.asFe() : null;
    }

    // What its window and its model show, on either side.

    /** Whether it mines now: its energy in, and its work offered to the chain. */
    public boolean mining() {
        return powered() && (status() == MinerSettings.Status.ALONE || status() == MinerSettings.Status.POOL);
    }

    /** Whether it has its energy. */
    public boolean powered() {
        return Boolean.TRUE.equals(powered.get());
    }

    public MinerSettings.Status status() {
        return STATUSES.byId(status.get(), MinerSettings.Status.OFF);
    }

    /** The address it pays; empty until its window sets one. */
    public String address() {
        return address.get();
    }

    /** The pool it mines for, as it last offered its work; empty when alone or before it mined. */
    public String poolShown() {
        return pool.get();
    }

    /** The server the pool it mines for runs on. */
    public String poolHost() {
        return poolHost.get();
    }

    /** Whether it was set to mine alone. */
    public boolean alone() {
        return Boolean.TRUE.equals(alone.get());
    }

    /** How many blocks its work found. */
    public int found() {
        return found.get();
    }

    /** How long it has mined without a stop, in ticks. */
    public long uptime() {
        return uptime.get();
    }

    /* A tick: its draw, and once a second its work offered to the chain; what its window shows follows. */
    private void tick(final ServerLevel level) {
        final AsicModel model = model();
        final boolean on = draw.draw(store, model.watts());
        if (on != powered()) {
            powered.set(on);
        }
        if (!on) {
            if (miner.status() != MinerSettings.Status.OFF) {
                miner.stalled(MinerSettings.Status.OFF, "");
                uptime.set(0L);
                shown();
            }
            return;
        }
        if (mining()) {
            uptime.set(uptime.get() + 1);
        }
        if (!miner.secondDue()) {
            return;
        }
        final String node = membership.node().value().toString();
        MinerRunner.offer(level, network(), worldPosition, miner,
                new MinerRunner.Miner(node, model.name() + " " + ShortId.of(node),
                        MinerKinds.asic(model.era().path()), model.hashrate()),
                block -> found.set(found.get() + 1));
        if (miner.status() != MinerSettings.Status.ALONE && miner.status() != MinerSettings.Status.POOL) {
            uptime.set(0L);
        }
        shown();
    }

    /* Copies what its window shows from its miner, sent to those who see it only when it changed. */
    private void shown() {
        if (status.get() != miner.status().id()) {
            status.set(miner.status().id());
        }
        if (!miner.poolShown().equals(pool.get())) {
            pool.set(miner.poolShown());
        }
        if (!miner.poolHost().equals(poolHost.get())) {
            poolHost.set(miner.poolHost());
        }
        if (!miner.address().equals(address.get())) {
            address.set(miner.address());
        }
        if (miner.alone() != alone()) {
            alone.set(miner.alone());
        }
    }
}
