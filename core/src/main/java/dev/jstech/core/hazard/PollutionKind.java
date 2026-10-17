/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import dev.jstech.core.text.TextKey;
import dev.jstech.core.world.field.ChunkField;
import dev.jstech.core.world.field.ChunkFields;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A kind of pollution: an amount over each chunk, given off by any mod's blocks, that spreads to the neighbouring
 * chunks, fades, and is taken up faster where plants grow; what it does to the bodies on a chunk is a hazard of its
 * own, built up from the chunk's amount, and what it does to the land, its effects. Its amounts are kept with each
 * dimension and sent to the players near enough to see them. Declared with {@link Pollution#declare}.
 *
 * <pre>{@code
 * public static final PollutionKind SPORES = Pollution.declare(PollutionKind.builder(id("spores"), Texts.SPORES,
 *         Texts.PER_CUBIC_METRE).spread(0.1).fades(0.01).plantsTakeUp(0.05).felt(MyHazards.SPORE_LUNG)
 *         .withersAt(500).enabledWhen(MySettings::spores).build());
 * Pollution.emit(level, pos, MyPollution.SPORES, 20);
 * }</pre>
 */
public final class PollutionKind {

    private final ResourceLocation id;
    private final TextKey name;
    private final TextKey unit;
    private final ChunkField field;
    private final double plantsTakeUp;
    private final double withersAt;
    private final @Nullable HazardKind felt;
    private final BooleanSupplier enabled;
    private final List<IPollutionEffect> effects = new CopyOnWriteArrayList<>();

    private PollutionKind(final Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.unit = builder.unit;
        this.plantsTakeUp = builder.plantsTakeUp;
        this.withersAt = builder.withersAt;
        this.felt = builder.felt;
        this.enabled = builder.enabled;
        this.field = ChunkFields.field(builder.id).spread(builder.spread).decay(builder.fades).every(builder.every)
                .synced().declare();
    }

    /** Starts declaring the kind {@code id}, called {@code name} and measured in {@code unit}. */
    public static Builder builder(final ResourceLocation id, final TextKey name, final TextKey unit) {
        return new Builder(id, name, unit);
    }

    public ResourceLocation id() {
        return this.id;
    }

    /** What it is called. */
    public TextKey name() {
        return this.name;
    }

    /** What it is measured in. */
    public TextKey unit() {
        return this.unit;
    }

    /** Its amounts over the chunks, which spread and fade. */
    public ChunkField field() {
        return this.field;
    }

    /** What share of a chunk's amount plants take up each sample, where every column of the chunk grows them. */
    public double plantsTakeUp() {
        return this.plantsTakeUp;
    }

    /** From how much of it the land withers; none when it never does. */
    public Optional<Double> withersAt() {
        return this.withersAt > 0 ? Optional.of(this.withersAt) : Optional.empty();
    }

    /** The hazard a body on a chunk builds up from the chunk's amount, if it harms bodies. */
    public Optional<HazardKind> felt() {
        return Optional.ofNullable(this.felt);
    }

    /** Whether the server has it on: off, nothing is given off and it does nothing. */
    public boolean enabled() {
        return this.enabled.getAsBoolean();
    }

    /** Adds what it does to the land it lies on. */
    public void addEffect(final IPollutionEffect effect) {
        this.effects.add(Objects.requireNonNull(effect, "effect"));
    }

    /** What it does to the land. */
    public List<IPollutionEffect> effects() {
        return List.copyOf(this.effects);
    }

    @Override
    public String toString() {
        return this.id.toString();
    }

    /** A kind of pollution being declared; it starts spreading a tenth a step and fading a hundredth, every 5 s. */
    public static final class Builder {

        private final ResourceLocation id;
        private final TextKey name;
        private final TextKey unit;
        private double spread = 0.1;
        private double fades = 0.01;
        private int every = 100;
        private double plantsTakeUp;
        private double withersAt;
        private @Nullable HazardKind felt;
        private BooleanSupplier enabled = () -> true;

        private Builder(final ResourceLocation id, final TextKey name, final TextKey unit) {
            this.id = Objects.requireNonNull(id, "id");
            this.name = Objects.requireNonNull(name, "name");
            this.unit = Objects.requireNonNull(unit, "unit");
        }

        /** The share of each chunk's amount handed to its neighbours each step, 0 to 1. */
        public Builder spread(final double share) {
            this.spread = share;
            return this;
        }

        /** The share of every chunk's amount that fades each step by itself, 0 to 1. */
        public Builder fades(final double share) {
            this.fades = share;
            return this;
        }

        /** It moves on a step every {@code ticks}. */
        public Builder every(final int ticks) {
            this.every = ticks;
            return this;
        }

        /** The share of a chunk's amount plants take up each sample, where every column grows them, 0 to 1. */
        public Builder plantsTakeUp(final double share) {
            this.plantsTakeUp = Math.clamp(share, 0, 1);
            return this;
        }

        /** From {@code amount} of it the land withers, through the Core's withering of the land. */
        public Builder withersAt(final double amount) {
            this.withersAt = amount;
            return this;
        }

        /** A body on a chunk builds up {@code hazard} from the chunk's amount each second, breathing or touching it. */
        public Builder felt(final HazardKind hazard) {
            this.felt = hazard;
            return this;
        }

        /** Whether the server has it on, as its setting says. */
        public Builder enabledWhen(final BooleanSupplier setting) {
            this.enabled = setting;
            return this;
        }

        public PollutionKind build() {
            return new PollutionKind(this);
        }
    }
}
