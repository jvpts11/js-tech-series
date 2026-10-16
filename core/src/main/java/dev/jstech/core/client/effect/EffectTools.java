/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.effect;

import dev.jstech.core.effect.EffectDefinition;
import dev.jstech.core.effect.EffectToolPayload;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * What the effect commands do on the player's game, at the block they look at (or a few blocks ahead of them): open
 * the effects editor on an effect, open the card of a new one, or play one.
 */
public final class EffectTools {

    private static final double AHEAD = 3.0;

    private EffectTools() {
    }

    /** Carries out an effect command; on the render thread. */
    public static void receive(final EffectToolPayload payload) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        final LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return;
        }
        final BlockPos at = lookedAt(minecraft, player);
        switch (payload.tool()) {
            case EDIT -> edit(minecraft, level, player, payload.effect(), at);
            case NEW -> minecraft.setScreen(new NewEffectScreen(at, BuiltInRegistries.BLOCK.getKey(
                    level.getBlockState(at).getBlock()), EffectAnchors.locators(level, at)));
            case PLAY -> {
                if (ClientEffects.has(payload.effect())) {
                    ClientEffects.playOnBlock(payload.effect(), at);
                } else {
                    missing(player, payload.effect());
                }
            }
        }
    }

    /** Opens the effects editor on the effect {@code id} at the block {@code at}. */
    public static void edit(final Minecraft minecraft, final ClientLevel level, final LocalPlayer player,
                            final ResourceLocation id, final BlockPos at) {
        final Optional<EffectDefinition> definition = EffectFiles.get(id);
        if (definition.isEmpty()) {
            missing(player, id);
            return;
        }
        minecraft.setScreen(new EffectEditorScreen(id, definition.get(), EffectAnchors.of(level, at,
                definition.get().anchor())));
    }

    private static BlockPos lookedAt(final Minecraft minecraft, final LocalPlayer player) {
        if (minecraft.hitResult instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK) {
            return block.getBlockPos();
        }
        return BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(AHEAD)));
    }

    private static void missing(final LocalPlayer player, final ResourceLocation id) {
        player.displayClientMessage(GameText.component(EffectTexts.NO_EFFECT.with(Text.literal(id.toString()))),
                true);
    }
}
