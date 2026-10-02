/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.connect.Connection;
import dev.jstech.core.energy.EnergyLoss;
import dev.jstech.core.fluid.PipeLimits;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.GridMember;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * A cable a mod lays in the Core's cable block: the line it carries and its generation, the grid it is a part of, the
 * lane it takes when it shares a block, how thick it is, how much it carries, how far a run of it reaches and what it
 * loses of what crosses it, and how it looks. Registered in {@link CoreCables#REGISTRY}, each with the item that lays
 * it, by a mod's own content.
 *
 * <p>A pipe, a cable of the fluid grid, also says what it is made for: the temperatures it stands and the marks it
 * takes. How much a pipe carries in a tick is the pressure it holds; a gas needs a pipe that takes gases.
 *
 * <p>A cable that never shares a block takes no lane: a block that holds it holds nothing else.
 *
 * <p>Some cables keep a shape: one that runs only straight joins along a single axis in a block, so it never turns a
 * corner; one that joins at most so many faces in a block never branches past that, so a cable that joins two runs
 * from one end to the other with nothing branching off it.
 */
public final class CableType {

    private final Connection line;
    private final GridKind grid;
    private final @Nullable Lane lane;
    private final int thickness;
    private final long throughput;
    private final int range;
    private final int loss;
    private final PipeLimits pipe;
    private final ResourceLocation jacket;
    private final ResourceLocation plug;
    private final Supplier<? extends Item> item;
    private final boolean straight;
    private final int mostJoins;

    private CableType(final Builder builder, final Supplier<? extends Item> item) {
        this.line = Objects.requireNonNull(builder.line, "a cable carries a line");
        this.grid = Objects.requireNonNull(builder.grid, "a cable is a part of a grid");
        if (!builder.alone && builder.lane == null) {
            throw new IllegalStateException("a cable that shares blocks takes a lane: " + this.line.line());
        }
        this.lane = builder.alone ? null : builder.lane;
        this.straight = builder.straight;
        this.mostJoins = builder.mostJoins;
        this.thickness = builder.thickness;
        this.throughput = builder.throughput;
        this.range = builder.range;
        this.loss = builder.loss;
        this.pipe = new PipeLimits(builder.coldest, builder.hottest, builder.takes);
        this.jacket = Objects.requireNonNull(builder.jacket, "a cable has a jacket");
        this.plug = Objects.requireNonNull(builder.plug, "a cable has a plug");
        this.item = Objects.requireNonNull(item, "item");
    }

    /** Starts declaring a cable of {@code line}. */
    public static Builder builder(final Connection line) {
        return new Builder(line);
    }

    /** The line it carries, in its generation. */
    public Connection line() {
        return this.line;
    }

    /** The grid it is a part of. */
    public GridKind grid() {
        return this.grid;
    }

    /** The lane it takes in a block it shares, or empty for a cable that never shares a block. */
    public Optional<Lane> lane() {
        return Optional.ofNullable(this.lane);
    }

    /** Whether a block that holds it holds nothing else. */
    public boolean alone() {
        return this.lane == null;
    }

    /** How thick it is, in pixels. */
    public int thickness() {
        return this.thickness;
    }

    /** How much it carries, in its grid's unit. */
    public long throughput() {
        return this.throughput;
    }

    /** How many cables a run of it reaches before it has to be renewed; 0 for no limit. */
    public int range() {
        return this.range;
    }

    /** The thousandths of what crosses one block of it that it loses; 0 for a cable that loses nothing. */
    public int loss() {
        return this.loss;
    }

    /** What it is made for as a pipe: the temperatures it stands and the marks it takes. */
    public PipeLimits pipe() {
        return this.pipe;
    }

    /** Whether it runs only straight: in a block it joins along one axis, so it never turns a corner. */
    public boolean runsStraight() {
        return this.straight;
    }

    /** The most faces it joins in a block; 0 for no limit. */
    public int mostJoins() {
        return this.mostJoins;
    }

    /**
     * Its jacket's texture, 32 pixels square: the side with the length along u in the top left (16 by its thickness),
     * the same side with the length along v beside it, and the cut end below.
     */
    public ResourceLocation jacket() {
        return this.jacket;
    }

    /** The standalone block model of the plug it ends in at a device, against the north face. */
    public ResourceLocation plug() {
        return this.plug;
    }

    /** The item that lays it. */
    public Item item() {
        return this.item.get();
    }

    /** One of the item that lays it. */
    public ItemStack stack() {
        return new ItemStack(item());
    }

    /** What a wire of it, in {@code colour} or none, stands for in its grid. */
    public GridMember member(final Optional<DyeColor> colour) {
        final GridMember plain = GridMember.cable(this.line.line().toString(), this.line.generation(),
                this.throughput, this.range);
        return colour.map(dye -> plain.coloured(dye.getId())).orElse(plain);
    }

    /** The id this cable is registered under, or null when it is not registered. */
    public @Nullable ResourceLocation id() {
        return CoreCables.REGISTRY.getKey(this);
    }

    /** Declares a cable one property at a time. */
    public static final class Builder {

        private final Connection line;
        private GridKind grid = GridKind.DATA;
        private @Nullable Lane lane;
        private boolean alone;
        private int thickness = STANDARD_THICKNESS;
        private long throughput;
        private int range;
        private int loss;
        private int coldest = PipeLimits.PLAIN.coldest();
        private int hottest = PipeLimits.PLAIN.hottest();
        private final Set<TagKey<Fluid>> takes = new HashSet<>();
        private @Nullable ResourceLocation jacket;
        private @Nullable ResourceLocation plug;
        private boolean straight;
        private int mostJoins;

        /** How thick a cable is unless it says otherwise, in pixels. */
        private static final int STANDARD_THICKNESS = 4;
        /* No thicker than half a block, so a junction box still holds it. */
        private static final int MOST_PIXELS = 8;

        private Builder(final Connection line) {
            this.line = Objects.requireNonNull(line, "line");
        }

        /** The grid it is a part of; data unless said. */
        public Builder grid(final GridKind kind) {
            this.grid = Objects.requireNonNull(kind, "kind");
            return this;
        }

        /** The lane it takes in a block it shares. */
        public Builder lane(final Lane taken) {
            this.lane = Objects.requireNonNull(taken, "taken");
            this.alone = false;
            return this;
        }

        /** It never shares a block: a block that holds it holds nothing else. */
        public Builder alone() {
            this.alone = true;
            this.lane = null;
            return this;
        }

        /** How thick it is, in pixels; four unless said. */
        public Builder thickness(final int pixels) {
            if (pixels <= 0 || pixels > MOST_PIXELS) {
                throw new IllegalArgumentException("a cable is between 1 and 8 pixels thick, not " + pixels);
            }
            this.thickness = pixels;
            return this;
        }

        /** How much it carries and how many cables a run of it reaches; a range of 0 reaches without limit. */
        public Builder carries(final long amount, final int reach) {
            if (amount < 0 || reach < 0) {
                throw new IllegalArgumentException("a cable carries and reaches no less than nothing");
            }
            this.throughput = amount;
            this.range = reach;
            return this;
        }

        /** The thousandths of what crosses one block of it that it loses; nothing unless said. */
        public Builder loses(final int thousandths) {
            if (thousandths < 0 || thousandths > EnergyLoss.WHOLE) {
                throw new IllegalArgumentException("a cable loses between 0 and " + EnergyLoss.WHOLE
                        + " thousandths, not " + thousandths);
            }
            this.loss = thousandths;
            return this;
        }

        /** As a pipe, the coldest and the hottest fluid it stands, in kelvin; any temperature unless said. */
        public Builder withstands(final int coldest, final int hottest) {
            if (coldest < 0 || hottest < coldest) {
                throw new IllegalArgumentException("a pipe stands from " + coldest + " K to " + hottest + " K");
            }
            this.coldest = coldest;
            this.hottest = hottest;
            return this;
        }

        /** As a pipe, it is made for fluids marked {@code mark}: a gas, a corrosive fluid. */
        public Builder takes(final TagKey<Fluid> mark) {
            this.takes.add(Objects.requireNonNull(mark, "mark"));
            return this;
        }

        /** It runs only straight: in a block it joins along one axis, and something else has to turn it. */
        public Builder runsStraight() {
            this.straight = true;
            return this;
        }

        /** It joins at most {@code faces} faces in a block, so it branches no further; two runs it end to end. */
        public Builder joinsAtMost(final int faces) {
            if (faces < 1 || faces > Direction.values().length) {
                throw new IllegalArgumentException("a cable joins between 1 and 6 faces, not " + faces);
            }
            this.mostJoins = faces;
            return this;
        }

        /** Its jacket's texture. */
        public Builder jacket(final ResourceLocation texture) {
            this.jacket = Objects.requireNonNull(texture, "texture");
            return this;
        }

        /** The standalone model of the plug it ends in at a device. */
        public Builder plug(final ResourceLocation model) {
            this.plug = Objects.requireNonNull(model, "model");
            return this;
        }

        /** The cable, laid by {@code item}. */
        public CableType build(final Supplier<? extends Item> item) {
            return new CableType(this, item);
        }
    }
}
