/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/**
 * The single per-type colour every Operations view uses, so an Operation reads the same across the desktop
 * apps: writes ({@code INSERT}/{@code CRAFT}) are amber, destructive ops ({@code DELETE}/{@code DROP}) are
 * red, {@code MOVE} is green, the maintenance ops ({@code ANALYZE}/{@code REINDEX}/{@code VACUUM}) are grey,
 * and reads ({@code SELECT}) plus anything else take the blue accent. The colours mirror the network
 * terminal's own type palette so the whole mod agrees. Pure ARGB, no Minecraft type. The colours are the palette
 * {@code jsc:operation/types}, which a resource pack can recolour.
 */
@PaletteHolder
public final class OperationPalette {

    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "operation/types",
            new Colours(0xFFE0A020, 0xFFD1495B, 0xFF2EA043, 0xFF9098A8, 0xFF3A6AE0));

    private OperationPalette() {
    }

    /** The ARGB colour associated with an Operation of the given {@link OperationRecord} type. */
    public static int colorFor(final byte type) {
        final Colours c = PALETTE.get();
        return switch (type) {
            case OperationRecord.TYPE_INSERT, OperationRecord.TYPE_CRAFT -> c.write();
            case OperationRecord.TYPE_DELETE, OperationRecord.TYPE_DROP -> c.destroy();
            case OperationRecord.TYPE_MOVE -> c.move();
            case OperationRecord.TYPE_ANALYZE, OperationRecord.TYPE_REINDEX, OperationRecord.TYPE_VACUUM -> c.upkeep();
            default -> c.read();
        };
    }

    /** The short upper-case label for an Operation type, matching the network terminal. */
    public static String labelFor(final byte type) {
        return switch (type) {
            case OperationRecord.TYPE_SELECT -> "SELECT";
            case OperationRecord.TYPE_INSERT -> "INSERT";
            case OperationRecord.TYPE_DELETE -> "DELETE";
            case OperationRecord.TYPE_MOVE -> "MOVE";
            case OperationRecord.TYPE_ANALYZE -> "ANALYZE";
            case OperationRecord.TYPE_REINDEX -> "REINDEX";
            case OperationRecord.TYPE_VACUUM -> "VACUUM";
            case OperationRecord.TYPE_DROP -> "DROP";
            case OperationRecord.TYPE_CRAFT -> "CRAFT";
            default -> "OP";
        };
    }

    /** The colour of a write, of what destroys, of a move, of upkeep, and of a read or anything else. */
    private record Colours(int write, int destroy, int move, int upkeep, int read) {
    }
}
