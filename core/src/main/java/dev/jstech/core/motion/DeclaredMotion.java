/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.motion;

import net.minecraft.resources.ResourceLocation;

/**
 * A system's motion profile as a mod declares it: the profile its code gives, and the one in force, which a resource
 * pack's file replaces.
 */
public final class DeclaredMotion {

    private final String namespace;
    private final String name;
    private final ResourceLocation file;
    private final MotionProfile declared;
    private volatile MotionProfile current;

    DeclaredMotion(final String namespace, final String name, final MotionProfile declared) {
        this.namespace = namespace;
        this.name = name;
        // Built now so a name a resource location refuses fails where the motion is declared, not at the first reload.
        this.file = ResourceLocation.fromNamespaceAndPath(namespace, "motions/" + name + ".json");
        this.declared = declared;
        this.current = declared;
    }

    /** The profile in force: a pack's when one gives it, the declared one otherwise. */
    public MotionProfile get() {
        return current;
    }

    /** The profile the code declares, which the data generation writes and a pack replaces. */
    public MotionProfile declared() {
        return declared;
    }

    public String namespace() {
        return namespace;
    }

    public String name() {
        return name;
    }

    /** Its file, {@code assets/<namespace>/motions/<name>.json}. */
    public ResourceLocation file() {
        return file;
    }

    /** Puts a pack's profile in force. */
    public void load(final MotionProfile profile) {
        this.current = profile;
    }

    /** Puts the declared profile back in force. */
    public void reset() {
        this.current = declared;
    }
}
