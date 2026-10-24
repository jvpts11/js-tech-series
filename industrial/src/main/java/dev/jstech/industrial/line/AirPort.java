/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.line;

import dev.jstech.core.blockentity.BlockEntityFields;
import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.matter.MatterTank;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * An air port (industrial/docs/design/machines.md): the small receiver of compressed air a machine or a station has,
 * which the Compressed Air Line fills with air alone, at the working pressure; it holds some uses of air, and its
 * owner spends one at a time, a machine each tick it works, a station each capsule it sends. Its gas face takes air and
 * gives none back; beside the owner's own tanks of gas, it is one more ({@link #with}).
 */
public final class AirPort {

    private final long use;
    private final AirTank tank;

    /** How many ticks of its air a working machine's port holds. */
    public static final int MACHINE_HOLDS_TICKS = 100;

    /** A port spending {@code use} millibuckets of air a time and holding {@code holds}, saved in {@code fields}. */
    public AirPort(final BlockEntityFields fields, final long use, final long holds, final Runnable onChange) {
        if (use <= 0 || holds < use) {
            throw new IllegalArgumentException("an air port spends some air and holds at least one use of it, not "
                    + use + " of " + holds);
        }
        this.use = use;
        this.tank = new AirTank(Math.ceilDiv(holds, CompressedAir.WORKING_PRESSURE), onChange);
        fields.part("Air", IFieldPart.of((tag, registries) -> this.tank.save(tag),
                (tag, registries) -> this.tank.load(tag))).save();
    }

    /** A working machine's port: {@code perTick} millibuckets a tick, holding {@link #MACHINE_HOLDS_TICKS} of them. */
    public static AirPort machine(final BlockEntityFields fields, final long perTick, final Runnable onChange) {
        return new AirPort(fields, perTick, Math.multiplyExact(perTick, MACHINE_HOLDS_TICKS), onChange);
    }

    /** What it spends a time, in millibuckets of air. */
    public long use() {
        return this.use;
    }

    /** Its receiver. */
    public MatterTank tank() {
        return this.tank;
    }

    /** What air it holds, in millibuckets. */
    public long held() {
        return this.tank.held().map(MatterStack::amount).orElse(0L);
    }

    /** Whether it holds a use of air. */
    public boolean ready() {
        return held() >= this.use;
    }

    /** Spends a use of air, if it holds one; whether it did. */
    public boolean spend() {
        if (!ready()) {
            return false;
        }
        this.tank.extract(CompressedAir.air(), this.use, false);
        return true;
    }

    /** Its owner's gas face: {@code others}, the owner's own tanks of gas, with the port beside them. */
    public IMatterHandler with(final @Nullable IMatterHandler others) {
        return new Face(others);
    }

    /* The receiver, which holds air alone. */
    private static final class AirTank extends MatterTank {

        AirTank(final long volume, final Runnable onChange) {
            super(MatterState.GAS, Math.max(1L, volume), CompressedAir.WORKING_PRESSURE, onChange);
        }

        @Override
        public long insert(final MatterStack stack, final boolean simulate) {
            return stack.material() == CompressedAir.air() ? super.insert(stack, simulate) : 0L;
        }

        @Override
        public long push(final MatterStack stack, final long pressure, final boolean simulate) {
            return stack.material() == CompressedAir.air() ? super.push(stack, pressure, simulate) : 0L;
        }
    }

    /* The owner's tanks of gas and the port, the port last: air fills it, the owner's tanks give what they give. */
    private final class Face implements IMatterHandler {

        private final @Nullable IMatterHandler others;

        Face(final @Nullable IMatterHandler others) {
            this.others = others;
        }

        @Override
        public MatterState state() {
            return MatterState.GAS;
        }

        @Override
        public int tanks() {
            return own() + 1;
        }

        @Override
        public Optional<MatterStack> stack(final int tank) {
            return tank < own() ? this.others.stack(tank) : AirPort.this.tank.held();
        }

        @Override
        public long capacity(final int tank) {
            return tank < own() ? this.others.capacity(tank) : AirPort.this.tank.capacity();
        }

        @Override
        public double pressureNow(final int tank) {
            return tank < own() ? this.others.pressureNow(tank) : AirPort.this.tank.pressureNow();
        }

        @Override
        public long insert(final MatterStack stack, final boolean simulate) {
            if (stack.material() == CompressedAir.air()) {
                final long into = AirPort.this.tank.insert(stack, simulate);
                if (into > 0 || this.others == null) {
                    return into;
                }
            }
            return this.others == null ? 0L : this.others.insert(stack, simulate);
        }

        @Override
        public long extract(final Material material, final long amount, final boolean simulate) {
            return this.others == null ? 0L : this.others.extract(material, amount, simulate);
        }

        @Override
        public Optional<MatterStack> extractAny(final long amount, final boolean simulate) {
            return this.others == null ? Optional.empty() : this.others.extractAny(amount, simulate);
        }

        private int own() {
            return this.others == null ? 0 : this.others.tanks();
        }
    }
}
