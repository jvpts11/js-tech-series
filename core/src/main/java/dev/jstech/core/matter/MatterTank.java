/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.matter;

import dev.jstech.core.JsCore;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MatterState;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import org.jetbrains.annotations.Nullable;

/**
 * One tank of one state of matter, holding one material at a time: a pressurised gas tank, a pressure vessel of
 * supercritical fluid, a tank of slurry. It holds its volume times its pressure, in millibuckets at the pressure of the
 * open air, so a tank of a thousand millibuckets at two hundred times that pressure holds two hundred thousand. A
 * block keeps one in its block entity, offers it by the capability of its state, and saves it with itself.
 */
public class MatterTank implements IMatterHandler {

    private final MatterState state;
    private final long volume;
    private final long pressure;
    private final long capacity;
    private final Runnable onChange;
    private @Nullable MatterStack held;

    private static final String HELD = "held";

    /**
     * A tank of {@code state}, of {@code volume} millibuckets at {@code pressure} times the pressure of the open air,
     * which tells {@code onChange} whenever what it holds changes (its block entity marking itself to be saved).
     */
    public MatterTank(final MatterState state, final long volume, final long pressure, final Runnable onChange) {
        this.state = Objects.requireNonNull(state, "state");
        if (volume <= 0 || pressure <= 0) {
            throw new IllegalArgumentException("a tank has a volume and a pressure, not " + volume + " at "
                    + pressure);
        }
        this.volume = volume;
        this.pressure = pressure;
        this.capacity = volume > Long.MAX_VALUE / pressure ? Long.MAX_VALUE : volume * pressure;
        this.onChange = Objects.requireNonNull(onChange, "onChange");
    }

    /** Its volume, in millibuckets. */
    public long volume() {
        return this.volume;
    }

    /** The pressure it holds, in times the pressure of the open air. */
    public long pressure() {
        return this.pressure;
    }

    /** What it holds, or none. */
    public Optional<MatterStack> held() {
        return Optional.ofNullable(this.held);
    }

    @Override
    public MatterState state() {
        return this.state;
    }

    @Override
    public int tanks() {
        return 1;
    }

    @Override
    public Optional<MatterStack> stack(final int tank) {
        return tank == 0 ? held() : Optional.empty();
    }

    @Override
    public long capacity(final int tank) {
        return tank == 0 ? capacity() : 0L;
    }

    /** How much it holds at most: its volume times its pressure. */
    public long capacity() {
        return this.capacity;
    }

    @Override
    public long insert(final MatterStack stack, final boolean simulate) {
        if (!takes() || !stack.material().takes(this.state)
                || this.held != null && this.held.material() != stack.material()) {
            return 0L;
        }
        final long room = capacity() - (this.held == null ? 0L : this.held.amount());
        final long taken = Math.min(room, stack.amount());
        if (taken > 0 && !simulate) {
            this.held = stack.withAmount((this.held == null ? 0L : this.held.amount()) + taken);
            this.onChange.run();
        }
        return Math.max(0L, taken);
    }

    @Override
    public long extract(final Material material, final long amount, final boolean simulate) {
        if (!gives() || this.held == null || this.held.material() != material || amount <= 0) {
            return 0L;
        }
        final long given = Math.min(amount, this.held.amount());
        if (!simulate) {
            this.held = given == this.held.amount() ? null : this.held.withAmount(this.held.amount() - given);
            this.onChange.run();
        }
        return given;
    }

    @Override
    public Optional<MatterStack> extractAny(final long amount, final boolean simulate) {
        if (this.held == null) {
            return Optional.empty();
        }
        final Material material = this.held.material();
        final long given = extract(material, amount, simulate);
        return given > 0 ? Optional.of(new MatterStack(material, given)) : Optional.empty();
    }

    /**
     * Sets what it holds, none for empty, as a tank placed from the item that kept its matter does, whatever it held
     * before gone; more than it holds is cut to what it holds. A material its state does not take is refused, and it
     * says whether it was set.
     */
    public boolean set(final @Nullable MatterStack stack) {
        if (stack != null && !stack.material().takes(this.state)) {
            return false;
        }
        this.held = stack == null || stack.amount() <= 0 ? null
                : stack.withAmount(Math.min(stack.amount(), capacity()));
        this.onChange.run();
        return true;
    }

    /** Empties it, and says what it held: what a broken tank lets out. */
    public Optional<MatterStack> release() {
        final Optional<MatterStack> out = held();
        if (this.held != null) {
            this.held = null;
            this.onChange.run();
        }
        return out;
    }

    /** Writes what it holds into {@code tag}. */
    public void save(final CompoundTag tag) {
        if (this.held != null) {
            MatterStack.CODEC.encodeStart(NbtOps.INSTANCE, this.held).result().ifPresent(saved -> tag.put(HELD,
                    saved));
        }
    }

    /** Reads what it holds back from {@code tag}; a material no longer in the game is let go, with a line in the log. */
    public void load(final CompoundTag tag) {
        this.held = null;
        if (tag.contains(HELD)) {
            try {
                this.held = MatterStack.CODEC.parse(NbtOps.INSTANCE, tag.get(HELD)).result().orElse(null);
            } catch (final IllegalArgumentException gone) {
                JsCore.LOGGER.warn("A tank held a material no longer in the game and is empty now: {}",
                        gone.getMessage());
            }
        }
    }

    /** Whether it takes anything now. */
    protected boolean takes() {
        return true;
    }

    /** Whether it gives anything now. */
    protected boolean gives() {
        return true;
    }

    /** Lets what it holds go without anyone taking it: what a containment without energy does to plasma. */
    protected void lose() {
        if (this.held != null) {
            this.held = null;
            this.onChange.run();
        }
    }
}
