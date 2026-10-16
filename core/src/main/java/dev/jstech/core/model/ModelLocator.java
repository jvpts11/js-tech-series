/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.Objects;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A named point of a model, moving with its bone: where particles come from, a sound plays, or something is held,
 * an item or a block drawn there.
 *
 * @param name     its name
 * @param offset   where it is, in the model's own space and its pixels
 * @param rotation how what is held there is turned, in degrees about X, Y and Z
 */
public record ModelLocator(String name, Vector3fc offset, Vector3fc rotation) {

    public ModelLocator {
        Objects.requireNonNull(name, "name");
        offset = new Vector3f(Objects.requireNonNull(offset, "offset"));
        rotation = new Vector3f(Objects.requireNonNull(rotation, "rotation"));
    }
}
