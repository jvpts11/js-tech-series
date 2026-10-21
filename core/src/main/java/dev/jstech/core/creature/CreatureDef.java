/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.creature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.air.AirMix;
import dev.jstech.core.dimension.Climate;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.room.SealedRooms;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * A creature as a data pack or a mod declares it, so a pack changes how a creature lives with no code: how much health
 * it has, how fast it goes, how hard it hits, what it does, in order of what matters most to it, where it appears,
 * and the air it breathes. A creature appears only where its air suits it, and lives only there: out of it, it chokes.
 *
 * @param health       its health, or what its kind is declared with
 * @param speed        its speed, or what its kind is declared with
 * @param attackDamage how hard it hits, or what its kind is declared with
 * @param followRange  how far it sees to follow or go for something, in blocks, or what its kind is declared with
 * @param behaviours   what it does, the first mattering most
 * @param spawns       where it appears
 * @param air          the air it breathes, or none for one that breathes nothing
 */
public record CreatureDef(Optional<Double> health, Optional<Double> speed, Optional<Double> attackDamage,
                          Optional<Double> followRange, List<ICreatureBehaviour> behaviours, List<Spawn> spawns,
                          Optional<Air> air) {

    public static final Codec<CreatureDef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(1, 4096).optionalFieldOf("health").forGetter(CreatureDef::health),
            Codec.doubleRange(0, 4).optionalFieldOf("speed").forGetter(CreatureDef::speed),
            Codec.doubleRange(0, 2048).optionalFieldOf("attack_damage").forGetter(CreatureDef::attackDamage),
            Codec.doubleRange(1, 256).optionalFieldOf("follow_range").forGetter(CreatureDef::followRange),
            CreatureBehaviours.CODEC.listOf().optionalFieldOf("behaviours", List.of())
                    .forGetter(CreatureDef::behaviours),
            Spawn.CODEC.listOf().optionalFieldOf("spawns", List.of()).forGetter(CreatureDef::spawns),
            Air.CODEC.optionalFieldOf("air").forGetter(CreatureDef::air))
            .apply(instance, CreatureDef::new));

    public CreatureDef {
        behaviours = List.copyOf(behaviours);
        spawns = List.copyOf(spawns);
    }

    /** Starts declaring a creature in code. */
    public static Builder builder() {
        return new Builder();
    }

    /** Whether the air at {@code pos} suits it: always for one that breathes nothing. */
    public boolean breathesAt(final Level level, final BlockPos pos) {
        return air.map(breathes -> breathes.suits(level, pos)).orElse(true);
    }

    /** Whether the air of {@code climate} suits it: always for one that breathes nothing. */
    public boolean breathes(final Climate climate) {
        return air.map(breathes -> breathes.suits(climate)).orElse(true);
    }

    /** How a creature is placed where it appears. */
    public enum Placement implements IStableName {

        /** On a block it can stand on, in air. */
        ON_GROUND("on_ground"),
        /** In water. */
        IN_WATER("in_water"),
        /** In lava. */
        IN_LAVA("in_lava"),
        /** Anywhere at all. */
        ANYWHERE("anywhere");

        private final String serializedName;

        Placement(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }

        /** The game's own rule for it. */
        public SpawnPlacementType rule() {
            return switch (this) {
                case ON_GROUND -> SpawnPlacementTypes.ON_GROUND;
                case IN_WATER -> SpawnPlacementTypes.IN_WATER;
                case IN_LAVA -> SpawnPlacementTypes.IN_LAVA;
                case ANYWHERE -> SpawnPlacementTypes.NO_RESTRICTIONS;
            };
        }
    }

    /**
     * Where it appears: in which biomes, by id or by tag, of which dimensions, how often against the other creatures of
     * its category there, and in groups of how many.
     *
     * @param biomes     the biomes, as {@code minecraft:plains} or {@code #minecraft:is_forest}; none for any
     * @param dimensions the dimensions; none for any
     * @param weight     how often, against the other creatures of its category in those biomes
     * @param least      the fewest of a group
     * @param most       the most of a group
     * @param category   the category it counts against, or its kind's own
     * @param placement  how it is placed
     */
    public record Spawn(List<ExtraCodecs.TagOrElementLocation> biomes, List<ResourceKey<Level>> dimensions, int weight,
                        int least, int most, Optional<MobCategory> category, Placement placement) {

        public static final Codec<Spawn> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.TAG_OR_ELEMENT_ID.listOf().optionalFieldOf("biomes", List.of()).forGetter(Spawn::biomes),
                ResourceKey.codec(Registries.DIMENSION).listOf().optionalFieldOf("dimensions", List.of())
                        .forGetter(Spawn::dimensions),
                Codec.intRange(1, 1000).fieldOf("weight").forGetter(Spawn::weight),
                Codec.intRange(1, 64).optionalFieldOf("least", 1).forGetter(Spawn::least),
                Codec.intRange(1, 64).optionalFieldOf("most", 1).forGetter(Spawn::most),
                MobCategory.CODEC.optionalFieldOf("category").forGetter(Spawn::category),
                StableCodecs.byName(Placement.class).optionalFieldOf("placement", Placement.ON_GROUND)
                        .forGetter(Spawn::placement))
                .apply(instance, Spawn::new));

        public Spawn {
            biomes = List.copyOf(biomes);
            dimensions = List.copyOf(dimensions);
            if (most < least) {
                throw new IllegalArgumentException("a group of creatures is no fewer than its least");
            }
        }

        /** Whether it appears in {@code biome}. */
        public boolean in(final Holder<Biome> biome) {
            if (biomes.isEmpty()) {
                return true;
            }
            for (final ExtraCodecs.TagOrElementLocation each : biomes) {
                if (each.tag() ? biome.is(TagKey.create(Registries.BIOME, each.id())) : biome.is(each.id())) {
                    return true;
                }
            }
            return false;
        }

        /** Whether it appears in {@code dimension}. */
        public boolean in(final ResourceKey<Level> dimension) {
            return dimensions.isEmpty() || dimensions.contains(dimension);
        }
    }

    /**
     * The air a creature breathes: how much of a gas it needs in it, as the gas's own pressure in the air, and how much
     * it bears. People need about 16 kilopascals of oxygen and bear about 50.
     *
     * @param gas   the gas, by id, as the air kit names it
     * @param least the least pressure of it the creature lives on, in kilopascals
     * @param most  the most it bears, in kilopascals
     */
    public record Air(ResourceLocation gas, double least, double most) {

        public static final Codec<Air> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.optionalFieldOf("gas", AirMix.OXYGEN).forGetter(Air::gas),
                Codec.doubleRange(0, 100_000).optionalFieldOf("least", 0.0).forGetter(Air::least),
                Codec.doubleRange(0, 100_000).optionalFieldOf("most", 100_000.0).forGetter(Air::most))
                .apply(instance, Air::new));

        public Air {
            if (most < least) {
                throw new IllegalArgumentException("a creature bears no less of its gas than it lives on");
            }
        }

        /** Whether the air at {@code pos} suits it: a sealed room's own, or the dimension's at that height. */
        public boolean suits(final Level level, final BlockPos pos) {
            return suits(SealedRooms.climateAt(level, pos));
        }

        /** Whether the air of {@code climate} suits it. */
        public boolean suits(final Climate climate) {
            final double partial = SealedRooms.airOf(climate).partial(gas, climate.pressure());
            return partial >= least && partial <= most;
        }
    }

    /** Declares a creature one property at a time, in code. */
    public static final class Builder {

        private final List<ICreatureBehaviour> behaviours = new ArrayList<>();
        private final List<Spawn> spawns = new ArrayList<>();
        private Optional<Double> health = Optional.empty();
        private Optional<Double> speed = Optional.empty();
        private Optional<Double> attackDamage = Optional.empty();
        private Optional<Double> followRange = Optional.empty();
        private Optional<Air> air = Optional.empty();

        private Builder() {
        }

        public Builder health(final double value) {
            this.health = Optional.of(value);
            return this;
        }

        public Builder speed(final double value) {
            this.speed = Optional.of(value);
            return this;
        }

        public Builder attackDamage(final double value) {
            this.attackDamage = Optional.of(value);
            return this;
        }

        public Builder followRange(final double value) {
            this.followRange = Optional.of(value);
            return this;
        }

        /** Adds what it does, after what matters more to it. */
        public Builder behaviour(final ICreatureBehaviour behaviour) {
            this.behaviours.add(behaviour);
            return this;
        }

        public Builder spawn(final Spawn spawn) {
            this.spawns.add(spawn);
            return this;
        }

        /** It breathes {@code gas}, living on {@code least} kilopascals of it up to {@code most}. */
        public Builder breathes(final ResourceLocation gas, final double least, final double most) {
            this.air = Optional.of(new Air(gas, least, most));
            return this;
        }

        public CreatureDef build() {
            return new CreatureDef(health, speed, attackDamage, followRange, behaviours, spawns, air);
        }
    }
}
