/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block.part;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;
import org.jetbrains.annotations.Nullable;

/**
 * The kinds of part that can be attached to a data cable's face.
 */
public enum CablePartType implements IStableId {

    IMPORT(0),
    EXPORT(1),
    INPUT(2),
    RECEIVING(3);

    private static final StableIds<CablePartType> IDS = StableIds.of(CablePartType.class);

    private final int id;

    CablePartType(final int id) {
        this.id = id;
    }

    @Override
    public int id() {
        return id;
    }

    /** The part type a save or the cable's sync names by {@code id}, or null for an id no type declares. */
    @Nullable
    public static CablePartType find(final int id) {
        return IDS.find(id);
    }

    public ICablePart create() {
        return switch (this) {
            case IMPORT -> new ImportBusPart();
            case EXPORT -> new ExportBusPart();
            case INPUT -> new InputBusPart();
            case RECEIVING -> new ReceivingBusPart();
        };
    }
}
