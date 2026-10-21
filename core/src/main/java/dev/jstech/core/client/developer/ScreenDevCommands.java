/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.developer;

import com.mojang.brigadier.Command;
import dev.jstech.core.developer.DeveloperMode;
import dev.jstech.core.text.GameText;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;

/**
 * The developer's commands for screens, on the player's game while developer mode is on: {@code /jstech screen new}
 * opens the new screen for the block looked at. What it says is in {@link DeveloperTexts}, which a server can read.
 */
public final class ScreenDevCommands {

    private ScreenDevCommands() {
    }

    /** Registers the commands, once, from the Core's client; each says so when developer mode is off. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, event -> event.getDispatcher().register(
                Commands.literal("jstech").then(Commands.literal("screen").then(Commands.literal("new")
                        .executes(context -> {
                            if (!DeveloperMode.on()) {
                                context.getSource().sendFailure(GameText.component(DeveloperTexts.OFF));
                                return 0;
                            }
                            final Minecraft minecraft = Minecraft.getInstance();
                            if (minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit)
                                    || hit.getType() != HitResult.Type.BLOCK) {
                                context.getSource().sendFailure(GameText.component(DeveloperTexts.NO_BLOCK));
                                return 0;
                            }
                            final ResourceLocation block = BuiltInRegistries.BLOCK.getKey(minecraft.level
                                    .getBlockState(hit.getBlockPos()).getBlock());
                            // The chat closes after its command; the new screen opens after it.
                            minecraft.tell(() -> minecraft.setScreen(new NewScreenScreen(block)));
                            return Command.SINGLE_SUCCESS;
                        })))));
    }
}
