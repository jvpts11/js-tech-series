/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.WorkshopApp;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.workshop.Workshop;
import dev.jstech.computers.workshop.WorkshopCard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The Workshop as a player sees it on a Frames XP Personal Computer with two of the cards in: a tab for each card,
 * the one whose card is missing dim and refusing a click, the furnace smelting what was left in it, and the player's
 * inventory under the station.
 */
public final class WorkshopClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 120;
    private static final int BOOT_WAIT = 1_200;
    private static final int TOASTS_GONE = 160;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final String WORKSHOP = "Workshop";
    private static final ResourceLocation FRAMES_XP = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "frames_xp");

    private WorkshopClientTests() {
    }

    @ClientTest(timeoutTicks = 3000)
    public static void workshop_opensWithATabPerCardAndTheFurnaceAtWork(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER, FRAMES_XP);
                    pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START + 1,
                            new ItemStack(ComputingModule.CRAFTING_TABLE_CARD.get()));
                    pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START + 2,
                            new ItemStack(ComputingModule.FURNACE_CARD.get()));
                    pc.console().install(Programs.WORKSHOP.toString());
                    pc.workshop().put(Workshop.FURNACE_IN, new ItemStack(Items.RAW_IRON, 16));
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).launcherLabels().contains(WORKSHOP), SCREEN_WAIT,
                        "the Workshop to be listed as installed")
                .then(SETTLE, () -> DesktopScreen.requestOpen(Programs.WORKSHOP.toString()))
                .thenWaitUntil(() -> workshop(ctx) != null && workshop(ctx).state() != null, SCREEN_WAIT,
                        "the Workshop window with the computer's state")
                .thenAssert(0, () -> workshop(ctx).activeTab() == WorkshopCard.CRAFTING_TABLE.index()
                                && WorkshopCard.FURNACE.in(workshop(ctx).state().cards())
                                && !WorkshopCard.ENCHANTING.in(workshop(ctx).state().cards()),
                        "it opens on the first card's tab and knows which cards are in")
                // Late enough for the advancement toasts of a first start to have gone from over the window.
                .thenScreenshot(TOASTS_GONE, "workshop-crafting")
                .then(0, () -> ctx.clickDesktop(point(ctx, workshop(ctx).tabCenter(WorkshopCard.FURNACE.index()))))
                .thenWaitUntil(() -> workshop(ctx).activeTab() == WorkshopCard.FURNACE.index()
                                && workshop(ctx).state().ticksPerItem() > 0, SCREEN_WAIT,
                        "the Furnace tab, smelting the raw iron")
                .thenScreenshot(SETTLE, "workshop-furnace")
                .then(0, () -> ctx.clickDesktop(point(ctx,
                        workshop(ctx).tabCenter(WorkshopCard.ENCHANTING.index()))))
                .thenAssert(SETTLE, () -> workshop(ctx).activeTab() == WorkshopCard.FURNACE.index(),
                        "the tab of a card that is not in cannot be chosen")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    @ClientTest(timeoutTicks = 3000)
    public static void workshop_showsTheEnchantingOffersAndTheAnvil(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER, FRAMES_XP);
                    pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START + 1,
                            new ItemStack(ComputingModule.ENCHANTING_CARD.get()));
                    pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START + 2,
                            new ItemStack(ComputingModule.ANVIL_CARD.get()));
                    pc.console().install(Programs.WORKSHOP.toString());
                    pc.workshop().put(Workshop.ENCHANT_ITEM, new ItemStack(Items.DIAMOND_SWORD));
                    final ItemStack worn = new ItemStack(Items.IRON_PICKAXE);
                    worn.setDamageValue(worn.getMaxDamage() - 40);
                    pc.workshop().put(Workshop.ANVIL_LEFT, worn);
                    pc.workshop().put(Workshop.ANVIL_RIGHT, new ItemStack(Items.IRON_INGOT, 2));
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenServer(SETTLE, level -> ctx.serverPlayer().setExperienceLevels(34))
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).launcherLabels().contains(WORKSHOP), SCREEN_WAIT,
                        "the Workshop to be listed as installed")
                .then(SETTLE, () -> DesktopScreen.requestOpen(Programs.WORKSHOP.toString()))
                .thenWaitUntil(() -> workshop(ctx) != null && workshop(ctx).state() != null
                                && workshop(ctx).state().offers().size() == 3, SCREEN_WAIT,
                        "the Workshop window with the three offers")
                .thenAssert(0, () -> workshop(ctx).activeTab() == WorkshopCard.ENCHANTING.index(),
                        "it opens on the Enchanting tab, the first card in")
                .thenScreenshot(SETTLE, "workshop-enchanting")
                .then(0, () -> ctx.clickDesktop(point(ctx, workshop(ctx).tabCenter(WorkshopCard.ANVIL.index()))))
                .thenWaitUntil(() -> workshop(ctx).activeTab() == WorkshopCard.ANVIL.index()
                                && !workshop(ctx).state().anvilResult().isEmpty(), SCREEN_WAIT,
                        "the Anvil tab with the repaired pickaxe")
                .thenScreenshot(SETTLE, "workshop-anvil")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    @Nullable
    private static WorkshopApp workshop(final ClientTestContext ctx) {
        if (!(ctx.mc().screen instanceof DesktopScreen desktop)) {
            return null;
        }
        final DesktopWindow window = desktop.windowFor(WORKSHOP);
        return window != null && window.app() instanceof WorkshopApp app ? app : null;
    }

    /* A window's content-local point on the desktop. */
    private static int[] point(final ClientTestContext ctx, final int[] local) {
        final DesktopWindow window = ctx.screen(DesktopScreen.class).windowFor(WORKSHOP);
        if (window == null) {
            throw new ClientTestFailure("the Workshop window is gone");
        }
        return new int[] {window.x() + 4 + local[0], window.y() + 18 + local[1]};
    }
}
