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
import java.util.Optional;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * The trigger any mod's advancements can stand on: something happened, named by a stable id under the mod's own
 * namespace, with a detail that tells apart the criteria of an advancement asking for all of something (every era,
 * every distribution).
 *
 * <p>One trigger for all of them rather than one each, because what differs between them is only which event and
 * which detail, and a trigger each would be dozens of registrations saying the same thing. A mod reports an event
 * with {@link Advancements#award} where it happens, for the player who caused it, and never on a tick, so an
 * advancement costs nothing until it is earned.
 */
public final class EventTrigger extends SimpleCriterionTrigger<EventTrigger.Instance> {

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    /** Reports that {@code player} caused {@code event}, with that detail, or an empty one when it has none. */
    public void trigger(final ServerPlayer player, final ResourceLocation event, final String detail) {
        this.trigger(player, instance -> instance.matches(event, detail));
    }

    /** A criterion met by the event, whatever its detail. */
    public static Criterion<Instance> on(final ResourceLocation event) {
        return CoreTriggers.EVENT.get().createCriterion(new Instance(Optional.empty(), event, Optional.empty()));
    }

    /** A criterion met by the event with exactly that detail: one of those an "all of them" advancement asks for. */
    public static Criterion<Instance> on(final ResourceLocation event, final String detail) {
        return CoreTriggers.EVENT.get().createCriterion(new Instance(Optional.empty(), event, Optional.of(detail)));
    }

    /**
     * What one criterion asks for.
     *
     * @param player who has to cause it, when the advancement narrows that down
     * @param event  the event's id
     * @param detail the detail it has to come with, or none for any
     */
    public record Instance(Optional<ContextAwarePredicate> player, ResourceLocation event, Optional<String> detail)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                ResourceLocation.CODEC.fieldOf("event").forGetter(Instance::event),
                Codec.STRING.optionalFieldOf("detail").forGetter(Instance::detail)
        ).apply(instance, Instance::new));

        public boolean matches(final ResourceLocation happened, final String itsDetail) {
            return this.event.equals(happened) && (this.detail.isEmpty() || this.detail.get().equals(itsDetail));
        }
    }
}
