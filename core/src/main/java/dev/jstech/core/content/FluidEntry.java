/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

/**
 * A declared fluid: its type, the still and the flowing fluid of the game, and, for a liquid, the block it pours as
 * and the bucket that holds it; and how it looks.
 */
public final class FluidEntry implements Supplier<Fluid> {

    private final ResourceLocation id;
    private final String english;
    private final Supplier<FluidType> type;
    private final Supplier<BaseFlowingFluid.Source> source;
    private final Supplier<BaseFlowingFluid.Flowing> flowing;
    private final @Nullable BlockEntry<LiquidBlock> block;
    private final @Nullable ItemEntry<BucketItem> bucket;
    private final Look look;
    private final boolean gas;
    private final boolean corrosive;

    FluidEntry(final ResourceLocation id, final String english, final Supplier<FluidType> type,
               final Supplier<BaseFlowingFluid.Source> source, final Supplier<BaseFlowingFluid.Flowing> flowing,
               final @Nullable BlockEntry<LiquidBlock> block, final @Nullable ItemEntry<BucketItem> bucket,
               final Look look, final boolean gas, final boolean corrosive) {
        this.id = id;
        this.english = english;
        this.type = type;
        this.source = source;
        this.flowing = flowing;
        this.block = block;
        this.bucket = bucket;
        this.look = Objects.requireNonNull(look, "look");
        this.gas = gas;
        this.corrosive = corrosive;
    }

    /** The still fluid. */
    @Override
    public Fluid get() {
        return this.source.get();
    }

    public ResourceLocation id() {
        return this.id;
    }

    /** What it is called in English. */
    public String english() {
        return this.english;
    }

    public FluidType type() {
        return this.type.get();
    }

    public BaseFlowingFluid.Source source() {
        return this.source.get();
    }

    public BaseFlowingFluid.Flowing flowing() {
        return this.flowing.get();
    }

    /** The block a liquid pours as; empty for a gas. */
    public Optional<BlockEntry<LiquidBlock>> block() {
        return Optional.ofNullable(this.block);
    }

    /** The bucket a liquid is held in; empty for a gas. */
    public Optional<ItemEntry<BucketItem>> bucket() {
        return Optional.ofNullable(this.bucket);
    }

    public Look look() {
        return this.look;
    }

    /** Whether it is a gas: lighter than air, never poured into the world nor held in a bucket. */
    public boolean gas() {
        return this.gas;
    }

    /** Whether it eats through what is not made to stand it. */
    public boolean corrosive() {
        return this.corrosive;
    }

    /** {@code millibuckets} of it. */
    public FluidStack stack(final int millibuckets) {
        return new FluidStack(get(), millibuckets);
    }

    /**
     * How a fluid looks: its still and flowing textures, tinted.
     *
     * @param still   the texture of a still pool of it
     * @param flowing the texture of it flowing
     * @param tint    the colour the textures are tinted with, as ARGB
     */
    public record Look(ResourceLocation still, ResourceLocation flowing, int tint) {
    }
}
