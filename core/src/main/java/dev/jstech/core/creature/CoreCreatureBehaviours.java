/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.creature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import net.minecraft.advancements.critereon.EntityTypePredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

/**
 * The behaviours every creature's data may name: wander, look at players, look around, keep afloat, panic when hurt,
 * avoid what it fears, follow the food it likes, go for what it fights and hit it, go for players, and hit back. Speeds
 * are shares of the creature's own; distances in blocks.
 */
public final class CoreCreatureBehaviours {

    /** The widest a creature looks or keeps away, in blocks. */
    public static final float MOST_RANGE = 64.0F;
    private static final Codec<Double> SPEED = Codec.doubleRange(0.0, 8.0);
    private static final Codec<Float> RANGE = Codec.floatRange(1.0F, MOST_RANGE);

    public static final CreatureBehaviourType<Wander> WANDER = CreatureBehaviours.declare(new CreatureBehaviourType<>(
            id("wander"), SPEED.optionalFieldOf("speed", 1.0).xmap(Wander::new, Wander::speed)));
    public static final CreatureBehaviourType<LookAtPlayers> LOOK_AT_PLAYERS = CreatureBehaviours.declare(
            new CreatureBehaviourType<>(id("look_at_players"), RANGE.optionalFieldOf("range", 8.0F)
                    .xmap(LookAtPlayers::new, LookAtPlayers::range)));
    public static final CreatureBehaviourType<LookAround> LOOK_AROUND = CreatureBehaviours.declare(
            new CreatureBehaviourType<>(id("look_around"), MapCodec.unit(LookAround::new)));
    public static final CreatureBehaviourType<KeepAfloat> KEEP_AFLOAT = CreatureBehaviours.declare(
            new CreatureBehaviourType<>(id("float"), MapCodec.unit(KeepAfloat::new)));
    public static final CreatureBehaviourType<Panic> PANIC = CreatureBehaviours.declare(new CreatureBehaviourType<>(
            id("panic"), SPEED.optionalFieldOf("speed", 1.25).xmap(Panic::new, Panic::speed)));
    public static final CreatureBehaviourType<Avoid> AVOID = CreatureBehaviours.declare(new CreatureBehaviourType<>(
            id("avoid"), RecordCodecBuilder.mapCodec(instance -> instance.group(
                    EntityTypePredicate.CODEC.fieldOf("entity").forGetter(Avoid::entity),
                    RANGE.optionalFieldOf("distance", 8.0F).forGetter(Avoid::distance),
                    SPEED.optionalFieldOf("speed", 1.2).forGetter(Avoid::speed))
                    .apply(instance, Avoid::new))));
    public static final CreatureBehaviourType<Tempt> TEMPT = CreatureBehaviours.declare(new CreatureBehaviourType<>(
            id("tempt"), RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ItemPredicate.CODEC.fieldOf("item").forGetter(Tempt::item),
                    SPEED.optionalFieldOf("speed", 1.0).forGetter(Tempt::speed))
                    .apply(instance, Tempt::new))));
    public static final CreatureBehaviourType<MeleeAttack> MELEE_ATTACK = CreatureBehaviours.declare(
            new CreatureBehaviourType<>(id("melee_attack"), SPEED.optionalFieldOf("speed", 1.0)
                    .xmap(MeleeAttack::new, MeleeAttack::speed)));
    public static final CreatureBehaviourType<TargetPlayers> TARGET_PLAYERS = CreatureBehaviours.declare(
            new CreatureBehaviourType<>(id("target_players"), MapCodec.unit(TargetPlayers::new)));
    public static final CreatureBehaviourType<Retaliate> RETALIATE = CreatureBehaviours.declare(
            new CreatureBehaviourType<>(id("retaliate"), MapCodec.unit(Retaliate::new)));

    /* How much faster than it walks away a creature runs when what it fears is close. */
    private static final double SPRINT = 1.2;

    private CoreCreatureBehaviours() {
    }

    /** Declares the Core's behaviours, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }

    /** Wanders about at {@code speed}, keeping out of water. */
    public record Wander(double speed) implements ICreatureBehaviour {

        @Override
        public CreatureBehaviourType<Wander> type() {
            return WANDER;
        }

        @Override
        public Goal goal(final CreatureEntity creature) {
            return new WaterAvoidingRandomStrollGoal(creature, speed);
        }
    }

    /** Looks at players within {@code range}. */
    public record LookAtPlayers(float range) implements ICreatureBehaviour {

        @Override
        public CreatureBehaviourType<LookAtPlayers> type() {
            return LOOK_AT_PLAYERS;
        }

        @Override
        public Goal goal(final CreatureEntity creature) {
            return new LookAtPlayerGoal(creature, Player.class, range);
        }
    }

    /** Looks about it now and then. */
    public record LookAround() implements ICreatureBehaviour {

        @Override
        public CreatureBehaviourType<LookAround> type() {
            return LOOK_AROUND;
        }

        @Override
        public Goal goal(final CreatureEntity creature) {
            return new RandomLookAroundGoal(creature);
        }
    }

    /** Keeps itself afloat in water and lava. */
    public record KeepAfloat() implements ICreatureBehaviour {

        @Override
        public CreatureBehaviourType<KeepAfloat> type() {
            return KEEP_AFLOAT;
        }

        @Override
        public Goal goal(final CreatureEntity creature) {
            return new FloatGoal(creature);
        }
    }

    /** Runs about at {@code speed} when hurt or burning. */
    public record Panic(double speed) implements ICreatureBehaviour {

        @Override
        public CreatureBehaviourType<Panic> type() {
            return PANIC;
        }

        @Override
        public Goal goal(final CreatureEntity creature) {
            return new PanicGoal(creature, speed);
        }
    }

    /** Keeps {@code distance} away from what {@code entity} names, at {@code speed}. */
    public record Avoid(EntityTypePredicate entity, float distance, double speed) implements ICreatureBehaviour {

        @Override
        public CreatureBehaviourType<Avoid> type() {
            return AVOID;
        }

        @Override
        public Goal goal(final CreatureEntity creature) {
            return new AvoidEntityGoal<>(creature, LivingEntity.class, feared -> entity.matches(feared.getType()),
                    distance, speed, speed * SPRINT, feared -> true);
        }
    }

    /** Follows a player holding what {@code item} names, at {@code speed}. */
    public record Tempt(ItemPredicate item, double speed) implements ICreatureBehaviour {

        @Override
        public CreatureBehaviourType<Tempt> type() {
            return TEMPT;
        }

        @Override
        public Goal goal(final CreatureEntity creature) {
            return new TemptGoal(creature, speed, item::test, false);
        }
    }

    /** Goes for what it fights at {@code speed} and hits it, as hard as its attack damage. */
    public record MeleeAttack(double speed) implements ICreatureBehaviour {

        @Override
        public CreatureBehaviourType<MeleeAttack> type() {
            return MELEE_ATTACK;
        }

        @Override
        public Goal goal(final CreatureEntity creature) {
            return new MeleeAttackGoal(creature, speed, false);
        }
    }

    /** Picks the nearest player it sees to fight. */
    public record TargetPlayers() implements ICreatureBehaviour {

        @Override
        public CreatureBehaviourType<TargetPlayers> type() {
            return TARGET_PLAYERS;
        }

        @Override
        public Goal goal(final CreatureEntity creature) {
            return new NearestAttackableTargetGoal<>(creature, Player.class, true);
        }

        @Override
        public boolean targets() {
            return true;
        }
    }

    /** Fights back whatever hurts it. */
    public record Retaliate() implements ICreatureBehaviour {

        @Override
        public CreatureBehaviourType<Retaliate> type() {
            return RETALIATE;
        }

        @Override
        public Goal goal(final CreatureEntity creature) {
            return new HurtByTargetGoal(creature);
        }

        @Override
        public boolean targets() {
            return true;
        }
    }
}
