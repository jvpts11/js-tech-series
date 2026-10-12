/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.item.GpuItem;
import dev.jstech.computers.item.ServerHardwareHandler;
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
        final PersonalComputerBlockEntity computer = computer(world, kit);
        world.placeMonitor(MONITOR, Direction.EAST, kit.monitor());
        return new EraNetwork(kit, mainframe, rack, computer);
    }

    /** The era's 1U server, built with its board, processor, memory and supply. */
    public static ItemStack server(final EraKit kit) {
        return built(kit.server(), kit.serverBoard());
    }

    /**
     * The era's storage server, built on the era's server board with its Vaultis Array, and with a graphics card
     * beside it when the board has no video of its own (the Legacy's), so a monitor can show its firmware.
     */
    public static ItemStack storageServer(final EraKit kit) {
        final Item board = kit.serverBoard();
        if (EraParts.board(board).formFactor() == FormFactor.EEB) {
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

    private static PersonalComputerBlockEntity computer(final TestWorldBuilder world, final EraKit kit) {
        world.setBlock(COMPUTER, kit.computer());
        world.faceRearTowardCable(COMPUTER);
        final PersonalComputerBlockEntity computer = world.blockEntity(COMPUTER, PersonalComputerBlockEntity.class);
        final ItemStackHandler hardware = computer.getHardware();
        final Item board = kit.computerBoard();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT, new ItemStack(board));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT, new ItemStack(EraParts.cpuFor(board)));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START, new ItemStack(EraParts.ramFor(board)));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(EraParts.psuFor(kit.era())));
        hardware.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                new ItemStack(EraParts.cardFor(board, GpuItem.class)));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(EraParts.diskFor(kit.era())));
        if (!computer.installOs(kit.desktopOs())) {
            throw new IllegalStateException("could not install " + kit.desktopOs() + " on the " + kit + " computer");
        }
        computer.togglePower();
        return computer;
    }

    /* Sets a multiblock's controller and forms it, as a player placing it does. */
    private static void formed(final TestWorldBuilder world, final BlockPos at, final BlockState state) {
        world.setBlock(at, state);
        final Block block = state.getBlock();
        block.setPlacedBy(world.level(), world.absolute(at), state, null, new ItemStack(block));
    }
}
