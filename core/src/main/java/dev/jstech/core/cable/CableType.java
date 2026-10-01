/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.connect.Connection;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.GridMember;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A cable a mod lays in the Core's cable block: the line it carries and its generation, the grid it is a part of, the
 * lane it takes when it shares a block, how thick it is, how much it carries and how far a run of it reaches, and how
 * it looks. Registered in {@link CoreCables#REGISTRY}, each with the item that lays it, by a mod's own content.
 *
 * <p>A cable that never shares a block takes no lane: a block that holds it holds nothing else.
 */
public final class CableType {

    private final Connection line;
    private final GridKind grid;
    private final @Nullable Lane lane;
    private final int thickness;
    private final long throughput;
    private final int range;
    private final ResourceLocation jacket;
    private final ResourceLocation plug;
    private final Supplier<? extends Item> item;

    private CableType(final Builder builder, final Supplier<? extends Item> item) {
        this.line = Objects.requireNonNull(builder.line, "a cable carries a line");
        this.grid = Objects.requireNonNull(builder.grid, "a cable is a part of a grid");
        if (!builder.alone && builder.lane == null) {
            throw new IllegalStateException("a cable that shares blocks takes a lane: " + this.line.line());
        }
        this.lane = builder.alone ? null : builder.lane;
        this.thickness = builder.thickness;
        this.throughput = builder.throughput;
        this.range = builder.range;
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
        private @Nullable ResourceLocation jacket;
        private @Nullable ResourceLocation plug;

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
