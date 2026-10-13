/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.content.EntityEntry;
import dev.jstech.core.projectile.CoreProjectile;
import dev.jstech.core.projectile.ProjectileSpec;
import dev.jstech.core.robot.RobotEntity;
import dev.jstech.core.vehicle.VehicleEntity;
import dev.jstech.core.vehicle.VehicleSpec;
import java.util.List;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A robot, a rover and a bolt of the test mod, declared the way a mod declares its own, to prove the Core's robots,
 * vehicles and projectiles. They are drawn as nothing: the tests look at what they do, not at them.
 */
public final class TestEntities {

    /** The rover: two seats, a battery, a walking pace. */
    public static final VehicleSpec ROVER_SPEC = new VehicleSpec(
            List.of(new Vec3(0.0, 0.4, 0.3), new Vec3(0.0, 0.4, -0.5)), 0.5, 0.08, 4.0F, 0.42, false, 10_000, 5);
    /** The bolt: it flies straight, harms by four, passes through one thing and stops in the next. */
    public static final ProjectileSpec BOLT_SPEC = new ProjectileSpec(4.0F, 0.0, 1, 0.0, 0.0F, 0.0F, false, 100);

    public static final EntityEntry<Robot> ROBOT = TestSounds.CONTENT.entity("test_robot", Robot::new,
                    MobCategory.MISC).size(0.6F, 1.2F).named("Test Robot").attributes(RobotEntity::robotAttributes)
            .register();
    public static final EntityEntry<Rover> ROVER = TestSounds.CONTENT.entity("test_rover", Rover::new,
            MobCategory.MISC).size(1.4F, 0.9F).named("Test Rover").holdsEnergy().register();
    public static final EntityEntry<Bolt> BOLT = TestSounds.CONTENT.entity("test_bolt", Bolt::new,
            MobCategory.MISC).size(0.25F, 0.25F).tracking(4, 10).named("Test Bolt").register();

    private TestEntities() {
    }

    /** Declares the entities, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The test robot, the Core's robot as it comes. */
    public static final class Robot extends RobotEntity {

        public Robot(final EntityType<? extends Robot> type, final Level level) {
            super(type, level);
        }
    }

    /** The test rover, the Core's vehicle as it comes. */
    public static final class Rover extends VehicleEntity {

        public Rover(final EntityType<? extends Rover> type, final Level level) {
            super(type, level);
        }

        @Override
        public VehicleSpec spec() {
            return ROVER_SPEC;
        }

        @Override
        protected ItemStack asItem() {
            return ItemStack.EMPTY;
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
}
