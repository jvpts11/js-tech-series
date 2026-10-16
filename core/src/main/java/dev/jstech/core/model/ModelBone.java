/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A bone of a model: a named part, turned about its pivot, carrying the bones under it, its cubes, its meshes and its
 * locators. Its rest pose is its place, its turn and its size before any animation; an animation moves it from there.
 * A bone whose name ends in {@code _glow} glows in the dark, everything on it drawn at full light.
 *
 * @param name      its name, by which animations and code move it
 * @param parent    the bone it hangs from, or none for a root
 * @param pivot     the point it turns about, in the model's own space and its pixels
 * @param rotation  its turn at rest
 * @param euler     its turn at rest in degrees about X, Y and Z, X first, as Bedrock and Blockbench write it; the
 *                  angles an animation of those formats adds to
 * @param translation how far it is moved at rest, besides its pivot, in pixels: what glTF and FBX nodes carry
 * @param scale     its size at rest
 * @param cubes     its cubes
 * @param meshes    its meshes
 * @param locators  its locators
 * @param hidden    whether it is hidden until something shows it
 */
public record ModelBone(String name, @Nullable String parent, Vector3fc pivot, Quaternionfc rotation,
                        Vector3fc euler, Vector3fc translation, Vector3fc scale, List<ModelCube> cubes,
                        List<ModelMesh> meshes, List<ModelLocator> locators, boolean hidden) {

    /** The suffix of a bone that glows in the dark. */
    public static final String GLOW = "_glow";

    public ModelBone {
        Objects.requireNonNull(name, "name");
        pivot = new Vector3f(Objects.requireNonNull(pivot, "pivot"));
        rotation = new Quaternionf(Objects.requireNonNull(rotation, "rotation"));
        euler = new Vector3f(Objects.requireNonNull(euler, "euler"));
        translation = new Vector3f(Objects.requireNonNull(translation, "translation"));
        scale = new Vector3f(Objects.requireNonNull(scale, "scale"));
        cubes = List.copyOf(cubes);
        meshes = List.copyOf(meshes);
        locators = List.copyOf(locators);
    }

    /**
     * A bone turned at rest by {@code euler}, in degrees about X, Y and Z, X first, as Bedrock and Blockbench write it,
     * and not moved or sized otherwise.
     */
    public static ModelBone ofEuler(final String name, @Nullable final String parent, final Vector3fc pivot,
                                    final Vector3fc euler, final List<ModelCube> cubes, final List<ModelMesh> meshes,
                                    final List<ModelLocator> locators, final boolean hidden) {
        return new ModelBone(name, parent, pivot, Rotations.ofEuler(euler), euler, new Vector3f(),
                new Vector3f(1, 1, 1), cubes, meshes, locators, hidden);
    }

    /** Whether everything on it glows in the dark. */
    public boolean glows() {
        return this.name.endsWith(GLOW);
    }
}
