/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio.catalog;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /soundfoundry catalog reload}: reads the server's music catalogue again, for an operator who has just put an
 * album in its folder, without restarting the server.
 */
@TextHolder
@EventBusSubscriber(modid = JsComputers.MODID)
public final class SoundfoundryCommand {

    static final TextKey READING = TextKey.of("jsc.soundfoundry.catalog.reading", "Reading the music catalogue...");
    static final TextKey READ = TextKey.of("jsc.soundfoundry.catalog.read",
            "The music catalogue has %s albums and %s songs");
    static final TextKey PASSED_OVER = TextKey.of("jsc.soundfoundry.catalog.passed_over",
            "%s files could not be read and were passed over; the server log says why");
    static final TextKey TURNED_OFF = TextKey.of("jsc.soundfoundry.catalog.turned_off",
            "The music catalogue is turned off in jscomputers-server.toml");
    static final TextKey NO_STORE = TextKey.of("jsc.soundfoundry.catalog.no_store",
            "The server keeps no recordings right now");

    /* What an operator needs to be to read the catalogue again: the level of the game's own reload. */
    private static final int OPERATOR = 2;

    private SoundfoundryCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(final RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("soundfoundry")
                .requires(source -> source.hasPermission(OPERATOR))
                .then(Commands.literal("catalog")
                        .then(Commands.literal("reload").executes(context -> reload(context.getSource())))));
    }

    private static int reload(final CommandSourceStack source) {
        if (!ComputersServerConfig.soundfoundryCatalog()) {
            source.sendFailure(GameText.component(TURNED_OFF));
            return 0;
        }
        if (MediaStore.current().isEmpty()) {
            source.sendFailure(GameText.component(NO_STORE));
            return 0;
        }
        final MinecraftServer server = source.getServer();
        source.sendSuccess(() -> GameText.component(READING), true);
        SoundfoundryCatalog.reload(server, server.getResourceManager(), read -> {
            source.sendSuccess(() -> GameText.component(READ.with(read.albums().size(), read.songs())), true);
            if (read.skipped() > 0) {
                source.sendFailure(GameText.component(PASSED_OVER.with(read.skipped())));
            }
        });
        return 1;
    }
}
