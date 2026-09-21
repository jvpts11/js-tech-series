/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import dev.jstech.computers.storage.ServerStorageContents;
import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.IStableName;

/**
 * The three content kinds a physical medium can carry.
 *
 * <ul>
 *   <li>{@link #OS_INSTALL}, a bootable OS installer; the payload identifies the OS by id.</li>
 *   <li>{@link #PROGRAM_INSTALL}, an add-on program installer; the payload identifies the program.</li>
 *   <li>{@link #DATA}, a portable storage snapshot; the content is a {@link ServerStorageContents} value.</li>
 * </ul>
 *
 * This enum is intentionally free of Minecraft and NeoForge imports so it can be used in pure-JUnit
 * tests without loading the game runtime. The medium's item component keeps the kind as its
 * {@link #serializedName()} and syncs it by {@link #id()}, through the codecs {@code ComputingModule}
 * registers the component with.
 */
public enum MediaKind implements IStableId, IStableName {
    OS_INSTALL(0, "os_install"),
    PROGRAM_INSTALL(1, "program_install"),
    DATA(2, "data");

    private final int id;
    private final String serializedName;

    MediaKind(final int id, final String serializedName) {
        this.id = id;
        this.serializedName = serializedName;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }
}
