/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.item.ServerHardwareHandler;
import dev.jstech.computers.registry.ComputingComponents;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jetbrains.annotations.Nullable;

/**
 * Rack computers built and ready to seat, for tests that need a working machine rather than the assembly that makes
 * one: the Standard board, CPU, RAM and supply, and whatever a kind of machine adds; the Advanced node with the
 * Advanced parts.
 */
public final class ServerStacks {

    private ServerStacks() {
    }

    /** A 1U server ready to run. */
    public static ItemStack defaultServer() {
        return built(ComputingModule.SERVER.get(), true, false);
    }

    /** A 2U storage server ready to run: the same board, CPU, RAM and supply as the default server. */
    public static ItemStack defaultStorageServer() {
        return built(ComputingModule.STORAGE_SERVER.get(), true, false);
    }

    /** A server with everything but a processor, which cannot pass its self-test. */
    public static ItemStack cpulessServer() {
        return built(ComputingModule.SERVER.get(), false, false);
    }

    /** A node ready to run: board, CPU, RAM, supply, and the entry crafting co-processor in its slot. */
    public static ItemStack defaultSupercomputerNode() {
        return built(ComputingModule.SUPERCOMPUTER_NODE.get(), true, true);
    }

    /**
     * An Advanced node ready to run: the era's server board, an Epic, DDR4, the Phi 9000 and a Tessera H100 in its
     * accelerator sleds, and a supply for them.
     */
    public static ItemStack advancedSupercomputerNode() {
        final ItemStack stack = new ItemStack(ComputingModule.ADVANCED_SUPERCOMPUTER_NODE.get());
        final NonNullList<ItemStack> hardware = NonNullList.withSize(ServerHardwareHandler.SLOTS, ItemStack.EMPTY);
        hardware.set(ServerHardwareHandler.MOBO, new ItemStack(HardwareItems.MOTHERBOARD_EEB_A_SP3.get()));
        hardware.set(ServerHardwareHandler.CPU_START, new ItemStack(HardwareItems.CPU_VELOCION_EPIC_7251.get()));
        hardware.set(ServerHardwareHandler.RAM_START, new ItemStack(HardwareItems.RAM_DDR4_16384.get()));
        hardware.set(ServerHardwareHandler.GPU_START, new ItemStack(ComputingModule.PHI_9000.get()));
        hardware.set(ServerHardwareHandler.GPU_START + 1, new ItemStack(HardwareItems.GPU_TESSERA_H100.get()));
        hardware.set(ServerHardwareHandler.PSU, new ItemStack(HardwareItems.PSU_1200P.get()));
        stack.set(ComputingComponents.SERVER_HARDWARE.get(), ItemContainerContents.fromItems(hardware));
        return stack;
    }

    /** The default server with an Optical Network Card in its first card slot, which puts its cabinet on the fibre. */
    public static ItemStack opticalServer() {
        return built(ComputingModule.SERVER.get(), true, ComputingModule.OPTICAL_NETWORK_CARD.get());
    }

    private static ItemStack built(final Item machine, final boolean withCpu, final boolean withPhi) {
        return built(machine, withCpu, withPhi ? ComputingModule.PHI_5100.get() : null);
    }

    private static ItemStack built(final Item machine, final boolean withCpu, @Nullable final Item card) {
        final ItemStack stack = new ItemStack(machine);
        final NonNullList<ItemStack> hardware = NonNullList.withSize(ServerHardwareHandler.SLOTS, ItemStack.EMPTY);
        hardware.set(ServerHardwareHandler.MOBO, new ItemStack(ComputingModule.MOTHERBOARD_EEB_S_2011.get()));
        if (withCpu) {
            hardware.set(ServerHardwareHandler.CPU_START, new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
        }
        hardware.set(ServerHardwareHandler.RAM_START, new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        if (card != null) {
            hardware.set(ServerHardwareHandler.GPU_START, new ItemStack(card));
        }
        hardware.set(ServerHardwareHandler.PSU, new ItemStack(ComputingModule.PSU_650G.get()));
        stack.set(ComputingComponents.SERVER_HARDWARE.get(), ItemContainerContents.fromItems(hardware));
        return stack;
    }
}
