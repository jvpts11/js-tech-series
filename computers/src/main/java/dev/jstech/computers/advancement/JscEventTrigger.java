/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * The trigger the mod's own advancements stand on: something happened, named by a stable id, with a detail that tells
 * apart the criteria of an advancement asking for all of something (every era, every distribution).
 *
 * <p>One trigger for all of them rather than one each, because what differs between them is only which event and
 * which detail, and a trigger each would be sixty registrations saying the same thing. It is fired where the event
 * happens, for the player who caused it, and never on a tick, so an advancement costs nothing until it is earned.
 */
public class JscEventTrigger extends SimpleCriterionTrigger<JscEventTrigger.Instance> {

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    /** Reports that {@code player} caused {@code event}, with that detail, or an empty one when it has none. */
    public void trigger(final ServerPlayer player, final String event, final String detail) {
        this.trigger(player, instance -> instance.matches(event, detail));
    }

    /** A criterion met by the event, whatever its detail. */
    public static Criterion<Instance> on(final String event) {
        return JscTriggers.EVENT.get().createCriterion(new Instance(Optional.empty(), event, Optional.empty()));
    }

    /** A criterion met by the event with exactly that detail: one of those an "all of them" advancement asks for. */
    public static Criterion<Instance> on(final String event, final String detail) {
        return JscTriggers.EVENT.get().createCriterion(
                new Instance(Optional.empty(), event, Optional.of(detail)));
    }

    public record Instance(Optional<ContextAwarePredicate> player, String event, Optional<String> detail)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Codec.STRING.fieldOf("event").forGetter(Instance::event),
                Codec.STRING.optionalFieldOf("detail").forGetter(Instance::detail)
        ).apply(inst, Instance::new));

        public boolean matches(final String happened, final String itsDetail) {
            return this.event.equals(happened) && (this.detail.isEmpty() || this.detail.get().equals(itsDetail));
        }
    }
}
