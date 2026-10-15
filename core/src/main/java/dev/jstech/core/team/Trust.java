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

/**
 * How far the owner of a thing trusts a player they name, beyond the thing's access: each level holds the ones under
 * it, so whoever may break it may set it up and use it too.
 */
@TextHolder
public enum Trust implements IStableId, IStableName {
    /** Use it: open it, put things in and take them out. */
    USE(0, "use", TextKey.of("jscore.trust.use", "Use")),
    /** Set it up as well: its settings, its sides, who else may use it. */
    SET_UP(1, "set_up", TextKey.of("jscore.trust.set_up", "Use and set up")),
    /** Break it as well. */
    BREAK(2, "break", TextKey.of("jscore.trust.break", "Use, set up and break"));

    private final int id;
    private final String serializedName;
    private final TextKey name;

    Trust(final int id, final String serializedName, final TextKey name) {
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

    /** Whether this level holds {@code other}: a player trusted to break may use. */
    public boolean holds(final Trust other) {
        return id >= other.id;
    }

    /** What it lets a player do, as a player reads it. */
    public Text text() {
        return name.text();
    }
}
