/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/**
 * What a player must have reached to pass: a step of an axis, written in data files as
 * {@code {"axis": "jscore:hardware_era", "step": "legacy"}}. A gate on an axis no mod registered never passes, so a
 * datapack that names a missing mod's axis keeps its gate shut rather than open.
 *
 * @param axis the axis's id
 * @param step the step's saved name
 */
public record ProgressionGate(ResourceLocation axis, String step) {

    public static final Codec<ProgressionGate> CODEC = RecordCodecBuilder.<ProgressionGate>create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("axis").forGetter(ProgressionGate::axis),
                    Codec.STRING.fieldOf("step").forGetter(ProgressionGate::step))
                    .apply(instance, ProgressionGate::new))
            .validate(gate -> gate.step().isEmpty()
                    ? DataResult.error(() -> "a gate names the step it asks for") : DataResult.success(gate));

    /** The gate that asks for {@code step} along {@code axis}. */
    public static <S extends IAxisStep> ProgressionGate of(final ProgressionAxis<S> axis, final S step) {
        return new ProgressionGate(axis.id(), step.serializedName());
    }

    /** Whether {@code player} has reached the gate's step. */
    public boolean passes(final MinecraftServer server, final UUID player) {
        final ProgressionAxis<?> found = ProgressionAxes.byId(axis);
        return found != null && passes(server, player, found);
    }

    /** Whether the gate's step is a step of a registered axis: a gate that is not can never pass. */
    public boolean resolves() {
        final ProgressionAxis<?> found = ProgressionAxes.byId(axis);
        return found != null && found.byName(step) != null;
    }

    private <S extends IAxisStep> boolean passes(final MinecraftServer server, final UUID player,
                                                 final ProgressionAxis<S> found) {
        final S wanted = found.byName(step);
        return wanted != null && PlayerProgress.reached(server, player, found).level() >= wanted.level();
    }
}
