/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.persistor;

import com.mojang.serialization.Codec;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineEnergy;
import dev.jstech.core.machine.MachineResource;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.moving.BlockMove;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.region.ChunkLoaders;
import dev.jstech.core.region.ChunkLoadingBalance;
import dev.jstech.core.team.Ownership;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Space Persistor: the series' chunk loader, a machine of the Core's kit. It keeps the chunks of its area loaded
 * and ticking for its owner, nearest first, as far as its owner's number and the server's budget allow, while it is
 * on and, when the server makes it spend energy, while it has the power its chunks cost. Switched off, short of power
 * or broken, it lets every one of them go.
 */
public final class SpacePersistorBlockEntity extends MachineBlockEntity {

    private final ValueField<PersistorArea> area;
    private final ValueField<String> name;
    private final ValueField<PersistorReadings> readings;
    /* The chunks its area covers, worked out again only when the area changes. */
    private @Nullable List<ChunkPos> wanted;
    /* Whether its area changed since its chunks were last kept, or a beat went by to try again. */
    private boolean unsettled = true;
    private boolean full;
    private int beat;
    /* What the setting of its energy was last tick, so its port is offered or taken away as it changes. */
    private boolean tookEnergy = true;

    /** What it holds, in joules: a few seconds of a large area. */
    public static final long CAPACITY = 200_000L;
    /** The most it takes in a tick, in joules. */
    public static final long TAKES = 20_000L;
    /** The longest name it takes. */
    public static final int LONGEST_NAME = 24;
    /** How far round it other owners' chunks are read for its map, in chunks. */
    public static final int NEAR = 64;
    private static final int TICKS_A_SECOND = 20;
    private static final int MOST_OTHERS = 4_096;

    public SpacePersistorBlockEntity(final BlockPos pos, final BlockState state) {
        super(SpacePersistor.ENTITY.get(), pos, state, 0, MachineEnergy.of(JsEnergy.ENERGY, JsEnergy.LOW, CAPACITY,
                TAKES));
        this.area = fields().value("Area", PersistorArea.CODEC, PersistorArea.START).save().toClient();
        this.name = fields().value("Name", Codec.STRING, "").save().toClient();
        this.readings = fields().value("Readings", PersistorReadings.CODEC, PersistorReadings.NONE).toClient();
        fields().whenBroken(ChunkLoaders::releaseAll);
    }

    /** Its area. */
    public PersistorArea area() {
        final PersistorArea set = this.area.get();
        return set == null ? PersistorArea.START : set;
    }

    /** The name its player gave it; empty for none, when its screen names it by its place. */
    public String name() {
        final String given = this.name.get();
        return given == null ? "" : given;
    }

    /** What its screen shows, as the server last worked it out. */
    public PersistorReadings screenReadings() {
        final PersistorReadings read = this.readings.get();
        return read == null ? PersistorReadings.NONE : read;
    }

    /** Whether a line of its area runs along x: across the way it faces. */
    public boolean alongX() {
        final Direction facing = getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        return facing.getAxis() == Direction.Axis.Z;
    }

    /** The chunks its area covers, nearest it first. */
    public List<ChunkPos> wantedChunks() {
        if (this.wanted == null && YardLayout.isYardBlock(this.worldPosition.getX())) {
            // Aboard a moving structure the structure keeps the world under it loaded wherever it goes; the persistor
            // keeps only its own chunk of the yard, which tells the structure a loader is aboard.
            this.wanted = List.of(new ChunkPos(this.worldPosition));
        }
        if (this.wanted == null) {
            final ChunkPos own = new ChunkPos(this.worldPosition);
            final List<ChunkPos> chunks = new ArrayList<>();
            for (final long offset : area().offsets(alongX())) {
                chunks.add(new ChunkPos(own.x + PersistorShapes.dx(offset), own.z + PersistorShapes.dz(offset)));
            }
            chunks.sort(Comparator.comparingInt(chunk -> Math.max(Math.abs(chunk.x - own.x),
                    Math.abs(chunk.z - own.z))));
            this.wanted = List.copyOf(chunks);
        }
        return this.wanted;
    }

    /** Gives its area the shape {@code shape}. */
    public void setShape(final PersistorShape shape) {
        changeArea(area().shaped(shape));
    }

    /** Gives its area the radius {@code radius}, held between none and {@link PersistorArea#MOST_RADIUS}. */
    public void setRadius(final int radius) {
        changeArea(area().reaching(radius));
    }

    /** Adds the chunk {@code dx, dz} from its own to its area, or takes it off, as a click on its map does. */
    public void click(final int dx, final int dz) {
        changeArea(area().clicked(dx, dz, alongX()));
    }

    /** Names it {@code given}, cut to {@link #LONGEST_NAME}; an empty name takes its name away. */
    public void rename(final String given) {
        final String trimmed = given.strip();
        this.name.set(trimmed.length() > LONGEST_NAME ? trimmed.substring(0, LONGEST_NAME) : trimmed);
        setChanged();
    }

    /** What its area costs a tick, in joules, at what the server's settings charge a chunk. */
    public long joulesPerTick() {
        return (long) Math.ceil((double) PersistorSettings.wattsPerChunk() * wantedChunks().size()
                / TICKS_A_SECOND);
    }

    @Override
    public boolean takesEnergy() {
        return PersistorSettings.spendsEnergy();
    }

    /* It holds no items: its face fills its screen, and the inventory shows only where its fuse and battery go. */
    @Override
    public boolean inventoryOnMain() {
        return false;
    }

    /* It shows nothing as it works: no smoke rises from a chunk loader. */
    @Override
    public void animate(final Level level, final BlockPos pos, final RandomSource random) {
    }

    @Override
    protected MachineStatus work(final ServerLevel level) {
        final Ownership owned = ownership();
        if (owned == null) {
            return MachineStatus.WAITING;
        }
        if (takesEnergy()) {
            final long cost = joulesPerTick();
            final long spent = power().spend(cost);
            drew(spent);
            if (spent < cost) {
                return MachineStatus.NO_ENERGY;
            }
        }
        if (this.unsettled) {
            keep(level, owned.owner());
        }
        return MachineStatus.WORKING;
    }

    @Override
    protected void settled(final ServerLevel level, final MachineStatus status) {
        if (status != MachineStatus.WORKING) {
            ChunkLoaders.releaseAll(level, this.worldPosition);
            this.unsettled = true;
            this.full = false;
        }
        if (takesEnergy() != this.tookEnergy) {
            this.tookEnergy = takesEnergy();
            level.invalidateCapabilities(this.worldPosition);
        }
        if (++this.beat >= TICKS_A_SECOND) {
            this.beat = 0;
            // A chunk refused for want of room is asked for again once a second.
            this.unsettled |= this.full;
            this.readings.set(read(level, status));
        }
    }

    /* Energy only, and only while the server makes it spend any: it holds no items. */
    @Override
    protected Set<MachineResource> resources() {
        return takesEnergy() ? Set.of(MachineResource.ENERGY) : Set.of();
    }

    /* Moved with a structure: the chunks it kept where it was are let go, and it keeps chunks again from here. */
    @Override
    public void moved(final BlockPos was, final BlockMove move) {
        super.moved(was, move);
        if (move.from() instanceof ServerLevel from) {
            ChunkLoaders.releaseAll(from, was);
        }
        this.wanted = null;
        this.unsettled = true;
        setChanged();
        wake();
    }

    private void changeArea(final PersistorArea to) {
        this.area.set(to);
        this.wanted = null;
        this.unsettled = true;
        setChanged();
        wake();
    }

    /* Lets go of the chunks it no longer covers and keeps the rest, nearest first, till its owner or server is full. */
    private void keep(final ServerLevel level, final UUID owner) {
        final List<ChunkPos> chunks = wantedChunks();
        final Set<ChunkPos> covered = new LinkedHashSet<>(chunks);
        final Set<ChunkPos> held = ChunkLoaders.loadedFrom(level, this.worldPosition);
        for (final ChunkPos chunk : held) {
            if (!covered.contains(chunk)) {
                ChunkLoaders.release(level, this.worldPosition, chunk);
            }
        }
        boolean refused = false;
        for (final ChunkPos chunk : chunks) {
            if (held.contains(chunk)) {
                continue;
            }
            final ChunkLoaders.LoadOutcome outcome = ChunkLoaders.load(level, this.worldPosition, owner, chunk,
                    true);
            if (outcome == ChunkLoaders.LoadOutcome.LIMIT_REACHED || outcome == ChunkLoaders.LoadOutcome.SERVER_FULL) {
                refused = true;
                break;
            }
        }
        this.full = refused;
        this.unsettled = false;
    }

    private PersistorReadings read(final ServerLevel level, final MachineStatus status) {
        final Ownership owned = ownership();
        final int kept = ChunkLoaders.loadedFrom(level, this.worldPosition).size();
        final int owners = owned == null ? 0 : ChunkLoaders.loadedBy(level.getServer(), owned.owner());
        final long watts = takesEnergy() && status == MachineStatus.WORKING
                ? (long) PersistorSettings.wattsPerChunk() * wantedChunks().size() : 0L;
        return new PersistorReadings(kept, owners, ChunkLoadingBalance.limit(),
                ChunkLoaders.loadedOnServer(level.getServer()), ChunkLoadingBalance.serverBudget(), watts,
                takesEnergy(), status == MachineStatus.WORKING && kept > 0, this.full, others(level, owned));
    }

    /* The chunks other owners keep near it, for its map; its own owner's other machines are theirs to know. */
    private List<Long> others(final ServerLevel level, final @Nullable Ownership owned) {
        final ChunkPos own = new ChunkPos(this.worldPosition);
        final Map<BlockPos, UUID> owners = ChunkLoaders.sources(level);
        final List<Long> near = new ArrayList<>();
        for (final Map.Entry<BlockPos, Set<ChunkPos>> source : ChunkLoaders.kept(level).entrySet()) {
            if (source.getKey().equals(this.worldPosition)
                    || owned != null && owned.owner().equals(owners.get(source.getKey()))) {
                continue;
            }
            for (final ChunkPos chunk : source.getValue()) {
                if (Math.abs(chunk.x - own.x) <= NEAR && Math.abs(chunk.z - own.z) <= NEAR
                        && near.size() < MOST_OTHERS) {
                    near.add(chunk.toLong());
                }
            }
        }
        return near;
    }
}
