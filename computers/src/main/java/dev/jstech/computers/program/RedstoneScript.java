/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

/**
 * The lines that set a Redstone Interface the way it is set now, written the two ways software writes them, as its
 * screen shows them under Software: an IQL statement and a Sigma call. Pure, so it is tested without the game.
 */
public final class RedstoneScript {

    private RedstoneScript() {
    }

    /** The IQL statement: {@code SET REDSTONE 'Gate' OUT 15}, or {@code SET REDSTONE 'Gate' IN}. */
    public static String iql(final String name, final boolean emits, final int strength) {
        return "SET REDSTONE " + quoted(name) + (emits ? " OUT " + strength : " IN");
    }

    /** The Sigma call: {@code redstone("Gate").Out(15);}, or {@code redstone("Gate").In();}. */
    public static String sigma(final String name, final boolean emits, final int strength) {
        return "redstone(" + text(name) + ")." + (emits ? "Out(" + strength + ");" : "In();");
    }

    /* A name in IQL's single quotes, a quote in it doubled. */
    private static String quoted(final String name) {
        return "'" + name.replace("'", "''") + "'";
    }

    /* A string in Sigma's double quotes, a quote or a backslash in it escaped. */
    private static String text(final String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
