/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.item.CraftingCardItem;
import dev.jstech.computers.item.GpuItem;
import dev.jstech.computers.item.ServerHardwareHandler;
import dev.jstech.computers.item.SoundCardItem;
import dev.jstech.computers.registry.ComputingComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The network of one era, laid the same way for every journey test of that era that does not build it by hand: a
 * Mainframe running the network, the backbone to a junction, a rack behind it holding the era's server with its drives,
 * and a Personal Computer on the access line with its monitor, running the era's desktop system. Every machine is
 * built from the era's own parts, as {@link EraParts} picks them.
 *
 * <p>The layout, at y 2 on the ground: the Mainframe faces east from (2, 2, 2), its footprint over x 1 to 2 and z 1
 * to 3; the backbone runs east to the junction at (5, 2, 2), then on to the access router at (7, 2, 2), and south to
 * the rack, whose back takes it at z 5 (the rack faces south from (5, 2, 6)); the access line runs east to the
 * computer at (9, 2, 2), its monitor beyond it at (10, 2, 2). Every backbone run is straight, as the fibre needs.
 *
 * @param kit       the era's machines, cables and boards
 * @param mainframe the Mainframe running the network
 * @param rack      the rack, with the era's server in its top row
 * @param computer  the Personal Computer on the access line
 */
public record EraNetwork(EraKit kit, MainframeBlockEntity mainframe, ServerRackBlockEntity rack,
                         PersonalComputerBlockEntity computer) {

    public static final BlockPos MAINFRAME = new BlockPos(2, 2, 2);
    public static final BlockPos JUNCTION = new BlockPos(5, 2, 2);
    public static final BlockPos ROUTER = new BlockPos(7, 2, 2);
    public static final BlockPos RACK = new BlockPos(5, 2, 6);
    public static final BlockPos COMPUTER = new BlockPos(9, 2, 2);
    public static final BlockPos MONITOR = new BlockPos(10, 2, 2);
    /** Where a player stands to use the computer: in front of its monitor, looking back at it. */
    public static final BlockPos AT_THE_DESK = new BlockPos(12, 2, 2);
    /** A second Personal Computer, for what one computer does to or with another: south of the first. */
    public static final BlockPos SECOND_COMPUTER = new BlockPos(9, 2, 6);
    public static final BlockPos SECOND_MONITOR = new BlockPos(10, 2, 6);
    public static final BlockPos AT_THE_SECOND_DESK = new BlockPos(12, 2, 6);
    /*
     * How the monitors are turned: as a player at the desk puts one down, looking west at it, so its screen faces
     * them. A monitor's front is against its facing.
     */
    private static final Direction TOWARD_THE_DESK = Direction.WEST;

    /** Lays the era's network through {@code world} and switches every machine on. */
    public static EraNetwork lay(final TestWorldBuilder world, final EraKit kit) {
        final MainframeBlockEntity mainframe = mainframe(world, kit);
        world.setBlock(new BlockPos(3, 2, 2), kit.backbone());
        world.setBlock(new BlockPos(4, 2, 2), kit.backbone());
        world.setBlock(JUNCTION, kit.junction());
        world.setBlock(new BlockPos(6, 2, 2), kit.backbone());
        world.setBlock(ROUTER, kit.router());
        world.setBlock(new BlockPos(8, 2, 2), kit.access());
        world.setBlock(new BlockPos(5, 2, 3), kit.backbone());
        world.setBlock(new BlockPos(5, 2, 4), kit.backbone());
        final ServerRackBlockEntity rack = rack(world, kit);
        final PersonalComputerBlockEntity computer = computer(world, kit, COMPUTER);
        world.placeMonitor(MONITOR, TOWARD_THE_DESK, kit.monitor());
        return new EraNetwork(kit, mainframe, rack, computer);
    }

    /**
     * A second Personal Computer built and switched on as the first, with its monitor: the access line runs on from
     * the router's south side down to it, so it is on the same network. Laid after {@link #lay}.
     */
    public static PersonalComputerBlockEntity secondComputer(final TestWorldBuilder world, final EraKit kit) {
        for (int z = ROUTER.getZ() + 1; z <= SECOND_COMPUTER.getZ(); z++) {
            world.setBlock(new BlockPos(ROUTER.getX(), ROUTER.getY(), z), kit.access());
        }
        world.setBlock(new BlockPos(ROUTER.getX() + 1, ROUTER.getY(), SECOND_COMPUTER.getZ()), kit.access());
        final PersonalComputerBlockEntity computer = computer(world, kit, SECOND_COMPUTER);
        world.placeMonitor(SECOND_MONITOR, TOWARD_THE_DESK, kit.monitor());
        return computer;
    }

    /**
     * The era's Crafting Computer where {@link #secondComputer} would stand, on the access line from the router's
     * south side, built from the era's parts with a crafting card beside its graphics card, running the era's desktop
     * system, with its monitor. Laid after {@link #lay}.
     */
    public static CraftingComputerBlockEntity craftingComputer(final TestWorldBuilder world, final EraKit kit) {
        for (int z = ROUTER.getZ() + 1; z <= SECOND_COMPUTER.getZ(); z++) {
            world.setBlock(new BlockPos(ROUTER.getX(), ROUTER.getY(), z), kit.access());
        }
        world.setBlock(new BlockPos(ROUTER.getX() + 1, ROUTER.getY(), SECOND_COMPUTER.getZ()), kit.access());
        world.setBlock(SECOND_COMPUTER, kit.craftingComputer());
        world.faceRearTowardCable(SECOND_COMPUTER);
        final CraftingComputerBlockEntity computer = world.blockEntity(SECOND_COMPUTER,
                CraftingComputerBlockEntity.class);
        final ItemStackHandler hardware = computer.getHardware();
        final Item board = kit.computerBoard();
        hardware.setStackInSlot(CraftingComputerBlockEntity.MOTHERBOARD_SLOT, new ItemStack(board));
        hardware.setStackInSlot(CraftingComputerBlockEntity.CPU_SLOT, new ItemStack(EraParts.cpuFor(board)));
        hardware.setStackInSlot(CraftingComputerBlockEntity.RAM_SLOTS_START, new ItemStack(EraParts.ramFor(board)));
        hardware.setStackInSlot(CraftingComputerBlockEntity.PSU_SLOT, new ItemStack(EraParts.psuFor(kit.era())));
        hardware.setStackInSlot(CraftingComputerBlockEntity.PCIE_SLOTS_START,
                new ItemStack(EraParts.cardFor(board, GpuItem.class)));
        hardware.setStackInSlot(CraftingComputerBlockEntity.PCIE_SLOTS_START + 1,
                new ItemStack(EraParts.cardFor(board, CraftingCardItem.class)));
        hardware.setStackInSlot(CraftingComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(EraParts.diskFor(kit.era())));
        if (!computer.installOs(kit.desktopOs())) {
            throw new IllegalStateException("could not install " + kit.desktopOs() + " on the " + kit
                    + " Crafting Computer");
        }
        computer.togglePower();
        world.placeMonitor(SECOND_MONITOR, TOWARD_THE_DESK, kit.monitor());
        return computer;
    }

    /**
     * The era's 1U server, built with its board, processor, memory and supply, and with a graphics card when the board
     * has no video of its own, so a monitor beside the rack can show it.
     */
    public static ItemStack server(final EraKit kit) {
        final Item board = kit.serverBoard();
        return hasOwnVideo(board) ? built(kit.server(), board)
                : built(kit.server(), board, EraParts.cardFor(board, GpuItem.class));
    }

    /**
     * The era's storage server, built on the era's server board with its Vaultis Array, and with a graphics card
     * beside it when the board has no video of its own (the Legacy's), so a monitor can show its firmware.
     */
    public static ItemStack storageServer(final EraKit kit) {
        final Item board = kit.serverBoard();
        if (hasOwnVideo(board)) {
            return built(kit.storageServer(), board, kit.array());
        }
        return built(kit.storageServer(), board, kit.array(), EraParts.cardFor(board, GpuItem.class));
    }

    /**
     * {@code server}, built on {@code board} with the processor, memory and supply that go with it, and
     * {@code cards} in its expansion slots from the first on.
     */
    public static ItemStack built(final Item server, final Item board, final Item... cards) {
        final ItemStack stack = new ItemStack(server);
        final NonNullList<ItemStack> hardware = NonNullList.withSize(ServerHardwareHandler.SLOTS, ItemStack.EMPTY);
        hardware.set(ServerHardwareHandler.MOBO, new ItemStack(board));
        hardware.set(ServerHardwareHandler.CPU_START, new ItemStack(EraParts.cpuFor(board)));
        hardware.set(ServerHardwareHandler.RAM_START, new ItemStack(EraParts.ramFor(board)));
        hardware.set(ServerHardwareHandler.PSU, new ItemStack(EraParts.psuFor(EraParts.board(board).era())));
        for (int card = 0; card < cards.length; card++) {
            hardware.set(ServerHardwareHandler.GPU_START + card, new ItemStack(cards[card]));
        }
        stack.set(ComputingComponents.SERVER_HARDWARE.get(), ItemContainerContents.fromItems(hardware));
        return stack;
    }

    /** The era's Personal Computer at {@code at}, built and running the era's desktop system, its back on a cable. */
    public static PersonalComputerBlockEntity runningComputer(final TestWorldBuilder world, final EraKit kit,
                                                              final BlockPos at) {
        return computer(world, kit, at);
    }

    /**
     * The era's Personal Computer at {@code at}, built from the era's parts with {@code disks} of the era's disks and
     * switched on with no system on any of them: what a player has before the first install. Its back goes to a
     * cable beside it when there is one.
     */
    public static PersonalComputerBlockEntity bareComputer(final TestWorldBuilder world, final EraKit kit,
                                                           final BlockPos at, final int disks) {
        final PersonalComputerBlockEntity computer = assembled(world, kit, at);
        for (int disk = 1; disk < disks; disk++) {
            computer.getHardware().setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START + disk,
                    new ItemStack(EraParts.diskFor(kit.era())));
        }
        computer.togglePower();
        return computer;
    }

    private static MainframeBlockEntity mainframe(final TestWorldBuilder world, final EraKit kit) {
        formed(world, MAINFRAME, kit.mainframe().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        final MainframeBlockEntity mainframe = world.blockEntity(MAINFRAME, MainframeBlockEntity.class);
        final ItemStackHandler slots = mainframe.getInventory();
        final Item board = kit.mainframeBoard();
        slots.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT, new ItemStack(board));
        slots.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START, new ItemStack(EraParts.cpuFor(board)));
        slots.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START, new ItemStack(EraParts.ramFor(board)));
        slots.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(EraParts.psuFor(kit.era())));
        slots.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START, new ItemStack(EraParts.diskFor(kit.era())));
        slots.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                new ItemStack(EraParts.cardFor(board, GpuItem.class)));
        if (kit.fibre()) {
            slots.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START + 1,
                    new ItemStack(ComputingModule.OPTICAL_NETWORK_CARD.get()));
        }
        if (!mainframe.installOs(EraKit.NETWORK_OS)) {
            throw new IllegalStateException("could not install the network system on the " + kit + " Mainframe");
        }
        mainframe.togglePower();
        return mainframe;
    }

    private static ServerRackBlockEntity rack(final TestWorldBuilder world, final EraKit kit) {
        formed(world, RACK, kit.rack().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        final ServerRackBlockEntity rack = world.blockEntity(RACK, ServerRackBlockEntity.class);
        rack.getServers().setStackInSlot(0, server(kit));
        for (int drive = 0; drive < 2; drive++) {
            rack.insertDrive(0, new ItemStack(EraParts.diskFor(kit.era())));
        }
        if (kit.fibreInterface() != null) {
            rack.getServers().setStackInSlot(1, new ItemStack(kit.fibreInterface()));
        }
        return rack;
    }

    private static PersonalComputerBlockEntity computer(final TestWorldBuilder world, final EraKit kit,
                                                        final BlockPos at) {
        final PersonalComputerBlockEntity computer = assembled(world, kit, at);
        if (!computer.installOs(kit.desktopOs())) {
            throw new IllegalStateException("could not install " + kit.desktopOs() + " on the " + kit + " computer");
        }
        computer.togglePower();
        return computer;
    }

    /* The era's computer put down at {@code at} and built from the era's parts, with one disk, switched off. */
    private static PersonalComputerBlockEntity assembled(final TestWorldBuilder world, final EraKit kit,
                                                         final BlockPos at) {
        world.setBlock(at, kit.computer());
        world.faceRearTowardCable(at);
        final PersonalComputerBlockEntity computer = world.blockEntity(at, PersonalComputerBlockEntity.class);
        final ItemStackHandler hardware = computer.getHardware();
        final Item board = kit.computerBoard();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT, new ItemStack(board));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT, new ItemStack(EraParts.cpuFor(board)));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START, new ItemStack(EraParts.ramFor(board)));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(EraParts.psuFor(kit.era())));
        hardware.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                new ItemStack(EraParts.cardFor(board, GpuItem.class)));
        // Before the Transition a board had no sound of its own: the era's player put a sound card in beside it.
        if (!EraParts.board(board).hasOnBoardAudio()) {
            hardware.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START + 1,
                    new ItemStack(EraParts.cardFor(board, SoundCardItem.class)));
        }
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(EraParts.diskFor(kit.era())));
        return computer;
    }

    /* Whether a server board drives a screen with no card: an EEB board's management controller has an output. */
    private static boolean hasOwnVideo(final Item board) {
        return EraParts.board(board).formFactor() == FormFactor.EEB;
    }

    /* Sets a multiblock's controller and forms it, as a player placing it does. */
    private static void formed(final TestWorldBuilder world, final BlockPos at, final BlockState state) {
        world.setBlock(at, state);
        final Block block = state.getBlock();
        block.setPlacedBy(world.level(), world.absolute(at), state, null, new ItemStack(block));
    }
}
