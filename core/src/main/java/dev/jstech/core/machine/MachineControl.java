/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.blockentity.BlockEntityFields;
import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.signal.Reading;
import java.util.Objects;

/**
 * How a machine is told to run: its ON/OFF switch, which turns it off by hand whatever the redstone says; how it
 * answers redstone ({@link RedstoneMode}); what a comparator reads of it ({@link ComparatorReads}); and the signal
 * coming in, kept for its screen's row of lamps. A pulse in {@link RedstoneMode#PULSE} lets one recipe run.
 */
public final class MachineControl {

    private final BoolField on;
    private final ValueField<RedstoneMode> mode;
    private final ValueField<ComparatorReads> reads;
    private final IntField signal;
    private final BoolField armed;

    private MachineControl(final BlockEntityFields fields) {
        this.on = fields.flag("On", true).save().toClient();
        this.mode = fields.value("RedstoneMode", StableCodecs.byName(RedstoneMode.class), RedstoneMode.IGNORE)
                .save().toClient();
        this.reads = fields.value("ComparatorReads", StableCodecs.byName(ComparatorReads.class),
                ComparatorReads.PROGRESS).save().toClient();
        this.signal = fields.integer("SignalIn", 0).save().toClient();
        this.armed = fields.flag("Armed", false).save();
    }

    /** The control of a machine, kept in {@code fields}, saved with them and sent to the players who see it. */
    public static MachineControl in(final BlockEntityFields fields) {
        return new MachineControl(fields);
    }

    /** Whether its switch is on. */
    public boolean on() {
        return this.on.get();
    }

    /** Turns its switch on or off. */
    public void setOn(final boolean to) {
        this.on.set(to);
    }

    /** How it answers redstone. */
    public RedstoneMode mode() {
        return Objects.requireNonNull(this.mode.get());
    }

    /** Sets how it answers redstone; a pulse given before does not carry over. */
    public void setMode(final RedstoneMode to) {
        this.mode.set(Objects.requireNonNull(to, "to"));
        this.armed.set(false);
    }

    /** What a comparator reads of it. */
    public ComparatorReads reads() {
        return Objects.requireNonNull(this.reads.get());
    }

    /** Sets what a comparator reads of it. */
    public void setReads(final ComparatorReads to) {
        this.reads.set(Objects.requireNonNull(to, "to"));
    }

    /** The redstone coming in, 0 to 15. */
    public int signal() {
        return this.signal.get();
    }

    /** The redstone coming in is now {@code strength}; a rising one arms a machine that runs one recipe a pulse. */
    public void hear(final int strength) {
        final int held = Math.max(0, Math.min(Reading.FULL, strength));
        if (held > 0 && this.signal.get() == 0 && mode() == RedstoneMode.PULSE) {
            this.armed.set(true);
        }
        this.signal.set(held);
    }

    /** Whether redstone lets it run now: by its mode, and in pulses only once armed. */
    public boolean redstoneAllows() {
        return switch (mode()) {
            case IGNORE -> true;
            case HIGH -> this.signal.get() > 0;
            case LOW -> this.signal.get() == 0;
            case PULSE -> this.armed.get();
        };
    }

    /** A recipe was finished: a machine run by pulses waits for the next. */
    public void finishedOne() {
        if (mode() == RedstoneMode.PULSE) {
            this.armed.set(false);
        }
    }
}
