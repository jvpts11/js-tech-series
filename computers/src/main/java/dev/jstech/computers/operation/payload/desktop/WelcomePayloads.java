/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.desktop;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.client.os.WelcomeApp;
import dev.jstech.computers.item.CpuItem;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.RequestWelcomePayload;
import dev.jstech.computers.operation.payload.WelcomePayload;
import dev.jstech.computers.operation.payload.WelcomeStartupPayload;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.boot.WelcomeFacts;
import dev.jstech.computers.os.install.Installers;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The payloads of the welcome a system puts up the first time it comes up: what the machine is, and whether the
 * welcome is still wanted on later starts.
 *
 * <p>Everything the window shows is answered here, off the machine, at the moment it is asked.
 */
public final class WelcomePayloads {

    private WelcomePayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, RequestWelcomePayload.TYPE, RequestWelcomePayload.STREAM_CODEC,
                ComputerAccess.machine(RequestWelcomePayload::hostPos), WelcomePayloads::handleRequest);
        registrar.playToClient(WelcomePayload.TYPE, WelcomePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) ->
                        WelcomeApp.accept(payload)));
        ComputerAccess.accept(registrar, WelcomeStartupPayload.TYPE, WelcomeStartupPayload.STREAM_CODEC,
                ComputerAccess.machine(WelcomeStartupPayload::hostPos), WelcomePayloads::handleStartup);
    }

    private static void handleRequest(final RequestWelcomePayload payload, final ServerPlayer player,
                                      final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer)) {
            return;
        }
        PacketDistributor.sendToPlayer(player, facts(computer, level, payload));
    }

    private static void handleStartup(final WelcomeStartupPayload payload, final ServerPlayer player,
                                      final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer) {
            computer.setSystemWelcome(computer.systemWelcome().showingAtStartup(payload.show()));
        }
    }

    /** The machine as it stands, which is the whole of what a welcome is allowed to say. */
    private static WelcomePayload facts(final IOsHost computer, final ServerLevel level,
                                        final RequestWelcomePayload payload) {
        final OsDef system = computer.installedOs();
        final String mirror = Installers.mirrorHost(computer, level);
        final String network = Installers.networkHost(computer, level);
        return new WelcomePayload(payload.hostPos(), Installers.machineName(computer), cpuText(computer),
                (int) Math.min(Integer.MAX_VALUE, computer.ramTotalMb()),
                system == null ? "" : system.displayName(), WelcomeFacts.bootedSlot(computer),
                WelcomeFacts.bootedDisk(computer), WelcomeFacts.others(computer), network, mirror,
                computer.systemWelcome().showAtStartup(),
                WelcomeFacts.tips(computer, mirror, !network.isEmpty()));
    }

    /**
     * The processor really in the machine, by its own model, not the clock it happens to run at, as text each player
     * reads in their own language.
     */
    static Text cpuText(final IOsHost computer) {
        final ItemStack chip = cpuStack(computer);
        return chip.isEmpty() ? Text.EMPTY : GameText.of(chip.getHoverName());
    }

    /* The first processor seated in the machine, or nothing. */
    private static ItemStack cpuStack(final IOsHost computer) {
        if (!(computer instanceof AbstractComputerBlockEntity machine)) {
            return ItemStack.EMPTY;
        }
        final ItemStackHandler hardware = machine.getHardware();
        for (int i = 0; i < hardware.getSlots(); i++) {
            final ItemStack part = hardware.getStackInSlot(i);
            if (!part.isEmpty() && part.getItem() instanceof CpuItem) {
                return part;
            }
        }
        return ItemStack.EMPTY;
    }
}
