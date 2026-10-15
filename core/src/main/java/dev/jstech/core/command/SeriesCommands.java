/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.jstech.core.JsCore;
import dev.jstech.core.team.CorePermissions;
import dev.jstech.core.util.DeclarationError;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;

/**
 * The commands of every mod of the series, under one root, {@code /jstech}: a mod declares {@code /jstech <name>}
 * once, from its constructor, with who may run it and how its arguments go, and the Core hands every declaration to
 * the game each time the commands are built. The series takes one name from the command line rather than one a mod.
 *
 * <pre>{@code
 * SeriesCommands.declare("media", SeriesCommands.ADMINS, (node, context) -> node
 *         .executes(source -> held(source.getSource())));
 * }</pre>
 */
public final class SeriesCommands {

    /** Anybody may run it. */
    public static final int EVERYONE = 0;
    /** The operators who may change the game: the level of {@code /give} and {@code /gamemode}. */
    public static final int GAME_MASTERS = 2;
    /** The operators who look after the server: the level of {@code /ban}. */
    public static final int ADMINS = 3;
    /** The root every command of the series hangs from. */
    public static final String ROOT = "jstech";
    /** What the name of a command's permission starts with, after the Core's id. */
    private static final String NODE_PREFIX = "command.";

    private static final List<Declared> DECLARED = new CopyOnWriteArrayList<>();
    private static final Map<String, Declared> BY_NAME = new ConcurrentHashMap<>();

    private SeriesCommands() {
    }

    /**
     * How a declared command's node is built: given {@code /jstech <name>}, the arguments and what runs, with the
     * context the game builds item and block arguments in.
     */
    @FunctionalInterface
    public interface IBuilder {

        LiteralArgumentBuilder<CommandSourceStack> build(LiteralArgumentBuilder<CommandSourceStack> node,
                                                         CommandBuildContext context);
    }

    /**
     * Declares {@code /jstech <name>}, which players of at least {@code permission} may run, unless the server's
     * permission mod says otherwise of its node, {@code jscore.command.<name>}.
     *
     * @throws IllegalStateException when a command of that name is declared already
     */
    public static void declare(final String name, final int permission, final IBuilder builder) {
        if (BY_NAME.containsKey(name)) {
            throw DeclarationError.of(null, "the command /" + ROOT + " " + name, "is declared twice",
                    "declare it once; a mod's own commands take names of their own under /" + ROOT);
        }
        final PermissionNode<Boolean> node = CorePermissions.declare(JsCore.MODID, NODE_PREFIX + name, permission,
                CorePermissions.COMMAND_TEXT.with(name));
        final Declared declared = new Declared(name, permission, node, builder);
        BY_NAME.put(name, declared);
        DECLARED.add(declared);
    }

    /** The node of the command {@code /jstech <name>}, or none when no such command is declared. */
    public static Optional<PermissionNode<Boolean>> node(final String name) {
        return Optional.ofNullable(BY_NAME.get(name)).map(Declared::node);
    }

    /** The names declared under the root, in the order they were declared. */
    public static List<String> names() {
        return DECLARED.stream().map(Declared::name).toList();
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, SeriesCommands::onRegisterCommands);
    }

    public static void onRegisterCommands(final RegisterCommandsEvent event) {
        final LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(ROOT);
        for (final Declared declared : DECLARED) {
            final LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(declared.name())
                    .requires(source -> mayRun(source, declared));
            root.then(declared.builder().build(node, event.getBuildContext()));
        }
        event.getDispatcher().register(root);
    }

    /*
     * The level the source runs at lets it, as the game's own commands go (the console, a command block, a function
     * run at a level of its own, an operator); a player below it may still be granted the command's node by the
     * server's permission mod.
     */
    private static boolean mayRun(final CommandSourceStack source, final Declared declared) {
        if (source.hasPermission(declared.permission())) {
            return true;
        }
        final ServerPlayer player = source.getPlayer();
        return player != null && CorePermissions.has(player, declared.node());
    }

    private record Declared(String name, int permission, PermissionNode<Boolean> node, IBuilder builder) {
    }
}
