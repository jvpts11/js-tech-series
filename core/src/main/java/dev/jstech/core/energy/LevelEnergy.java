/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import dev.jstech.core.JsCore;
import dev.jstech.core.cable.CableParts;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.diagnostic.Section;
import dev.jstech.core.energy.internal.EnergyNetwork;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.Grid;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.GridPlace;
import dev.jstech.core.grid.IGridMover;
import dev.jstech.core.log.CoreLogs;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * The grid of one class of an energy in one dimension as it moves the energy: each connected part of the grid with the
 * blocks its wires plug into, and what each part moved in the last tick. The parts are worked out again only when the
 * grid's shape changes or a wire comes to meet another block; a tick only shares the energy out. Nothing here is saved:
 * it is worked out from the wires as they load.
 *
 * <p>A block gives energy when it can give and not take (a generator), takes it when it can take and not give, and
 * holds it when it can do both: for FE as the game's energy capability on the face a wire plugs into says, for a mod's
 * own energy as its port does ({@link EnergyPorts#PORT}). Each block of a wire loses its cable's own share and its
 * class's loss by distance. A port of another class takes and gives nothing on the line; each tick the line is live,
 * it is told what befalls it, as its energy's rule says.
 */
public final class LevelEnergy implements IGridMover {

    private final GridKind kind;
    private final Energy energy;
    private final EnergyClass lineClass;
    private final List<EnergyNetwork> parts = new ArrayList<>();
    private final List<List<PortNode>> portsOfParts = new ArrayList<>();
    private final List<Map<Long, Long>> nodesOfParts = new ArrayList<>();
    private final Map<Long, Integer> partOfWire = new HashMap<>();
    private final Map<Long, CableType> typeOfWire = new HashMap<>();
    private final Map<Long, Long> watchedUntil = new HashMap<>();
    private List<EnergyDistributionResult> lastTick = List.of();
    private long builtVersion = -1L;
    private long builtTaps = -1L;
    private long taps;

    /** What the debug screen calls the time the energy grid takes to move a tick's energy. */
    public static final ResourceLocation TIMING = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "energy_grid");
    /** The work of moving a tick's energy, timed, named in profilers and watched for stalls. */
    public static final Section SECTION = Section.builder(TIMING, CoreLogs.ENERGY).declare();

    /** The work that shares FE along the grid of {@code kind}, one for each dimension. */
    public LevelEnergy(final GridKind kind) {
        this(kind, Energies.FE, Energies.FE.lowest());
    }

    /** The work that shares {@code energy} along the grid of {@code kind}, its lines of {@code lineClass}. */
    public LevelEnergy(final GridKind kind, final Energy energy, final EnergyClass lineClass) {
        this.kind = kind;
        this.energy = energy;
        this.lineClass = lineClass;
    }

    /**
     * Whether a line of {@code energy} plugs into the block at {@code pos}, met on its {@code face}: for FE, one that
     * offers the game's energy capability; for a mod's own energy, one with a port of that energy, of whatever class,
     * so a port of another class learns what befalls it.
     */
    public static boolean plugsInto(final Energy energy, final ServerLevel level, final BlockPos pos,
                                    final Direction face) {
        if (energy == Energies.FE) {
            return level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, face) != null;
        }
        final IEnergyPort port = level.getCapability(EnergyPorts.PORT, pos, face);
        if (port != null) {
            return port.energy() == energy;
        }
        return energy.acceptsFe() && level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, face) != null;
    }

    /** The energy it moves. */
    public Energy energy() {
        return this.energy;
    }

    /** What each connected part of the grid moved in the last tick. */
    public List<EnergyDistributionResult> lastTick() {
        return this.lastTick;
    }

    /** How many connected parts the grid is in. */
    public int partCount() {
        return this.parts.size();
    }

    /** What the part of the grid the wire numbered {@code wire} is in moved in the last tick; empty for none. */
    public Optional<EnergyDistributionResult> lastTickOf(final long wire) {
        final Integer part = this.partOfWire.get(wire);
        return part == null || part >= this.lastTick.size() ? Optional.empty()
                : Optional.of(this.lastTick.get(part));
    }

    /**
     * Keeps which way the energy runs through the wire numbered {@code wire} until tick {@code until}, as an instrument
     * clamped on it asks: only watched wires pay for it.
     */
    public void watch(final long wire, final long until) {
        this.watchedUntil.merge(wire, until, Math::max);
    }

    /** Which connected part of the grid the wire numbered {@code wire} is in, counted from 0; none for no wire of it. */
    public OptionalInt partOf(final long wire) {
        final Integer part = this.partOfWire.get(wire);
        return part == null ? OptionalInt.empty() : OptionalInt.of(part);
    }

    /** The cable of the wire numbered {@code wire}, when it is a wire of this grid. */
    public Optional<CableType> typeOf(final long wire) {
        return Optional.ofNullable(this.typeOfWire.get(wire));
    }

    /**
     * Where the energy through the wire numbered {@code wire} went in the last tick: the block of the wire or the
     * machine most of it ran toward. Only for a watched wire through which anything ran.
     */
    public Optional<BlockPos> towardOf(final ServerLevel level, final long wire) {
        final Integer part = this.partOfWire.get(wire);
        final Optional<EnergyDistributionResult> tick = lastTickOf(wire);
        if (part == null || tick.isEmpty()) {
            return Optional.empty();
        }
        final OptionalLong toward = tick.get().towardOf(wire);
        if (toward.isEmpty()) {
            return Optional.empty();
        }
        final long next = toward.getAsLong();
        if (next < 0) {
            for (final Map.Entry<Long, Long> machine : this.nodesOfParts.get(part).entrySet()) {
                if (machine.getValue() == next) {
                    return Optional.of(BlockPos.of(machine.getKey()));
                }
            }
            return Optional.empty();
        }
        final GridPlace place = CoreGrids.places(level).place(next);
        return place == null ? Optional.empty() : Optional.of(BlockPos.of(place.pos()));
    }

    /**
     * What the block at {@code pos} took in and gave out in the last tick, through the part of the grid the wire
     * numbered {@code wire} is in; empty when the wire does not plug into it.
     */
    public Optional<Flow> flowAt(final long wire, final BlockPos pos) {
        final Integer part = this.partOfWire.get(wire);
        if (part == null || part >= this.lastTick.size()) {
            return Optional.empty();
        }
        final Long node = this.nodesOfParts.get(part).get(pos.asLong());
        if (node == null) {
            return Optional.empty();
        }
        final EnergyDistributionResult tick = this.lastTick.get(part);
        return Optional.of(new Flow(tick.perConsumerDelivered().getOrDefault(node, 0L),
                tick.perSupplierSent().getOrDefault(node, 0L)));
    }

    /** A wire came to meet a machine or stopped meeting one: the parts are worked out again before the next tick. */
    @Override
    public void tapsChanged() {
        this.taps++;
    }

    /** Moves the tick's energy through every part of the grid of {@code level}. */
    @Override
    public void tick(final ServerLevel level) {
        final Grid grid = CoreGrids.of(level, this.kind);
        if (grid.size() == 0) {
            this.parts.clear();
            this.portsOfParts.clear();
            this.nodesOfParts.clear();
            this.partOfWire.clear();
            this.typeOfWire.clear();
            this.lastTick = List.of();
            return;
        }
        SECTION.run(() -> move(level, grid));
    }

    /* The parts worked out again when the grid changed, and the tick's energy shared out through each. */
    private void move(final ServerLevel level, final Grid grid) {
        if (grid.version() != this.builtVersion || this.taps != this.builtTaps) {
            build(level, grid);
            this.builtVersion = grid.version();
            this.builtTaps = this.taps;
        }
        final long now = level.getGameTime();
        this.watchedUntil.values().removeIf(until -> until < now);
        final Set<Long> watched = this.watchedUntil.isEmpty() ? Set.of() : Set.copyOf(this.watchedUntil.keySet());
        final List<EnergyDistributionResult> results = new ArrayList<>(this.parts.size());
        for (int i = 0; i < this.parts.size(); i++) {
            final EnergyDistributionResult result = this.parts.get(i).tickDistribute(watched);
            results.add(result);
            if (result.totalSupply() > 0) {
                for (final PortNode port : this.portsOfParts.get(i)) {
                    port.meetIfWrong(level.getGameTime());
                }
            }
        }
        this.lastTick = results;
    }

    /*
     * Each wire of a part is a cable of its network; each block a wire plugs into is a node of it, numbered below zero
     * so it never meets a wire's number.
     */
    private void build(final ServerLevel level, final Grid grid) {
        this.parts.clear();
        this.portsOfParts.clear();
        this.nodesOfParts.clear();
        this.partOfWire.clear();
        this.typeOfWire.clear();
        final int classLoss = this.energy.lossPerBlock(this.lineClass);
        for (final CableParts.Part part : CableParts.of(level, grid)) {
            final EnergyNetwork network = new EnergyNetwork();
            final List<PortNode> ports = new ArrayList<>();
            for (final CableParts.WireAt wire : part.wires()) {
                final long carries = wire.runTooLong() ? 0L : wire.type().throughput();
                network.addCable(wire.number(), new WireCable(carries,
                        EnergyLoss.along(wire.type().loss(), classLoss)));
                this.partOfWire.put(wire.number(), this.parts.size());
                this.typeOfWire.put(wire.number(), wire.type());
            }
            for (final CableParts.WireAt wire : part.wires()) {
                for (final long next : grid.neighbours(wire.number())) {
                    if (next > wire.number() && network.contains(next)) {
                        network.connect(wire.number(), next);
                    }
                }
            }
            final Map<Long, Long> machines = new HashMap<>();
            for (final CableParts.Plug plug : part.plugs()) {
                Long node = machines.get(plug.at().asLong());
                if (node == null) {
                    node = -(machines.size() + 1L);
                    machines.put(plug.at().asLong(), node);
                    network.addNode(node, node(level, plug, ports));
                }
                network.connect(plug.wire(), node);
            }
            this.parts.add(network);
            this.portsOfParts.add(ports);
            this.nodesOfParts.add(machines);
        }
    }

    /* The block a wire plugs into, as a node: FE through the game's capability, a mod's energy through its port. */
    private IEnergyNode node(final ServerLevel level, final CableParts.Plug plug, final List<PortNode> ports) {
        if (this.energy == Energies.FE) {
            return new MachineNode(BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, level, plug.at(),
                    plug.face()));
        }
        final PortNode port = new PortNode(BlockCapabilityCache.create(EnergyPorts.PORT, level, plug.at(),
                plug.face()), BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, level, plug.at(),
                plug.face()), this.energy, this.lineClass);
        ports.add(port);
        return port;
    }

    /**
     * What a block took in and gave out in a tick through one part of a grid.
     *
     * @param in  what it took in
     * @param out what it gave out
     */
    public record Flow(long in, long out) {
    }

    /* A wire as a cable of its part: what it carries and what it loses. */
    private record WireCable(long maxThroughput, int loss) implements IEnergyCable {
    }

    /* A machine a wire plugs into, seen through the game's energy capability on the face the wire meets. */
    private record MachineNode(BlockCapabilityCache<IEnergyStorage, @Nullable Direction> cache)
            implements IEnergyNode {

        @Override
        public EnergyNodeRole role() {
            final IEnergyStorage storage = this.cache.getCapability();
            if (storage == null) {
                return EnergyNodeRole.CONSUMER;
            }
            if (storage.canExtract() && storage.canReceive()) {
                return EnergyNodeRole.STORAGE;
            }
            return storage.canExtract() ? EnergyNodeRole.GENERATOR : EnergyNodeRole.CONSUMER;
        }

        @Override
        public long supply() {
            final IEnergyStorage storage = this.cache.getCapability();
            return storage == null || !storage.canExtract() ? 0L
                    : Math.max(0, storage.extractEnergy(Integer.MAX_VALUE, true));
        }

        @Override
        public long demand() {
            final IEnergyStorage storage = this.cache.getCapability();
            return storage == null || !storage.canReceive() ? 0L
                    : Math.max(0, storage.receiveEnergy(Integer.MAX_VALUE, true));
        }

        @Override
        public void onSupplied(final long amount) {
            final IEnergyStorage storage = this.cache.getCapability();
            long left = amount;
            while (storage != null && left > 0) {
                final int given = storage.extractEnergy((int) Math.min(Integer.MAX_VALUE, left), false);
                if (given <= 0) {
                    return;
                }
                left -= given;
            }
        }

        @Override
        public void onConsumed(final long amount) {
            final IEnergyStorage storage = this.cache.getCapability();
            long left = amount;
            while (storage != null && left > 0) {
                final int taken = storage.receiveEnergy((int) Math.min(Integer.MAX_VALUE, left), false);
                if (taken <= 0) {
                    return;
                }
                left -= taken;
            }
        }
    }

    /*
     * A block's port of a mod's energy, seen on the face the wire meets. A port of another class than the line's
     * takes and gives nothing on it, unless the energy's rule lets it, and is told what befalls it while the line is
     * live.
     */
    private static final class PortNode implements IEnergyNode {

        private final BlockCapabilityCache<IEnergyPort, @Nullable Direction> cache;
        private final BlockCapabilityCache<IEnergyStorage, @Nullable Direction> fe;
        private final Energy energy;
        private final EnergyClass lineClass;

        private PortNode(final BlockCapabilityCache<IEnergyPort, @Nullable Direction> cache,
                         final BlockCapabilityCache<IEnergyStorage, @Nullable Direction> fe, final Energy energy,
                         final EnergyClass lineClass) {
            this.cache = cache;
            this.fe = fe;
            this.energy = energy;
            this.lineClass = lineClass;
        }

        @Override
        public EnergyNodeRole role() {
            final IEnergyPort port = working();
            if (port == null) {
                return EnergyNodeRole.CONSUMER;
            }
            if (port.canExtract() && port.canReceive()) {
                return EnergyNodeRole.STORAGE;
            }
            return port.canExtract() ? EnergyNodeRole.GENERATOR : EnergyNodeRole.CONSUMER;
        }

        @Override
        public long supply() {
            final IEnergyPort port = working();
            return port == null || !port.canExtract() ? 0L : Math.max(0L, port.extract(Long.MAX_VALUE, true));
        }

        @Override
        public long demand() {
            final IEnergyPort port = working();
            return port == null || !port.canReceive() ? 0L : Math.max(0L, port.receive(Long.MAX_VALUE, true));
        }

        @Override
        public void onSupplied(final long amount) {
            final IEnergyPort port = working();
            if (port != null && amount > 0) {
                port.extract(amount, false);
            }
        }

        @Override
        public void onConsumed(final long amount) {
            final IEnergyPort port = working();
            if (port != null && amount > 0) {
                port.receive(amount, false);
            }
        }

        /* Tells the port what befalls it, when it is of another class than the live line. */
        private void meetIfWrong(final long gameTime) {
            final IEnergyPort port = this.cache.getCapability();
            if (port != null && port.energy() == this.energy && port.portClass() != this.lineClass) {
                final WrongClass what = this.energy.meeting(port.portClass(), this.lineClass);
                if (what != WrongClass.NONE) {
                    port.meetWrongClass(what, this.lineClass, gameTime);
                }
            }
        }

        /*
         * The port, when it is of this energy and works on this line: of its class, or let by the rule; or, while the
         * energy takes FE, a block with only the game's energy capability, at its worth in FE.
         */
        private @Nullable IEnergyPort working() {
            final IEnergyPort port = this.cache.getCapability();
            if (port == null) {
                final IEnergyStorage storage = this.energy.acceptsFe() ? this.fe.getCapability() : null;
                return storage == null ? null : FeBridge.port(storage, this.energy, this.lineClass);
            }
            if (port.energy() != this.energy) {
                return null;
            }
            return port.portClass() == this.lineClass
                    || this.energy.meeting(port.portClass(), this.lineClass).works() ? port : null;
        }
    }
}
