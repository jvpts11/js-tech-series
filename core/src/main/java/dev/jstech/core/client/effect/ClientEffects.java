/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.effect;

import dev.jstech.core.effect.EffectDefinition;
import dev.jstech.core.effect.EffectPayload;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Effects on the player's own game, from their files: played once where something happens, held while something goes
 * on (a chimney's smoke, an engine's plume, a lamp's sparks) and let go when it stops, at a place that may move (a
 * locator of a model, a moving structure's engine). What the server fires arrives here too.
 *
 * <pre>{@code
 * ClientEffects.play(rl("sparks"), at, new Vec3(0, 1, 0));
 * ClientEffects.holdOnBlock(this, rl("cooling_tower_steam"), worldPosition);   // a block entity, while it works
 * ClientEffects.release(this);                                                   // when it stops
 * }</pre>
 */
public final class ClientEffects {

    private static final Vec3 UP = new Vec3(0, 1, 0);

    private ClientEffects() {
    }

    /** Whether a file holds the effect {@code id}. */
    public static boolean has(final ResourceLocation id) {
        return EffectFiles.get(id).isPresent();
    }

    /** Plays the effect {@code id} once at {@code at}, facing {@code way}. */
    public static void play(final ResourceLocation id, final Vec3 at, final Vec3 way) {
        start(id, null, () -> at, () -> way, Optional::empty, 0, 1.0F);
    }

    /** Plays the effect {@code id} once from {@code at} to {@code to}: a beam's or an arc's ends. */
    public static void play(final ResourceLocation id, final Vec3 at, final Vec3 way, final Vec3 to) {
        start(id, null, () -> at, () -> way, () -> Optional.of(to), 0, 1.0F);
    }

    /** Plays the effect {@code id} once on the block at {@code pos}, where its file anchors it. */
    public static void playOnBlock(final ResourceLocation id, final BlockPos pos) {
        onBlock(id, null, pos);
    }

    /**
     * Plays the effect {@code id} where {@code at} says, facing {@code way}, both asked each tick, until
     * {@link #release} with {@code holder}; a holder holds one effect, a new one taking the old one's place.
     */
    public static void hold(final Object holder, final ResourceLocation id, final Supplier<Vec3> at,
                            final Supplier<Vec3> way) {
        EffectEngine.stop(holder);
        start(id, holder, at, way, Optional::empty, 0, 1.0F);
    }

    /**
     * Holds the effect {@code id} as {@link #hold(Object, ResourceLocation, Supplier, Supplier)} does, in the colour
     * {@code rgb} ({@code 0xRRGGBB}) in place of its own, each particle keeping its own transparency: rain of any
     * liquid, dust of any colour.
     */
    public static void hold(final Object holder, final ResourceLocation id, final Supplier<Vec3> at,
                            final Supplier<Vec3> way, final int rgb) {
        EffectEngine.stop(holder);
        start(id, holder, at, way, Optional::empty, 0xFF << 24 | rgb, 1.0F);
    }

    /**
     * Holds the effect {@code id} as {@link #hold(Object, ResourceLocation, Supplier, Supplier)} does, {@code scale}
     * times its size: an engine's plume as big as it pushes, a glow as wide as the face it wraps.
     */
    public static void holdScaled(final Object holder, final ResourceLocation id, final Supplier<Vec3> at,
                                  final Supplier<Vec3> way, final float scale) {
        EffectEngine.stop(holder);
        start(id, holder, at, way, Optional::empty, 0, scale);
    }

    /** Plays the effect {@code id} on the block at {@code pos}, where its file anchors it, until let go. */
    public static void holdOnBlock(final Object holder, final ResourceLocation id, final BlockPos pos) {
        EffectEngine.stop(holder);
        onBlock(id, holder, pos);
    }

    /** Lets go of what {@code holder} holds: it emits no more, and its particles live out their lives. */
    public static void release(final Object holder) {
        EffectEngine.stop(holder);
    }

    /** Whether {@code holder} holds an effect that plays. */
    public static boolean holds(final Object holder) {
        return EffectEngine.held(holder).isPresent();
    }

    /** Plays what the server fired. */
    public static void receive(final EffectPayload payload) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (payload.block()) {
            final BlockPos pos = BlockPos.containing(payload.at());
            EffectFiles.get(payload.effect()).ifPresent(definition -> {
                final EffectAnchors.Placed placed = EffectAnchors.of(level, pos, definition.anchor());
                EffectEngine.start(level, payload.effect(), definition, null, placed::at, placed::way,
                        () -> payload.to(), payload.colour(), payload.scale());
            });
            return;
        }
        final Entity followed = payload.follow() >= 0 ? level.getEntity(payload.follow()) : null;
        final Vec3 offset = followed == null ? Vec3.ZERO : payload.at().subtract(followed.position());
        final Supplier<Vec3> at = followed == null ? payload::at : () -> followed.position().add(offset);
        final Vec3 way = new Vec3(payload.way().x(), payload.way().y(), payload.way().z());
        start(payload.effect(), null, at, () -> way, payload::to, payload.colour(), payload.scale());
    }

    /** Plays {@code definition}, not from a file, held by {@code holder}, as the effects editor's preview does. */
    static RunningEffect preview(final Object holder, final EffectDefinition definition, final Supplier<Vec3> at,
                                 final Supplier<Vec3> way) {
        EffectEngine.stop(holder);
        final ClientLevel level = Minecraft.getInstance().level;
        return EffectEngine.start(level, null, definition, holder, at, way, Optional::empty, 0, 1.0F);
    }

    private static void onBlock(final ResourceLocation id, final Object holder, final BlockPos pos) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        EffectFiles.get(id).ifPresent(definition -> EffectEngine.start(level, id, definition, holder,
                () -> EffectAnchors.of(level, pos, definition.anchor()).at(),
                () -> EffectAnchors.of(level, pos, definition.anchor()).way(), Optional::empty, 0, 1.0F));
    }

    private static void start(final ResourceLocation id, final Object holder, final Supplier<Vec3> at,
                              final Supplier<Vec3> way, final Supplier<Optional<Vec3>> to, final int colour,
                              final float scale) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        EffectFiles.get(id).ifPresent(definition -> EffectEngine.start(level, id, definition, holder, at,
                way.get().lengthSqr() > 0 ? way : () -> UP, to, colour, scale));
    }
}
