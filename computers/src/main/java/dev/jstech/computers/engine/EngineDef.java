/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine;

import dev.jstech.core.tier.HardwareEra;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A Network Operations Engine: the software a Mainframe runs to turn what is asked of its network into the
 * Operations that do it.
 *
 * <p>An engine is installed as a package, so it is named by the program that installs it, and that program's
 * name is the engine's. It speaks a dialect of the network's language (every dialect accepts the language's core),
 * ships in a version for each age of Mainframe it was made for, and offers the extras it lists. The work it
 * plans runs on the Operations core whichever engine planned it, so an Operation never belongs to the engine that
 * made it.
 *
 * @param program  the id of the package that installs the engine, which is also the engine's id
 * @param dialect  the name of the language it speaks, shown where the engine is managed
 * @param versions the version it ships in, by the age of the Mainframe it is installed on
 * @param extras   what it offers beyond what every engine answers
 */
@ApiStatus.Experimental
public record EngineDef(ResourceLocation program, String dialect, Map<HardwareEra, String> versions,
                        Set<EngineCapability> extras) {

    public EngineDef {
        if (versions.isEmpty()) {
            throw new IllegalArgumentException("an engine is made for at least one age: " + program);
        }
        versions = Map.copyOf(new EnumMap<>(versions));
        extras = extras.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(extras));
    }

    /** The version a Mainframe of {@code era} installs, or {@code null} when the engine was not made for it. */
    @Nullable
    public String versionFor(final HardwareEra era) {
        return versions.get(era);
    }

    /** Whether the engine was made for a Mainframe of {@code era}. */
    public boolean madeFor(final HardwareEra era) {
        return versions.containsKey(era);
    }

    /** Whether the engine offers {@code capability}. */
    public boolean offers(final EngineCapability capability) {
        return extras.contains(capability);
    }
}
