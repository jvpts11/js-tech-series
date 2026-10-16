/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model.network;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.jstech.core.JsCore;
import dev.jstech.core.command.SeriesCommands;
import dev.jstech.core.content.DeclaredModel;
import dev.jstech.core.content.ModContent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The tools of whoever makes models, run from the game: {@code /jstech model preview <model>} opens the preview of a
 * declared model, or of any model file by its path ({@code jsc:geo/rack.geo.json}), on the game of the player who
 * runs it; {@code /jstech model reload} reads the models again there, and nothing else of the resources.
 */
public final class ModelCommands {

    private static final String MODEL = "model";
    private static final SuggestionProvider<CommandSourceStack> DECLARED = (context, builder) ->
            SharedSuggestionProvider.suggestResource(ModContent.all().stream()
                    .flatMap(content -> content.declaredModels().stream()).map(DeclaredModel::id), builder);
    private static final ResourceLocation NOTHING = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "none");

    private ModelCommands() {
    }

    /** Declares the commands, from the Core's constructor. */
    public static void declare() {
        SeriesCommands.declare(MODEL, SeriesCommands.GAME_MASTERS, (node, context) -> node
                .then(Commands.literal("preview")
                        .then(Commands.argument(MODEL, ResourceLocationArgument.id()).suggests(DECLARED)
                                .executes(ModelCommands::preview)))
                .then(Commands.literal("reload").executes(ModelCommands::reload)));
    }

    private static int preview(final CommandContext<CommandSourceStack> command) throws CommandSyntaxException {
        final ServerPlayer player = command.getSource().getPlayerOrException();
        PacketDistributor.sendToPlayer(player, new ModelToolPayload(true,
                ResourceLocationArgument.getId(command, MODEL)));
        return 1;
    }

    private static int reload(final CommandContext<CommandSourceStack> command) throws CommandSyntaxException {
        final ServerPlayer player = command.getSource().getPlayerOrException();
        PacketDistributor.sendToPlayer(player, new ModelToolPayload(false, NOTHING));
        return 1;
    }
}
