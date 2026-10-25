/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.industrial.world.ProspectReport;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * J's Industrial's prospecting on an item of the test mod's own: used, it says what the prospecting pick says where
 * the player stands; used crouching, what the portable scanner says.
 */
public final class TestProspecting {

    public static final ItemEntry<Pick> PICK = TestSounds.CONTENT.item("test_prospecting_pick", Pick::new)
            .named("Test prospecting pick").look(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();

    private TestProspecting() {
    }

    /** Declares the item, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** The test pick. */
    public static final class Pick extends Item {

        public Pick(final Properties properties) {
            super(properties.stacksTo(1));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(final Level level, final Player player,
                                                      final InteractionHand hand) {
            if (level instanceof ServerLevel server) {
                final List<Text> lines = player.isShiftKeyDown()
                        ? ProspectReport.scan(server, player.blockPosition())
                        : ProspectReport.pick(server, player.blockPosition());
                lines.forEach(line -> player.sendSystemMessage(GameText.component(line)));
            }
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
        }
    }
}
