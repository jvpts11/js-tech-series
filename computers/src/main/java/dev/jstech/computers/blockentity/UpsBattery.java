/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.power.UpsRating;
import dev.jstech.computers.power.UpsReading;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * The battery of a UPS (computers/docs/design/power.md), the MF PowerGuard's or a rack's: while the line brings the
 * load its tick, the line carries the load and what it brings beyond it charges the battery; when it falls short, the
 * battery makes up the tick and the load runs on; above what the UPS carries it is in bypass and protects nothing. At a
 * tenth of its charge the load is shut down cleanly.
 *
 * <p>The charge is the block's and is saved with it; where the power comes from is worked out every tick. How it last
 * stood is kept too: on the server as its players were last told, on the client as they were.
 */
final class UpsBattery {

    private long charge;
    private UpsReading.Mode mode = UpsReading.Mode.NONE;
    private UpsReading shown = UpsReading.NONE;

    private static final String CHARGE = "UpsCharge";
    private static final String SHOWN_MODE = "UpsMode";
    private static final String SHOWN_PERMILLE = "UpsPermille";
    private static final String SHOWN_MINUTES = "UpsMinutes";

    /**
     * Before the load draws its tick of {@code watts} from {@code line}: works out where the power comes from, and
     * makes up from the battery a line that falls short of the tick.
     */
    UpsReading.Mode carry(@Nullable final UpsRating rating, final IUpsLine line, final double watts) {
        if (rating == null) {
            this.charge = 0L;
            this.mode = UpsReading.Mode.NONE;
            return this.mode;
        }
        this.charge = Math.min(this.charge, rating.batteryJoules());
        if (!ComputersServerConfig.drawEnergy()) {
            this.mode = UpsReading.Mode.ON_LINE;
        } else if (rating.bypassedBy(watts)) {
            this.mode = UpsReading.Mode.BYPASS;
        } else if (watts <= 0.0) {
            // With nothing to carry: on the line while it is there, else on the battery while any is left.
            this.mode = line.fed() ? UpsReading.Mode.ON_LINE
                    : this.charge > 0L ? UpsReading.Mode.ON_BATTERY : UpsReading.Mode.NONE;
        } else if (line.fed() && line.canRun(watts)) {
            this.mode = UpsReading.Mode.ON_LINE;
        } else {
            /*
             * The line is gone, or brings too little. Only what the tick lacks comes from the battery, the store run
             * down first: more would be left in the store to read as the line's.
             */
            final long lacks = Math.max(0L, UpsRating.tickOf(watts) - line.stored());
            this.charge -= line.fromBattery(Math.min(this.charge, lacks));
            this.mode = UpsReading.Mode.ON_BATTERY;
        }
        return this.mode;
    }

    /** After the load drew its tick of {@code watts}: what the line brought beyond it charges the battery. */
    void charge(@Nullable final UpsRating rating, final IUpsLine line, final double watts) {
        if (rating == null || this.mode == UpsReading.Mode.ON_BATTERY) {
            return;
        }
        this.charge += line.spareForCharging(Math.min(rating.chargePerTick(), rating.batteryJoules() - this.charge),
                watts);
    }

    /**
     * Whether the battery carries the load and is down to where the load is shut down cleanly, as real UPS software
     * does: a machine started again on so low a battery is shut down again.
     */
    boolean low(@Nullable final UpsRating rating) {
        return rating != null && this.mode == UpsReading.Mode.ON_BATTERY && rating.shutsDownAt(this.charge);
    }

    /** How the UPS stands for a load of {@code watts}: none without a UPS. */
    UpsReading reading(@Nullable final UpsRating rating, final double watts) {
        return rating == null ? UpsReading.NONE
                : new UpsReading(this.mode, rating.permille(this.charge), UpsRating.minutesLeft(this.charge, watts));
    }

    /**
     * Whether {@code now} differs from how its players were last told it stood, by where the power comes from, a
     * percent of charge or a minute of what is left; it is then what they are told.
     */
    boolean shownChanged(final UpsReading now) {
        if (now.mode() == this.shown.mode() && now.percent() == this.shown.percent()
                && now.minutesLeft() == this.shown.minutesLeft()) {
            return false;
        }
        this.shown = now;
        return true;
    }

    /** How the UPS last stood as its players were told; on the client, as they were. */
    UpsReading shown() {
        return this.shown;
    }

    /** Where the power comes from, as last worked out. */
    UpsReading.Mode mode() {
        return this.mode;
    }

    /** What the battery holds, in joules. */
    long charge() {
        return this.charge;
    }

    /** Sets what the battery holds, for a test to start from. */
    void setCharge(final long joules) {
        this.charge = Math.max(0L, joules);
    }

    void save(final CompoundTag tag) {
        if (this.charge > 0L) {
            tag.putLong(CHARGE, this.charge);
        }
    }

    void load(final CompoundTag tag) {
        this.charge = Math.max(0L, tag.getLong(CHARGE));
    }

    /** Writes how the UPS stands, {@code reading}, for its players. */
    static void writeShown(final CompoundTag tag, final UpsReading reading) {
        tag.putByte(SHOWN_MODE, (byte) reading.mode().id());
        tag.putShort(SHOWN_PERMILLE, (short) reading.permille());
        tag.putLong(SHOWN_MINUTES, reading.minutesLeft());
    }

    /** Reads how the UPS stands as the server wrote it, on the client. */
    void readShown(final CompoundTag tag) {
        this.shown = new UpsReading(UpsReading.Mode.byId(tag.getByte(SHOWN_MODE)), tag.getShort(SHOWN_PERMILLE),
                tag.getLong(SHOWN_MINUTES));
    }
}
