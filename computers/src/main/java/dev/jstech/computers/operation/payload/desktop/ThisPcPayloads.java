/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.desktop;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.ThisPcApp;
import dev.jstech.computers.item.CpuItem;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.item.GpuItem;
import dev.jstech.computers.item.HardwareTooltip;
import dev.jstech.computers.item.MotherboardItem;
import dev.jstech.computers.item.PsuItem;
import dev.jstech.computers.operation.payload.CancelSetupPayload;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.EjectMediaPayload;
import dev.jstech.computers.operation.payload.InstallFromMediaPayload;
import dev.jstech.computers.operation.payload.RequestThisPcPayload;
import dev.jstech.computers.operation.payload.SetupProgressPayload;
import dev.jstech.computers.operation.payload.ThisPcPayload;
import dev.jstech.computers.os.Branding;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.MinSpecTooltip;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.install.SetupJob;
import dev.jstech.computers.os.install.SetupRunner;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import static dev.jstech.computers.operation.payload.network.NetworkLookup.networkLabel;

/**
 * The payloads of This PC: the machine's page and its media, ejecting and installing from a disc, and the progress
 * of a setup.
 */
public final class ThisPcPayloads {

    private ThisPcPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, RequestThisPcPayload.TYPE, RequestThisPcPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestThisPcPayload::hostPos), ThisPcPayloads::handleRequestThisPc);
        registrar.playToClient(ThisPcPayload.TYPE, ThisPcPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(ThisPcPayloads::handleThisPc));
        ComputerAccess.accept(registrar, EjectMediaPayload.TYPE, EjectMediaPayload.STREAM_CODEC,
                ComputerAccess.machine(EjectMediaPayload::hostPos), ThisPcPayloads::handleEjectMedia);
        ComputerAccess.accept(registrar, InstallFromMediaPayload.TYPE, InstallFromMediaPayload.STREAM_CODEC,
                ComputerAccess.machine(InstallFromMediaPayload::hostPos), ThisPcPayloads::handleInstallFromMedia);
        registrar.playToClient(SetupProgressPayload.TYPE, SetupProgressPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(ThisPcPayloads::handleSetupProgress));
        ComputerAccess.accept(registrar, CancelSetupPayload.TYPE, CancelSetupPayload.STREAM_CODEC,
                ComputerAccess.machine(CancelSetupPayload::hostPos), ThisPcPayloads::handleCancelSetup);
    }

    private static void handleRequestThisPc(final RequestThisPcPayload payload, final ServerPlayer player,
                                            final ServerLevel level) {
        final List<ThisPcPayload.WireDisk> disks = new ArrayList<>();
        final List<ThisPcPayload.WireMedia> media = new ArrayList<>();
        final List<String> installed = new ArrayList<>();
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer) {
            final ItemStack sys = computer.systemDisk();
            int slot = 0;
            for (final ItemStack stack : computer.diskStacks()) {
                if (stack.getItem() instanceof DiskItem diskItem) {
                    final long cap = diskItem.spec().capacityItems();
                    final long storageW =
                            DriveVolumes.usedWeight(stack);
                    final long fsW = DiskFilesystem.filesWeight(stack);
                    final ResourceLocation osId =
                            stack.get(ComputingModule.SYSTEM_OS.get());
                    final OsDef os =
                            osId != null ? OsRegistry.getOs(osId) : null;
                    final long osItems = os != null ? os.footprintItemsOn(diskItem.spec().era()) : 0L;
                    final long mbEq = StorageKey.MB_EQ_PER_ITEM;
                    final long storeItems = storageW / mbEq;
                    final long fileItems = fsW / mbEq;
                    final long usedItems = storeItems + fileItems + osItems;
                    /*
                     * The three shares travel separately, so the disk can show where its space
                     * actually went instead of one anonymous "used" number.
                     */
                    disks.add(new ThisPcPayload.WireDisk(slot, stack.getHoverName().getString(),
                            cap, usedItems, stack == sys, osId != null ? osId.getPath() : "",
                            osItems, storeItems, fileItems));
                }
                slot++;
            }
            for (final long endpoint : computer.linkedEndpoints()) {
                if (level.getBlockEntity(BlockPos.of(endpoint))
                        instanceof MediaReaderBlockEntity reader) {
                    media.add(mediaRow(computer, payload.hostPos(), endpoint, reader));
                }
            }
            installed.addAll(computer.console().installed());
            PacketDistributor.sendToPlayer(player, new ThisPcPayload(machineCard(level, computer, payload.hostPos()),
                    disks, media, installed));
            return;
        }
        PacketDistributor.sendToPlayer(player,
                new ThisPcPayload(ThisPcPayload.WireMachine.EMPTY, disks, media, installed));
    }

    /** One drive row for This PC: what is in the drive and, for an installer, what it would install. */
    private static ThisPcPayload.WireMedia mediaRow(
            final IOsHost computer, final BlockPos host,
            final long endpoint, final MediaReaderBlockEntity reader) {
        final ItemStack m = reader.mediaSlot().getStackInSlot(0);
        final MediaKind kind = m.isEmpty() ? null : reader.insertedKind();
        final ResourceLocation pl = m.isEmpty() ? null : reader.insertedPayload();
        String payloadName = "";
        int payloadYear = 0;
        String packageId = "";
        String needs = "";
        boolean installable = false;
        if (pl != null && kind == MediaKind.PROGRAM_INSTALL) {
            final ProgramSpec spec =
                    OsRegistry.getProgram(pl);
            installable = !computer.console().isInstalled(pl.toString());
            if (spec != null) {
                payloadName = spec.displayName();
                payloadYear = Branding.year(spec.era());
                packageId = spec.commandName();
                needs = joinPlain(MinSpecTooltip.programMinSpec(pl));
            }
        } else if (pl != null && kind == MediaKind.OS_INSTALL) {
            final OsDef os =
                    OsRegistry.getOs(pl);
            if (os != null) {
                payloadName = os.displayName();
                payloadYear = Branding.osYear(os.displayName(), os.minEra());
                packageId = os.id().getPath();
                needs = joinPlain(MinSpecTooltip.osMinSpec(pl));
            }
        }
        final long stored = kind == MediaKind.DATA
                ? reader.insertedData().total() : 0L;
        final BlockPos at = BlockPos.of(endpoint);
        final int blocksAway = Math.abs(at.getX() - host.getX()) + Math.abs(at.getY() - host.getY())
                + Math.abs(at.getZ() - host.getZ());
        return new ThisPcPayload.WireMedia(endpoint, reader.driveType().serializedName(),
                m.isEmpty() ? "" : m.getHoverName().getString(), kind != null ? kind.serializedName() : "",
                pl != null ? pl.getPath() : "", installable, payloadName, payloadYear, packageId, needs,
                stored, blocksAway);
    }

    /** The machine card for This PC: what this computer is, in one block of text the client draws. */
    private static ThisPcPayload.WireMachine machineCard(
            final ServerLevel level, final IOsHost computer,
            final BlockPos host) {
        final String kind;
        if (computer instanceof MainframeBlockEntity) {
            kind = "Mainframe";
        } else if (computer instanceof CraftingComputerBlockEntity) {
            kind = "Crafting Computer";
        } else if (computer instanceof ClusterManagementComputerBlockEntity) {
            kind = "Cluster Management Computer";
        } else if (computer instanceof ServerRackBlockEntity) {
            kind = "Server";
        } else {
            kind = "Personal Computer";
        }
        final OsDef os = computer.installedOs();
        final String osLabel = os == null ? "" : os.displayName();
        final int osYear = os == null ? 0
                : Branding.osYear(os.displayName(), os.minEra());
        final NetworkUuid network = computer.networkUuid();
        /*
         * Hardware by what is seated, read off the parts themselves so every computer type answers
         * the same way whatever its slot layout.
         */
        String board = "";
        String cpu = "";
        String architecture = "";
        int cpus = 0;
        int gpus = 0;
        String psu = "";
        boolean valid = false;
        if (computer instanceof AbstractComputerBlockEntity be) {
            final ItemStackHandler hardware = be.getHardware();
            for (int i = 0; i < hardware.getSlots(); i++) {
                final ItemStack part = hardware.getStackInSlot(i);
                if (part.isEmpty()) {
                    continue;
                }
                if (part.getItem() instanceof MotherboardItem) {
                    board = part.getHoverName().getString();
                } else if (part.getItem() instanceof CpuItem chip) {
                    cpus++;
                    if (cpu.isEmpty()) {
                        cpu = part.getHoverName().getString();
                        // A machine has one architecture, so the first chip answers for all of them.
                        architecture = HardwareTooltip.architecture(chip.spec());
                    }
                } else if (part.getItem() instanceof GpuItem) {
                    gpus++;
                } else if (part.getItem() instanceof PsuItem) {
                    psu = part.getHoverName().getString();
                }
            }
            valid = be.buildValid();
        }
        final int mhz = computer.maxCpuMhz();
        if (!cpu.isEmpty() && mhz > 0) {
            cpu = cpu + " · " + (mhz >= 1000 ? String.format(Locale.ROOT, "%.1f GHz", mhz / 1000.0) : mhz + " MHz");
        }
        // Linked peripherals by name, each kind counted once.
        final Map<String, Integer> peripherals = new LinkedHashMap<>();
        for (final long endpoint : computer.linkedEndpoints()) {
            final BlockEntity be =
                    level.getBlockEntity(BlockPos.of(endpoint));
            final String label;
            if (be instanceof MediaReaderBlockEntity reader) {
                label = switch (reader.driveType()) {
                    case FLOPPY_DRIVE -> "Floppy Drive";
                    case CD_DRIVE -> "CD Drive";
                    case DVD_DRIVE -> "DVD Drive";
                    case DOCK_STATION -> "Dock Station";
                };
            } else if (be instanceof MonitorBlockEntity) {
                label = "Monitor";
            } else if (be != null) {
                label = be.getBlockState().getBlock().getName().getString();
            } else {
                continue;
            }
            peripherals.merge(label, 1, Integer::sum);
        }
        final StringBuilder joined = new StringBuilder();
        for (final Map.Entry<String, Integer> e : peripherals.entrySet()) {
            if (joined.length() > 0) {
                joined.append(", ");
            }
            if (e.getValue() > 1) {
                joined.append(e.getValue()).append(" × ");
            }
            joined.append(e.getKey());
        }
        return new ThisPcPayload.WireMachine(computer.customName(), kind,
                MinSpecTooltip.eraLabel(computer.displayEra()),
                osLabel, osYear, network == null ? "" : networkLabel(network), board, cpu, cpus, architecture,
                (int) Math.min(Integer.MAX_VALUE, computer.ramBuffer()), computer.totalVramMb(), gpus, psu,
                valid, joined.toString());
    }

    private static String joinPlain(final List<Component> lines) {
        final StringBuilder sb = new StringBuilder();
        for (final Component line : lines) {
            final String text = line.getString().trim();
            if (text.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(text);
        }
        return sb.length() > 190 ? sb.substring(0, 190) : sb.toString();
    }

    private static void handleEjectMedia(final EjectMediaPayload payload, final ServerPlayer player,
                                         final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer
                && computer.linkedEndpoints().contains(payload.readerPos())
                && level.getBlockEntity(BlockPos.of(payload.readerPos()))
                        instanceof MediaReaderBlockEntity reader) {
            final ItemStack ejected = reader.ejectMedia();
            if (!ejected.isEmpty() && !player.addItem(ejected)) {
                final BlockPos at = BlockPos.of(payload.readerPos());
                Containers.dropItemStack(level, at.getX() + 0.5, at.getY() + 1.0,
                        at.getZ() + 0.5, ejected);
            }
        }
    }

    private static void handleThisPc(final ThisPcPayload payload, final Player player) {
        ThisPcApp.accept(payload);
    }

    private static void handleInstallFromMedia(final InstallFromMediaPayload payload, final ServerPlayer player,
                                               final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer)) {
            return;
        }
        // The drive must be a media reader currently linked to this computer.
        if (!computer.linkedEndpoints().contains(payload.readerPos())
                || !(level.getBlockEntity(BlockPos.of(payload.readerPos()))
                        instanceof MediaReaderBlockEntity reader)) {
            return;
        }
        if (reader.insertedKind()
                != MediaKind.PROGRAM_INSTALL) {
            return;
        }
        final ResourceLocation pl = reader.insertedPayload();
        final ProgramSpec spec =
                pl == null ? null : OsRegistry.getProgram(pl);
        if (spec == null) {
            return;
        }
        /*
         * Whether the machine can take the program, and why not, is the machine's answer, given in
         * the Setup window that opens for it. It used to be a line in the chat, or nothing at all
         * when the program was already there, which is what made the disc's setup look inert.
         */
        SetupRunner.begin(computer, level, payload.hostPos(), spec,
                reader.insertedFormat(), false, SetupJob.VIA_SETUP);
    }

    /** A machine telling a desktop how its setup is going: the desktop's Setup window is a view of it. */
    private static void handleSetupProgress(final SetupProgressPayload payload, final Player player) {
        DesktopScreen.acceptSetup(payload);
    }

    /** A player at the Setup window's Cancel: the machine stops and nothing is installed. */
    private static void handleCancelSetup(final CancelSetupPayload payload, final ServerPlayer player,
                                          final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost host) {
            SetupRunner.cancel(host, level, payload.hostPos());
        }
    }
}
