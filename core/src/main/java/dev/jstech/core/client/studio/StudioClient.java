/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import com.mojang.brigadier.Command;
import dev.jstech.core.client.developer.DeveloperTexts;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.developer.DeveloperMode;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.multiblock.ShapePreview;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;

/**
 * How the studio is opened, on the player's game while developer mode is on: {@code /jstech studio}, or the key
 * {@link CoreKeys#OPEN_STUDIO} once the player gives it one. Off, the command says how to turn it on.
 */
public final class StudioClient {

    private StudioClient() {
    }

    /** Registers the command and the key, once, from the Core's client. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, event -> event.getDispatcher().register(
                Commands.literal("jstech").then(Commands.literal("studio").executes(context -> {
                    if (!DeveloperMode.on()) {
                        context.getSource().sendFailure(GameText.component(DeveloperTexts.OFF));
                        return 0;
                    }
                    final Minecraft minecraft = Minecraft.getInstance();
                    // The chat closes after its command; the studio opens after it.
                    minecraft.tell(StudioClient::open);
                    return Command.SINGLE_SUCCESS;
                }))));
        KeyActionsClient.onPress(CoreKeys.OPEN_STUDIO, () -> {
            if (DeveloperMode.on()) {
                open();
            }
        });
        StudioHeatMap.listen();
        StudioLogs.listen();
        TestRecorder.listen();
    }

    /** Opens the studio over whatever is open, on the page it was last left on. */
    public static StudioScreen open() {
        final StudioScreen studio = new StudioScreen();
        Minecraft.getInstance().setScreen(studio);
        return studio;
    }

    /** A studio on the page of {@code tool}, to be opened. */
    public static StudioScreen openOn(final StudioTool tool) {
        final StudioScreen studio = new StudioScreen();
        studio.open(tool);
        return studio;
    }

    /**
     * The structure the studio caught, while the player asks for it to be shown in the world, for whatever shows
     * multiblocks where they stand to show it there; none otherwise.
     */
    public static Optional<Caught> caught() {
        return StructuresPage.projected();
    }

    /**
     * A structure the studio caught.
     *
     * @param shape its blocks, each at its place from its lowest corner
     * @param at    where its lowest corner stands
     * @param name  what it is called while shown
     * @param by    what shows it
     */
    public record Caught(ShapePreview shape, BlockPos at, Text name, Text by) {
    }
}
