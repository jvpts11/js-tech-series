/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Example.
 */
package dev.jstech.example.screen;

import dev.jstech.core.text.GameText;
import dev.jstech.example.ExampleState;
import dev.jstech.example.ExampleTexts;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The logbook: used, it counts one more opening for its player and opens its screen. */
public final class LogbookItem extends Item {

    public LogbookItem(final Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        if (player instanceof ServerPlayer reader) {
            ExampleState.LOGBOOK_OPENED.update(reader.server, reader.getUUID(), opened -> opened + 1);
            reader.openMenu(new SimpleMenuProvider((id, inventory, opener) -> new LogbookMenu(id, inventory),
                    GameText.component(ExampleTexts.LOGBOOK_TITLE)));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
