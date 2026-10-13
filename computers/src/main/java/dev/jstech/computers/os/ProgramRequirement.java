/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.engine.EngineCapability;
import dev.jstech.core.id.StableCodecs;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * What a program needs of the network it runs on, checked when it is opened rather than when it is installed.
 *
 * <p>A software house's tools are written for its own engine: a studio made for one engine says so by naming it,
 * and the oldest version of it that it works with. A program written for any engine that offers something names
 * the capabilities instead. Installed on a network that cannot give it what it needs, a program stays installed
 * and says, when it is opened, what it did not find.
 *
 * @param engine       the package of the engine it is written for, or {@code null} when any engine will do
 * @param minVersion   the oldest version of that engine it works with; empty for any version
 * @param capabilities what the running engine must offer
 */
@ApiStatus.Experimental
public record ProgramRequirement(@Nullable ResourceLocation engine, String minVersion,
                                 Set<EngineCapability> capabilities) {

    /** What a program that needs nothing of the network needs. */
    public static final ProgramRequirement NONE = new ProgramRequirement(null, "", Set.of());

    public static final Codec<ProgramRequirement> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC.optionalFieldOf("engine").forGetter(r -> Optional.ofNullable(r.engine())),
            Codec.STRING.optionalFieldOf("min_version", "").forGetter(ProgramRequirement::minVersion),
            StableCodecs.byName(EngineCapability.class).listOf().xmap(Set::copyOf, List::copyOf)
                    .optionalFieldOf("capabilities", Set.of()).forGetter(ProgramRequirement::capabilities)
    ).apply(inst, (engine, minVersion, capabilities) ->
            new ProgramRequirement(engine.orElse(null), minVersion, capabilities)));

    public ProgramRequirement {
        minVersion = minVersion == null ? "" : minVersion;
        capabilities = capabilities.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(capabilities));
    }

    /** A program written for one engine, from {@code minVersion} on. */
    public static ProgramRequirement engine(final ResourceLocation engine, final String minVersion) {
        return new ProgramRequirement(engine, minVersion, Set.of());
    }

    /** A program written for any engine that offers every one of {@code capabilities}. */
    public static ProgramRequirement capabilities(final Set<EngineCapability> capabilities) {
        return new ProgramRequirement(null, "", capabilities);
    }

    /** Whether the program needs nothing of the network. */
    public boolean none() {
        return engine == null && capabilities.isEmpty();
    }
}
