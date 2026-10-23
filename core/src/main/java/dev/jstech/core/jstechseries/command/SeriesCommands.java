/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.command;

import dev.jstech.core.command.CommandRoot;
import java.util.List;
import java.util.Optional;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;

/**
 * The commands of every mod of the series, under one root, {@code /jstech}, the Core's own ({@link CommandRoot#CORE}):
 * a mod of the series declares {@code /jstech <name>} once, from its constructor, with who may run it and how its
 * arguments go. The series takes one name from the command line rather than one a mod.
 *
 * <pre>{@code
 * SeriesCommands.declare("rockets", SeriesCommands.ADMINS, (node, context) -> node
 *         .executes(source -> listed(source.getSource())));
 * }</pre>
 */
public final class SeriesCommands {

    /** Anybody may run it. */
    public static final int EVERYONE = CommandRoot.EVERYONE;
    /** The operators who may change the game: the level of {@code /give} and {@code /gamemode}. */
    public static final int GAME_MASTERS = CommandRoot.GAME_MASTERS;
    /** The operators who look after the server: the level of {@code /ban}. */
    public static final int ADMINS = CommandRoot.ADMINS;
    /** The root every command of the series hangs from. */
    public static final String ROOT = CommandRoot.CORE.name();

    private SeriesCommands() {
    }

    /**
     * Declares {@code /jstech <name>}, which players of at least {@code permission} may run, unless the server's
     * permission mod says otherwise of its node, {@code jscore.command.<name>}.
     *
     * @throws IllegalStateException when a command of that name is declared already
     */
    public static void declare(final String name, final int permission, final CommandRoot.IBuilder builder) {
        CommandRoot.CORE.declare(name, permission, builder);
    }

    /** The node of the command {@code /jstech <name>}, or none when no such command is declared. */
    public static Optional<PermissionNode<Boolean>> node(final String name) {
        return CommandRoot.CORE.node(name);
    }

    /** The names declared under the root, in the order they were declared. */
    public static List<String> names() {
        return CommandRoot.CORE.names();
    }
}
