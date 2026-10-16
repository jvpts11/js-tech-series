/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.conveyor;

import java.util.Objects;

/**
 * A kind of conveyor a mod makes: how fast it moves, how many items a block of it holds, what makes it move, what it
 * does with what reaches its end with nowhere to go, whether it carries what stands on it, and how high its belt is.
 * The numbers are the mod's own: the kit runs any.
 *
 * <pre>{@code
 * public static final ConveyorType BELT = ConveyorType.moving(2.0).perBlock(4)
 *         .drive(ConveyorDrive.mechanical(60, 2)).carriesEntities().build();
 * }</pre>
 */
public final class ConveyorType {

    private final double speed;
    private final int perBlock;
    private final ConveyorDrive drive;
    private final ConveyorEnd end;
    private final boolean carriesEntities;
    private final int height;

    /** The most items a block of any conveyor holds. */
    public static final int MOST_A_BLOCK = 8;
    /** The fastest any conveyor moves, in blocks a second: a block a tick. */
    public static final double FASTEST = 20.0;
    private static final int PIXELS = 16;

    private ConveyorType(final Builder builder) {
        this.speed = builder.speed;
        this.perBlock = builder.perBlock;
        this.drive = builder.drive;
        this.end = builder.end;
        this.carriesEntities = builder.carriesEntities;
        this.height = builder.height;
    }

    /** Starts a kind that moves {@code blocksPerSecond} at its full speed. */
    public static Builder moving(final double blocksPerSecond) {
        return new Builder(blocksPerSecond);
    }

    /** How fast it moves at its full speed, in blocks a second. */
    public double speed() {
        return this.speed;
    }

    /** How many items a block of it holds, one behind another. */
    public int perBlock() {
        return this.perBlock;
    }

    /** The gap its items keep, in blocks. */
    public double gap() {
        return 1.0 / this.perBlock;
    }

    /** What makes it move. */
    public ConveyorDrive drive() {
        return this.drive;
    }

    /** What it does with an item at its end with nowhere to go. */
    public ConveyorEnd end() {
        return this.end;
    }

    /** Whether it carries the players, mobs and items that stand on it. */
    public boolean carriesEntities() {
        return this.carriesEntities;
    }

    /** How high its belt is, in pixels: where its items lie. */
    public int height() {
        return this.height;
    }

    /** Declares a kind of conveyor. */
    public static final class Builder {

        private final double speed;
        private int perBlock = 4;
        private ConveyorDrive drive = ConveyorDrive.free();
        private ConveyorEnd end = ConveyorEnd.HOLD;
        private boolean carriesEntities;
        private int height = 4;

        private Builder(final double blocksPerSecond) {
            if (!(blocksPerSecond > 0.0) || blocksPerSecond > FASTEST) {
                throw new IllegalArgumentException("a conveyor moves faster than nothing and no faster than a block a "
                        + "tick, got " + blocksPerSecond + " blocks a second");
            }
            this.speed = blocksPerSecond;
        }

        /** How many items a block holds, one behind another, from 1 to 8; four unless said. */
        public Builder perBlock(final int items) {
            if (items < 1 || items > MOST_A_BLOCK) {
                throw new IllegalArgumentException("a block of conveyor holds from 1 to 8 items, got " + items);
            }
            this.perBlock = items;
            return this;
        }

        /** What makes it move; it moves by itself unless said. */
        public Builder drive(final ConveyorDrive by) {
            this.drive = Objects.requireNonNull(by, "drive");
            return this;
        }

        /** What it does with an item at its end with nowhere to go; it holds it unless said. */
        public Builder end(final ConveyorEnd atEnd) {
            this.end = Objects.requireNonNull(atEnd, "end");
            return this;
        }

        /** It carries the players, mobs and items that stand on it. */
        public Builder carriesEntities() {
            this.carriesEntities = true;
            return this;
        }

        /** How high its belt is, in pixels, from 1 to 16; four unless said. */
        public Builder height(final int pixels) {
            if (pixels < 1 || pixels > PIXELS) {
                throw new IllegalArgumentException("a belt is from 1 to 16 pixels high, got " + pixels);
            }
            this.height = pixels;
            return this;
        }

        public ConveyorType build() {
            return new ConveyorType(this);
        }
    }
}
