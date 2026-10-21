/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

/** Says what the Core's plugin set the mod up with: its id, the Core it is built on, its runs and its tasks. */
@DisableCachingByDefault(because = "It only says what is set up")
public abstract class JsTechInfo extends DefaultTask {

    @Input
    public abstract Property<String> getModId();

    @Input
    public abstract Property<String> getCore();

    @Input
    public abstract Property<String> getTestKit();

    @Input
    public abstract ListProperty<String> getRuns();

    @TaskAction
    public void say() {
        getLogger().lifecycle("mod: {}", getModId().get());
        getLogger().lifecycle("core: {}", getCore().get());
        getLogger().lifecycle("test kit: {}", getTestKit().get());
        getLogger().lifecycle("runs: {}", String.join(", ", getRuns().get()));
    }
}
