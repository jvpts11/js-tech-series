/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import dev.jstech.computers.ComputersPermissions;
import dev.jstech.core.team.CorePermissions;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /jstech crypto halving reset}: an operator puts Teracoin's block reward back to its first value, the halvings
 * counting again from the chain's last block (computers/docs/design/teracoin.md, "Settings").
 */
@TextHolder
public final class TeracoinCommand {

    static final TextKey RESET = TextKey.of("jsc.teracoin.halving_reset",
            "Teracoin's reward is back to %s TRC a block, from block %s");

    /* What an operator needs to be, without a permission mod: the level of the game's own server settings. */
    private static final int OPERATOR = 2;

    private TeracoinCommand() {
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, TeracoinCommand::onRegisterCommands);
    }

    public static void onRegisterCommands(final RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("jstech")
                .then(Commands.literal("crypto").requires(TeracoinCommand::mayReset)
                        .then(Commands.literal("halving")
                                .then(Commands.literal("reset").executes(context -> reset(context.getSource()))))));
    }

    private static boolean mayReset(final CommandSourceStack source) {
        final ServerPlayer player = source.getPlayer();
        return player == null ? source.hasPermission(OPERATOR)
                : CorePermissions.has(player, ComputersPermissions.resetHalving());
    }

    private static int reset(final CommandSourceStack source) {
        final TeracoinState state = TeracoinState.of(source.getServer());
        final TeracoinChain chain = state.chain();
        chain.resetHalving();
        state.setDirty();
        source.sendSuccess(() -> GameText.component(RESET.with(Trc.format(chain.nextReward()),
                String.format(Locale.ROOT, "%,d", chain.height() + 1))), true);
        return 1;
    }
}
