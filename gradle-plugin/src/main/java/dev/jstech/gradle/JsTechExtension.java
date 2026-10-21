/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

/**
 * What a mod tells the Core's plugin, in its build's {@code jstech { }} block; each value has a default from the
 * project's {@code gradle.properties} where it can.
 *
 * <pre>{@code
 * jstech {
 *     licence = 'MIT'          // mod_license by default
 *     holder = 'Jane Doe'      // mod_authors by default
 * }
 * }</pre>
 */
public abstract class JsTechExtension {

    /** The mod's id: {@code mod_id}, or the project's name. Read as the plugin is applied, for the runs. */
    public abstract Property<String> getModId();

    /** The mod's name, as a licence header says the file is part of it: {@code mod_name}, or its id. */
    public abstract Property<String> getModName();

    /** The version of J's Core the mod is built on, and of its test kit: the plugin's own unless set. */
    public abstract Property<String> getCoreVersion();

    /** The licence every source's header names, an SPDX id: {@code mod_license}; none checks only for a header. */
    public abstract Property<String> getLicence();

    /** Whose the copyright is, as a header new to a file says: {@code mod_authors}. */
    public abstract Property<String> getHolder();

    /** The folder of the API's photo: {@code src/test/resources/api}. */
    public abstract DirectoryProperty getApiPhotos();

    /** How the mod's API class's file is named, the one that says its {@code VERSION}: any {@code *Api.java}. */
    public abstract Property<String> getApiClass();
}
