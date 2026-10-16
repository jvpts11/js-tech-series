/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * A model as the Core holds it, whatever file it came from: its bones in a hierarchy, each with its cubes, meshes and
 * locators, the skins its meshes bend with, the textures it names, and the animations its file carries besides those
 * of animation files of their own. Its space is the model's own, in pixels, a sixteenth of a block, Y up, north toward
 * -Z, its X as a player sees it. Read by {@link ModelReaders}; pure, so it is tested without the game.
 *
 * @param name          what it is called, its file's name unless the file says
 * @param textureWidth  how wide the texture its cubes' pictures are counted on is, in pixels
 * @param textureHeight how tall it is
 * @param bones         its bones, every parent before its children
 * @param skins         the skins its meshes bend with, by the name of the bone they hang from
 * @param textures      the textures it names or carries
 * @param animations    the animations its file carries
 */
public record ModelData(String name, int textureWidth, int textureHeight, List<ModelBone> bones,
                        Map<String, ModelSkin> skins, List<ModelTexture> textures, List<ModelAnimation> animations) {

    public ModelData {
        Objects.requireNonNull(name, "name");
        bones = ordered(bones);
        skins = Map.copyOf(skins);
        textures = List.copyOf(textures);
        animations = List.copyOf(animations);
    }

    /** The bone named {@code name}, if it has one. */
    public Optional<ModelBone> bone(final String name) {
        for (final ModelBone bone : this.bones) {
            if (bone.name().equals(name)) {
                return Optional.of(bone);
            }
        }
        return Optional.empty();
    }

    /** The bones that hang from {@code parent}, or the roots for none, in their order. */
    public List<ModelBone> childrenOf(@Nullable final String parent) {
        final List<ModelBone> out = new ArrayList<>();
        for (final ModelBone bone : this.bones) {
            if (Objects.equals(bone.parent(), parent)) {
                out.add(bone);
            }
        }
        return out;
    }

    /** The animation called {@code name} its file carries, if it carries one. */
    public Optional<ModelAnimation> animation(final String name) {
        for (final ModelAnimation animation : this.animations) {
            if (animation.name().equals(name)) {
                return Optional.of(animation);
            }
        }
        return Optional.empty();
    }

    /** Every locator of the model, by name. */
    public Map<String, ModelLocator> locators() {
        final Map<String, ModelLocator> out = new LinkedHashMap<>();
        for (final ModelBone bone : this.bones) {
            for (final ModelLocator locator : bone.locators()) {
                out.putIfAbsent(locator.name(), locator);
            }
        }
        return out;
    }

    /*
     * The bones with each parent before its children, each in the order it came; a bone whose parent is not in the
     * model hangs from the root, so a file that names a parent it lacks still reads.
     */
    private static List<ModelBone> ordered(final List<ModelBone> bones) {
        final Map<String, ModelBone> byName = new LinkedHashMap<>();
        for (final ModelBone bone : bones) {
            if (byName.putIfAbsent(bone.name(), bone) != null) {
                throw new ModelFormatException("two bones are called " + bone.name());
            }
        }
        final List<ModelBone> out = new ArrayList<>(bones.size());
        final Set<String> placed = new HashSet<>();
        int before = -1;
        while (out.size() < bones.size() && out.size() != before) {
            before = out.size();
            for (final ModelBone bone : byName.values()) {
                if (placed.contains(bone.name())) {
                    continue;
                }
                final String parent = bone.parent();
                if (parent == null || !byName.containsKey(parent) || placed.contains(parent)) {
                    out.add(parent != null && !byName.containsKey(parent) ? orphan(bone) : bone);
                    placed.add(bone.name());
                }
            }
        }
        if (out.size() < bones.size()) {
            throw new ModelFormatException("the bones hang from each other in a ring");
        }
        return List.copyOf(out);
    }

    private static ModelBone orphan(final ModelBone bone) {
        return new ModelBone(bone.name(), null, bone.pivot(), bone.rotation(), bone.euler(), bone.translation(),
                bone.scale(), bone.cubes(), bone.meshes(), bone.locators(), bone.hidden());
    }
}
