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
import java.util.Objects;
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
import org.jetbrains.annotations.ApiStatus;

/**
 * A root of commands, {@code /<root> <name>}: a mod declares each command once, from its constructor, with who may run
 * it and how its arguments go, and every declaration is handed to the game each time the commands are built. Each
 * command has a permission node of the root's mod, {@code <mod>.command.<name>}, so a permission mod grants it to a
 * player below its level. The Core's own commands hang from {@link #CORE}, {@code /jstech}, which the mods built on
 * the Core may share; a mod wanting a root of its own makes one with {@link #of}.
 *
 * <pre>{@code
 * CommandRoot.CORE.declare("media", CommandRoot.ADMINS, (node, context) -> node
 *         .executes(source -> held(source.getSource())));
 * }</pre>
 */
public final class CommandRoot {

    private final String modid;
    private final String name;
    private final List<Declared> declared = new CopyOnWriteArrayList<>();
    private final Map<String, Declared> byName = new ConcurrentHashMap<>();

    /** Anybody may run it. */
    public static final int EVERYONE = 0;
    /** The operators who may change the game: the level of {@code /give} and {@code /gamemode}. */
    public static final int GAME_MASTERS = 2;
    /** The operators who look after the server: the level of {@code /ban}. */
    public static final int ADMINS = 3;
    /** What the name of a command's permission starts with, after the root's mod's id. */
    private static final String NODE_PREFIX = "command.";

    private static final List<CommandRoot> ROOTS = new CopyOnWriteArrayList<>();
    /** The Core's own root, {@code /jstech}. */
    public static final CommandRoot CORE = of(JsCore.MODID, "jstech");

    private CommandRoot(final String modid, final String name) {
        this.modid = modid;
        this.name = name;
    }

    /** The root {@code /<name>} of the mod {@code modid}, whose commands' nodes are the mod's. */
    public static CommandRoot of(final String modid, final String name) {
        final CommandRoot root = new CommandRoot(Objects.requireNonNull(modid, "modid"),
                Objects.requireNonNull(name, "name"));
        ROOTS.add(root);
        return root;
    }

    /** Hands the game's bus what builds every root, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, CommandRoot::onRegisterCommands);
    }

    /** The root's name, as typed after the slash. */
    public String name() {
        return this.name;
    }

    /**
     * Declares {@code /<root> <name>}, which players of at least {@code permission} may run, unless the server's
     * permission mod says otherwise of its node, {@code <mod>.command.<name>}.
     *
     * @throws IllegalStateException when a command of that name is declared already under this root
     */
    public void declare(final String command, final int permission, final IBuilder builder) {
        if (this.byName.containsKey(command)) {
            throw DeclarationError.of(null, "the command /" + this.name + " " + command, "is declared twice",
                    "declare it once; a mod's own commands take names of their own under /" + this.name);
        }
        final PermissionNode<Boolean> node = CorePermissions.declare(this.modid, NODE_PREFIX + command, permission,
                CorePermissions.COMMAND_TEXT.with(command));
        final Declared made = new Declared(command, permission, node, builder);
        this.byName.put(command, made);
        this.declared.add(made);
    }

    /** The node of the command {@code /<root> <command>}, or none when no such command is declared. */
    public Optional<PermissionNode<Boolean>> node(final String command) {
        return Optional.ofNullable(this.byName.get(command)).map(Declared::node);
    }

    /** The names declared under the root, in the order they were declared. */
    public List<String> names() {
        return this.declared.stream().map(Declared::name).toList();
    }

    private void register(final RegisterCommandsEvent event) {
        if (this.declared.isEmpty()) {
            return;
        }
        final LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(this.name);
        for (final Declared one : this.declared) {
            final LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(one.name())
                    .requires(source -> mayRun(source, one));
            root.then(one.builder().build(node, event.getBuildContext()));
        }
        event.getDispatcher().register(root);
    }

    private static void onRegisterCommands(final RegisterCommandsEvent event) {
        ROOTS.forEach(root -> root.register(event));
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

    /**
     * How a declared command's node is built: given {@code /<root> <name>}, the arguments and what runs, with the
     * context the game builds item and block arguments in.
     */
    @FunctionalInterface
    public interface IBuilder {

        LiteralArgumentBuilder<CommandSourceStack> build(LiteralArgumentBuilder<CommandSourceStack> node,
                                                         CommandBuildContext context);
    }

    private record Declared(String name, int permission, PermissionNode<Boolean> node, IBuilder builder) {
    }
}
