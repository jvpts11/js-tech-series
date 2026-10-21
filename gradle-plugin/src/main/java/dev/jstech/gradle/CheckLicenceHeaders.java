/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.gradle;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/** Fails when a source of the mod has no licence header, or one naming another licence than the mod's. */
public abstract class CheckLicenceHeaders extends DefaultTask {

    /** The sources looked at: every Java source of every source set. */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSources();

    /** The licence every header names, an SPDX id; none asks only for a header. */
    @Input
    @Optional
    public abstract Property<String> getLicence();

    @TaskAction
    public void check() {
        final String licence = getLicence().getOrNull();
        final List<String> wrong = new ArrayList<>();
        for (final File file : getSources().getFiles()) {
            LicenceHeaders.problemOf(read(file), licence).ifPresent(why -> wrong.add(file + " " + why));
        }
        if (!wrong.isEmpty()) {
            throw new GradleException(wrong.size() + " sources have no licence header as the mod's"
                    + (licence == null ? "" : ", " + licence) + "; addLicenceHeaders writes the ones missing:\n  "
                    + String.join("\n  ", wrong));
        }
    }

    static String read(final File file) {
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
