/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.motion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every system's motion profile, as the mods declare them.
 *
 * <p>A mod declares each of its systems' profiles once, with the timings of the real system, as it declares a
 * palette. The data generation writes each to {@code assets/<mod>/motions/<name>.json}, the file a resource pack puts
 * its own in place of to slow a system down, speed it up or keep it still; the client reads the files back each time
 * the packs load.
 *
 * <pre>{@code
 * public static final DeclaredMotion FRAMES_XP = MotionProfiles.declare("jsc", "frames_xp", MotionProfile.builder()
 *         .kind(MotionKinds.MENU_SHOW, MotionSpec.of(MotionStyles.SLIDE, 200, IEasing.named("ease-out"), "menus"))
 *         .build());
 * }</pre>
 */
public final class MotionProfiles {

    private static final Map<String, DeclaredMotion> DECLARED = new LinkedHashMap<>();

    private MotionProfiles() {
    }

    /**
     * Declares a system's profile under {@code namespace:name}.
     *
     * @throws IllegalStateException when that profile is declared twice
     */
    public static synchronized DeclaredMotion declare(final String namespace, final String name,
                                                      final MotionProfile profile) {
        final String id = namespace + ":" + name;
        if (DECLARED.containsKey(id)) {
            throw new IllegalStateException("the motion profile " + id + " is declared twice");
        }
        final DeclaredMotion declared = new DeclaredMotion(namespace, name, profile);
        DECLARED.put(id, declared);
        return declared;
    }

    /** Every declared profile, in the order they were declared. */
    public static synchronized Collection<DeclaredMotion> all() {
        return List.copyOf(DECLARED.values());
    }

    /** The profiles one mod declares. */
    public static synchronized List<DeclaredMotion> of(final String namespace) {
        final List<DeclaredMotion> out = new ArrayList<>();
        for (final DeclaredMotion declared : DECLARED.values()) {
            if (declared.namespace().equals(namespace)) {
                out.add(declared);
            }
        }
        return out;
    }
}
