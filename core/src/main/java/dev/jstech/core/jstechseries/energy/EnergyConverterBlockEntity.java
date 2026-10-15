/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.LongField;
import dev.jstech.core.blockentity.PartField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.energy.Energy;
import dev.jstech.core.energy.EnergyClass;
import dev.jstech.core.energy.EnergyConversion;
import dev.jstech.core.energy.EnergyFaults;
import dev.jstech.core.energy.EnergyStore;
import dev.jstech.core.energy.EnergyTooltips;
import dev.jstech.core.energy.IEnergyPort;
import dev.jstech.core.energy.WrongClass;
import dev.jstech.core.look.IDescribed;
import dev.jstech.core.text.Text;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * An energy converter: a bridge between the game's FE and J's Energy that works either way, up to its limit a tick,
 * under its class's ceiling. Its FE side and its J's Energy side each take or give, as its direction says; inside, what
 * it takes waits at most a tick's worth of its limit before it crosses, so it never draws more than it moves. Crossing
 * rounds down at the server's rate of joules to FE, so it never makes energy. While J's Energy gives FE out, it pushes
 * the FE into the block on its FE side, as the game's generators do.
 *
 * <p>Its J's Energy side is a port of its class: a line of another class meets it as any port, burning it, shorting
 * it or blowing the fuse in its way, and while that is so it stops.
 */
public final class EnergyConverterBlockEntity extends SyncedBlockEntity implements IDescribed {

    private final @Nullable VoltageClass fixed;
    private final EnergyStore store;
    private final PartField energyField;
    private final BoolField on;
    private final BoolField toJe;
    private final IntField voltage;
    private final IntField digits;
    private final IntField power;
    private final LongField feHeld;
    private final JeSide jeSide = new JeSide();
    private final FeSide feSide = new FeSide();
    private final FeInside feInside = new FeInside();
    private long movedFe;
    private boolean limited;
    private @Nullable BlockCapabilityCache<IEnergyStorage, Direction> pushTo;
    private @Nullable Direction pushFace;

    /** Ticks in a second, to turn watts into joules a tick. */
    public static final int TICKS_PER_SECOND = 20;

    public EnergyConverterBlockEntity(final BlockPos pos, final BlockState state) {
        super(EnergyConverters.CONVERTER_BE.get(), pos, state);
        this.fixed = state.getBlock() instanceof EnergyConverterBlock block ? block.fixedClass().orElse(null) : null;
        final VoltageClass initial = this.fixed == null ? VoltageClass.LOW : this.fixed;
        final ConverterLimit limit = ConverterLimit.of(ConverterLimit.ceiling(initial));
        final long room = ConverterLimit.ceiling(this.fixed == null ? VoltageClass.ULTRA_HIGH : this.fixed)
                / TICKS_PER_SECOND;
        this.store = new EnergyStore("Energy", JsEnergy.ENERGY, JsEnergy.classOf(initial), room, room, room,
                this::storeChanged);
        this.energyField = fields().part("Energy", this.store).save();
        this.on = fields().flag("On", true).save();
        this.toJe = fields().flag("ToJe", true).save();
        this.voltage = fields().integer("Voltage", initial.rank()).save();
        this.digits = fields().integer("LimitDigits", limit.digits()).save();
        this.power = fields().integer("LimitPower", limit.power()).save();
        this.feHeld = fields().longInteger("Fe", 0L).save();
        this.store.onFault(this::fault);
        fields().mirror(EnergyConverterBlock.VOLTAGE, () -> voltageClass().rank());
    }

    /** Converts, once a tick, on the server. */
    public void tick() {
        syncClass();
        if (!running()) {
            this.movedFe = 0L;
            this.limited = false;
            return;
        }
        final long limitFe = limitFe();
        final EnergyConversion.Way way = this.toJe.get() ? EnergyConversion.Way.FE_TO_ENERGY
                : EnergyConversion.Way.ENERGY_TO_FE;
        this.movedFe = EnergyConversion.convert(this.feInside, this.store, way, limitFe);
        this.limited = this.movedFe > 0 && this.movedFe >= limitFe;
        if (!this.toJe.get()) {
            pushFe();
        }
    }

    /** Its class: a fixed converter's own, or the one a configurable one is set to. */
    public VoltageClass voltageClass() {
        if (this.fixed != null) {
            return this.fixed;
        }
        return VoltageClass.ofRank(this.voltage.get()).orElse(VoltageClass.LOW);
    }

    /** Whether it is a converter of one class. */
    public boolean fixed() {
        return this.fixed != null;
    }

    /** The limit on its thumbwheels. */
    public ConverterLimit limit() {
        return new ConverterLimit(this.digits.get(), this.power.get());
    }

    /** Whether it works from FE to J's Energy; from J's Energy to FE otherwise. */
    public boolean toJe() {
        return this.toJe.get();
    }

    /** Whether its switch is on. */
    public boolean on() {
        return this.on.get();
    }

    /** Its J's Energy store, the energy waiting to cross or just crossed. */
    public EnergyStore store() {
        return this.store;
    }

    /** What crossed it in the last tick, in FE. */
    public long movedFe() {
        return this.movedFe;
    }

    /** What it moves at most in a tick, in joules: its limit, under its class's ceiling. */
    public long limitJoules() {
        return Math.min(limit().watts(), ConverterLimit.ceiling(voltageClass())) / TICKS_PER_SECOND;
    }

    /** The same, in FE, at the server's rate. */
    public long limitFe() {
        return JsEnergy.ENERGY.toFe(limitJoules());
    }

    /** Turns its switch. */
    public void setOn(final boolean to) {
        this.on.set(to);
        sidesChanged();
    }

    /** Sets which way it works. */
    public void setToJe(final boolean to) {
        if (to != this.toJe.get()) {
            this.toJe.set(to);
            sidesChanged();
        }
    }

    /**
     * Sets a configurable converter to {@code to}; a fixed one keeps its class. A limit above the new class's ceiling
     * comes down to it.
     */
    public void setVoltage(final VoltageClass to) {
        if (this.fixed != null || to == voltageClass()) {
            return;
        }
        this.voltage.set(to.rank());
        syncClass();
        setLimit(limit());
        sidesChanged();
    }

    /** Turns a digit drum a step, up or down; the limit never goes above the class's ceiling. */
    public void turnDrum(final int drum, final boolean up) {
        setLimit(limit().turned(drum, up));
    }

    /** Turns the prefix drum a step; the limit never goes above the class's ceiling. */
    public void turnPrefix() {
        setLimit(limit().nextPrefix());
    }

    /** What its screen shows of it. */
    public ConverterView view() {
        final long limitJ = limitJoules();
        return new ConverterView(voltageClass().rank(), fixed(), this.on.get(), this.toJe.get(), this.digits.get(),
                this.power.get(), this.movedFe, JsEnergy.ENERGY.fromFe(this.movedFe), JsEnergy.ENERGY.toFe(limitJ),
                limitJ, this.limited, faulted());
    }

    /** What the game's FE capability sees of it on {@code side}: its FE side, and nothing on the others. */
    public @Nullable IEnergyStorage fe(final @Nullable Direction side) {
        return side != null && side == EnergyConverterBlock.feFace(facing()) ? this.feSide : null;
    }

    /** What the Core's energy capability sees of it on {@code side}: its J's Energy side, and nothing on the others. */
    public @Nullable IEnergyPort port(final @Nullable Direction side) {
        return side != null && side == EnergyConverterBlock.jeFace(facing()) ? this.jeSide : null;
    }

    @Override
    public void describe(final List<Text> lines, final boolean details) {
        lines.add(EnergyConverters.way(this.toJe.get()).text());
        final Energy energy = JsEnergy.ENERGY;
        lines.add(EnergyConverters.LIMIT_LINE.with(energy.rate(limitJoules())));
        lines.add(EnergyConverters.MOVED_LINE.with(energy.rate(energy.fromFe(this.movedFe))));
        if (!this.on.get()) {
            lines.add(EnergyConverters.SWITCHED_OFF.text());
        }
        lines.addAll(EnergyTooltips.store(this.store, gameTime()));
    }

    private boolean running() {
        final long now = gameTime();
        return this.on.get() && !this.store.damaged() && !this.store.cutByFuse() && !this.store.undervoltage(now)
                && !this.store.overvoltage(now);
    }

    private boolean faulted() {
        final long now = gameTime();
        return this.store.damaged() || this.store.cutByFuse() || this.store.undervoltage(now)
                || this.store.overvoltage(now);
    }

    /* Keeps the limit as set, unless it is above the class's ceiling, when it comes down to the ceiling. */
    private void setLimit(final ConverterLimit to) {
        final long ceiling = ConverterLimit.ceiling(voltageClass());
        final ConverterLimit kept = to.watts() > ceiling ? ConverterLimit.of(ceiling) : to;
        this.digits.set(kept.digits());
        this.power.set(kept.power());
    }

    /* The store's port takes the class a configurable converter is set to, once it is, or once it is loaded so. */
    private void syncClass() {
        final EnergyClass wanted = JsEnergy.classOf(voltageClass());
        if (this.store.portClass() != wanted) {
            this.store.setPortClass(wanted);
        }
    }

    /* Lines and pipes look its sides up again, as a side that took now gives, or one that was on is off. */
    private void sidesChanged() {
        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
        }
    }

    /* What the store holds changed: it is saved. */
    private void storeChanged() {
        this.energyField.changed();
    }

    private void fault(final WrongClass what) {
        if (this.level instanceof ServerLevel server) {
            EnergyFaults.strike(server, this.worldPosition, what);
        }
    }

    /* Gives what crossed to the block on its FE side, as much as that takes. */
    private void pushFe() {
        if (this.feHeld.get() <= 0 || !(this.level instanceof ServerLevel server)) {
            return;
        }
        final Direction face = EnergyConverterBlock.feFace(facing());
        if (this.pushTo == null || this.pushFace != face) {
            this.pushTo = BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, server,
                    this.worldPosition.relative(face), face.getOpposite());
            this.pushFace = face;
        }
        final IEnergyStorage target = this.pushTo.getCapability();
        if (target == null || !target.canReceive()) {
            return;
        }
        final int offered = (int) Math.min(Integer.MAX_VALUE, this.feHeld.get());
        final int taken = target.receiveEnergy(offered, false);
        if (taken > 0) {
            this.feHeld.set(this.feHeld.get() - taken);
        }
    }

    private Direction facing() {
        return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    private long gameTime() {
        return this.level == null ? 0L : this.level.getGameTime();
    }

    /*
     * The FE inside, between the FE side and the bridge: it holds a tick's worth of the limit at most, so the side
     * takes no more than the converter moves.
     */
    private final class FeInside implements IEnergyStorage {

        @Override
        public int receiveEnergy(final int toReceive, final boolean simulate) {
            final long taken = Math.max(0L, Math.min(toReceive, limitFe() - feHeld.get()));
            if (!simulate && taken > 0) {
                feHeld.set(feHeld.get() + taken);
            }
            return (int) taken;
        }

        @Override
        public int extractEnergy(final int toExtract, final boolean simulate) {
            final long given = Math.max(0L, Math.min(toExtract, feHeld.get()));
            if (!simulate && given > 0) {
                feHeld.set(feHeld.get() - given);
            }
            return (int) given;
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, feHeld.get());
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, Math.max(limitFe(), feHeld.get()));
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }

    /* Its FE side: takes FE while it works toward J's Energy, gives it while it works toward FE. */
    private final class FeSide implements IEnergyStorage {

        @Override
        public int receiveEnergy(final int toReceive, final boolean simulate) {
            return canReceive() ? feInside.receiveEnergy(toReceive, simulate) : 0;
        }

        @Override
        public int extractEnergy(final int toExtract, final boolean simulate) {
            return canExtract() ? feInside.extractEnergy(toExtract, simulate) : 0;
        }

        @Override
        public int getEnergyStored() {
            return feInside.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return feInside.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return on.get() && !toJe.get();
        }

        @Override
        public boolean canReceive() {
            return on.get() && toJe.get();
        }
    }

    /*
     * Its J's Energy side: gives what crossed while it works toward J's Energy, takes, a tick's worth of its limit at
     * most, while it works toward FE. A line of another class meets its store.
     */
    private final class JeSide implements IEnergyPort {

        @Override
        public Energy energy() {
            return JsEnergy.ENERGY;
        }

        @Override
        public EnergyClass portClass() {
            return JsEnergy.classOf(voltageClass());
        }

        @Override
        public long stored() {
            return store.stored();
        }

        @Override
        public long capacity() {
            return Math.max(limitJoules(), store.stored());
        }

        @Override
        public boolean canReceive() {
            return on.get() && !toJe.get() && store.canReceive();
        }

        @Override
        public boolean canExtract() {
            return on.get() && toJe.get() && store.canExtract();
        }

        @Override
        public long receive(final long amount, final boolean simulate) {
            if (!canReceive()) {
                return 0L;
            }
            return store.receive(Math.min(amount, Math.max(0L, limitJoules() - store.stored())), simulate);
        }

        @Override
        public long extract(final long amount, final boolean simulate) {
            return canExtract() ? store.extract(amount, simulate) : 0L;
        }

        @Override
        public void meetWrongClass(final WrongClass what, final EnergyClass line, final long gameTime) {
            syncClass();
            store.meetWrongClass(what, line, gameTime);
        }
    }
}
