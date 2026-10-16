/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.connect.RelativeFace;
import dev.jstech.core.mechanical.IMechanicalPart;
import dev.jstech.core.mechanical.ITorque;
import dev.jstech.core.mechanical.RotationMath;
import dev.jstech.core.mechanical.ShaftContact;
import dev.jstech.core.mechanical.TorqueCurve;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * A machine's shaft, between its energy and a network of mechanical power, on one face of the machine: an electric
 * motor turns the network by its torque curve and spends the power it gives, over its efficiency, from the machine's
 * energy, whatever energy that is; a dynamo brakes the network the harder the faster it turns, up to its own curve at
 * its rated speed (the curve's free speed), and makes energy of what it takes, at its efficiency, while the machine
 * has room. A joule of energy is an FE, and a mod's energy counts at its worth in FE.
 *
 * <p>A machine driven by a shaft asks its torque of the network while it has work, and works as fast as the shaft
 * turns against its rated speed, up to its whole speed there; below the least speed it works at, it stops, turning too
 * slowly.
 *
 * <pre>{@code
 * turnsWith(MachineShaft.motor(TorqueCurve.of(50, 5_000, 1_500), 0.9, RelativeFace.BACK));   // in the constructor
 * ...
 * protected MachineStatus work(ServerLevel level) {
 *     return shaft().run();
 * }
 * }</pre>
 */
public final class MachineShaft implements IMechanicalPart, IFieldPart {

    private final Kind kind;
    private final TorqueCurve curve;
    private final double efficiency;
    private final RelativeFace face;
    private final double inertia;
    private final double friction;
    private final double load;
    private final double least;
    private final double rated;
    private @Nullable MachineBlockEntity machine;
    private @Nullable Runnable synced;
    private double rpm;
    private double share;
    private double asked;
    private double carried;

    /** What a machine's shaft weighs in turning unless said, in kilogram square metres: a small motor's rotor. */
    public static final double ROTOR = 0.05;
    /** What it holds back in its bearings unless said, in newton-metres. */
    public static final double BEARINGS = 0.2;
    private static final String KEY = "Shaft";
    private static final String RPM = "rpm";
    private static final String CARRIED = "carried";
    /* Below this, in revolutions a minute, a shaft stands, and a dynamo makes nothing. */
    private static final double STANDING = 0.01;
    private static final double TICK_SECONDS = 0.05;

    private MachineShaft(final Kind kind, final TorqueCurve curve, final double efficiency, final RelativeFace face,
                         final double load, final double least, final double rated) {
        if (!(efficiency > 0.0) || efficiency > 1.0) {
            throw new IllegalArgumentException("an efficiency is above nothing and no more than whole: " + efficiency);
        }
        if (!(load >= 0.0) || !(least >= 0.0) || kind == Kind.DRIVEN && !(rated >= least && rated > 0.0)) {
            throw new IllegalArgumentException("a driven machine asks no less than none, and its rated speed is above "
                    + "none and no less than its least");
        }
        this.kind = kind;
        this.curve = Objects.requireNonNull(curve, "curve");
        this.efficiency = efficiency;
        this.face = Objects.requireNonNull(face, "face");
        this.inertia = ROTOR;
        this.friction = BEARINGS;
        this.load = load;
        this.least = least;
        this.rated = rated;
    }

    /** An electric motor of {@code curve}, at {@code efficiency}, its shaft out of {@code face}. */
    public static MachineShaft motor(final TorqueCurve curve, final double efficiency, final RelativeFace face) {
        return new MachineShaft(Kind.MOTOR, curve, efficiency, face, 0.0, 0.0, 0.0);
    }

    /** A dynamo that takes up to {@code curve}, at {@code efficiency}, its shaft in from {@code face}. */
    public static MachineShaft dynamo(final TorqueCurve curve, final double efficiency, final RelativeFace face) {
        return new MachineShaft(Kind.DYNAMO, curve, efficiency, face, 0.0, 0.0, 0.0);
    }

    /**
     * A machine driven by its shaft in from {@code face}: it asks {@code newtonMetres} while it has work, stops below
     * {@code leastRpm}, and works at its whole speed from {@code ratedRpm}.
     */
    public static MachineShaft driven(final double newtonMetres, final double leastRpm, final double ratedRpm,
                                      final RelativeFace face) {
        return new MachineShaft(Kind.DRIVEN, TorqueCurve.NONE, 1.0, face, newtonMetres, leastRpm, ratedRpm);
    }

    /** Whether it is a motor. */
    public boolean motor() {
        return this.kind == Kind.MOTOR;
    }

    /** Whether it drives its machine, which works as fast as it turns. */
    public boolean drives() {
        return this.kind == Kind.DRIVEN;
    }

    /** Whether it drives its machine and turns slower than the least it works at. */
    public boolean tooSlow() {
        return this.kind == Kind.DRIVEN && Math.abs(this.rpm) < this.least;
    }

    /** How fast it lets its machine work, from none to whole: how it turns against its rated speed, when it drives. */
    public double share() {
        return this.kind == Kind.DRIVEN ? Math.min(1.0, Math.abs(this.rpm) / this.rated) : 1.0;
    }

    /** Tells a driving shaft whether its machine has work, and so asks its torque of the network. */
    public void loaded(final boolean working) {
        if (this.kind == Kind.DRIVEN) {
            this.asked = working ? this.load : 0.0;
        }
    }

    /** The face its shaft comes out of. */
    public RelativeFace face() {
        return this.face;
    }

    /**
     * One tick of its machine's work: a motor spends from the machine's energy what its torque gives at its speed, or
     * as much of it as there is; a dynamo makes energy of what it takes, while the machine has room. Says what the
     * machine is doing.
     */
    public MachineStatus run() {
        final MachineBlockEntity on = Objects.requireNonNull(this.machine, "a machine's shaft is declared on it");
        final double turning = Math.abs(this.rpm);
        if (this.kind == Kind.DRIVEN) {
            return tooSlow() ? MachineStatus.TOO_SLOW : MachineStatus.WORKING;
        }
        if (this.kind == Kind.MOTOR) {
            if (on.power().stored() <= 0L) {
                this.share = 0.0;
                return MachineStatus.NO_ENERGY;
            }
            // What its torque gives at its speed, over its efficiency; a stalled motor gives no power and spends none.
            final double joules = RotationMath.power(this.curve.torque(turning), turning) / this.efficiency
                    * TICK_SECONDS;
            final double owed = Math.max(0.0, joules - this.carried);
            final long spent = on.power().spend(on.power().energy().fromFe((long) Math.ceil(owed)));
            final double got = this.carried + on.power().energy().toFe(spent);
            this.share = joules <= 0.0 ? 1.0 : Math.min(1.0, got / joules);
            this.carried = Math.max(0.0, got - joules);
            return MachineStatus.WORKING;
        }
        final long room = on.power().capacity() - on.power().stored();
        if (room <= 0L) {
            this.asked = 0.0;
            return MachineStatus.OUTPUT_FULL;
        }
        // A generator brakes the harder the faster it turns, up to its curve at its rated speed: it never holds a
        // network still that is starting.
        this.asked = this.curve.freeRpm() <= 0.0 ? 0.0
                : Math.min(this.curve.stall(), RotationMath.torque(this.curve.power(), turning))
                * Math.min(1.0, turning / this.curve.freeRpm());
        if (turning < STANDING) {
            return MachineStatus.WAITING;
        }
        this.carried += RotationMath.power(this.asked, turning) * this.efficiency * TICK_SECONDS;
        final long made = on.power().generate(on.power().energy().fromFe((long) Math.floor(this.carried)));
        this.carried = Math.max(0.0, this.carried - on.power().energy().toFe(made));
        return MachineStatus.WORKING;
    }

    /** Its machine stopped: a motor gives no torque, and a dynamo or a driven machine asks none. */
    public void halt() {
        this.share = 0.0;
        this.asked = 0.0;
    }

    @Override
    public ShaftContact contact(final Direction side) {
        final MachineBlockEntity on = this.machine;
        if (on == null) {
            return ShaftContact.NONE;
        }
        final Direction facing = RelativeFace.facingOf(on.getBlockState()).orElse(Direction.NORTH);
        return RelativeFace.of(facing, side) == this.face ? ShaftContact.axle(side.getAxis()) : ShaftContact.NONE;
    }

    @Override
    public double inertia() {
        return this.inertia;
    }

    @Override
    public double friction() {
        return this.friction;
    }

    @Override
    public ITorque torque() {
        return this.kind == Kind.MOTOR ? own -> this.curve.torque(Math.abs(own)) * this.share
                : own -> -Math.signum(own) * this.asked;
    }

    @Override
    public double rpm() {
        return this.rpm;
    }

    @Override
    public void turning(final double rpm) {
        // A machine resting for want of a turn wakes as its shaft starts, or passes the least speed it works at.
        final boolean woke = crossed(this.rpm, rpm, STANDING) || this.kind == Kind.DRIVEN
                && crossed(this.rpm, rpm, this.least);
        this.rpm = rpm;
        if (woke && this.machine != null) {
            this.machine.wake();
        }
        // The players who see it are sent its speed, as the network tells it of every change worth telling.
        if (this.synced != null) {
            this.synced.run();
        }
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final CompoundTag own = new CompoundTag();
        own.putDouble(RPM, this.rpm);
        own.putDouble(CARRIED, this.carried);
        tag.put(KEY, own);
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final CompoundTag own = tag.getCompound(KEY);
        this.rpm = own.getDouble(RPM);
        this.carried = own.getDouble(CARRIED);
    }

    @Override
    public void writeClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        final CompoundTag own = new CompoundTag();
        own.putDouble(RPM, this.rpm);
        tag.put(KEY, own);
    }

    @Override
    public void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        this.rpm = tag.getCompound(KEY).getDouble(RPM);
    }

    /* The machine it is declared on, and how it tells its field it changed, from the machine's constructor. */
    void on(final MachineBlockEntity owner, final Runnable changed) {
        this.machine = owner;
        this.synced = changed;
    }

    /* Whether a speed went from below {@code mark} to at or above it. */
    private static boolean crossed(final double from, final double to, final double mark) {
        return Math.abs(from) < mark && Math.abs(to) >= mark;
    }

    /* What a machine's shaft does. */
    private enum Kind {
        MOTOR,
        DYNAMO,
        DRIVEN
    }
}
