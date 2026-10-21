/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration;

import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.optional.OptionalMod;
import java.util.Optional;
import net.neoforged.fml.ModList;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.slf4j.Logger;

/**
 * The versions of another mod the Core's work with it is tested against: a work that reaches into that mod's own
 * classes, and so may break as they change, starts only on those, and on any other is left off with a warning in the
 * log, the game going on without it.
 */
public final class PinnedVersions {

    private PinnedVersions() {
    }

    /**
     * Whether {@code mod} is installed in a version {@code versions} covers, a range as Maven writes it
     * ({@code "[6.0.11,6.1)"}); installed in another, it says so in the log.
     */
    public static boolean covers(final OptionalMod mod, final String versions) {
        if (!mod.isLoaded()) {
            return false;
        }
        final Optional<ArtifactVersion> version = ModList.get().getModContainerById(mod.modid())
                .map(container -> container.getModInfo().getVersion());
        try {
            if (version.isPresent() && VersionRange.createFromVersionSpec(versions).containsVersion(version.get())) {
                return true;
            }
        } catch (final InvalidVersionSpecificationException wrong) {
            throw new IllegalArgumentException("the versions " + versions + " are no range of versions", wrong);
        }
        final Logger log = CoreLogs.INTEGRATIONS.logger();
        log.warn("{} {} is not a version the Core is tested with ({}), so the Core leaves its work with it off",
                mod.modid(), version.map(Object::toString).orElse("?"), versions);
        return false;
    }
}
