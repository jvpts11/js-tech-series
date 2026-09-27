/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.gui.layout.ServerAssemblyLayout;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.item.MotherboardItem;
import dev.jstech.computers.item.ServerHardwareHandler;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.computers.operation.payload.RenameServerPayload;
import dev.jstech.computers.rack.RackChassis;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.PlayerSlots;
import dev.jstech.core.menu.SlotGroup;
import dev.jstech.core.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Menu for assembling a Server: hardware slots (board, CPU, RAM, GPU, PSU) over the held Server item's hardware,
 * plus the player inventory.
 */
public class ServerAssemblyMenu extends CoreMenu {

    private static final int HARDWARE_SLOTS = ServerHardwareHandler.SLOTS;

    private final Player owner;
    private final InteractionHand hand;
    private final ServerHardwareHandler hw;

    public ServerAssemblyMenu(final int containerId, final Inventory playerInventory,
                              final InteractionHand hand) {
        super(ComputingMenus.SERVER_ASSEMBLY_MENU.get(), containerId, playerInventory,
                player -> player == playerInventory.player
                        && playerInventory.player.getItemInHand(hand).getItem() instanceof ServerItem);
        this.owner = playerInventory.player;
        this.hand = hand;
        this.hw = new ServerHardwareHandler(owner, hand);

        final GuiLayout layout = ServerAssemblyLayout.layout();
        final List<Slot> hardwareSlots = new ArrayList<>();
        hardwareSlots.add(slot(hw, ServerHardwareHandler.MOBO, layout.slotAt("mobo")));
        hardwareSlots.add(slot(hw, ServerHardwareHandler.PSU, layout.slotAt("psu")));
        for (int i = 0; i < ServerHardwareHandler.CPU; i++) {
            final GuiLayout.SlotPosition at = layout.slotAt("cpu_" + i);
            hardwareSlots.add(new BoardSlot(hw, ServerHardwareHandler.CPU_START + i, at.x(), at.y(), i,
                    this::boardCpuSlots));
        }
        for (int i = 0; i < ServerHardwareHandler.RAM; i++) {
            final GuiLayout.SlotPosition at = layout.slotAt("ram_" + i);
            hardwareSlots.add(new BoardSlot(hw, ServerHardwareHandler.RAM_START + i, at.x(), at.y(), i,
                    this::boardRamSlots));
        }
        for (int i = 0; i < ServerHardwareHandler.GPU; i++) {
            final GuiLayout.SlotPosition at = layout.slotAt("gpu_" + i);
            hardwareSlots.add(new BoardSlot(hw, ServerHardwareHandler.GPU_START + i, at.x(), at.y(), i,
                    this::boardGpuSlots));
        }
        final SlotGroup hardware = slots(hardwareSlots.toArray(Slot[]::new));

        final PlayerSlots player = playerInventory(playerInventory, layout.playerInventoryAt());
        shiftClick(hardware, player.all());
        shiftClick(player.all(), hardware);
    }

    private MotherboardSpec boardSpec() {
        return hw.getStackInSlot(ServerHardwareHandler.MOBO).getItem() instanceof MotherboardItem board
                ? board.spec() : null;
    }

    private RackChassis chassis() {
        final var chassis = ServerItem.chassisOf(owner.getItemInHand(hand));
        return chassis != null ? chassis : RackChassis.SERVER;
    }

    public int boardCpuSlots() {
        // The chassis caps the sockets the board offers: the lower of the two wins.
        final MotherboardSpec spec = boardSpec();
        return spec == null ? 0
                : Math.min(Math.min(ServerHardwareHandler.CPU, spec.cpuSlots()), chassis().maxCpus());
    }

    public int boardRamSlots() {
        final MotherboardSpec spec = boardSpec();
        return spec == null ? 0 : Math.min(ServerHardwareHandler.RAM, spec.ramSlots());
    }

    public int boardGpuSlots() {
        final MotherboardSpec spec = boardSpec();
        return spec == null ? 0
                : Math.min(Math.min(ServerHardwareHandler.GPU, spec.pcieSlots()), chassis().maxPcie());
    }

    public static ServerAssemblyMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                                 final RegistryFriendlyByteBuf buf) {
        return new ServerAssemblyMenu(containerId, playerInventory, buf.readEnum(InteractionHand.class));
    }

    @Nullable
    public ComputerBuild currentBuild() {
        final List<ItemStack> parts = new ArrayList<>(HARDWARE_SLOTS);
        for (int i = 0; i < HARDWARE_SLOTS; i++) {
            parts.add(slots.get(i).getItem());
        }
        return ServerItem.buildFrom(parts);
    }

    @Nullable
    public HardwareEra hardwareEra() {
        final ComputerBuild build = currentBuild();
        return build == null ? null : build.motherboard().era();
    }

    @Nullable
    public UUID nodeUuid() {
        return ServerItem.nodeUuid(owner.getItemInHand(hand));
    }

    public String serverName() {
        return ServerItem.customName(owner.getItemInHand(hand));
    }

    public void setServerName(final String name) {
        final String capped = name.strip();
        ServerItem.setCustomName(owner.getItemInHand(hand),
                capped.length() > RenameServerPayload.MAX_LEN
                        ? capped.substring(0,
                            RenameServerPayload.MAX_LEN)
                        : capped);
        broadcastChanges();
    }
}
