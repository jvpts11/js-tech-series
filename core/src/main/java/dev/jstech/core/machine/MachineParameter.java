/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.signal.Reading;
import dev.jstech.core.text.TextKey;
import java.util.Objects;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import org.jetbrains.annotations.Nullable;

/**
 * One value a machine says can be read, and perhaps set, from outside: by J's Industrial's Industrial Controller
 * Computer, by J's Computers' network and its tables, by a sensor's redstone. It is read over a span, as a reading
 * is, and a value set from outside is held to it.
 *
 * @param id    its name in data and to a program, unique on its machine: lowercase letters, digits and underscores
 * @param name  what a player calls it
 * @param unit  the unit its value is in; none for a count or a share
 * @param least the least it can be
 * @param most  the most it can be
 * @param read  what it is now
 * @param write how it is set; none for a value that is only read
 */
public record MachineParameter(String id, TextKey name, @Nullable TextKey unit, double least, double most,
                               DoubleSupplier read, @Nullable DoubleConsumer write) {

    public MachineParameter {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(read, "read");
        if (!id.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("a parameter's id is lowercase letters, digits and underscores: " + id);
        }
        if (!(most > least)) {
            throw new IllegalArgumentException("the parameter " + id + " runs from the less to the more");
        }
    }

    /** A value only read. */
    public static MachineParameter read(final String id, final TextKey name, @Nullable final TextKey unit,
                                        final double least, final double most, final DoubleSupplier read) {
        return new MachineParameter(id, name, unit, least, most, read, null);
    }

    /** A value read and set. */
    public static MachineParameter set(final String id, final TextKey name, @Nullable final TextKey unit,
                                       final double least, final double most, final DoubleSupplier read,
                                       final DoubleConsumer write) {
        return new MachineParameter(id, name, unit, least, most, read, Objects.requireNonNull(write, "write"));
    }

    /** Whether it can be set from outside. */
    public boolean settable() {
        return this.write != null;
    }

    /** What it is now. */
    public double value() {
        return this.read.getAsDouble();
    }

    /** Sets it to {@code to}, held to its span; whether it could be set. */
    public boolean setTo(final double to) {
        if (this.write == null || Double.isNaN(to)) {
            return false;
        }
        this.write.accept(Math.max(this.least, Math.min(this.most, to)));
        return true;
    }

    /** It as a reading, which a sensor's redstone speaks. */
    public Reading reading() {
        return new Reading(this.name, value(), this.least, this.most, this.unit);
    }
}
