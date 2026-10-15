/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.Wire;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.id.IStableName;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * What an instrument reads of energy, as a clamp meter does: through a wire of a line (how much crossed it in the last
 * tick, which way, and its ceiling), and through each face of a block (what went in and out, and the faces with no
 * port). Amounts are in the energy's own unit and in FE alike, for an instrument that shows either.
 */
public final class EnergyReadings {

    /** How long a wire stays watched after it was last read, in ticks, so its direction is known. */
    public static final int WATCH_TICKS = 40;
    /** A block's faces in the order a reading lists them: around it, then above and below. */
    public static final List<Direction> FACES = List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST,
            Direction.WEST, Direction.UP, Direction.DOWN);

    private EnergyReadings() {
    }

    /**
     * What crosses the wire of {@code type} in the cable block at {@code pos}, or none when it is no wire of an
     * energy's grid. Reading it watches it, so from the next tick its direction is known too.
     */
    public static Optional<LineReading> line(final ServerLevel level, final BlockPos pos, final CableType type) {
        if (!(level.getBlockEntity(pos) instanceof CableBlockEntity cable) || !cable.holds(type)) {
            return Optional.empty();
        }
        final Optional<Energy> energy = Energies.ofGrid(type.grid());
        final Optional<LevelEnergy> grid = moverOf(level, type.grid());
        final OptionalLong number = CoreGrids.places(level).find(cable.place(type));
        if (energy.isEmpty() || grid.isEmpty() || number.isEmpty()) {
            return Optional.empty();
        }
        final long wire = number.getAsLong();
        grid.get().watch(wire, level.getGameTime() + WATCH_TICKS);
        final long perTick = grid.get().lastTickOf(wire).map(tick -> tick.perCableUsage().getOrDefault(wire, 0L))
                .orElse(0L);
        final Direction toward = grid.get().towardOf(level, wire).map(next -> facing(pos, next)).orElse(null);
        final EnergyClass lineClass = energy.get().classOf(type.grid()).orElseThrow();
        return Optional.of(new LineReading(energy.get(), lineClass, type, perTick, toward, type.throughput()));
    }

    /** The first wire of an energy's grid in the cable block at {@code pos}, or none. */
    public static Optional<CableType> energyWireAt(final ServerLevel level, final BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof CableBlockEntity cable)) {
            return Optional.empty();
        }
        for (final Wire wire : cable.wires()) {
            if (Energies.ofGrid(wire.type().grid()).isPresent()) {
                return Optional.of(wire.type());
            }
        }
        return Optional.empty();
    }

    /**
     * What the block at {@code pos} holds, and what went in and out through each of its faces in the last tick; none
     * when it holds no energy at all.
     */
    public static Optional<BlockReading> block(final ServerLevel level, final BlockPos pos) {
        final IEnergyPort port = level.getCapability(EnergyPorts.PORT, pos, null);
        final IEnergyStorage fe = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
        if (port == null && fe == null) {
            return Optional.empty();
        }
        final Energy energy = port != null ? port.energy() : Energies.FE;
        final long stored = port != null ? port.stored() : fe.getEnergyStored();
        final long capacity = port != null ? port.capacity() : fe.getMaxEnergyStored();
        final List<FaceReading> faces = new ArrayList<>(FACES.size());
        final Set<String> counted = new HashSet<>();
        for (final Direction face : FACES) {
            faces.add(face(level, pos, face, counted));
        }
        return Optional.of(new BlockReading(energy, port == null ? null : port.portClass(), stored, capacity,
                faces));
    }

    /* The block of {@code next} seen from {@code pos}: the face it lies beyond, or none when it is no neighbour. */
    private static @Nullable Direction facing(final BlockPos pos, final BlockPos next) {
        return Direction.fromDelta(next.getX() - pos.getX(), next.getY() - pos.getY(), next.getZ() - pos.getZ());
    }

    /*
     * What went through {@code face} of the block at {@code pos}: the flow of the wire plugged into it there, counted
     * once for each part of a grid, so a block met by one part on two faces shows its flow on the first.
     */
    private static FaceReading face(final ServerLevel level, final BlockPos pos, final Direction face,
                                    final Set<String> counted) {
        final boolean hasPort = level.getCapability(EnergyPorts.PORT, pos, face) != null
                || level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, face) != null;
        if (!hasPort) {
            return new FaceReading(face, FaceReading.Kind.NO_PORT, 0L);
        }
        if (level.getBlockEntity(pos.relative(face)) instanceof CableBlockEntity cable) {
            for (final Wire wire : cable.wiresThrough(face.getOpposite())) {
                final Optional<LevelEnergy> grid = moverOf(level, wire.type().grid());
                final OptionalLong number = CoreGrids.places(level).find(cable.place(wire.type()));
                if (grid.isEmpty() || number.isEmpty()
                        || (cable.plugs(wire.slot()) & 1 << face.getOpposite().get3DDataValue()) == 0) {
                    continue;
                }
                final Optional<LevelEnergy.Flow> flow = grid.get().flowAt(number.getAsLong(), pos);
                if (flow.isEmpty()) {
                    continue;
                }
                final String part = wire.type().grid().id() + "/" + grid.get().partOf(number.getAsLong()).orElse(-1);
                if (!counted.add(part)) {
                    return new FaceReading(face, FaceReading.Kind.IDLE, 0L);
                }
                final long in = flow.get().in();
                final long out = flow.get().out();
                return in >= out ? new FaceReading(face, in > 0 ? FaceReading.Kind.IN : FaceReading.Kind.IDLE, in)
                        : new FaceReading(face, FaceReading.Kind.OUT, out);
            }
        }
        return new FaceReading(face, FaceReading.Kind.IDLE, 0L);
    }

    private static Optional<LevelEnergy> moverOf(final ServerLevel level, final GridKind grid) {
        return CoreGrids.mover(level, grid).filter(LevelEnergy.class::isInstance).map(LevelEnergy.class::cast);
    }

    /**
     * What crossed a wire of a line in the last tick.
     *
     * @param energy    the line's energy
     * @param lineClass its class
     * @param cable     the wire's cable
     * @param perTick   what crossed it, in the energy's unit
     * @param toward    the face beyond which most of it went; none when nothing ran or it is not known yet
     * @param ceiling   the most it carries in a tick
     */
    public record LineReading(Energy energy, EnergyClass lineClass, CableType cable, long perTick,
                              @Nullable Direction toward, long ceiling) {

        public LineReading {
            Objects.requireNonNull(energy, "energy");
            Objects.requireNonNull(lineClass, "lineClass");
            Objects.requireNonNull(cable, "cable");
        }

        /** Whether it carries all its ceiling lets through. */
        public boolean full() {
            return this.ceiling != Long.MAX_VALUE && this.perTick >= this.ceiling;
        }
    }

    /**
     * What a block holds, and what went through each of its faces in the last tick.
     *
     * @param energy    the energy it holds
     * @param portClass the class of its port; none for FE, which has no classes
     * @param stored    what it holds
     * @param capacity  the most it holds
     * @param faces     each face, in the order of {@link #FACES}
     */
    public record BlockReading(Energy energy, @Nullable EnergyClass portClass, long stored, long capacity,
                               List<FaceReading> faces) {

        public BlockReading {
            Objects.requireNonNull(energy, "energy");
            faces = List.copyOf(faces);
        }

        /** What went in through all its faces, less what went out. */
        public long net() {
            long net = 0L;
            for (final FaceReading face : this.faces) {
                net += face.kind() == FaceReading.Kind.IN ? face.perTick()
                        : face.kind() == FaceReading.Kind.OUT ? -face.perTick() : 0L;
            }
            return net;
        }
    }

    /**
     * What went through one face of a block.
     *
     * @param face    the face
     * @param kind    whether energy went in, out, nothing, or there is no port there
     * @param perTick how much, in the block's energy
     */
    public record FaceReading(Direction face, Kind kind, long perTick) {

        /** What a face did. */
        public enum Kind implements IStableName {
            /** No port on that face. */
            NO_PORT("no_port"),
            /** A port through which nothing went. */
            IDLE("idle"),
            /** Energy went in. */
            IN("in"),
            /** Energy went out. */
            OUT("out");

            private final String serializedName;

            Kind(final String serializedName) {
                this.serializedName = serializedName;
            }

            @Override
            public String serializedName() {
                return this.serializedName;
            }
        }
    }
}
