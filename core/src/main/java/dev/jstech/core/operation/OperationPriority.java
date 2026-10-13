/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.operation;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableIds;
import dev.jstech.core.id.StableNames;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

import java.util.Locale;
import java.util.Optional;

/**
 * Scheduling priority of an Operation. The Mainframe hands its queue slots to the highest level first and
 * keeps submission order inside a level; a manual request and an automation job both start at
 * {@link #MEDIUM} unless the requester says otherwise. Levels are declared lowest first, so {@code compareTo}
 * orders them.
 */
@TextHolder
public enum OperationPriority implements IStableId, IStableName {
    /*
     * Numbered in tens rather than one after another, so that a level can be put between two of these later
     * without any of the numbers already written into a world having to move.
     */
    LOW(10, "low", TextKey.of("jscore.priority.low", "LOW")),
    MEDIUM_LOW(20, "medium_low", TextKey.of("jscore.priority.medium_low", "MED-")),
    MEDIUM(30, "medium", TextKey.of("jscore.priority.medium", "MED")),
    MEDIUM_HIGH(40, "medium_high", TextKey.of("jscore.priority.medium_high", "MED+")),
    HIGH(50, "high", TextKey.of("jscore.priority.high", "HIGH"));

    private final int id;
    private final String serializedName;
    private final TextKey label;

    /** The level every Operation starts at when the requester does not choose one. */
    public static final OperationPriority DEFAULT = MEDIUM;

    private static final StableIds<OperationPriority> IDS = StableIds.of(OperationPriority.class);
    private static final StableNames<OperationPriority> NAMES = StableNames.of(OperationPriority.class);

    OperationPriority(final int id, final String serializedName, final TextKey label) {
        this.id = id;
        this.serializedName = serializedName;
        this.label = label;
    }

    @Override
    public int id() {
        return id;
    }

    /** The keyword a statement, a command or a ComputerCraft call names the level by. */
    @Override
    public String serializedName() {
        return serializedName;
    }

    /** A four-character tag for dense views (task lists, dialogs), in the English a machine writes down. */
    public String label() {
        return label.english();
    }

    /** The same tag as text, for a player to read in their language. */
    public Text text() {
        return label.text();
    }

    /** The next level up, saturating at {@link #HIGH}. */
    public OperationPriority raise() {
        return switch (this) {
            case LOW -> MEDIUM_LOW;
            case MEDIUM_LOW -> MEDIUM;
            case MEDIUM -> MEDIUM_HIGH;
            case MEDIUM_HIGH, HIGH -> HIGH;
        };
    }

    /** The next level down, saturating at {@link #LOW}. */
    public OperationPriority lower() {
        return switch (this) {
            case LOW, MEDIUM_LOW -> LOW;
            case MEDIUM -> MEDIUM_LOW;
            case MEDIUM_HIGH -> MEDIUM;
            case HIGH -> MEDIUM_HIGH;
        };
    }

    /** The level that declares {@code id}; an id no level declares reads as {@link #DEFAULT}, so a stale byte never throws. */
    public static OperationPriority byId(final int id) {
        return IDS.byId(id, DEFAULT);
    }

    /**
     * Resolves a keyword as typed in a statement or a command, case-insensitively: the levels' names, their
     * hyphenated spellings ({@code medium-high}), and {@code normal} as an alias of {@link #MEDIUM}.
     */
    public static Optional<OperationPriority> fromKeyword(final String keyword) {
        if (keyword == null) {
            return Optional.empty();
        }
        final String normalized = keyword.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if (normalized.equals("normal")) {
            return Optional.of(MEDIUM);
        }
        return Optional.ofNullable(NAMES.find(normalized));
    }
}
