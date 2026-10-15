/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * How one end of a line meets the block it plugs into: as the block itself says (normal: a machine's output gives, its
 * input takes, a tank both), pushing what the line carries into it and never drawing from it, pulling from it and never
 * filling it, or not at all.
 */
@TextHolder
public enum EndMode implements IStableName {

    NORMAL("normal", TextKey.of("jscore.end_mode.normal", "Normal")),
    PUSH("push", TextKey.of("jscore.end_mode.push", "Push")),
    PULL("pull", TextKey.of("jscore.end_mode.pull", "Pull")),
    OFF("off", TextKey.of("jscore.end_mode.off", "Off"));

    private final String serializedName;
    private final TextKey name;

    EndMode(final String serializedName, final TextKey name) {
        this.serializedName = serializedName;
        this.name = name;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** What a player calls it. */
    public TextKey text() {
        return this.name;
    }

    /** Whether the line may draw from the block. */
    public boolean draws() {
        return this == NORMAL || this == PULL;
    }

    /** Whether the line may fill the block. */
    public boolean fills() {
        return this == NORMAL || this == PUSH;
    }

    /** The next mode, as a wrench that goes round them sets it: normal, push, pull, off, normal. */
    public EndMode next() {
        return switch (this) {
            case NORMAL -> PUSH;
            case PUSH -> PULL;
            case PULL -> OFF;
            case OFF -> NORMAL;
        };
    }
}
