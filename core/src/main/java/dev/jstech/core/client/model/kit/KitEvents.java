/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import dev.jstech.core.model.IAnimationListener;
import dev.jstech.core.model.ModelAnimation;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * What the events of a model's animations do in the world on the player's game: a sound plays where the model is,
 * a particle shows at the locator it names, or at the model's middle, and a word goes to the code that drives it.
 * A sound or particle the game has none of by that name is let go.
 */
final class KitEvents {

    private static final float PIXEL = 1.0F / 16.0F;
    /* Where a particle of no locator shows: the middle of a block-sized model, in its pixels. */
    private static final float MIDDLE = 8.0F;

    private KitEvents() {
    }

    /**
     * The listener of the events of a thing drawn where {@code placed} says: the matrix from its model's space, in
     * blocks, to the world's, worked out only when an event needs it.
     */
    static <T> IAnimationListener of(final T thing, final IKitDriver<? super T> driver, final KitInstance instance,
                                     @Nullable final Level level, final Supplier<Matrix4f> placed,
                                     final SoundSource source) {
        return (controller, animation, event) -> {
            switch (event.kind()) {
                case SOUND -> sound(level, placed.get(), event, source);
                case PARTICLE -> particle(level, placed.get(), instance, event);
                case INSTRUCTION -> driver.instruction(thing, animation, event.value());
            }
        };
    }

    private static void sound(@Nullable final Level level, final Matrix4f placed, final ModelAnimation.Event event,
                              final SoundSource source) {
        final ResourceLocation name = ResourceLocation.tryParse(event.value());
        if (level == null || name == null) {
            return;
        }
        final SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(name);
        if (sound != null) {
            final Vector3f at = placed.transformPosition(new Vector3f());
            level.playLocalSound(at.x, at.y, at.z, sound, source, 1.0F, 1.0F, false);
        }
    }

    private static void particle(@Nullable final Level level, final Matrix4f placed, final KitInstance instance,
                                 final ModelAnimation.Event event) {
        final ResourceLocation name = ResourceLocation.tryParse(event.value());
        if (level == null || name == null) {
            return;
        }
        final ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(name);
        if (!(type instanceof SimpleParticleType simple)) {
            return;
        }
        final Vector3f at = new Vector3f(0, MIDDLE, 0);
        if (!event.locator().isEmpty()) {
            instance.pose().locator(event.locator(), instance.world(), at);
        }
        // The locator is in the model's pixels; the placement counts in blocks.
        placed.transformPosition(at.mul(PIXEL));
        level.addParticle(simple, at.x, at.y, at.z, 0, 0, 0);
    }
}
