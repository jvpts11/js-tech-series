/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.creature;

import dev.jstech.core.hazard.Lungs;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * A creature any mod declares, which lives as its data says ({@link Creatures}): its health, speed and how hard it
 * hits, what it does, in order of what matters most to it, and the air it breathes. What it does is built from the data
 * as it first wakes, and again when the server reads its data packs again. Out of the air it breathes, it chokes, a
 * point of harm a second.
 *
 * <pre>{@code
 * public static final EntityEntry<CreatureEntity> GRAZER = CONTENT.entity("grazer", CreatureEntity::new,
 *         MobCategory.CREATURE).size(0.9F, 1.2F).attributes(CreatureEntity::creatureAttributes).named("Grazer")
 *         .register();
 * }</pre>
 */
public class CreatureEntity extends PathfinderMob {

    /* The reading of the data packs what it does was built from; none yet. */
    private long builtFrom = Long.MIN_VALUE;

    private static final double HEALTH = 10.0;
    private static final double SPEED = 0.25;
    private static final double ATTACK_DAMAGE = 2.0;
    private static final double FOLLOW_RANGE = 16.0;
    /* How often it breathes, in ticks, and the harm it takes each time it cannot. */
    private static final int BREATHES_EVERY = 20;
    private static final float CHOKING = 1.0F;

    public CreatureEntity(final EntityType<? extends CreatureEntity> type, final Level level) {
        super(type, level);
    }

    /** What a creature starts with before its data says otherwise, for a mod declaring one. */
    public static AttributeSupplier.Builder creatureAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE).add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    /** How it lives, or none while its data names nothing for its kind. */
    public Optional<CreatureDef> def() {
        return Creatures.get(level(), getType());
    }

    @Override
    public void aiStep() {
        if (!level().isClientSide()) {
            fitToData();
            if (tickCount % BREATHES_EVERY == 0) {
                breathe();
            }
        }
        super.aiStep();
    }

    /** A creature that appears starts at the health its data gives it, whole. */
    @Override
    public SpawnGroupData finalizeSpawn(final ServerLevelAccessor level, final DifficultyInstance difficulty,
                                        final MobSpawnType spawnType, @Nullable final SpawnGroupData group) {
        fitToData();
        setHealth(getMaxHealth());
        return super.finalizeSpawn(level, difficulty, spawnType, group);
    }

    /* What it does and its numbers built from its data, when the server has read its data packs since. */
    private void fitToData() {
        final long reading = Creatures.version();
        if (reading == builtFrom) {
            return;
        }
        builtFrom = reading;
        goalSelector.removeAllGoals(goal -> true);
        targetSelector.removeAllGoals(goal -> true);
        final CreatureDef def = def().orElse(null);
        if (def == null) {
            return;
        }
        int priority = 0;
        for (final ICreatureBehaviour behaviour : def.behaviours()) {
            (behaviour.targets() ? targetSelector : goalSelector).addGoal(priority++, behaviour.goal(this));
        }
        base(Attributes.MAX_HEALTH, def.health());
        base(Attributes.MOVEMENT_SPEED, def.speed());
        base(Attributes.ATTACK_DAMAGE, def.attackDamage());
        base(Attributes.FOLLOW_RANGE, def.followRange());
        setHealth(Math.min(getHealth(), getMaxHealth()));
    }

    private void base(final Holder<Attribute> attribute, final Optional<Double> value) {
        final AttributeInstance instance = getAttribute(attribute);
        if (instance != null && value.isPresent()) {
            instance.setBaseValue(value.get());
        }
    }

    /* Out of the air it breathes, it chokes. */
    private void breathe() {
        final CreatureDef def = def().orElse(null);
        if (def != null && !def.breathesAt(level(), BlockPos.containing(getEyePosition()))) {
            hurt(new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(Lungs.FAILURE)), CHOKING);
        }
    }
}
