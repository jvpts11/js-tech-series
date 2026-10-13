/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.block.part.AbstractBusPart;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.bus.AbstractBusScreen;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.NetworkInteractorApp;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.tests.testkit.EraKit;
import dev.jstech.tests.testkit.EraNetwork;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Each era's network moving items in a player's hands. The era's Import Bus, put on the access line by a click on a
 * chest beside it, pulls the chest's stone into the network's storage. At a computer with a desktop, the Network
 * Interactor finds that stone, requests some to the computer, and takes logs from the player's hand into the network.
 * The era's Export Bus, put on the line by a click on a chest above it, is told what to send in its own window, the
 * stone set in its filter from the player's hand, and fills the chest.
 */
public final class EraStorageFlowClientTests {

    /** How long a step may take to show on the screen or on the server. */
    private static final int WAIT = 160;
    /** The chest the Import Bus empties, south of the access cable. */
    private static final BlockPos IMPORT_CHEST = new BlockPos(8, 2, 3);
    /** The chest the Export Bus fills, on top of the access cable. */
    private static final BlockPos EXPORT_CHEST = new BlockPos(8, 3, 2);
    /** The access cable between the router and the computer, which both buses go on. */
    private static final BlockPos ACCESS_CABLE = new BlockPos(8, 2, 2);
    /** Where the player stands to reach both chests and the cable. */
    private static final BlockPos BY_THE_CHESTS = new BlockPos(8, 2, 5);
    private static final int STONE = 32;
    private static final int LOGS = 4;
    /* Hotbar slots: the bus in hand, the item the Export Bus is set to send, the logs, and an empty hand. */
    private static final int BUS_SLOT = 0;
    private static final int SENT_SLOT = 1;
    private static final int LOG_SLOT = 2;
    private static final int EMPTY_SLOT = 8;
    /** The Network Interactor's inventory band lists the 27 slots of the backpack before the hotbar. */
    private static final int BAND_HOTBAR = 27;

    private EraStorageFlowClientTests() {
    }

    @ClientTest(timeoutTicks = 4000)
    public static void vintage_itemsFlowInAndOutOfTheNetwork(final ClientTestContext ctx) {
        flow(ctx, EraKit.VINTAGE);
    }

    @ClientTest(timeoutTicks = 4000)
    public static void legacy_itemsFlowInAndOutOfTheNetwork(final ClientTestContext ctx) {
        flow(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = 4000)
    public static void transition_itemsFlowInAndOutOfTheNetwork(final ClientTestContext ctx) {
        flow(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = 4000)
    public static void standard_itemsFlowInAndOutOfTheNetwork(final ClientTestContext ctx) {
        flow(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = 4000)
    public static void advanced_itemsFlowInAndOutOfTheNetwork(final ClientTestContext ctx) {
        flow(ctx, EraKit.ADVANCED);
    }

    private static void flow(final ClientTestContext ctx, final EraKit kit) {
        EraJourney.lay(ctx, kit)
                .thenBuild(0, world -> {
                    world.setBlock(IMPORT_CHEST, Blocks.CHEST);
                    world.setBlock(EXPORT_CHEST, Blocks.CHEST);
                })
                .thenServer(EraJourney.SETTLE, level -> chest(ctx, level, IMPORT_CHEST)
                        .setItem(0, new ItemStack(Items.COBBLESTONE, STONE)));
        importBus(ctx, kit);
        if (kit != EraKit.VINTAGE) {
            interactor(ctx, kit);
        }
        exportBus(ctx, kit);
    }

    /* The Import Bus put on the line by a click on the chest with it in hand: the chest's stone goes into storage. */
    private static void importBus(final ClientTestContext ctx, final EraKit kit) {
        ctx.thenTeleport(EraJourney.SETTLE, BY_THE_CHESTS, Direction.NORTH)
                .thenGive(0, new ItemStack(kit.importBus()))
                .then(EraJourney.SETTLE, () -> ctx.selectHotbar(BUS_SLOT))
                // Sneaking, as a block is put against a chest without opening it.
                .thenSneakClick(EraJourney.SETTLE, IMPORT_CHEST, Direction.SOUTH)
                .thenWaitUntilServer(level -> chest(ctx, level, IMPORT_CHEST).isEmpty()
                                && stored(ctx, level, Items.COBBLESTONE) == STONE, WAIT,
                        "the Import Bus to pull the chest's stone into the network",
                        level -> "chest empty=" + chest(ctx, level, IMPORT_CHEST).isEmpty() + " stored="
                                + stored(ctx, level, Items.COBBLESTONE))
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "import-bus"));
    }

    /*
     * At the computer, the Network Interactor's network grid lists the stone; a double click on it and Request bring
     * some to the computer's own storage; the logs in hand, clicked in the band of the player's inventory and then on
     * the grid, go into the network.
     */
    private static void interactor(final ClientTestContext ctx, final EraKit kit) {
        final String label = "Network";
        ctx.thenGive(0, ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.OAK_LOG, LOGS))
                .then(EraJourney.SETTLE, () -> ctx.selectHotbar(EMPTY_SLOT))
                .thenTeleport(EraJourney.SETTLE, EraNetwork.AT_THE_DESK, Direction.WEST)
                .thenRightClick(EraJourney.SETTLE, EraNetwork.MONITOR)
                .thenAwaitScreen(EraJourney.systemScreen(kit), EraJourney.BOOT_WAIT);
        EraJourney.launch(ctx, kit, label)
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor(label) != null, WAIT,
                        "the Network Interactor to open from Start")
                // It reopens on the tab last used anywhere on this client, so the network's grid is picked.
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(point(ctx, label, interactor(ctx, label)
                        .networkTabCenter())))
                .thenWaitUntil(() -> interactor(ctx, label).listedNames().contains("Cobblestone"), WAIT,
                        "the network's grid to list the stone")
                .then(EraJourney.SETTLE, () -> {
                    final NetworkInteractorApp app = interactor(ctx, label);
                    final int[] cell = point(ctx, label, app.gridCellCenter(app.listedNames().indexOf("Cobblestone")));
                    ctx.clickDesktop(cell);
                    ctx.clickDesktop(cell);
                })
                .thenWaitUntil(() -> interactor(ctx, label).isRequestPopupOpen(), WAIT,
                        "a double click on the stone to open its request")
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(point(ctx, label, interactor(ctx, label)
                        .requestPopupSubmitCenter())));
        ctx.thenWaitUntilServer(level -> computer(ctx, level).localStore().view()
                        .getOrDefault(StorageKey.of(Items.COBBLESTONE), 0L) > 0, WAIT,
                "the request to bring stone to the computer's own storage",
                level -> "local=" + computer(ctx, level).localStore().view())
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(point(ctx, label, interactor(ctx, label)
                        .inventoryBandSlotCenter(BAND_HOTBAR + LOG_SLOT))))
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(point(ctx, label, interactor(ctx, label)
                        .gridFirstCellCenter())));
        ctx.thenWaitUntilServer(level -> stored(ctx, level, Items.OAK_LOG) == LOGS, WAIT,
                        "the logs from the player's hand to go into the network",
                        level -> "logs=" + stored(ctx, level, Items.OAK_LOG))
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "interactor"))
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .then(EraJourney.SETTLE, () -> {
                    if (ctx.mc().screen != null) {
                        ctx.key(GLFW.GLFW_KEY_ESCAPE);
                    }
                })
                .thenAwaitNoScreen(WAIT);
    }

    /*
     * The Export Bus put on the line by a click on the chest above it; its window opened with the empty hand; the
     * item to send picked up from the window's inventory and dropped in the cell that says what the bus sends (on the
     * Transition's, which lists items with their counts, on "+ item"), and put back; the chest fills with it. It is the
     * logs the Interactor put in where one ran, which the request left the network holding, and the stone elsewhere.
     */
    private static void exportBus(final ClientTestContext ctx, final EraKit kit) {
        final Item sent = kit == EraKit.VINTAGE ? Items.COBBLESTONE : Items.OAK_LOG;
        ctx.thenTeleport(EraJourney.SETTLE, BY_THE_CHESTS, Direction.NORTH)
                .thenGive(0, new ItemStack(kit.exportBus()), new ItemStack(sent))
                .then(EraJourney.SETTLE, () -> ctx.selectHotbar(BUS_SLOT))
                .thenSneakClick(EraJourney.SETTLE, EXPORT_CHEST, Direction.SOUTH)
                .thenWaitUntilServer(level -> level.getBlockEntity(ctx.abs(ACCESS_CABLE))
                                instanceof CableBlockEntity cable && cable.getPart(Direction.UP) instanceof AbstractBusPart,
                        WAIT,
                        "the Export Bus to go on the cable under the chest", level -> "no bus on the cable's top")
                .then(EraJourney.SETTLE, () -> ctx.selectHotbar(EMPTY_SLOT))
                /*
                 * The cable answers the part the player looks at: the bus sits on its top under the chest, so the eyes
                 * go to its edge facing the player, and the turn reaches the server before the click does.
                 */
                .then(EraJourney.SETTLE, () -> ctx.lookAt(Vec3.atLowerCornerOf(ctx.abs(ACCESS_CABLE))
                        .add(0.5, 0.94, 0.9)))
                .then(EraJourney.SETTLE, () -> ctx.rightClick(ACCESS_CABLE, Direction.UP))
                .thenAwaitScreen(AbstractBusScreen.class, WAIT)
                .thenWaitUntil(() -> bus(ctx).getMenu().state() != null
                        && (bus(ctx).firstCellPoint() != null || bus(ctx).addItemPoint() != null), WAIT,
                        "the bus's window to lay out what it sends")
                // The item taken up on the cursor, dropped in the cell, or on "+ item" where the list has no cell yet.
                .then(EraJourney.SETTLE, () -> clickHeld(ctx, sent))
                .then(EraJourney.SETTLE, () -> {
                    final int[] at = bus(ctx).firstCellPoint() != null ? bus(ctx).firstCellPoint()
                            : bus(ctx).addItemPoint();
                    ctx.clickGui(at[0], at[1]);
                })
                .then(EraJourney.SETTLE, () -> clickHeld(ctx, sent))
                .thenWaitUntil(() -> bus(ctx).firstCellPoint() != null, WAIT,
                        "the window to show the item the bus sends")
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "export-bus-window"))
                .then(EraJourney.SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(WAIT)
                .thenWaitUntilServer(level -> count(chest(ctx, level, EXPORT_CHEST), sent) > 0, WAIT,
                        "the Export Bus to fill the chest with what it was set to send",
                        level -> "chest=" + count(chest(ctx, level, EXPORT_CHEST), sent) + " stored="
                                + stored(ctx, level, sent))
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "export-bus"));
    }

    /* A click on the window's inventory slot that holds the item to send, or on the empty one it was taken from. */
    private static void clickHeld(final ClientTestContext ctx, final Item item) {
        final AbstractBusScreen<?> screen = bus(ctx);
        final boolean carrying = !screen.getMenu().getCarried().isEmpty();
        for (final Slot slot : screen.getMenu().slots) {
            if (slot.container instanceof Inventory && slot.getContainerSlot() == SENT_SLOT
                    && (carrying || slot.getItem().is(item))) {
                ctx.clickGui(slot.x + 8, slot.y + 8);
                return;
            }
        }
        throw new ClientTestFailure("the bus window shows no slot for the item in hand");
    }

    private static AbstractBusScreen<?> bus(final ClientTestContext ctx) {
        return ctx.screen(AbstractBusScreen.class);
    }

    private static NetworkInteractorApp interactor(final ClientTestContext ctx, final String label) {
        return DesktopSteps.app(ctx, label, NetworkInteractorApp.class);
    }

    /* A point of the Interactor's content, where the desktop has it. */
    private static int[] point(final ClientTestContext ctx, final String label, final int[] local) {
        final DesktopWindow window = ctx.screen(DesktopScreen.class).windowFor(label);
        if (window == null) {
            throw new ClientTestFailure("the " + label + " window is gone");
        }
        return DesktopSteps.contentPoint(window, local);
    }

    private static Container chest(final ClientTestContext ctx, final ServerLevel level, final BlockPos at) {
        if (level.getBlockEntity(ctx.abs(at)) instanceof Container container) {
            return container;
        }
        throw new ClientTestFailure("no chest at " + at);
    }

    private static int count(final Container container, final Item item) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).is(item)) {
                total += container.getItem(slot).getCount();
            }
        }
        return total;
    }

    private static long stored(final ClientTestContext ctx, final ServerLevel level, final Item item) {
        final MainframeBlockEntity mainframe = TestWorldBuilder.at(level, ctx.origin())
                .blockEntity(EraNetwork.MAINFRAME, MainframeBlockEntity.class);
        return mainframe.networkUuid() == null ? -1
                : NetworkStorage.of(level, mainframe.networkUuid()).count(StorageKey.of(item));
    }

    private static PersonalComputerBlockEntity computer(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.COMPUTER,
                PersonalComputerBlockEntity.class);
    }

    private static String shot(final EraKit kit, final String what) {
        return kit.name().toLowerCase(Locale.ROOT) + "-" + what;
    }
}
