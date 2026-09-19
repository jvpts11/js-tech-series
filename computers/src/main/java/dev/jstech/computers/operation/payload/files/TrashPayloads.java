/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.files;

import dev.jstech.computers.client.os.TrashApp;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.DesktopBalloonPayload;
import dev.jstech.computers.operation.payload.RequestTrashPayload;
import dev.jstech.computers.operation.payload.TrashActionPayload;
import dev.jstech.computers.operation.payload.TrashFilePayload;
import dev.jstech.computers.operation.payload.TrashListingPayload;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.KernelDef;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.os.UnixTree;
import dev.jstech.computers.os.fs.DiskTrash;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.TrashFolder;
import dev.jstech.computers.os.fs.TrashKind;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The payloads of a desktop's trash: putting a file in, listing what is there, and putting back, destroying or
 * emptying it.
 *
 * <p>The trash lives on the system disk only. A medium or another machine's share has none, and what a desktop deletes
 * there it deletes for good, after asking, through the ordinary delete.
 */
public final class TrashPayloads {

    private TrashPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, TrashFilePayload.TYPE, TrashFilePayload.STREAM_CODEC,
                ComputerAccess.machine(TrashFilePayload::hostPos), TrashPayloads::handleTrashFile);
        ComputerAccess.accept(registrar, RequestTrashPayload.TYPE, RequestTrashPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestTrashPayload::hostPos), TrashPayloads::handleRequestTrash);
        ComputerAccess.accept(registrar, TrashActionPayload.TYPE, TrashActionPayload.STREAM_CODEC,
                ComputerAccess.machine(TrashActionPayload::hostPos), TrashPayloads::handleTrashAction);
        registrar.playToClient(TrashListingPayload.TYPE, TrashListingPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) -> TrashApp.accept(payload)));
    }

    /**
     * The trash of the desktop a machine runs, or null for a machine with no desktop to keep one. The desktop decides
     * rather than the system alone, since CDE keeps its own on whichever system it runs; a machine that is off is
     * asked about the desktop it would start.
     */
    @Nullable
    public static TrashFolder trashOf(final IOsHost computer) {
        final OsDef os = computer.installedOs();
        if (os == null || FileAccess.filesystemKindOf(computer) != FilesystemKind.HIERARCHICAL) {
            return null;
        }
        final ResourceLocation booted = computer.bootedDesktopId();
        final ResourceLocation desktopId = booted != null ? booted : computer.installedDesktopId();
        final DesktopEnvironmentDef desktop = desktopId == null ? null : OsRegistry.getDesktop(desktopId);
        final KernelDef kernel = OsRegistry.getKernel(os.kernelId());
        final boolean unix = kernel != null && kernel.shellFamily() == ShellFamily.POSIX;
        final boolean cde = desktop != null && desktop.panelStyle() == PanelStyle.CDE;
        return TrashFolder.of(TrashKind.of(unix, cde), unix ? UnixTree.of(os.platform()).homePath() : "");
    }

    private static void handleTrashFile(final TrashFilePayload payload, final ServerPlayer player,
                                        final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer)) {
            return;
        }
        final String path = payload.path();
        final TrashFolder trash = trashOf(computer);
        final ItemStack disk = computer.systemDisk();
        // A medium or a share has no trash, and asks for the ordinary delete instead; a path here is refused.
        if (trash == null || disk.isEmpty() || path.startsWith("media:") || path.startsWith(FileAccess.NET_ROOT)) {
            return;
        }
        final long now = level.getGameTime();
        if (trash.holds(path)) {
            if (DiskTrash.destroyWithin(disk, trash, path, now)) {
                computer.setChanged();
            }
            return;
        }
        final String refusal = switch (DiskTrash.put(disk, trash, path, computer.systemDiskFreeWeight(), now)) {
            case DONE -> {
                computer.setChanged();
                yield "";
            }
            case NO_ROOM -> "There is no room left on the disk to keep " + FsPaths.fileName(path) + ".";
            case HOLDS_TRASH -> FsPaths.fileName(path) + " holds the " + trash.kind().title() + " itself.";
            // Already gone, most likely by a second click on the same thing: nothing to say.
            case MISSING -> "";
        };
        if (!refusal.isEmpty()) {
            PacketDistributor.sendToPlayer(player,
                    new DesktopBalloonPayload(payload.hostPos(), trash.kind().title(), refusal, ""));
        }
    }

    private static void handleRequestTrash(final RequestTrashPayload payload, final ServerPlayer player,
                                           final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer) {
            sendListing(player, payload.hostPos(), computer);
        }
    }

    private static void handleTrashAction(final TrashActionPayload payload, final ServerPlayer player,
                                          final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer)) {
            return;
        }
        final TrashFolder trash = trashOf(computer);
        final ItemStack disk = computer.systemDisk();
        if (trash == null || disk.isEmpty()) {
            return;
        }
        final long now = level.getGameTime();
        boolean changed = false;
        switch (payload.action()) {
            case EMPTY -> changed = DiskTrash.empty(disk, trash);
            case RESTORE -> {
                for (final String stored : payload.stored()) {
                    changed |= DiskTrash.restore(disk, trash, stored, now);
                }
            }
            case SHRED -> {
                for (final String stored : payload.stored()) {
                    changed |= DiskTrash.shred(disk, trash, stored, now);
                }
            }
        }
        if (changed) {
            computer.setChanged();
        }
        sendListing(player, payload.hostPos(), computer);
    }

    /** Tells a player what is in the trash of a machine, which every trash window of that machine then shows. */
    private static void sendListing(final ServerPlayer player, final BlockPos hostPos, final IOsHost computer) {
        final TrashFolder trash = trashOf(computer);
        final ItemStack disk = computer.systemDisk();
        final List<TrashListingPayload.WireEntry> wire = new ArrayList<>();
        if (trash != null && !disk.isEmpty()) {
            for (final DiskTrash.Entry one : DiskTrash.list(disk, trash)) {
                wire.add(new TrashListingPayload.WireEntry(one.stored(), one.original(), one.directory(),
                        one.weight()));
            }
        }
        PacketDistributor.sendToPlayer(player, new TrashListingPayload(hostPos, wire));
    }
}
