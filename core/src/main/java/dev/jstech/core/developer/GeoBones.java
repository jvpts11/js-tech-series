/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The bones of a geometry model and the bones its animations move, read from their files as Blockbench writes them:
 * a model's {@code minecraft:geometry} holds its bones by name, an animation file's {@code animations} each animation
 * and, under it, the bones it moves. What is not of that form is read as no bones.
 */
public final class GeoBones {

    private static final String GEOMETRY = "minecraft:geometry";
    private static final String BONES = "bones";
    private static final String NAME = "name";
    private static final String ANIMATIONS = "animations";

    private GeoBones() {
    }

    /** The names of the bones of every geometry of a model file. */
    public static List<String> ofModel(final JsonObject model) {
        final List<String> out = new ArrayList<>();
        if (!(model.get(GEOMETRY) instanceof JsonArray geometries)) {
            return out;
        }
        for (final JsonElement geometry : geometries) {
            if (geometry instanceof JsonObject whole && whole.get(BONES) instanceof JsonArray bones) {
                for (final JsonElement bone : bones) {
                    final JsonElement name = bone instanceof JsonObject named ? named.get(NAME) : null;
                    if (name != null && name.isJsonPrimitive()) {
                        out.add(name.getAsString());
                    }
                }
            }
        }
        return out;
    }

    /**
     * Every bone each animation of an animation file moves, by the animation's short name: the last part of
     * {@code animation.computer.work}, {@code work}.
     */
    public static List<Moved> ofAnimations(final JsonObject file) {
        final List<Moved> out = new ArrayList<>();
        if (!(file.get(ANIMATIONS) instanceof JsonObject animations)) {
            return out;
        }
        animations.entrySet().forEach(animation -> {
            if (animation.getValue() instanceof JsonObject body && body.get(BONES) instanceof JsonObject bones) {
                final String name = animation.getKey().substring(animation.getKey().lastIndexOf('.') + 1);
                bones.keySet().forEach(bone -> out.add(new Moved(name, bone)));
            }
        });
        return out;
    }

    /**
     * A bone an animation moves.
     *
     * @param animation the animation's short name
     * @param bone      the bone's name
     */
    public record Moved(String animation, String bone) {

        public Moved {
            Objects.requireNonNull(animation, "animation");
            Objects.requireNonNull(bone, "bone");
        }
    }
}
