/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.item.ServerHardwareHandler;
import dev.jstech.computers.registry.ComputingComponents;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Rack computers built and ready to seat, for tests that need a working machine rather than the assembly that makes
 * one: the Standard board, CPU, RAM and supply, and whatever a kind of machine adds.
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

    private static ItemStack built(final Item machine, final boolean withCpu, final boolean withPhi) {
        final ItemStack stack = new ItemStack(machine);
        final NonNullList<ItemStack> hardware = NonNullList.withSize(ServerHardwareHandler.SLOTS, ItemStack.EMPTY);
        hardware.set(ServerHardwareHandler.MOBO, new ItemStack(ComputingModule.MOTHERBOARD_EEB_P.get()));
        if (withCpu) {
            hardware.set(ServerHardwareHandler.CPU_START, new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
        }
        hardware.set(ServerHardwareHandler.RAM_START, new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        if (withPhi) {
            hardware.set(ServerHardwareHandler.GPU_START, new ItemStack(ComputingModule.PHI_5100.get()));
        }
        hardware.set(ServerHardwareHandler.PSU, new ItemStack(ComputingModule.PSU_650G.get()));
        stack.set(ComputingComponents.SERVER_HARDWARE.get(), ItemContainerContents.fromItems(hardware));
        return stack;
    }
}
