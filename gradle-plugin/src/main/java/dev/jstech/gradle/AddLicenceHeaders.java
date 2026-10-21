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
import java.time.Year;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

/**
 * Writes the mod's licence header above every source that has none, this year's, and leaves every other source as it
 * is: a header naming another licence is the author's to change.
 */
@DisableCachingByDefault(because = "It changes the sources in place")
public abstract class AddLicenceHeaders extends DefaultTask {

    /** The sources looked at: every Java source of every source set. */
    @Internal
    public abstract ConfigurableFileCollection getSources();

    @Input
    public abstract Property<String> getLicence();

    @Input
    public abstract Property<String> getHolder();

    @Input
    public abstract Property<String> getWork();

    @TaskAction
    public void add() {
        if (!getLicence().isPresent() || !getHolder().isPresent()) {
            throw new GradleException("A licence header needs the mod's licence and whose it is: set jstech.licence"
                    + " and jstech.holder, or mod_license and mod_authors in gradle.properties");
        }
        final int year = Year.now().getValue();
        int written = 0;
        for (final File file : getSources().getFiles()) {
            final String source = CheckLicenceHeaders.read(file);
            final String headed = LicenceHeaders.withHeader(source, getLicence().get(), year, getHolder().get(),
                    getWork().get());
            if (!headed.equals(source)) {
                write(file, headed);
                written++;
            }
        }
        getLogger().lifecycle("Wrote the licence header into {} sources", written);
    }

    private static void write(final File file, final String text) {
        try {
            Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
