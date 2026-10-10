/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.menu.MenuValue;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.tier.HardwareEra;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The readings the terminal's tabs show, one synced value each, named for what they hold. The server reads each off
 * the host machine and the client gets the synced copy, so a screen asks the same question on both sides. The values
 * are registered with the menu in the order they are declared here, which is also the order they travel in.
 */
final class ComputerReadings {

    @Nullable
    private final IComputerTerminalHost host;
    private final Level level;
    private final MenuValue running;
    private final MenuValue buildValid;
    private final MenuValue networkLinkState;
    private final MenuValue capacity;
    private final MenuValue queues;
    private final MenuValue ramBuffer;
    private final MenuValue serverCount;
    private final MenuValue installedCpus;
    private final MenuValue cpuSlots;
    private final MenuValue installedRam;
    private final MenuValue ramSlots;
    private final MenuValue installedGpus;
    private final MenuValue gpuSlots;
    private final MenuValue installedDisks;
    private final MenuValue diskSlots;
    private final MenuValue storageUsed;
    private final MenuValue storageCapacity;
    private final MenuValue mainframeHost;
    private final MenuValue usableStorageSlots;
    private final MenuValue pendingOps;
    private final MenuValue runningOps;
    private final MenuValue completedOps;
    private final MenuValue pcCount;
    private final MenuValue subframeCount;
    private final MenuValue indexedTypes;
    private final MenuValue indexedServers;
    private final MenuValue activeLocks;
    private final MenuValue networkStorageUsed;
    private final MenuValue networkStorageTotal;
    private final MenuValue craftComputers;
    private final MenuValue era;
    private final MenuValue indexHealth;
    private final MenuValue indexHealthTypes;
    private final MenuValue patternsHost;

    /** Declares every reading on the menu that owns it, through that menu's own value and flag factories. */
    ComputerReadings(@Nullable final IComputerTerminalHost host, final Level level,
                     final Function<IntSupplier, MenuValue> value, final Function<BooleanSupplier, MenuValue> flag) {
        this.host = host;
        this.level = level;
        this.running = flag.apply(() -> hostFlag(IComputerTerminalHost::computerRunning));
        this.buildValid = flag.apply(() -> hostFlag(IComputerTerminalHost::computerBuildValid));
        this.networkLinkState = value.apply(() -> hostInt(IComputerTerminalHost::networkLinkState));
        this.capacity = value.apply(() -> hostClamped(IComputerTerminalHost::orchestrationCapacity));
        this.queues = value.apply(() -> hostInt(IComputerTerminalHost::computerQueues));
        this.ramBuffer = value.apply(() -> hostClamped(IComputerTerminalHost::computerRamBuffer));
        this.serverCount = value.apply(() -> hostInt(IComputerTerminalHost::networkServerCount));
        this.installedCpus = value.apply(() -> hostInt(IComputerTerminalHost::installedCpus));
        this.cpuSlots = value.apply(() -> hostInt(IComputerTerminalHost::cpuSlots));
        this.installedRam = value.apply(() -> hostInt(IComputerTerminalHost::installedRam));
        this.ramSlots = value.apply(() -> hostInt(IComputerTerminalHost::ramSlots));
        this.installedGpus = value.apply(() -> hostInt(IComputerTerminalHost::installedGpus));
        this.gpuSlots = value.apply(() -> hostInt(IComputerTerminalHost::gpuSlots));
        this.installedDisks = value.apply(() -> hostInt(IComputerTerminalHost::installedDisks));
        this.diskSlots = value.apply(() -> hostInt(IComputerTerminalHost::diskSlots));
        this.storageUsed = value.apply(() -> hostClamped(IComputerTerminalHost::localStorageUsed));
        this.storageCapacity = value.apply(() -> hostClamped(IComputerTerminalHost::localStorageCapacity));
        this.mainframeHost = flag.apply(() -> hostFlag(IComputerTerminalHost::isMainframeHost));
        this.usableStorageSlots = value.apply(() -> hostInt(IComputerTerminalHost::usableStorageSlots));
        this.pendingOps = value.apply(() -> hostInt(IComputerTerminalHost::pendingOperations));
        this.runningOps = value.apply(() -> hostInt(IComputerTerminalHost::runningOperations));
        this.completedOps = value.apply(() -> hostInt(IComputerTerminalHost::completedOperations));
        this.pcCount = value.apply(() -> hostInt(IComputerTerminalHost::networkPcCount));
        this.subframeCount = value.apply(() -> hostInt(IComputerTerminalHost::networkSubframeCount));
        this.indexedTypes = value.apply(() -> hostInt(IComputerTerminalHost::indexedTypes));
        this.indexedServers = value.apply(() -> hostInt(IComputerTerminalHost::indexedServers));
        this.activeLocks = value.apply(() -> hostInt(IComputerTerminalHost::activeLocks));
        this.networkStorageUsed = value.apply(() -> hostClamped(IComputerTerminalHost::networkStorageUsed));
        this.networkStorageTotal = value.apply(() -> hostClamped(IComputerTerminalHost::networkStorageTotal));
        this.craftComputers = value.apply(this::craftComputerCount);
        // Read fresh on every poll rather than cached at open: a board swap repaints the era live.
        this.era = value.apply(this::eraId);
        this.indexHealth = value.apply(() -> hostInt(IComputerTerminalHost::indexHealthState));
        this.indexHealthTypes = value.apply(() -> hostInt(IComputerTerminalHost::indexHealthTypeCount));
        /*
         * A synced reading, not a guess from the block: the Crafting Card that makes a host teachable
         * can be pulled while a player is looking straight at this tab.
         */
        this.patternsHost = flag.apply(() -> hostFlag(ComputerTerminalMenu::teachable));
    }

    public boolean running() {
        return running.isSet();
    }

    public boolean buildValid() {
        return buildValid.isSet();
    }

    public int networkLinkState() {
        return networkLinkState.get();
    }

    public int capacity() {
        return capacity.get();
    }

    public int queues() {
        return queues.get();
    }

    public int ramBuffer() {
        return ramBuffer.get();
    }

    public int serverCount() {
        return serverCount.get();
    }

    public int installedCpus() {
        return installedCpus.get();
    }

    public int cpuSlots() {
        return cpuSlots.get();
    }

    public int installedRam() {
        return installedRam.get();
    }

    public int ramSlots() {
        return ramSlots.get();
    }

    public int installedGpus() {
        return installedGpus.get();
    }

    public int gpuSlots() {
        return gpuSlots.get();
    }

    public int installedDisks() {
        return installedDisks.get();
    }

    public int diskSlots() {
        return diskSlots.get();
    }

    public int storageUsed() {
        return storageUsed.get();
    }

    public int storageCapacity() {
        return storageCapacity.get();
    }

    public boolean mainframeHost() {
        return mainframeHost.isSet();
    }

    public int usableStorageSlots() {
        return usableStorageSlots.get();
    }

    public int pendingOps() {
        return pendingOps.get();
    }

    public int runningOps() {
        return runningOps.get();
    }

    public int completedOps() {
        return completedOps.get();
    }

    public int pcCount() {
        return pcCount.get();
    }

    public int subframeCount() {
        return subframeCount.get();
    }

    public int indexedTypes() {
        return indexedTypes.get();
    }

    public int indexedServers() {
        return indexedServers.get();
    }

    public int activeLocks() {
        return activeLocks.get();
    }

    public int networkStorageUsed() {
        return networkStorageUsed.get();
    }

    public int networkStorageTotal() {
        return networkStorageTotal.get();
    }

    public int craftComputers() {
        return craftComputers.get();
    }

    public int era() {
        return era.get();
    }

    public int indexHealth() {
        return indexHealth.get();
    }

    public int indexHealthTypes() {
        return indexHealthTypes.get();
    }

    public boolean patternsHost() {
        return patternsHost.isSet();
    }

    private int craftComputerCount() {
        if (host == null || host.networkUuid() == null || !(level instanceof ServerLevel serverLevel)) {
            return 0;
        }
        return NetworkSystem.get(serverLevel)
                .craftingComputersOf(host.networkUuid()).size();
    }

    private int eraId() {
        if (host == null) {
            return 0;
        }
        final HardwareEra hostEra = host.displayEra();
        return hostEra == null ? -1 : hostEra.id();
    }

    private int hostInt(final ToIntFunction<IComputerTerminalHost> getter) {
        return host == null ? 0 : getter.applyAsInt(host);
    }

    private int hostClamped(final ToLongFunction<IComputerTerminalHost> getter) {
        return host == null ? 0 : clampInt(getter.applyAsLong(host));
    }

    private boolean hostFlag(final Predicate<IComputerTerminalHost> test) {
        return host != null && test.test(host);
    }

    private static int clampInt(final long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, value));
    }
}