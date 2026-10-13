/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.workshop;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.os.WorkshopApp;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.terminal.TerminalHosts;
import dev.jstech.computers.workshop.Workshop;
import dev.jstech.computers.workshop.WorkshopCard;
import dev.jstech.computers.workshop.WorkshopRates;
import dev.jstech.computers.workshop.WorkshopTexts;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

/**
 * The Workshop's server side: applies what its window does to the personal-use cards of the Personal Computer the
 * player is at and answers with the state. Items move between the cards' slots and the cursor of the desktop's own
 * container, so nothing is copied or lost; a slot of a card that is not in the computer takes nothing new, though
 * what it still holds can always be taken out.
 */
@EventBusSubscriber(modid = JsComputers.MODID)
public final class WorkshopPayloads {

    private WorkshopPayloads() {
    }

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        ComputerAccess.accept(registrar, WorkshopActionPayload.TYPE, WorkshopActionPayload.STREAM_CODEC,
                ComputerAccess.machineAt(WorkshopActionPayload::host, WorkshopActionPayload::monitorPos),
                WorkshopPayloads::handleAction);
        registrar.playToClient(WorkshopStatePayload.TYPE, WorkshopStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) -> WorkshopApp.accept(payload)));
    }

    /** The state of {@code computer}'s Workshop as {@code player} sees it, with {@code status} as its last line. */
    public static WorkshopStatePayload state(final ServerPlayer player, final PersonalComputerBlockEntity computer,
                                             final Text status) {
        final Workshop workshop = computer.workshop();
        final ServerLevel level = player.serverLevel();
        final int speed = computer.furnaceSpeed();
        final List<WorkshopStatePayload.Offer> offers = new ArrayList<>();
        for (final Workshop.Offer offer : workshop.offers(player)) {
            offers.add(new WorkshopStatePayload.Offer(offer.required(), offer.clue(), offer.levels()));
        }
        final Workshop.AnvilResult anvil = workshop.anvil(player);
        return new WorkshopStatePayload(computer.workshopCards(), workshop.contents(), workshop.craftResult(level),
                workshop.moreFromInventory(player), speed, workshop.ticksPerItem(level, speed),
                workshop.progressTicks(), workshop.smeltsInto(level), offers, anvil.result(), anvil.anvilLevels(),
                anvil.levels(), workshop.anvilName(), player.experienceLevel, player.experienceProgress, status);
    }

    private static void handleAction(final WorkshopActionPayload payload, final ServerPlayer player,
                                     final ServerLevel level) {
        final PersonalComputerBlockEntity computer = computer(player, level, payload.host(), payload.monitorPos());
        if (computer == null) {
            return;
        }
        final Workshop workshop = computer.workshop();
        final int cards = computer.workshopCards();
        final AbstractContainerMenu menu = player.containerMenu;
        Text status = Text.EMPTY;
        switch (payload.action()) {
            case WorkshopActionPayload.CLICK -> {
                final WorkshopCard card = cardOf(payload.index());
                // Taking out is always allowed: what a card left behind must never be stuck.
                if (card != null && (card.in(cards) || menu.getCarried().isEmpty())) {
                    workshop.click(player, menu, payload.index(), payload.button());
                    menu.broadcastChanges();
                }
            }
            case WorkshopActionPayload.SHIFT_INSERT -> {
                shiftInsert(player, workshop, cards, payload.index(), payload.button());
                menu.broadcastChanges();
            }
            case WorkshopActionPayload.CRAFT -> {
                if (WorkshopCard.CRAFTING_TABLE.in(cards)) {
                    final ItemStack made = workshop.craftResult(level);
                    final int times = workshop.craft(player, payload.index() == 1);
                    status = times <= 0 ? WorkshopTexts.NO_RECIPE.text() : times == 1
                            ? WorkshopTexts.CRAFTED_ONE.with(GameText.of(made.getHoverName()))
                            : WorkshopTexts.CRAFTED_MANY.with(GameText.of(made.getHoverName()), times);
                    menu.broadcastChanges();
                }
            }
            case WorkshopActionPayload.CLEAR_GRID -> {
                workshop.clearGrid(player);
                menu.broadcastChanges();
            }
            case WorkshopActionPayload.ENCHANT -> {
                if (WorkshopCard.ENCHANTING.in(cards)) {
                    final int levels = WorkshopRates.enchantLevels(payload.index());
                    status = workshop.enchant(player, payload.index()) ? WorkshopTexts.ENCHANTED.with(levels)
                            : WorkshopTexts.CANNOT_ENCHANT.text();
                }
            }
            case WorkshopActionPayload.ANVIL_NAME -> {
                if (WorkshopCard.ANVIL.in(cards)) {
                    workshop.setAnvilName(payload.text());
                }
            }
            case WorkshopActionPayload.ANVIL_TAKE -> {
                if (WorkshopCard.ANVIL.in(cards)) {
                    final Workshop.AnvilResult anvil = workshop.anvil(player);
                    status = workshop.takeAnvil(player)
                            ? WorkshopTexts.TOOK.with(GameText.of(anvil.result().getHoverName()), anvil.levels())
                            : WorkshopTexts.CANNOT_TAKE.text();
                    menu.broadcastChanges();
                }
            }
            case WorkshopActionPayload.CLOSED -> {
                workshop.giveBack(player);
                menu.broadcastChanges();
            }
            default -> {
            }
        }
        PacketDistributor.sendToPlayer(player, state(player, computer, status));
    }

    /* The Personal Computer at {@code hostPos}, if the player is at one of its monitors. */
    @Nullable
    private static PersonalComputerBlockEntity computer(final ServerPlayer player, final ServerLevel level,
                                                        final BlockPos hostPos, final BlockPos monitorPos) {
        return TerminalHosts.niHost(player, level, hostPos, monitorPos) instanceof PersonalComputerBlockEntity pc
                ? pc : null;
    }

    /* The card a Workshop slot belongs to. */
    @Nullable
    private static WorkshopCard cardOf(final int slot) {
        if (slot >= Workshop.GRID && slot < Workshop.GRID + Workshop.GRID_SIZE) {
            return WorkshopCard.CRAFTING_TABLE;
        }
        return switch (slot) {
            case Workshop.FURNACE_IN, Workshop.FURNACE_OUT -> WorkshopCard.FURNACE;
            case Workshop.ENCHANT_ITEM -> WorkshopCard.ENCHANTING;
            case Workshop.ANVIL_LEFT, Workshop.ANVIL_RIGHT -> WorkshopCard.ANVIL;
            default -> null;
        };
    }

    /*
     * A shift-click on inventory slot {@code inventorySlot} with tab {@code tab} up: the whole stack goes where that
     * card takes it, the first free grid cell, the furnace, the enchanting slot or the anvil's first free side.
     */
    private static void shiftInsert(final ServerPlayer player, final Workshop workshop, final int cards,
                                    final int inventorySlot, final int tab) {
        final WorkshopCard card = WorkshopCard.byIndex(tab);
        if (card == null || inventorySlot < 0 || inventorySlot >= player.getInventory().items.size()) {
            return;
        }
        final ItemStack stack = player.getInventory().items.get(inventorySlot);
        if (!card.in(cards) || stack.isEmpty()) {
            return;
        }
        final int target = switch (card) {
            case CRAFTING_TABLE -> firstFree(workshop, Workshop.GRID, Workshop.GRID + Workshop.GRID_SIZE);
            case FURNACE -> Workshop.FURNACE_IN;
            case ENCHANTING -> Workshop.ENCHANT_ITEM;
            case ANVIL -> firstFree(workshop, Workshop.ANVIL_LEFT, Workshop.ANVIL_RIGHT + 1);
        };
        if (target < 0 || !workshop.accepts(player.level(), target, stack)) {
            return;
        }
        final ItemStack there = workshop.slot(target);
        final int room = target == Workshop.ENCHANT_ITEM ? 1 : stack.getMaxStackSize();
        if (!there.isEmpty() && !ItemStack.isSameItemSameComponents(there, stack)) {
            return;
        }
        final int moved = Math.min(stack.getCount(), room - there.getCount());
        if (moved <= 0) {
            return;
        }
        workshop.put(target, there.isEmpty() ? stack.copyWithCount(moved) : there.copyWithCount(there.getCount()
                + moved));
        stack.shrink(moved);
        player.getInventory().setChanged();
        if (target == Workshop.ANVIL_LEFT && workshop.anvilName().isEmpty()) {
            workshop.setAnvilName(workshop.slot(Workshop.ANVIL_LEFT).getHoverName().getString());
        }
    }

    private static int firstFree(final Workshop workshop, final int from, final int to) {
        for (int i = from; i < to; i++) {
            if (workshop.slot(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }
}
