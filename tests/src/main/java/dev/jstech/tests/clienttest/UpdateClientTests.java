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
import dev.jstech.computers.client.os.NetworkInteractorApp;
import dev.jstech.computers.client.os.UpdatePopup;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The Network Interactor's Update window as a player uses it on a Frames XP Personal Computer with the Enchanting,
 * Furnace and Anvil Cards in: opened from an item's dialog, a tab per action greyed where the item does not take
 * it, and each of the three sent to the network, paid for with the player's own levels.
 */
public final class UpdateClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int WORK_WAIT = 600;
    private static final int BOOT_WAIT = 1_200;
    private static final int TOASTS_GONE = 160;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 3);
    private static final BlockPos COMPUTER_CABLE = new BlockPos(4, 2, 3);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 3);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 3);
    private static final String NETWORK_LAUNCHER = "Network";
    private static final int LEVELS = 34;
    private static final ResourceLocation FRAMES_XP = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "frames_xp");

    private UpdateClientTests() {
    }

    @ClientTest(timeoutTicks = 4000)
    public static void update_enchantsSmeltsAndRepairsFromTheInteractor(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
                    world.setBlock(COMPUTER_CABLE, ComputingModule.ETHERNET_CABLE);
                    final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER, FRAMES_XP);
                    pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START + 1,
                            new ItemStack(ComputingModule.ENCHANTING_CARD.get()));
                    pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START + 2,
                            new ItemStack(ComputingModule.FURNACE_CARD.get()));
                    pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START + 3,
                            new ItemStack(ComputingModule.ANVIL_CARD.get()));
                    world.placeMonitor(MONITOR, Direction.EAST);
                    net.seed(Items.DIAMOND_SWORD, 1);
                    net.seed(Items.RAW_IRON, 4);
                    net.seed(Items.IRON_INGOT, 4);
                    final ItemStack worn = new ItemStack(Items.IRON_PICKAXE);
                    worn.setDamageValue(worn.getMaxDamage() - 40);
                    net.rack().getServerStorage(0).insert(StorageKey.of(worn), 1);
                })
                .thenServer(SETTLE, level -> ctx.serverPlayer().setExperienceLevels(LEVELS))
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .then(2, () -> launch(ctx))
                .thenWaitUntil(() -> interactor(ctx) != null, SCREEN_WAIT, "the Network Interactor window")
                .then(2, () -> ctx.clickDesktop(point(ctx, interactor(ctx).networkTabCenter())))
                /*
                 * The grid lists what the network's index holds, and the index is built on a virtual thread: on a
                 * loaded machine that takes more ticks than a window does to open, so this waits as long as work does.
                 */
                .thenWaitUntil(() -> interactor(ctx).listedNames().contains("Diamond Sword")
                                && interactor(ctx).listedNames().contains("Iron Pickaxe"), WORK_WAIT,
                        "the network grid to list what was seeded")
                // Enchant: the sword's dialog, its Update button, and the window on its Enchant tab.
                .then(2, () -> openDialog(ctx, "Diamond Sword"))
                .thenWaitUntil(() -> interactor(ctx).hasPopup(), SCREEN_WAIT, "the sword's dialog")
                .then(1, () -> ctx.clickDesktop(point(ctx, interactor(ctx).requestUpdateCenter())))
                .thenWaitUntil(() -> window(ctx) != null && window(ctx).preview() != null, SCREEN_WAIT,
                        "the Update window with what the network says of the sword")
                .then(1, () -> ctx.assertTrue(window(ctx).tabOpen(UpdatePopup.TAB_ENCHANT_INDEX)
                                && !window(ctx).tabOpen(UpdatePopup.TAB_SMELT_INDEX)
                                && window(ctx).tabOpen(UpdatePopup.TAB_REPAIR_INDEX)
                                && window(ctx).tab() == UpdatePopup.TAB_ENCHANT_INDEX
                                && window(ctx).offer() >= 0,
                        "the sword takes an enchantment and an anvil but does not smelt"))
                .thenScreenshot(TOASTS_GONE, "update-enchant")
                .then(1, () -> ctx.clickDesktop(point(ctx, interactor(ctx).updateOfferCenter(0))))
                .then(1, () -> ctx.clickDesktop(point(ctx, interactor(ctx).updateSubmitCenter())))
                .thenWaitUntil(() -> window(ctx) == null || !window(ctx).isOpen(), SCREEN_WAIT,
                        "the window to close once the network took the UPDATE")
                .thenWaitUntilServer(level -> enchantedSword(ctx, level), WORK_WAIT,
                        "the sword to come back enchanted", level -> "levels=" + ctx.serverPlayer().experienceLevel)
                .thenServer(0, level -> ctx.assertTrue(ctx.serverPlayer().experienceLevel == LEVELS - 1,
                        "the first offer cost one level; levels=" + ctx.serverPlayer().experienceLevel))
                // Smelt: the raw iron opens on the Smelt tab, the only one it takes.
                .then(2, () -> openDialog(ctx, "Raw Iron"))
                .thenWaitUntil(() -> interactor(ctx).hasPopup(), SCREEN_WAIT, "the raw iron's dialog")
                .then(1, () -> ctx.clickDesktop(point(ctx, interactor(ctx).requestUpdateCenter())))
                .thenWaitUntil(() -> window(ctx) != null && window(ctx).preview() != null
                                && window(ctx).tab() == UpdatePopup.TAB_SMELT_INDEX, SCREEN_WAIT,
                        "the Update window on the Smelt tab")
                .thenScreenshot(SETTLE, "update-smelt")
                .then(1, () -> ctx.clickDesktop(point(ctx, interactor(ctx).updateSubmitCenter())))
                .thenWaitUntilServer(level -> held(ctx, level, Items.IRON_INGOT) >= 5L, WORK_WAIT,
                        "a fifth ingot to come back smelted",
                        level -> "ingots=" + held(ctx, level, Items.IRON_INGOT))
                // Repair, from the details panel's own button: the pickaxe's Repair tab, mended with the ingots.
                .then(2, () -> select(ctx, "Iron Pickaxe"))
                .then(2, () -> ctx.clickDesktop(point(ctx, interactor(ctx).detailUpdateCenter())))
                .thenWaitUntil(() -> window(ctx) != null && window(ctx).preview() != null, SCREEN_WAIT,
                        "the Update window over the pickaxe")
                .then(1, () -> ctx.clickDesktop(point(ctx,
                        interactor(ctx).updateTabCenter(UpdatePopup.TAB_REPAIR_INDEX))))
                .thenWaitUntil(() -> window(ctx).tab() == UpdatePopup.TAB_REPAIR_INDEX
                                && !window(ctx).preview().repaired().isEmpty(), SCREEN_WAIT,
                        "the Repair tab with what the anvil would make")
                .thenScreenshot(SETTLE, "update-repair")
                .then(1, () -> ctx.clickDesktop(point(ctx, interactor(ctx).updateSubmitCenter())))
                .thenWaitUntilServer(level -> mended(ctx, level), WORK_WAIT, "the pickaxe to come back mended",
                        level -> "ingots=" + held(ctx, level, Items.IRON_INGOT))
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .then(2, () -> {
                    if (ctx.mc().screen != null) {
                        ctx.key(GLFW.GLFW_KEY_ESCAPE);
                    }
                })
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    /* A double click on the item's cell, which opens its dialog. */
    private static void openDialog(final ClientTestContext ctx, final String name) {
        final int[] cell = point(ctx, interactor(ctx).gridCellCenter(indexOf(ctx, name)));
        ctx.clickDesktop(cell);
        ctx.clickDesktop(cell);
    }

    /* One click on the item's cell, which picks it for the details panel. */
    private static void select(final ClientTestContext ctx, final String name) {
        ctx.clickDesktop(point(ctx, interactor(ctx).gridCellCenter(indexOf(ctx, name))));
    }

    private static int indexOf(final ClientTestContext ctx, final String name) {
        final int index = interactor(ctx).listedNames().indexOf(name);
        ctx.assertTrue(index >= 0, "the grid lists " + name + "; got " + interactor(ctx).listedNames());
        return index;
    }

    private static boolean enchantedSword(final ClientTestContext ctx, final ServerLevel level) {
        for (final Map.Entry<StorageKey, Long> entry : stock(ctx, level).entrySet()) {
            if (entry.getKey().item() == Items.DIAMOND_SWORD && entry.getKey().stack(1).isEnchanted()) {
                return true;
            }
        }
        return false;
    }

    private static boolean mended(final ClientTestContext ctx, final ServerLevel level) {
        for (final Map.Entry<StorageKey, Long> entry : stock(ctx, level).entrySet()) {
            final ItemStack pickaxe = entry.getKey().stack(1);
            if (pickaxe.is(Items.IRON_PICKAXE) && pickaxe.getDamageValue() < pickaxe.getMaxDamage() - 40) {
                return true;
            }
        }
        return false;
    }

    private static long held(final ClientTestContext ctx, final ServerLevel level, final Item item) {
        return stock(ctx, level).getOrDefault(StorageKey.of(item), 0L);
    }

    private static Map<StorageKey, Long> stock(final ClientTestContext ctx, final ServerLevel level) {
        return level.getBlockEntity(ctx.abs(COMPUTER)) instanceof PersonalComputerBlockEntity pc
                && pc.networkUuid() != null ? NetworkStorage.of(level, pc.networkUuid()).query() : Map.of();
    }

    private static void launch(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        ctx.click(desktop.startButtonX(), desktop.startButtonY());
        final int item = desktop.launcherLabels().indexOf(NETWORK_LAUNCHER);
        ctx.assertTrue(item >= 0, "the Start menu must list " + NETWORK_LAUNCHER + "; got " + desktop.launcherLabels());
        ctx.click(desktop.startMenuItemX(item), desktop.startMenuItemY(item));
    }

    @Nullable
    private static NetworkInteractorApp interactor(final ClientTestContext ctx) {
        if (!(ctx.mc().screen instanceof DesktopScreen desktop)) {
            return null;
        }
        final DesktopWindow window = desktop.windowFor(NETWORK_LAUNCHER);
        return window != null && window.app() instanceof NetworkInteractorApp app ? app : null;
    }

    @Nullable
    private static UpdatePopup window(final ClientTestContext ctx) {
        final NetworkInteractorApp app = interactor(ctx);
        return app == null || !app.updatePopup().isOpen() ? null : app.updatePopup();
    }

    /** Converts a Network Interactor content-local point into desktop coordinates. */
    private static int[] point(final ClientTestContext ctx, final int[] local) {
        final DesktopWindow window = ctx.screen(DesktopScreen.class).windowFor(NETWORK_LAUNCHER);
        if (window == null) {
            throw new ClientTestFailure("the " + NETWORK_LAUNCHER + " window is gone");
        }
        return DesktopSteps.contentPoint(window, local);
    }
}
