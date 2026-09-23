/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.operation.MoveLabels;
import dev.jstech.computers.program.iql.IIqlCondition;
import dev.jstech.computers.program.iql.IqlOperation;
import dev.jstech.computers.program.iql.IqlVerb;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.operation.OperationPriority;
import dev.jstech.core.peripheral.IPeripheralOwnerSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * A shell's network: the other machines it reaches, what the network holds, and the Operations and IQL it asks of
 * it. Effecting verbs route through the same Mainframe dispatch the graphical terminal uses. The layer of
 * {@link ServerCliComputer} over its files.
 */
abstract class ServerCliNetwork extends ServerCliFiles {

    protected ServerCliNetwork(final IComputerTerminalHost host, final ServerLevel level,
                               @Nullable final ServerPlayer typist) {
        super(host, level, typist);
    }

    @Override
    public boolean onNetwork() {
        return networkReads().online();
    }

    @Override
    public String networkId() {
        return networkReads().id();
    }

    @Override
    public boolean isMainframe() {
        return networkReads().isMainframe();
    }

    /**
     * Every machine on this network a remote shell can reach, keyed by host name: the Mainframe, the personal
     * computers, and the servers in their racks, for callers outside the CLI (Remote Control's list). The local
     * machine is left out, since you cannot ssh into the terminal you are already sitting at.
     */
    public Map<String, BlockEntity> remoteMachines() {
        return remotes().machines();
    }

    /** The machine on this network that {@code name} picks out, as its own shell; null when none or several. */
    @Nullable
    public ServerCliComputer remoteShell(final String name) {
        return remotes().find(name);
    }

    /** Whether this machine takes programs and commands from the other computers on its network. */
    public boolean remoteAllowed() {
        final ComputerConsoleState console = host.console();
        return console != null && console.settings().remoteAllowed();
    }

    @Override
    public List<RemoteHost> reachableHosts() {
        return remotes().hosts();
    }

    @Override
    public OpResult sshConnect(final String hostname) {
        return remotes().connect(hostname);
    }

    @Override
    public OpResult sshDisconnect() {
        return remotes().disconnect();
    }

    @Override
    public String sshSession() {
        return remotes().session();
    }

    @Override
    public List<NetworkShare> networkShares() {
        return remotes().networkShares();
    }

    @Override
    public NetSummary network() {
        return networkReads().summary();
    }

    @Override
    public List<StoredItem> query(final IIqlCondition where, final String server, final int limit) {
        return networkReads().query(where, server, limit);
    }

    @Override
    public List<StoredItem> queryObject(final String object, final IIqlCondition where, final String server,
                                        final int limit) {
        return networkReads().queryObject(object, where, server, limit);
    }

    @Override
    public List<ServerUse> servers() {
        return networkReads().servers();
    }

    @Override
    public ServerUse networkUse() {
        return networkReads().use();
    }

    @Override
    public List<Holding> find(final String item) {
        return networkReads().find(item);
    }

    @Override
    public List<ItemMatch> matching(final String text) {
        return networkReads().matching(text);
    }

    @Override
    public ItemDetail itemDetail(final String id) {
        return networkReads().itemDetail(id);
    }

    @Override
    public OpResult select(final String item, final long quantity) {
        return select(item, quantity, MoveLabels.SHELL);
    }

    @Override
    public OpResult select(final String item, final long quantity, final String origin) {
        return operations().select(item, quantity, origin);
    }

    @Override
    public OpResult takeToHand(final String item, final long quantity) {
        return operations().takeToHand(typist, item, quantity, MoveLabels.SHELL);
    }

    @Override
    public OpResult storeFromHand(final long quantity) {
        return operations().storeFromHand(typist, quantity, MoveLabels.SHELL);
    }

    @Override
    public OpResult fillHeld(final String item) {
        return operations().fillHeld(typist, item, MoveLabels.SHELL);
    }

    @Override
    public List<OperationStat> operationStats() {
        return mainframeStats().work();
    }

    @Override
    public int peakOperationsToday() {
        return mainframeStats().peakToday();
    }

    @Override
    public OpResult repriorityOperation(final String id, final String priority) {
        return operations().reprioritise(id, priority);
    }

    public OpResult cancelOperation(final String id) {
        return operations().cancel(id);
    }

    @Override
    public OpResult insert(final String item, final long quantity) {
        return operations().insert(item, quantity, OperationPriority.DEFAULT, MoveLabels.SHELL);
    }

    @Override
    public OpResult insert(final String item, final long quantity, final String origin) {
        return operations().insert(item, quantity, OperationPriority.DEFAULT, origin);
    }

    @Override
    public OpResult craft(final String item, final long quantity) {
        return operations().craft(item, quantity, OperationPriority.DEFAULT, MoveLabels.SHELL);
    }

    @Override
    public OpResult craft(final String item, final long quantity, final String origin) {
        return operations().craft(item, quantity, OperationPriority.DEFAULT, origin);
    }

    @Override
    public OpResult lock(final String item, final long quantity) {
        return operations().lock(item, quantity);
    }

    @Override
    public OpResult unlock(final String item) {
        return operations().unlock(item);
    }

    @Override
    public List<StoredItem> locks() {
        return operations().locks();
    }

    @Override
    public List<ActiveOp> activeOps() {
        return operations().list();
    }

    @Override
    public OpResult maintenance(final IqlVerb action) {
        return operations().maintenance(action);
    }

    @Override
    public List<String> peripherals() {
        if (hostBlock instanceof IPeripheralOwnerSupport owner) {
            final List<String> rows = new ArrayList<>();
            for (final long endpoint : owner.peripheralEndpoints()) {
                final BlockPos pos = BlockPos.of(endpoint);
                rows.add(level.getBlockState(pos).getBlock().getName().getString()
                        + " @ " + pos.getX() + "," + pos.getY() + "," + pos.getZ());
            }
            return rows;
        }
        return List.of();
    }

    @Override
    public OpResult engineControl(final String action) {
        return iql().control(action);
    }

    @Override
    public List<ServiceStatus> services() {
        return packages().mirror().services();
    }

    @Override
    public boolean iqlEngineInstalled() {
        return iql().installed();
    }

    @Override
    public OpResult execute(final IqlOperation op) {
        return iql().execute(op);
    }
}
