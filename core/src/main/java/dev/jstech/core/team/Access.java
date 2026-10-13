/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.team;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** Who may use a thing besides its owner. */
@TextHolder
public enum Access implements IStableId, IStableName {
    /** Its owner alone. */
    PRIVATE(0, "private", TextKey.of("jscore.access.private", "Only its owner")),
    /** Its owner and whoever is on the owner's team. */
    TEAM(1, "team", TextKey.of("jscore.access.team", "Its owner's team")),
    /** Anybody. */
    PUBLIC(2, "public", TextKey.of("jscore.access.public", "Everyone"));

    private final int id;
    private final String serializedName;
    private final TextKey name;

    Access(final int id, final String serializedName, final TextKey name) {
        this.id = id;
        this.serializedName = serializedName;
        this.name = name;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }

    /** Who it lets in, as a player reads it. */
    public Text text() {
        return name.text();
    }
}
