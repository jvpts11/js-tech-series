/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.item.ServerHardwareHandler;
import dev.jstech.computers.menu.ServerAssemblyMenu;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraParts;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * A server's card slots take its era's storage cards as a player puts them in, through the server's assembly screen:
 * every server of every era, its board seated first, takes the era's Vaultis Array and Vaultis Cache in its first card
 * slot.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ServerCardSlotGameTests {

    private static final String ARENA = "empty";

    private ServerCardSlotGameTests() {
    }

    @GameTest(template = ARENA)
    public static void everyServer_takesItsErasStorageCardsInItsCardSlot(final GameTestHelper helper) {
        final List<String> refused = new ArrayList<>();
        for (final EraKit kit : EraKit.values()) {
            for (final Item server : List.of(kit.server(), kit.storageServer(), kit.computeServer())) {
                for (final Item card : List.of(kit.array(), kit.cache())) {
                    if (!takes(helper, server, kit.serverBoard(), card)) {
                        refused.add(kit.name() + " " + server + " refused " + card);
                    }
                }
            }
        }
        helper.assertTrue(refused.isEmpty(), "storage cards refused: " + refused);
        helper.succeed();
    }

    /**
     * The same with every server board of each era, not only the one its kit builds with: a player may seat any board
     * the server takes, and each opens a card slot that takes its era's Vaultis Array.
     */
    @GameTest(template = ARENA)
    public static void everyServerBoard_opensACardSlotForItsErasArray(final GameTestHelper helper) {
        final List<String> refused = new ArrayList<>();
        int tried = 0;
        for (final EraKit kit : EraKit.values()) {
            for (final Item server : List.of(kit.server(), kit.storageServer(), kit.computeServer())) {
                for (final Item item : BuiltInRegistries.ITEM) {
                    final MotherboardSpec board = HardwareRegistry.board(new ItemStack(item));
                    if (board == null || board.era() != kit.era() || !seats(helper, server, item)) {
                        continue;
                    }
                    tried++;
                    if (!takes(helper, server, item, kit.array())) {
                        refused.add(server + " with " + item + " (" + board.pcieSlots() + " slots)");
                    }
                }
            }
        }
        helper.assertTrue(tried > EraKit.values().length * 3, "every era's servers were tried; only " + tried);
        helper.assertTrue(refused.isEmpty(), "the array was refused by " + refused);
        helper.succeed();
    }

    /**
     * Every era's storage server has two card slots: its Vaultis Array in one and a graphics card beside it, which a
     * board with no video of its own needs for a monitor.
     */
    @GameTest(template = ARENA)
    public static void everyStorageServer_takesItsArrayBesideAGraphicsCard(final GameTestHelper helper) {
        final List<String> refused = new ArrayList<>();
        for (final EraKit kit : EraKit.values()) {
            final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(kit.storageServer()));
            final ServerAssemblyMenu menu = new ServerAssemblyMenu(0, player.getInventory(),
                    InteractionHand.MAIN_HAND);
            slotOf(menu, ServerHardwareHandler.MOBO).set(new ItemStack(kit.serverBoard()));
            final Item graphics = EraParts.cardFor(kit.serverBoard(), GpuSpec.class);
            final Slot first = slotOf(menu, ServerHardwareHandler.GPU_START);
            final Slot second = slotOf(menu, ServerHardwareHandler.GPU_START + 1);
            if (!first.mayPlace(new ItemStack(kit.array())) || !second.isActive()
                    || !second.mayPlace(new ItemStack(graphics))) {
                refused.add(kit.name() + " " + kit.storageServer());
                continue;
            }
            first.set(new ItemStack(kit.array()));
            second.set(new ItemStack(graphics));
            if (!first.getItem().is(kit.array()) || !second.getItem().is(graphics)) {
                refused.add(kit.name() + " " + kit.storageServer() + " did not keep both");
            }
        }
        helper.assertTrue(refused.isEmpty(), "no room for an array beside a graphics card in " + refused);
        helper.succeed();
    }

    /* Whether a server in a player's hand, its board seated through its screen, takes the card in its first slot. */
    private static boolean takes(final GameTestHelper helper, final Item server, final Item board, final Item card) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(server));
        final ServerAssemblyMenu menu = new ServerAssemblyMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);
        final Slot boardSlot = slotOf(menu, ServerHardwareHandler.MOBO);
        if (!boardSlot.mayPlace(new ItemStack(board))) {
            helper.fail(server + " refused its own era's board " + board);
        }
        boardSlot.set(new ItemStack(board));
        final Slot cardSlot = slotOf(menu, ServerHardwareHandler.GPU_START);
        if (!cardSlot.isActive() || !cardSlot.mayPlace(new ItemStack(card))) {
            return false;
        }
        cardSlot.set(new ItemStack(card));
        return cardSlot.getItem().is(card);
    }

    /* Whether the server in a player's hand takes that board in its board slot. */
    private static boolean seats(final GameTestHelper helper, final Item server, final Item board) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(server));
        final ServerAssemblyMenu menu = new ServerAssemblyMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);
        return slotOf(menu, ServerHardwareHandler.MOBO).mayPlace(new ItemStack(board));
    }

    private static Slot slotOf(final ServerAssemblyMenu menu, final int handlerIndex) {
        for (final Slot slot : menu.slots) {
            if (slot instanceof SlotItemHandler part
                    && part.getItemHandler() instanceof ServerHardwareHandler && part.getSlotIndex() == handlerIndex) {
                return slot;
            }
        }
        throw new IllegalStateException("no slot " + handlerIndex);
    }
}
