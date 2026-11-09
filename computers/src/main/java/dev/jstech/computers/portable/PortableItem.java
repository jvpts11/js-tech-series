/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A portable device (computers/docs/design/wireless.md): a PDA, a smartphone or a tablet of its era, with its system
 * from the factory. Carried, it runs on its battery, the bar under its icon, and joins the network of an access point
 * that reaches its player; used, it is held up in the middle of the view, its own face framing its screen.
 */
public final class PortableItem extends Item {

    private final PortableModel model;

    public PortableItem(final Properties properties, final PortableModel model) {
        super(properties.stacksTo(1));
        this.model = model;
    }

    /** Which of the twelve it is. */
    public PortableModel model() {
        return model;
    }

    @Override
    public void inventoryTick(final ItemStack stack, final Level level, final Entity entity, final int slot,
                              final boolean selected) {
        if (entity instanceof ServerPlayer player) {
            Portables.carried(player, stack, model);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        if (level.isClientSide()) {
            Portables.open(hand);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    /** Its battery is always in view, even full, as a battery is. */
    @Override
    public boolean isBarVisible(final ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(final ItemStack stack) {
        return PortableBattery.barWidth(fraction(stack));
    }

    /** From green full to red flat, as the game colours a tool's wear. */
    @Override
    public int getBarColor(final ItemStack stack) {
        return Mth.hsvToRgb(fraction(stack) / 3.0F, 1.0F, 1.0F);
    }

    /* A battery drawn or a link changed is the same device in the same hand: it is not taken out and put back. */
    @Override
    public boolean shouldCauseReequipAnimation(final ItemStack oldStack, final ItemStack newStack,
                                               final boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem()
                || !Objects.equals(Portables.id(oldStack), Portables.id(newStack));
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        tooltip.add(grey(PortableTexts.WHAT_IT_IS.with(model.era().name(), model.kind().word(),
                model.system().family().word())));
        tooltip.add(grey(model.cardMb() > 0
                ? PortableTexts.HARDWARE_WITH_CARD.with(model.chip(), PortableModel.size(model.memoryMb()),
                        PortableModel.size(model.storageMb()), PortableModel.size(model.cardMb()))
                : PortableTexts.HARDWARE.with(model.chip(), PortableModel.size(model.memoryMb()),
                        PortableModel.size(model.storageMb()))));
        final int percent = PortableBattery.percent(Portables.battery(stack, model), PortableBattery.capacity(model));
        tooltip.add(GameText.component(PortableTexts.BATTERY.with(percent)).copy().withStyle(
                switch (PortableBattery.level(percent)) {
                    case GOOD -> ChatFormatting.GREEN;
                    case LOW -> ChatFormatting.YELLOW;
                    case CRITICAL -> ChatFormatting.RED;
                }));
        if (Portables.id(stack) == null) {
            return;
        }
        final PortableLink link = stack.get(ComputingComponents.PORTABLE_LINK.get());
        if (!Portables.isOn(stack)) {
            tooltip.add(GameText.component(PortableTexts.SWITCHED_OFF).copy().withStyle(ChatFormatting.RED));
        } else if (link == null) {
            tooltip.add(GameText.component(PortableTexts.NO_ACCESS_POINT).copy().withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(grey(PortableTexts.LINKED.with(link.speed().accessPointName(), link.speed().itemsPerTick())));
        }
    }

    /* How full its battery is, from 0 to 1. */
    private float fraction(final ItemStack stack) {
        return PortableBattery.fraction(Portables.battery(stack, model), PortableBattery.capacity(model));
    }

    private static Component grey(final Text text) {
        return GameText.component(text).copy().withStyle(ChatFormatting.GRAY);
    }
}
