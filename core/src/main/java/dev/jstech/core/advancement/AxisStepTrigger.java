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
import dev.jstech.core.progression.IAxisStep;
import dev.jstech.core.progression.ProgressionAxes;
import dev.jstech.core.progression.ProgressionAxis;
import java.util.Optional;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * The trigger of an advancement earned by coming far enough along a progression axis: reaching the Legacy era, the
 * third industrial tier. It is met by the step it names or any step past it, so a player who skips a step still earns
 * what the skipped step gives.
 */
public final class AxisStepTrigger extends SimpleCriterionTrigger<AxisStepTrigger.Instance> {

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    /** Reports that {@code player} now stands at {@code level} along the axis {@code axis}. */
    public void trigger(final ServerPlayer player, final ResourceLocation axis, final int level) {
        this.trigger(player, instance -> instance.matches(axis, level));
    }

    /** A criterion met by reaching {@code step} along {@code axis}, or any step past it. */
    public static <S extends IAxisStep> Criterion<Instance> reached(final ProgressionAxis<S> axis, final S step) {
        return CoreTriggers.AXIS_STEP.get().createCriterion(new Instance(Optional.empty(), axis.id(),
                step.serializedName()));
    }

    /**
     * What one criterion asks for.
     *
     * @param player who has to reach it, when the advancement narrows that down
     * @param axis   the axis's id
     * @param step   the step's saved name, which stays the same when steps are added around it
     */
    public record Instance(Optional<ContextAwarePredicate> player, ResourceLocation axis, String step)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                ResourceLocation.CODEC.fieldOf("axis").forGetter(Instance::axis),
                Codec.STRING.fieldOf("step").forGetter(Instance::step)
        ).apply(instance, Instance::new));

        /* An axis or a step the game does not know, from a mod no longer there, is never met. */
        public boolean matches(final ResourceLocation reachedAxis, final int reachedLevel) {
            if (!this.axis.equals(reachedAxis)) {
                return false;
            }
            final ProgressionAxis<?> known = ProgressionAxes.byId(this.axis);
            final IAxisStep wanted = known == null ? null : known.byName(this.step);
            return wanted != null && reachedLevel >= wanted.level();
        }
    }
}
