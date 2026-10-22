/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What redstone does to one end of a line: nothing, the end working as its mode says whatever reaches the cable block;
 * the end working only while a signal reaches the cable block, off without one; or working only while none does, off
 * while one does.
 */
@TextHolder
public enum EndRedstone implements IStableName {

    IGNORED("ignored", TextKey.of("jscore.end_redstone.ignored", "Redstone ignored")),
    WITH_SIGNAL("with_signal", TextKey.of("jscore.end_redstone.with_signal", "Works with a redstone signal")),
    WITHOUT_SIGNAL("without_signal", TextKey.of("jscore.end_redstone.without_signal",
            "Works without a redstone signal"));

    private final String serializedName;
    private final TextKey name;

    EndRedstone(final String serializedName, final TextKey name) {
        this.serializedName = serializedName;
        this.name = name;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** What a player reads of it. */
    public TextKey text() {
        return this.name;
    }

    /** Whether the end works while a signal reaches its cable block, {@code powered}, or none does. */
    public boolean lets(final boolean powered) {
        return switch (this) {
            case IGNORED -> true;
            case WITH_SIGNAL -> powered;
            case WITHOUT_SIGNAL -> !powered;
        };
    }

    /** The next, as the end's screen goes round them: ignored, with a signal, without one, ignored. */
    public EndRedstone next() {
        return switch (this) {
            case IGNORED -> WITH_SIGNAL;
            case WITH_SIGNAL -> WITHOUT_SIGNAL;
            case WITHOUT_SIGNAL -> IGNORED;
        };
    }
}
