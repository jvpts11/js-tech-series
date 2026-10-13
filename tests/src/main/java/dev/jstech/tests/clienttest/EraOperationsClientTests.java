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
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.workshop.UpdateDoor;
import dev.jstech.computers.workshop.WorkshopCard;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.tests.testkit.EraKit;
import dev.jstech.tests.testkit.EraNetwork;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Each era's network worked in its own language at a computer's prompt. The era's Export Bus, put on the access line
 * by a click on a chest above it, is named in its window, which is how a statement addresses it. At the computer the
 * player brings stone the network holds to the computer's own storage with a SELECT, gives part of it back with an
 * INSERT, and sends some out through the named bus with a DELETE, which fills the chest. From the Legacy on, with the
 * Furnace Card in the computer, an UPDATE smelts raw iron the network holds and the ingots come back to it.
 */
public final class EraOperationsClientTests {

    /** How long a step may take to show on the screen or on the server. */
    private static final int WAIT = 160;
    /** The chest the Export Bus fills, on top of the access cable. */
    private static final BlockPos EXPORT_CHEST = new BlockPos(8, 3, 2);
    /** The access cable between the router and the computer, which the bus goes on. */
    private static final BlockPos ACCESS_CABLE = new BlockPos(8, 2, 2);
    /** Where the player stands to reach the chest and the cable. */
    private static final BlockPos BY_THE_CHEST = new BlockPos(8, 2, 5);
    /** The name the bus is given in its window, and the one the DELETE sends to. */
    private static final String BUS_NAME = "out";
    private static final int STONE = 32;
    private static final int SELECTED = 8;
    private static final int INSERTED = 3;
    private static final int DELETED = 4;
    private static final int RAW_IRON = 4;
    private static final int SMELTED = 2;
    /** Two items at the slowest card's pace, a hundred ticks each, with room to spare. */
    private static final int SMELT_WAIT = 400;
    /* Hotbar slots: the bus in hand, then an empty hand. */
    private static final int BUS_SLOT = 0;
    private static final int EMPTY_SLOT = 8;

    private EraOperationsClientTests() {
    }

    @ClientTest(timeoutTicks = 4000)
    public static void vintage_operationsWorkTheNetwork(final ClientTestContext ctx) {
        operations(ctx, EraKit.VINTAGE);
    }

    @ClientTest(timeoutTicks = 4000)
    public static void legacy_operationsWorkTheNetwork(final ClientTestContext ctx) {
        operations(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = 4000)
    public static void transition_operationsWorkTheNetwork(final ClientTestContext ctx) {
        operations(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = 4000)
    public static void standard_operationsWorkTheNetwork(final ClientTestContext ctx) {
        operations(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = 4000)
    public static void advanced_operationsWorkTheNetwork(final ClientTestContext ctx) {
        operations(ctx, EraKit.ADVANCED);
    }

    private static void operations(final ClientTestContext ctx, final EraKit kit) {
        /*
         * The Furnace Card fits a slot of the PCI family, which the Vintage board lacks: from the Legacy on, the
         * computer is built with one, as the player who wants to smelt from the prompt builds it.
         */
        final boolean smelts = kit.era().isAtLeast(EraKit.LEGACY.era());
        EraJourney.lay(ctx, kit)
                .thenBuild(0, world -> world.setBlock(EXPORT_CHEST, Blocks.CHEST));
        if (smelts) {
            ctx.thenServer(EraJourney.SETTLE, level -> withFurnaceCard(computer(ctx, level)));
        }
        namedBus(ctx, kit);
        ctx.thenTeleport(EraJourney.SETTLE, EraNetwork.AT_THE_DESK, Direction.WEST)
                .thenRightClick(EraJourney.SETTLE, EraNetwork.MONITOR)
                .thenAwaitScreen(EraJourney.systemScreen(kit), EraJourney.BOOT_WAIT);
        if (kit != EraKit.VINTAGE) {
            final String terminal = EraJourney.terminal(kit);
            EraJourney.launch(ctx, kit, terminal)
                    .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor(terminal) != null, WAIT,
                            "the " + terminal + " window to open from Start");
        }
        // What the storage journey leaves in the network: stone, and raw iron to smelt.
        ctx.thenServer(EraJourney.SETTLE, level -> {
            final NetworkStorage storage = NetworkStorage.of(level, mainframe(ctx, level).networkUuid());
            ctx.assertTrue(storage.insert(StorageKey.of(Items.COBBLESTONE), STONE) == STONE,
                    "the network should take the stone");
            if (smelts) {
                ctx.assertTrue(storage.insert(StorageKey.of(Items.RAW_IRON), RAW_IRON) == RAW_IRON,
                        "the network should take the raw iron");
            }
        });
        statement(ctx, "iql SELECT " + SELECTED + " Cobblestone");
        ctx.thenWaitUntilServer(level -> local(ctx, level, Items.COBBLESTONE) == SELECTED
                        && stored(ctx, level, Items.COBBLESTONE) == STONE - SELECTED, WAIT,
                "the SELECT to bring the stone to the computer's own storage", level -> state(ctx, level))
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "select"));
        statement(ctx, "iql INSERT " + INSERTED + " Cobblestone");
        ctx.thenWaitUntilServer(level -> local(ctx, level, Items.COBBLESTONE) == SELECTED - INSERTED
                        && stored(ctx, level, Items.COBBLESTONE) == STONE - SELECTED + INSERTED, WAIT,
                "the INSERT to give part of the stone back to the network", level -> state(ctx, level))
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "insert"));
        statement(ctx, "iql DELETE " + DELETED + " Cobblestone TO " + BUS_NAME);
        ctx.thenWaitUntilServer(level -> count(chest(ctx, level), Items.COBBLESTONE) == DELETED
                        && stored(ctx, level, Items.COBBLESTONE) == STONE - SELECTED + INSERTED - DELETED, WAIT,
                "the DELETE to send the stone out through the named bus into the chest",
                level -> "chest=" + count(chest(ctx, level), Items.COBBLESTONE) + " " + state(ctx, level))
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "delete"));
        if (!smelts) {
            return;
        }
        statement(ctx, "iql UPDATE " + SMELTED + " raw_iron SET SMELT");
        ctx.thenWaitUntilServer(level -> stored(ctx, level, Items.IRON_INGOT) == SMELTED
                        && stored(ctx, level, Items.RAW_IRON) == RAW_IRON - SMELTED, SMELT_WAIT,
                "the UPDATE to smelt the raw iron at the Furnace Card and give the ingots back to the network",
                level -> "raw=" + stored(ctx, level, Items.RAW_IRON) + " ingots="
                        + stored(ctx, level, Items.IRON_INGOT) + " " + state(ctx, level))
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "update"));
    }

    /*
     * The Export Bus put on the line by a click on the chest above it, its window opened with the empty hand and the
     * name typed in its field: with no filter it sends nothing of its own, only what a statement sends to that name.
     */
    private static void namedBus(final ClientTestContext ctx, final EraKit kit) {
        ctx.thenTeleport(EraJourney.SETTLE, BY_THE_CHEST, Direction.NORTH)
                .thenGive(0, new ItemStack(kit.exportBus()))
                .then(EraJourney.SETTLE, () -> ctx.selectHotbar(BUS_SLOT))
                .thenSneakClick(EraJourney.SETTLE, EXPORT_CHEST, Direction.SOUTH)
                .thenWaitUntilServer(level -> bus(ctx, level) != null, WAIT,
                        "the Export Bus to go on the cable under the chest", level -> "no bus on the cable's top")
                .then(EraJourney.SETTLE, () -> ctx.selectHotbar(EMPTY_SLOT))
                // The bus sits on the cable's top under the chest: the eyes go to its edge facing the player.
                .then(EraJourney.SETTLE, () -> ctx.lookAt(Vec3.atLowerCornerOf(ctx.abs(ACCESS_CABLE))
                        .add(0.5, 0.94, 0.9)))
                .then(EraJourney.SETTLE, () -> ctx.rightClick(ACCESS_CABLE, Direction.UP))
                .thenAwaitScreen(AbstractBusScreen.class, WAIT)
                .then(EraJourney.SETTLE, () -> {
                    final int[] field = ctx.screen(AbstractBusScreen.class).namePoint();
                    ctx.clickGui(field[0], field[1]);
                })
                .then(EraJourney.SETTLE, () -> ctx.type(BUS_NAME))
                .thenWaitUntilServer(level -> bus(ctx, level) != null && BUS_NAME.equals(bus(ctx, level).name()),
                        WAIT, "the name typed in the bus's window to name the bus",
                        level -> "name=" + (bus(ctx, level) == null ? null : bus(ctx, level).name()))
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "named-bus"))
                // The first Escape leaves the field, the second closes the window.
                .then(EraJourney.SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .then(EraJourney.SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(WAIT);
    }

    /* Types the statement at the prompt and sends it. */
    private static void statement(final ClientTestContext ctx, final String line) {
        ctx.then(EraJourney.SETTLE, () -> {
            ctx.type(line);
            ctx.key(GLFW.GLFW_KEY_ENTER);
        });
    }

    /* Puts the Furnace Card in the computer's first free card slot. */
    private static void withFurnaceCard(final PersonalComputerBlockEntity computer) {
        for (int slot = PersonalComputerBlockEntity.GPU_SLOTS_START; slot < PersonalComputerBlockEntity.GPU_SLOTS_START
                + PersonalComputerBlockEntity.GPU_SLOTS; slot++) {
            if (computer.getHardware().getStackInSlot(slot).isEmpty()) {
                computer.getHardware().setStackInSlot(slot, UpdateDoor.cardItem(WorkshopCard.FURNACE));
                return;
            }
        }
        throw new ClientTestFailure("the computer has no free card slot for the Furnace Card");
    }

    /* What the computer and the network hold, and the last lines the prompt answered with. */
    private static String state(final ClientTestContext ctx, final ServerLevel level) {
        final List<WireLine> glass = computer(ctx, level).console().glass();
        final StringBuilder said = new StringBuilder();
        for (final WireLine line : glass.subList(Math.max(0, glass.size() - 3), glass.size())) {
            line.spans().forEach(span -> said.append(span.text().english()));
            said.append(" | ");
        }
        return "local=" + local(ctx, level, Items.COBBLESTONE) + " stored=" + stored(ctx, level, Items.COBBLESTONE)
                + " prompt=" + said;
    }

    private static AbstractBusPart bus(final ClientTestContext ctx, final ServerLevel level) {
        return level.getBlockEntity(ctx.abs(ACCESS_CABLE)) instanceof CableBlockEntity cable
                && cable.getPart(Direction.UP) instanceof AbstractBusPart bus ? bus : null;
    }

    private static Container chest(final ClientTestContext ctx, final ServerLevel level) {
        if (level.getBlockEntity(ctx.abs(EXPORT_CHEST)) instanceof Container container) {
            return container;
        }
        throw new ClientTestFailure("no chest at " + EXPORT_CHEST);
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

    private static long local(final ClientTestContext ctx, final ServerLevel level, final Item item) {
        return computer(ctx, level).localStore().view().getOrDefault(StorageKey.of(item), 0L);
    }

    private static long stored(final ClientTestContext ctx, final ServerLevel level, final Item item) {
        final MainframeBlockEntity mainframe = mainframe(ctx, level);
        return mainframe.networkUuid() == null ? -1
                : NetworkStorage.of(level, mainframe.networkUuid()).count(StorageKey.of(item));
    }

    private static MainframeBlockEntity mainframe(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.MAINFRAME, MainframeBlockEntity.class);
    }

    private static PersonalComputerBlockEntity computer(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.COMPUTER,
                PersonalComputerBlockEntity.class);
    }

    private static String shot(final EraKit kit, final String what) {
        return kit.name().toLowerCase(Locale.ROOT) + "-" + what;
    }
}
