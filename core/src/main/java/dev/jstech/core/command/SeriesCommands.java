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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

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
@EventBusSubscriber(modid = JsCore.MODID)
public final class SeriesCommands {

    /** Anybody may run it. */
    public static final int EVERYONE = 0;
    /** The operators who may change the game: the level of {@code /give} and {@code /gamemode}. */
    public static final int GAME_MASTERS = 2;
    /** The operators who look after the server: the level of {@code /ban}. */
    public static final int ADMINS = 3;
    /** The root every command of the series hangs from. */
    public static final String ROOT = "jstech";

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
     * Declares {@code /jstech <name>}, which players of at least {@code permission} may run.
     *
     * @throws IllegalStateException when a command of that name is declared already
     */
    public static void declare(final String name, final int permission, final IBuilder builder) {
        final Declared declared = new Declared(name, permission, builder);
        if (BY_NAME.putIfAbsent(name, declared) != null) {
            throw new IllegalStateException("the command /" + ROOT + " " + name + " is declared twice");
        }
        DECLARED.add(declared);
    }

    /** The names declared under the root, in the order they were declared. */
    public static List<String> names() {
        return DECLARED.stream().map(Declared::name).toList();
    }

    @SubscribeEvent
    public static void onRegisterCommands(final RegisterCommandsEvent event) {
        final LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(ROOT);
        for (final Declared declared : DECLARED) {
            final LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(declared.name())
                    .requires(source -> source.hasPermission(declared.permission()));
            root.then(declared.builder().build(node, event.getBuildContext()));
        }
        event.getDispatcher().register(root);
    }

    private record Declared(String name, int permission, IBuilder builder) {
    }
}
