/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A kind of hazard: something that builds up in a body from where it is, and harms by how much has built up. It says
 * how it builds up (its sources), what it does by bands, how it goes away (a body recovers away from the sources),
 * what protects from it (a kind of harm worn things stop a share of), what it is measured in, what treats it, how it
 * kills (its damage type, whose death message is its own), whether creatures suffer it, and whether the server has it
 * on at all.
 *
 * <pre>{@code
 * public static final HazardKind SPORES = Hazards.declare(HazardKind.builder(id("spores"), Texts.SPORES, Texts.SPORE)
 *         .band(10, Texts.ITCHY, HazardEffects.effect(MobEffects.WEAKNESS, 0))
 *         .band(50, Texts.CHOKING, HazardEffects.damage(1))
 *         .recovers(0.5).protectedBy(DamageKinds.INHALED).deadlyAs(MyDamageTypes.SPORES).affectsMobs().build());
 * SPORES.addSource((body, level) -> sporesNear(level, body.blockPosition()));
 * }</pre>
 */
public final class HazardKind {

    private final ResourceLocation id;
    private final TextKey name;
    private final TextKey unit;
    private final List<HazardBand> bands;
    private final double recovery;
    private final double recoveryShare;
    private final @Nullable DamageKind protection;
    private final @Nullable TextKey treatment;
    private final BooleanSupplier enabled;
    private final @Nullable ResourceKey<DamageType> damage;
    private final boolean mobs;
    private final double most;
    private final @Nullable TextKey rateUnit;
    private final double rateFactor;
    private final List<Double> mapSteps;
    private final TextKey shortName;
    private final double background;
    private final @Nullable IHazardGauge gauge;
    private final List<IHazardSource> sources = new CopyOnWriteArrayList<>();
    private final List<IHazardRecovery> recoveries = new CopyOnWriteArrayList<>();

    private HazardKind(final Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.shortName = builder.shortName == null ? builder.name : builder.shortName;
        this.background = builder.background;
        this.unit = builder.unit;
        this.rateUnit = builder.rateUnit;
        this.rateFactor = builder.rateFactor;
        this.mapSteps = builder.mapSteps;
        this.bands = builder.bands.stream().sorted(Comparator.comparingDouble(HazardBand::from)).toList();
        this.recovery = builder.recovery;
        this.recoveryShare = builder.recoveryShare;
        this.protection = builder.protection;
        this.treatment = builder.treatment;
        this.enabled = builder.enabled;
        this.damage = builder.damage;
        this.mobs = builder.mobs;
        this.most = builder.most;
        this.gauge = builder.gauge;
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

    /** What it is called where there is little room, on a device's button: RAD for radiation. */
    public TextKey shortName() {
        return this.shortName;
    }

    /** What it is measured in. */
    public TextKey unit() {
        return this.unit;
    }

    /** The unit its rate is shown in, if it has one of its own; otherwise its unit a second. */
    public Optional<TextKey> rateUnit() {
        return Optional.ofNullable(this.rateUnit);
    }

    /** A rate of {@code perSecond} of its unit a second, in the unit its rate is shown in. */
    public double shownRate(final double perSecond) {
        return perSecond * this.rateFactor;
    }

    /** A rate of {@code shown} in the unit its rate is shown in, in its unit a second. */
    public double perSecond(final double shown) {
        return shown / this.rateFactor;
    }

    /** Whether a map of hazards shows it, chunk by chunk. */
    public boolean mapped() {
        return !this.mapSteps.isEmpty();
    }

    /** Where a map's colours step up, in the unit its rate is shown in, the lowest first; none if it is not mapped. */
    public List<Double> mapSteps() {
        return this.mapSteps;
    }

    /** Which of a map's steps a rate shown as {@code shown} reached, the lowest 0; -1 below the first, normal. */
    public int mapStep(final double shown) {
        int reached = -1;
        for (int i = 0; i < this.mapSteps.size(); i++) {
            if (shown >= this.mapSteps.get(i)) {
                reached = i;
            }
        }
        return reached;
    }

    /** Its bands, the mildest first. */
    public List<HazardBand> bands() {
        return this.bands;
    }

    /** The band {@code level} stands in: the last whose start it has reached; none for nothing built up. */
    public Optional<HazardBand> band(final double level) {
        HazardBand found = null;
        for (final HazardBand band : this.bands) {
            if (level > 0 && level >= band.from()) {
                found = band;
            }
        }
        return Optional.ofNullable(found);
    }

    /**
     * The background every body lives in, in its unit a second: what comes at a body no faster than this builds up
     * nothing, and the body recovers as if away from every source.
     */
    public double background() {
        return this.background;
    }

    /** What a body loses of it in a second away from its sources: so much, and so much a share of what it has. */
    public double recovered(final double level) {
        return Math.min(level, this.recovery + level * this.recoveryShare);
    }

    /** The kind of harm worn things are protected from it by, if any. */
    public Optional<DamageKind> protection() {
        return Optional.ofNullable(this.protection);
    }

    /** What treats it, in words, if said. */
    public Optional<TextKey> treatment() {
        return Optional.ofNullable(this.treatment);
    }

    /** Whether the server has it on. */
    public boolean enabled() {
        return this.enabled.getAsBoolean();
    }

    /** Whether creatures suffer it too, not only players. */
    public boolean affectsMobs() {
        return this.mobs;
    }

    /** The most a body can build up. */
    public double most() {
        return this.most;
    }

    /**
     * What measures it, for a kind that is measured rather than built up: read once a second in place of its sources
     * and its recovery; none for a kind that builds up.
     */
    public Optional<IHazardGauge> gauge() {
        return Optional.ofNullable(this.gauge);
    }

    /** Adds a source it builds up from. */
    public void addSource(final IHazardSource source) {
        this.sources.add(Objects.requireNonNull(source, "source"));
    }

    /** What it builds up from. */
    public List<IHazardSource> sources() {
        return List.copyOf(this.sources);
    }

    /** Adds something that speeds a body's recovery from it: a medicine, a shower. */
    public void addRecovery(final IHazardRecovery recovery) {
        this.recoveries.add(Objects.requireNonNull(recovery, "recovery"));
    }

    /** How much faster than its own pace {@code body} recovers from it now: every recovery added up. */
    public double faster(final LivingEntity body, final ServerLevel level) {
        double faster = 0;
        for (final IHazardRecovery recovery : this.recoveries) {
            faster += Math.max(0, recovery.faster(body, level));
        }
        return faster;
    }

    /** How it harms in {@code level}: as its own damage type, whose death message is its own, or as generic harm. */
    public DamageSource damageSource(final Level level) {
        if (this.damage == null) {
            return level.damageSources().generic();
        }
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(this.damage));
    }

    @Override
    public String toString() {
        return this.id.toString();
    }

    /** A kind of hazard being declared. */
    public static final class Builder {

        private final ResourceLocation id;
        private final TextKey name;
        private final TextKey unit;
        private final List<HazardBand> bands = new ArrayList<>();
        private double recovery;
        private double recoveryShare;
        private @Nullable DamageKind protection;
        private @Nullable TextKey treatment;
        private BooleanSupplier enabled = () -> true;
        private @Nullable ResourceKey<DamageType> damage;
        private boolean mobs;
        private double most = Double.MAX_VALUE;
        private @Nullable TextKey rateUnit;
        private double rateFactor = 1;
        private List<Double> mapSteps = List.of();
        private @Nullable TextKey shortName;
        private double background;
        private @Nullable IHazardGauge gauge;

        private Builder(final ResourceLocation id, final TextKey name, final TextKey unit) {
            this.id = id;
            this.name = name;
            this.unit = unit;
        }

        /** A band from {@code from} built up, called {@code bandName}, doing {@code effects}. */
        public Builder band(final double from, final TextKey bandName, final IHazardEffect... effects) {
            this.bands.add(new HazardBand(from, bandName, List.of(effects)));
            return this;
        }

        /** How much a body loses in a second away from its sources. */
        public Builder recovers(final double perSecond) {
            this.recovery = perSecond;
            return this;
        }

        /** What share of what it has a body loses in a second away from its sources; 1 to lose it all at once. */
        public Builder recoversShare(final double share) {
            this.recoveryShare = Math.clamp(share, 0, 1);
            return this;
        }

        /** The kind of harm worn things are protected from it by. */
        public Builder protectedBy(final DamageKind kind) {
            this.protection = kind;
            return this;
        }

        /** What treats it, in words, for the manual and the body's screen. */
        public Builder treatedBy(final TextKey words) {
            this.treatment = words;
            return this;
        }

        /** Whether the server has it on, as its setting says. */
        public Builder enabledWhen(final BooleanSupplier setting) {
            this.enabled = setting;
            return this;
        }

        /** The damage type it harms as, whose death message is its own. */
        public Builder deadlyAs(final ResourceKey<DamageType> type) {
            this.damage = type;
            return this;
        }

        /** Creatures suffer it too. */
        public Builder affectsMobs() {
            this.mobs = true;
            return this;
        }

        /**
         * The background every body lives in, in its unit a second: what comes no faster builds up nothing, as the
         * sky's radiation gives no one a dose worth counting, and the body goes on recovering in it.
         */
        public Builder background(final double perSecond) {
            this.background = Math.max(0, perSecond);
            return this;
        }

        /** What it is called where there is little room, on a device's button; its name when not said. */
        public Builder shortName(final TextKey shorter) {
            this.shortName = shorter;
            return this;
        }

        /** The most a body can build up. */
        public Builder upTo(final double max) {
            this.most = max;
            return this;
        }

        /**
         * It is measured rather than built up: each second a body stands in it as far as {@code measure} reads, its
         * sources and recovery unused, as hypothermia stands as far as the core is below the edge of its warmth.
         */
        public Builder gauged(final IHazardGauge measure) {
            this.gauge = Objects.requireNonNull(measure, "measure");
            return this;
        }

        /**
         * Its rate is shown in {@code rateUnit}, {@code factor} of it to one of its unit a second: a dose in sieverts
         * whose rate reads in millisieverts an hour has a factor of 3 600 000.
         */
        public Builder rateShownIn(final TextKey rateUnit, final double factor) {
            this.rateUnit = rateUnit;
            this.rateFactor = factor;
            return this;
        }

        /**
         * A map of hazards shows it chunk by chunk, its colours stepping up at each of {@code steps}, in the unit its
         * rate is shown in: the rate an instrument read there, below the first step normal.
         */
        public Builder mapped(final double... steps) {
            this.mapSteps = Arrays.stream(steps).sorted().boxed().toList();
            return this;
        }

        public HazardKind build() {
            return new HazardKind(this);
        }
    }
}
