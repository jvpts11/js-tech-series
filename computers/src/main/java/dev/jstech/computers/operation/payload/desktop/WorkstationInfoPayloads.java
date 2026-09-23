/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.desktop;

import dev.jstech.computers.client.os.WorkstationInfoApp;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.RequestWorkstationInfoPayload;
import dev.jstech.computers.operation.payload.WorkstationInfoPayload;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.KernelNames;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ProgramVersions;
import dev.jstech.computers.os.UnixTree;
import dev.jstech.computers.os.WorkstationFacts;
import dev.jstech.computers.os.install.Installers;
import dev.jstech.core.uuid.NetworkUuid;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The payloads of CDE's Workstation Info: the window asks, and the machine says what it is, read off it at the
 * moment it is asked. The window asks again every few seconds while it is open, which is what keeps its memory
 * figure current.
 */
public final class WorkstationInfoPayloads {

    private WorkstationInfoPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, RequestWorkstationInfoPayload.TYPE,
                RequestWorkstationInfoPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestWorkstationInfoPayload::hostPos), WorkstationInfoPayloads::handle);
        registrar.playToClient(WorkstationInfoPayload.TYPE, WorkstationInfoPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) -> WorkstationInfoApp.accept(payload)));
    }

    /** What the machine is, as the window shows it. */
    public static WorkstationFacts factsOf(final IOsHost computer) {
        final OsDef system = computer.installedOs();
        final Platform platform = system == null ? Platform.UNIX : system.platform();
        final NetworkUuid network = computer.networkUuid();
        final ItemStack disk = computer.systemDisk();
        final long diskMb = disk.getItem() instanceof DiskItem item ? item.spec().capacityMb() : 0L;
        return new WorkstationFacts(UnixTree.of(platform).home().getLast(), Installers.hostName(computer),
                network == null ? "" : network.value().toString(), systemOf(system),
                KernelNames.architecture(platform, computer.processorBits()), windowSystemOf(computer),
                WelcomePayloads.cpuName(computer), computer.maxCpuMhz(), computer.ramTotalMb(),
                computer.ramLedger().usedMb(), computer.totalVramMb(), diskMb, SettingsSnapshots.usedMb(disk));
    }

    private static void handle(final RequestWorkstationInfoPayload payload, final ServerPlayer player,
                               final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer) {
            PacketDistributor.sendToPlayer(player, new WorkstationInfoPayload(payload.hostPos(), factsOf(computer)));
        }
    }

    /**
     * The system by name with its release: a Linux names its distribution and then the kernel under it, which is
     * where its release belongs, and the others put their release after their name, as their own report does.
     */
    private static String systemOf(final OsDef system) {
        if (system == null) {
            return "";
        }
        final Platform platform = system.platform();
        final String release = KernelNames.release(platform);
        return platform == Platform.LINUX
                ? system.displayName() + ", " + KernelNames.name(platform) + " " + release
                : system.displayName() + " " + release;
    }

    /** The desktop the machine booted with, by name and version, or empty when it came up at a prompt. */
    private static String windowSystemOf(final IOsHost computer) {
        final ResourceLocation id = computer.bootedDesktopId();
        final DesktopEnvironmentDef desktop = id == null ? null : OsRegistry.getDesktop(id);
        return desktop == null ? "" : desktop.displayName() + " " + ProgramVersions.of(id);
    }
}
