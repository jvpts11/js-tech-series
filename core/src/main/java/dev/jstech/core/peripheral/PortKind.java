/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.peripheral;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableIds;
import dev.jstech.core.id.StableNames;
import org.jetbrains.annotations.Nullable;

/**
 * The kind of port a peripheral takes on the machine it is linked to, as on a real computer: a screen takes a video
 * output, a loudspeaker the audio output, and everything else (drives, writers, gateways, sensors) a device port.
 * Each machine has so many of each kind, and a peripheral finds a free one of its own kind or is not linked.
 *
 * <p>Each kind is numbered and named, the number being what an owner saves its links by.
 */
public enum PortKind implements IStableId, IStableName {

    DEVICE(0, "device"),
    VIDEO(1, "video"),
    AUDIO(2, "audio");

    private final int id;
    private final String serializedName;

    private static final StableIds<PortKind> IDS = StableIds.of(PortKind.class);
    private static final StableNames<PortKind> NAMES = StableNames.of(PortKind.class);

    PortKind(final int id, final String serializedName) {
        this.id = id;
        this.serializedName = serializedName;
    }

    /** The kind numbered {@code id}, or null when no kind is. */
    public static @Nullable PortKind byId(final int id) {
        return IDS.find(id);
    }

    /** The kind named {@code name}, or null when no kind is. */
    public static @Nullable PortKind byName(final @Nullable String name) {
        return NAMES.find(name);
    }

    @Override
    public int id() {
        return this.id;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }
}
