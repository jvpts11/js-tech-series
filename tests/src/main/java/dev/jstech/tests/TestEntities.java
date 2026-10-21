/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.content.EntityEntry;
import dev.jstech.core.creature.CoreCreatureBehaviours;
import dev.jstech.core.creature.CreatureDef;
import dev.jstech.core.creature.CreatureEntity;
import dev.jstech.core.creature.Creatures;
import dev.jstech.core.projectile.CoreProjectile;
import dev.jstech.core.projectile.ProjectileSpec;
import dev.jstech.core.robot.RobotEntity;
import dev.jstech.core.robot.RobotMovement;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;

/**
 * Robots, projectiles and creatures of the test mod, declared the way a mod declares its own, to prove the Core's
 * robots, projectiles and creatures: a robot that walks on nothing, one that walks on a battery, one that flies and one
 * that swims; a bolt that flies straight and a slug that flies by real ballistics and gets through glass; a critter
 * that lives in the overworld's air and a choker that needs more oxygen than it has. They are drawn as nothing: the
 * tests look at what they do, not at them.
 */
public final class TestEntities {

    /** The bolt: it flies straight, harms by four, passes through one thing and stops in the next. */
    public static final ProjectileSpec BOLT_SPEC = new ProjectileSpec(4.0F, 0.0, 1, 0.0, 0.0F, 0.0F, false, 100);
    /**
     * The slug: ten grams with a large drag area, so a test sees the air slow it within a second; it falls as a
     * snowball does, and gets through a hardness of one, a pane of glass but not a plank.
     */
    public static final ProjectileSpec SLUG_SPEC = new ProjectileSpec(4.0F, 0.03, 0, 0.0, 0.0F, 0.0F, false, 200)
            .withBallistics(0.01, 0.001).withPenetration(1.0F);
    /** What the battery robots hold, in joules. */
    public static final int BATTERY = 20_000;

    public static final EntityEntry<Robot> ROBOT = TestSounds.CONTENT.entity("test_robot", Robot::new,
                    MobCategory.MISC).size(0.6F, 1.2F).named("Test Robot").attributes(RobotEntity::robotAttributes)
            .register();
    public static final EntityEntry<Worker> WORKER = TestSounds.CONTENT.entity("test_worker", Worker::new,
                    MobCategory.MISC).size(0.6F, 1.2F).named("Test Worker").attributes(RobotEntity::robotAttributes)
            .holdsEnergy().register();
    public static final EntityEntry<Flyer> FLYER = TestSounds.CONTENT.entity("test_flyer", Flyer::new,
                    MobCategory.MISC).size(0.8F, 0.5F).named("Test Flyer").attributes(RobotEntity::robotAttributes)
            .holdsEnergy().register();
    public static final EntityEntry<Diver> DIVER = TestSounds.CONTENT.entity("test_diver", Diver::new,
                    MobCategory.MISC).size(0.7F, 0.7F).named("Test Diver").attributes(RobotEntity::robotAttributes)
            .register();
    public static final EntityEntry<Bolt> BOLT = TestSounds.CONTENT.entity("test_bolt", Bolt::new,
            MobCategory.MISC).size(0.25F, 0.25F).tracking(4, 10).named("Test Bolt").register();
    public static final EntityEntry<Slug> SLUG = TestSounds.CONTENT.entity("test_slug", Slug::new,
            MobCategory.MISC).size(0.25F, 0.25F).tracking(4, 10).named("Test Slug").register();
    public static final EntityEntry<CreatureEntity> CRITTER = TestSounds.CONTENT.entity("test_critter",
                    CreatureEntity::new, MobCategory.CREATURE).size(0.6F, 0.6F).named("Test Critter")
            .attributes(CreatureEntity::creatureAttributes).register();
    public static final EntityEntry<CreatureEntity> CHOKER = TestSounds.CONTENT.entity("test_choker",
                    CreatureEntity::new, MobCategory.CREATURE).size(0.6F, 0.6F).named("Test Choker")
            .attributes(CreatureEntity::creatureAttributes).register();

    /**
     * The critter: it keeps afloat, panics, wanders and looks at players, in the overworld's air. It appears only in
     * the biomes of a tag no biome is in, so it never appears in a world of its own accord, only where a test puts it.
     */
    public static final CreatureDef CRITTER_DEF = Creatures.declare(CRITTER.getId(), CreatureDef.builder()
            .health(14).speed(0.3)
            .behaviour(new CoreCreatureBehaviours.KeepAfloat())
            .behaviour(new CoreCreatureBehaviours.Panic(1.25))
            .behaviour(new CoreCreatureBehaviours.Wander(1.0))
            .behaviour(new CoreCreatureBehaviours.LookAtPlayers(6.0F))
            .spawn(new CreatureDef.Spawn(List.of(new ExtraCodecs.TagOrElementLocation(
                    ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "nowhere"), true)), List.of(Level.OVERWORLD),
                    10, 1, 2, Optional.empty(), CreatureDef.Placement.ON_GROUND))
            .breathes(AirMix.OXYGEN, 16, 50)
            .build());
    /**
     * The choker: it needs thirty kilopascals of oxygen, half as much again as the overworld's air has, so it may
     * appear anywhere and appears nowhere a world has ordinary air.
     */
    public static final CreatureDef CHOKER_DEF = Creatures.declare(CHOKER.getId(), CreatureDef.builder()
            .health(20)
            .behaviour(new CoreCreatureBehaviours.Wander(1.0))
            .spawn(new CreatureDef.Spawn(List.of(), List.of(), 1, 1, 1, Optional.empty(),
                    CreatureDef.Placement.ON_GROUND))
            .breathes(AirMix.OXYGEN, 30, 100)
            .build());

    private TestEntities() {
    }

    /** Declares the entities, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The test robot, the Core's robot as it comes: it walks, and runs on nothing. */
    public static final class Robot extends RobotEntity {

        public Robot(final EntityType<? extends Robot> type, final Level level) {
            super(type, level);
        }
    }

    /** A robot that walks on a battery. */
    public static final class Worker extends RobotEntity {

        public Worker(final EntityType<? extends Worker> type, final Level level) {
            super(type, level);
        }

        @Override
        public int batteryCapacity() {
            return BATTERY;
        }
    }

    /** A robot that flies, on a battery. */
    public static final class Flyer extends RobotEntity {

        public Flyer(final EntityType<? extends Flyer> type, final Level level) {
            super(type, level);
        }

        @Override
        public RobotMovement movement() {
            return RobotMovement.FLY;
        }

        @Override
        public int batteryCapacity() {
            return BATTERY;
        }
    }

    /** A robot that swims, and runs on nothing. */
    public static final class Diver extends RobotEntity {

        public Diver(final EntityType<? extends Diver> type, final Level level) {
            super(type, level);
        }

        @Override
        public RobotMovement movement() {
            return RobotMovement.SWIM;
        }
    }

    /** The test bolt, the Core's projectile as it comes. */
    public static final class Bolt extends CoreProjectile {

        public Bolt(final EntityType<? extends Bolt> type, final Level level) {
            super(type, level);
        }

        @Override
        public ProjectileSpec spec() {
            return BOLT_SPEC;
        }
    }

    /** The test slug, a projectile of real ballistics that gets through glass. */
    public static final class Slug extends CoreProjectile {

        public Slug(final EntityType<? extends Slug> type, final Level level) {
            super(type, level);
        }

        @Override
        public ProjectileSpec spec() {
            return SLUG_SPEC;
        }
    }
}
