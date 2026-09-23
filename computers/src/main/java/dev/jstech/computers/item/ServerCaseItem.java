/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * An empty server chassis.
 */
@TextHolder
public class ServerCaseItem extends Item {

    private static final TextKey TOOLTIP =
            TextKey.of("item.jsc.server_case.tooltip", "Crafting ingredient for a Server");

    public ServerCaseItem(final Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        tooltip.add(GameText.component(TOOLTIP).withStyle(ChatFormatting.GRAY));
    }
}
