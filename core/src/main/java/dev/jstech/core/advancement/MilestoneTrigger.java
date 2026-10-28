/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.progression.Milestone;
import java.util.Optional;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * The trigger of an advancement earned by a milestone of the player's team: the first satellite, the first landing on
 * the Moon. Milestones belong to the team, so every member earns it, whoever reached it, and a member who was away
 * earns it as they next join.
 */
public final class MilestoneTrigger extends SimpleCriterionTrigger<MilestoneTrigger.Instance> {

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    /** Reports that {@code player}'s team has reached the milestone {@code milestone}. */
    public void trigger(final ServerPlayer player, final ResourceLocation milestone) {
        this.trigger(player, instance -> instance.milestone().equals(milestone));
    }

    /** A criterion met once the player's team has reached {@code milestone}. */
    public static Criterion<Instance> reached(final Milestone milestone) {
        return CoreTriggers.MILESTONE.get().createCriterion(new Instance(Optional.empty(), milestone.id()));
    }

    /**
     * What one criterion asks for.
     *
     * @param player    who has to earn it, when the advancement narrows that down
     * @param milestone the milestone's id, as worlds save it
     */
    public record Instance(Optional<ContextAwarePredicate> player, ResourceLocation milestone)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                ResourceLocation.CODEC.fieldOf("milestone").forGetter(Instance::milestone)
        ).apply(instance, Instance::new));
    }
}
