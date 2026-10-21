/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.item;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.tooltip.ItemTooltip;
import dev.jstech.core.client.gui.tooltip.ItemTooltips;
import dev.jstech.core.client.gui.tooltip.TooltipTexts;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.energy.ItemEnergyStorage;
import dev.jstech.core.format.Unit;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.gui.tooltip.TooltipBar;
import dev.jstech.core.gui.tooltip.TooltipContents;
import dev.jstech.core.gui.tooltip.TooltipSettings;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.item.ItemState;
import dev.jstech.core.item.ItemStates;
import dev.jstech.core.item.ItemTexts;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.tool.BuildingTools;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/**
 * Items that hold something, on a player's game: the mode key ({@link CoreKeys#CHANGE_ITEM_MODE}) is sent to the
 * server only while the main hand holds an item with modes, the mouse wheel turned while sneaking with one does what
 * the key does (down to the next mode, up to the one before) instead of changing the slot, and a tooltip shows what
 * an item holds: a bar each for its energy, fluid and gas, what it holds in slots (the whole list with Shift), how
 * fast it charges with Ctrl, and how worn a worn item is. The key has none of its own until the player gives it one,
 * since the game and the mods beside it already take nearly every key; the tooltip of an item with modes says so.
 */
public final class ItemStateClient {

    private ItemStateClient() {
    }

    /** Hands the mod's bus and the game's what this answers, once, from the mod's constructor on a player's game. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(FMLClientSetupEvent.class, ItemStateClient::onClientSetup);
        ItemTooltips.add(JsCore.MODID, ItemStateClient::describe);
        NeoForge.EVENT_BUS.addListener(InputEvent.MouseScrollingEvent.class, ItemStateClient::onScroll);
    }

    public static void onScroll(final InputEvent.MouseScrollingEvent event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;
        if (minecraft.screen != null || player == null || !player.isShiftKeyDown() || event.getScrollDeltaY() == 0
                || !ItemStates.of(player.getMainHandItem()).hasModes()) {
            return;
        }
        event.setCanceled(true);
        KeyActionsClient.send(CoreKeys.CHANGE_ITEM_MODE, event.getScrollDeltaY() > 0);
    }

    public static void onClientSetup(final FMLClientSetupEvent event) {
        KeyActionsClient.sendsWhen(CoreKeys.CHANGE_ITEM_MODE, () -> {
            final Minecraft minecraft = Minecraft.getInstance();
            return minecraft.player != null && ItemStates.of(minecraft.player.getMainHandItem()).hasModes();
        });
    }

    /** What an item's tooltip shows of what it holds. */
    public static void describe(final ItemStack stack, final ItemTooltip tooltip) {
        final ItemState state = ItemStates.of(stack);
        ItemStates.mode(stack).ifPresent(mode -> tooltip.line(ItemTexts.MODE.with(mode.name())));
        final Boolean on = stack.get(ItemStates.ON.get());
        if (on != null) {
            tooltip.line(on ? ItemTexts.ON.text() : ItemTexts.OFF.text());
        }
        final UnitFormatter format = UnitFormatter.forCurrentLocale();
        if (state.holdsEnergy()) {
            energy(stack, state, format, tooltip);
        }
        if (state.holdsFluid()) {
            final SimpleFluidContent fluid = stack.getOrDefault(ItemStates.FLUID.get(), SimpleFluidContent.EMPTY);
            final Text said = fluid.isEmpty() ? ItemTexts.FLUID_EMPTY.with(state.fluidCapacity())
                    : ItemTexts.FLUID.with(GameText.of(fluid.copy().getHoverName()), fluid.getAmount(),
                            state.fluidCapacity());
            tooltip.piece(TooltipBar.of(fluid.getAmount(), state.fluidCapacity(),
                    new TooltipBar.Paint.Fluid(fluid.copy()), grey(said)));
        }
        if (state.holdsGas()) {
            final MatterStack gas = stack.get(ItemStates.GAS.get());
            final Text said = gas == null ? ItemTexts.GAS_EMPTY.with(state.gasCapacity())
                    : ItemTexts.GAS.with(gas.material().name(), gas.amount(), state.gasCapacity());
            tooltip.piece(TooltipBar.of(gas == null ? 0 : gas.amount(), state.gasCapacity(), TooltipBar.Paint.GAS,
                    grey(said)));
        }
        if (state.holdsItems()) {
            items(stack, state, tooltip);
        }
        if (TooltipSettings.durability() && stack.isDamageableItem() && stack.isDamaged()
                && !tooltip.flags().isAdvanced()) {
            final int most = stack.getMaxDamage();
            final int left = most - stack.getDamageValue();
            tooltip.piece(TooltipBar.of(left, most, new TooltipBar.Paint.Colour(stack.getBarColor()),
                    grey(TooltipTexts.DURABILITY.with(left, most))));
        }
        BuildingTools.describe(stack).forEach(tooltip::line);
        if (state.hasModes()) {
            final KeyMapping key = KeyActionsClient.mapping(CoreKeys.CHANGE_ITEM_MODE);
            final Text hint = key.isUnbound() ? ItemTexts.MODE_NO_KEY.text()
                    : ItemTexts.MODE_HINT.with(GameText.of(key.getTranslatedKeyMessage()));
            // How to change the mode is what the item does, not what it is: Shift shows it.
            tooltip.shift().line(GameText.component(hint).copy().withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /* The energy's bar, and with Ctrl how fast the item takes and gives it. */
    private static void energy(final ItemStack stack, final ItemState state, final UnitFormatter format,
                               final ItemTooltip tooltip) {
        final long stored = new ItemEnergyStorage(stack, state.energyCapacity(), state.energyIn(),
                state.energyOut()).stored();
        tooltip.piece(TooltipBar.of(stored, state.energyCapacity(), TooltipBar.Paint.ENERGY,
                grey(ItemTexts.ENERGY.with(format.full(stored, Unit.FE), format.full(state.energyCapacity(),
                        Unit.FE)))));
        if (state.energyIn() > 0) {
            tooltip.ctrl().line(ItemTexts.CHARGES.with(format.full(state.energyIn(), Unit.FE)));
        }
        if (state.energyOut() > 0) {
            tooltip.ctrl().line(ItemTexts.GIVES.with(format.full(state.energyOut(), Unit.FE)));
        }
    }

    /* What the item holds in its slots, how many of them, and with Shift the whole list. */
    private static void items(final ItemStack stack, final ItemState state, final ItemTooltip tooltip) {
        final ItemContainerContents held = stack.getOrDefault(ItemStates.INVENTORY.get(),
                ItemContainerContents.EMPTY);
        final TooltipContents contents = TooltipContents.of(held.nonEmptyItems());
        if (!contents.isEmpty()) {
            tooltip.piece(contents);
        }
        tooltip.line(ItemTexts.ITEMS.with(held.nonEmptyStream().count(), state.slots()));
        for (final ItemStack kind : TooltipContents.kinds(held.nonEmptyItems())) {
            tooltip.shift().line(TooltipTexts.HELD.with(kind.getCount(), GameText.of(kind.getHoverName())));
        }
    }

    private static Component grey(final Text text) {
        return GameText.component(text).copy().withStyle(ChatFormatting.GRAY);
    }
}
