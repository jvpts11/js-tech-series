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
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import org.jetbrains.annotations.Nullable;

/**
 * The kinds of part that can be attached to a data cable's face.
 */
@TextHolder
public enum CablePartType implements IStableId {

    IMPORT(0, TextKey.of("jsc.bus.kind.import", "Import")),
    EXPORT(1, TextKey.of("jsc.bus.kind.export", "Export")),
    INPUT(2, TextKey.of("jsc.bus.kind.input", "Input")),
    RECEIVING(3, TextKey.of("jsc.bus.kind.receiving", "Receiving"));

    private final int id;
    private final TextKey name;

    private static final StableIds<CablePartType> IDS = StableIds.of(CablePartType.class);

    CablePartType(final int id, final TextKey name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public int id() {
        return id;
    }

    /** What a player calls this kind of part: "Import", "Export". */
    public Text text() {
        return name.text();
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
