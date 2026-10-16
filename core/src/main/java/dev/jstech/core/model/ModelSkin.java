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

/**
 * The skeleton a skinned mesh bends with: the bones that move its corners, in the order the mesh's joints count them,
 * and the matrix that takes each bone's rest pose back to the model's own space.
 *
 * @param joints       the bones, by name, in the order the mesh's joints count them
 * @param inverseBinds a matrix for each, sixteen numbers in columns; empty when each bone's rest pose is the model's
 */
public record ModelSkin(List<String> joints, float[] inverseBinds) {

    public ModelSkin {
        joints = List.copyOf(joints);
        Objects.requireNonNull(inverseBinds, "inverseBinds");
        if (inverseBinds.length != 0 && inverseBinds.length != joints.size() * 16) {
            throw new IllegalArgumentException("a skin has a matrix for each of its bones, or none");
        }
    }
}
