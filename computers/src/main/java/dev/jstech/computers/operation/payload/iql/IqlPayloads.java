/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.iql;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.NmsApp;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.IqlFileContentPayload;
import dev.jstech.computers.operation.payload.IqlFileListPayload;
import dev.jstech.computers.operation.payload.IqlResultPayload;
import dev.jstech.computers.operation.payload.NmsSchemaPayload;
import dev.jstech.computers.operation.payload.OpenIqlFilePayload;
import dev.jstech.computers.operation.payload.RequestIqlFileListPayload;
import dev.jstech.computers.operation.payload.RequestNmsSchemaPayload;
import dev.jstech.computers.operation.payload.RunIqlPayload;
import dev.jstech.computers.operation.payload.SaveIqlFilePayload;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.iql.IqlDefinition;
import dev.jstech.computers.program.iql.IqlSavedObject;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.network.ServerNode;
import dev.jstech.core.peripheral.IPeripheralOwner;
import dev.jstech.core.uuid.NetworkUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

import static dev.jstech.computers.operation.payload.files.FileAccess.filesystemKindOf;
import static dev.jstech.computers.operation.payload.network.NetworkLookup.networkLabel;
import static dev.jstech.computers.operation.payload.network.NetworkLookup.resolveMainframe;
import static dev.jstech.computers.operation.payload.network.NetworkLookup.serverLabel;

/**
 * The payloads of IQL: running a statement, the schema the NMS shows and the query files kept on the Mainframe's
 * disk.
 */
public final class IqlPayloads {

    private IqlPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, RunIqlPayload.TYPE, RunIqlPayload.STREAM_CODEC,
                ComputerAccess.machine(RunIqlPayload::hostPos), IqlPayloads::handleRunIql);
        registrar.playToClient(IqlResultPayload.TYPE, IqlResultPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(IqlPayloads::handleIqlResult));
        ComputerAccess.accept(registrar, RequestNmsSchemaPayload.TYPE, RequestNmsSchemaPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestNmsSchemaPayload::hostPos), IqlPayloads::handleRequestNmsSchema);
        registrar.playToClient(NmsSchemaPayload.TYPE, NmsSchemaPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(IqlPayloads::handleNmsSchema));
        ComputerAccess.accept(registrar, SaveIqlFilePayload.TYPE, SaveIqlFilePayload.STREAM_CODEC,
                ComputerAccess.machine(SaveIqlFilePayload::hostPos), IqlPayloads::handleSaveIqlFile);
        ComputerAccess.accept(registrar, RequestIqlFileListPayload.TYPE, RequestIqlFileListPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestIqlFileListPayload::hostPos), IqlPayloads::handleRequestIqlFileList);
        registrar.playToClient(IqlFileListPayload.TYPE, IqlFileListPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(IqlPayloads::handleIqlFileList));
        ComputerAccess.accept(registrar, OpenIqlFilePayload.TYPE, OpenIqlFilePayload.STREAM_CODEC,
                ComputerAccess.machine(OpenIqlFilePayload::hostPos), IqlPayloads::handleOpenIqlFile);
        registrar.playToClient(IqlFileContentPayload.TYPE, IqlFileContentPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(IqlPayloads::handleIqlFileContent));
    }

    /**
     * Anti-spoof for the windowed NMS (it has no container menu to authenticate against): the player must be
     * within 8 blocks of the host computer or one of its linked monitors, so a forged packet aimed at a
     * foreign computer is rejected.
     */
    private static boolean nmsNear(final ServerPlayer player, final BlockPos hostPos,
                                   final IComputerTerminalHost host) {
        final Vec3 p = player.position();
        if (hostPos.distToCenterSqr(p) <= 64.0) {
            return true;
        }
        if (host instanceof IPeripheralOwner owner) {
            for (final long endpoint : owner.linkedEndpoints()) {
                if (BlockPos.of(endpoint).distToCenterSqr(p) <= 64.0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void handleRunIql(final RunIqlPayload payload, final ServerPlayer player, final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos())
                instanceof IComputerTerminalHost host)
                || !nmsNear(player, payload.hostPos(), host)) {
            return;
        }
        final MainframeBlockEntity mainframe = resolveMainframe(level, host.networkUuid());
        if (mainframe == null) {
            PacketDistributor.sendToPlayer(player,
                    new IqlResultPayload(false, "the network has no running Mainframe", List.of()));
            return;
        }
        final var computer = new ServerCliComputer(host, level);
        final var engine = new IqlEngine(
                mainframe, computer, IqlResultPayload.MAX_ROWS);
        final var outcome = engine.run(payload.statement());
        final List<IqlResultPayload.Row> rows = new ArrayList<>(outcome.rows().size());
        for (final var item : outcome.rows()) {
            rows.add(new IqlResultPayload.Row(
                    item.detail().isEmpty() ? item.name() : item.name() + "  ·  " + item.detail(),
                    item.quantity()));
        }
        PacketDistributor.sendToPlayer(player,
                new IqlResultPayload(outcome.ok(), outcome.message(), rows));
    }

    private static void handleIqlResult(final IqlResultPayload payload, final Player player) {
        NmsApp.accept(payload);
    }

    private static void handleRequestNmsSchema(final RequestNmsSchemaPayload payload, final ServerPlayer player,
                                               final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos())
                instanceof IComputerTerminalHost host)
                || !nmsNear(player, payload.hostPos(), host)) {
            return;
        }
        PacketDistributor.sendToPlayer(player, nmsSchema(level, host));
    }

    private static void handleNmsSchema(final NmsSchemaPayload payload, final Player player) {
        NmsApp.acceptSchema(payload);
    }

    /**
     * The Object Explorer snapshot for an open Studio: the network label, the real server labels, and live item-type and active-operation counts. The IQL schema (table and column names) is fixed on the client; this fills in only the parts that reflect the running network.
     */
    public static NmsSchemaPayload nmsSchema(final ServerLevel level,
            final IComputerTerminalHost host) {
        final NetworkUuid net = host.networkUuid();
        if (net == null) {
            return new NmsSchemaPayload("jsc-net (offline)", List.of(), 0, 0,
                    NmsSchemaPayload.EngineSnapshot.offline());
        }
        final NetworkSystem system = NetworkSystem.get(level);
        final List<String> servers = new ArrayList<>();
        for (final ServerNode server : system.serversOf(net)) {
            if (servers.size() >= NmsSchemaPayload.MAX_SERVERS) {
                break;
            }
            servers.add(serverLabel(level, server.nodeUuid()));
        }
        final int itemTypes = NetworkStorage
                .of(level, net).query().size();
        final MainframeBlockEntity mainframe = resolveMainframe(level, net);
        final int operations = mainframe != null ? mainframe.activeOperationRecords().size() : 0;
        return new NmsSchemaPayload(networkLabel(net), List.copyOf(servers), itemTypes, operations,
                engineSnapshot(mainframe));
    }

    private static NmsSchemaPayload.EngineSnapshot engineSnapshot(final MainframeBlockEntity mainframe) {
        if (mainframe == null || !mainframe.isIqlEngineInstalled()) {
            return NmsSchemaPayload.EngineSnapshot.offline();
        }
        final var catalog = mainframe.iqlCatalog();
        return new NmsSchemaPayload.EngineSnapshot(mainframe.isIqlEngineRunning() ? "running" : "stopped",
                objectNames(catalog.ofType(
                        IqlDefinition.ObjectType.VIEW)),
                objectNames(catalog.ofType(
                        IqlDefinition.ObjectType.PROCEDURE)),
                objectNames(catalog.ofType(
                        IqlDefinition.ObjectType.JOB)),
                mainframe.savedScript());
    }

    private static List<String> objectNames(
            final List<IqlSavedObject> objects) {
        final List<String> names = new ArrayList<>();
        for (final var object : objects) {
            if (names.size() >= NmsSchemaPayload.MAX_OBJECTS) {
                break;
            }
            names.add(object.name());
        }
        return names;
    }

    /**
     * Computes the available free weight on the given disk, mirroring the formula used in
     * {@link IOsHost#installOs}:
     * capacity minus storage used minus filesystem used minus the OS footprint.
     */
    private static long computeDiskFreeWeight(
            final IOsHost computer,
            final ItemStack disk) {
        if (!(disk.getItem() instanceof DiskItem diskItem)) {
            return 0L;
        }
        final long capacityWeight = diskItem.spec().capacityItems() * StorageKey.MB_EQ_PER_ITEM;
        final long storageUsed = DriveVolumes.usedWeight(disk);
        final long fsUsed = DiskFilesystem.filesWeight(disk);
        final long osReserved = computer.reservedByOs() * StorageKey.MB_EQ_PER_ITEM;
        return Math.max(0L, capacityWeight - storageUsed - fsUsed - osReserved);
    }

    /**
     * Resolves the Mainframe reachable from {@code hostPos}, then writes the editor content to an
     * {@code .iql} file on its system disk. Replies with a refreshed {@link IqlFileListPayload}
     * carrying a short outcome message in the status field.
     */
    private static void handleSaveIqlFile(final SaveIqlFilePayload payload, final ServerPlayer player,
                                          final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos())
                instanceof IComputerTerminalHost host)
                || !nmsNear(player, payload.hostPos(), host)
                || host.networkUuid() == null) {
            return;
        }
        final MainframeBlockEntity mainframe = resolveMainframe(level, host.networkUuid());
        if (mainframe == null) {
            PacketDistributor.sendToPlayer(player,
                    new IqlFileListPayload(List.of(), "no Mainframe on network", false));
            return;
        }
        final ItemStack sysDisk = mainframe.systemDisk();
        if (sysDisk.isEmpty()) {
            PacketDistributor.sendToPlayer(player,
                    new IqlFileListPayload(List.of(), "Mainframe has no system disk", false));
            return;
        }
        final FilesystemKind kind = filesystemKindOf(mainframe);
        if (kind == FilesystemKind.NONE) {
            PacketDistributor.sendToPlayer(player,
                    new IqlFileListPayload(List.of(), "no OS installed on Mainframe disk", false));
            return;
        }
        final String fileName = sanitizeIqlName(payload.fileName()) + ".iql";
        final long freeWeight = computeDiskFreeWeight(mainframe, sysDisk);
        final DiskFilesystem.WriteResult result =
                DiskFilesystem.write(sysDisk, fileName, FileType.IQL, payload.content(),
                        freeWeight, kind, mainframe.getLevel() == null ? 0L : mainframe.getLevel().getGameTime());
        final boolean ok = result == DiskFilesystem.WriteResult.OK;
        if (ok) {
            mainframe.setChanged();
        }
        final String status = switch (result) {
            case OK -> "saved: " + fileName;
            case DISK_FULL -> "disk full, free space on the Mainframe's system disk";
            case INVALID_PATH -> "invalid file name";
            case READ_ONLY -> "file type is read-only";
        };
        PacketDistributor.sendToPlayer(player, iqlFileList(sysDisk, kind, status, ok));
    }

    /** Sends the list of {@code .iql} files on the Mainframe's system disk to the NMS client. */
    private static void handleRequestIqlFileList(final RequestIqlFileListPayload payload, final ServerPlayer player,
                                                 final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos())
                instanceof IComputerTerminalHost host)
                || !nmsNear(player, payload.hostPos(), host)
                || host.networkUuid() == null) {
            return;
        }
        final MainframeBlockEntity mainframe = resolveMainframe(level, host.networkUuid());
        if (mainframe == null) {
            PacketDistributor.sendToPlayer(player,
                    new IqlFileListPayload(List.of(), "", false));
            return;
        }
        final ItemStack sysDisk = mainframe.systemDisk();
        if (sysDisk.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new IqlFileListPayload(List.of(), "", true));
            return;
        }
        final FilesystemKind kind = filesystemKindOf(mainframe);
        PacketDistributor.sendToPlayer(player, iqlFileList(sysDisk, kind, "", true));
    }

    /** Forwards the {@link IqlFileListPayload} to the open NMS screen. */
    private static void handleIqlFileList(final IqlFileListPayload payload, final Player player) {
        NmsApp.acceptFileList(payload);
    }

    /** Reads an {@code .iql} file from the Mainframe's disk and sends its content back. */
    private static void handleOpenIqlFile(final OpenIqlFilePayload payload, final ServerPlayer player,
                                          final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos())
                instanceof IComputerTerminalHost host)
                || !nmsNear(player, payload.hostPos(), host)
                || host.networkUuid() == null) {
            return;
        }
        final MainframeBlockEntity mainframe = resolveMainframe(level, host.networkUuid());
        if (mainframe == null) {
            PacketDistributor.sendToPlayer(player,
                    new IqlFileContentPayload("", "", false));
            return;
        }
        final ItemStack sysDisk = mainframe.systemDisk();
        if (sysDisk.isEmpty()) {
            PacketDistributor.sendToPlayer(player,
                    new IqlFileContentPayload("", "", false));
            return;
        }
        final var content = DiskFilesystem.read(sysDisk, payload.fileName());
        if (content.isEmpty()) {
            PacketDistributor.sendToPlayer(player,
                    new IqlFileContentPayload("", "", false));
            return;
        }
        PacketDistributor.sendToPlayer(player,
                new IqlFileContentPayload(payload.fileName(), content.get(), true));
    }

    /** Forwards the {@link IqlFileContentPayload} to the open NMS screen. */
    private static void handleIqlFileContent(final IqlFileContentPayload payload, final Player player) {
        NmsApp.acceptFileContent(payload);
    }

    /** Builds the payload listing every {@code .iql} file on the given disk. */
    private static IqlFileListPayload iqlFileList(final ItemStack disk, final FilesystemKind kind,
                                                   final String status, final boolean ok) {
        final List<DiskFilesystem.FileEntry> entries = DiskFilesystem.list(disk, "", kind);
        final List<String> names = new ArrayList<>();
        for (final DiskFilesystem.FileEntry entry : entries) {
            if (entry.type() == FileType.IQL && names.size() < IqlFileListPayload.MAX_FILES) {
                names.add(entry.path());
            }
        }
        return new IqlFileListPayload(names, status, ok);
    }

    /** Sanitizes a user-provided base name for an {@code .iql} file (strips extension and invalid chars). */
    private static String sanitizeIqlName(final String raw) {
        String name = raw == null ? "" : raw.trim();
        final int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        name = name.replaceAll("[/\\\\\\x00-\\x1F]", "_");
        if (name.isEmpty()) {
            name = "query";
        }
        if (name.length() > SaveIqlFilePayload.MAX_NAME_LEN) {
            name = name.substring(0, SaveIqlFilePayload.MAX_NAME_LEN);
        }
        return name;
    }
}
