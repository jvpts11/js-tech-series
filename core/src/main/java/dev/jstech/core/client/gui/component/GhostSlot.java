/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A ghost slot: a filter's or a recipe's place that shows an item without holding it, drawn faded. A click with an
 * item on the pointer sets it to that item, one of it, and the item stays on the pointer; the right button clears it.
 */
public final class GhostSlot extends UiComponent {

    private final Supplier<ItemStack> ghost;
    private final Consumer<ItemStack> onSet;

    private static final int RIGHT_BUTTON = 1;
    /** How far the ground of the slot covers the ghost, so it reads as not really there. */
    private static final int VEIL_ALPHA = 0x80;

    /**
     * @param ghost what the slot shows now
     * @param onSet what is done with the item set, an empty stack when it is cleared
     */
    public GhostSlot(final Supplier<ItemStack> ghost, final Consumer<ItemStack> onSet) {
        this.ghost = Objects.requireNonNull(ghost, "ghost");
        this.onSet = Objects.requireNonNull(onSet, "onSet");
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        ctx.skin().slot(g, x() + 1, y() + 1);
        final ItemStack shown = ghost.get();
        if (!shown.isEmpty()) {
            g.renderFakeItem(shown, x() + 1, y() + 1);
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            g.fill(x() + 1, y() + 1, x() + 17, y() + 17, ctx.skin().fieldBg() & 0xFFFFFF | VEIL_ALPHA << 24);
            g.pose().popPose();
        }
        if (hovered(ctx)) {
            g.fill(x() + 1, y() + 1, x() + 17, y() + 17, ctx.skin().listHover() & 0xFFFFFF | VEIL_ALPHA << 24);
        }
    }

    @Override
    public boolean mouseClicked(final double mx, final double my, final int button) {
        if (button == RIGHT_BUTTON) {
            onSet.accept(ItemStack.EMPTY);
            return true;
        }
        final Minecraft game = Minecraft.getInstance();
        final ItemStack carried = game.player == null ? ItemStack.EMPTY : game.player.containerMenu.getCarried();
        onSet.accept(carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
        return true;
    }

    @Override
    public List<Component> tooltip(final double mx, final double my) {
        final ItemStack shown = ghost.get();
        final Minecraft game = Minecraft.getInstance();
        if (shown.isEmpty() || game.player == null) {
            return List.of();
        }
        return shown.getTooltipLines(Item.TooltipContext.of(game.level), game.player, TooltipFlag.Default.NORMAL);
    }
}
