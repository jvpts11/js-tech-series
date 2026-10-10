/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.desktop;

import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.client.os.ActiveDesktop;
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
import dev.jstech.computers.os.KernelNames;
import dev.jstech.computers.os.MinSpecTooltip;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.WorkstationFacts;
import dev.jstech.computers.os.install.Installers;
import dev.jstech.computers.os.install.SetupRunner;
import dev.jstech.computers.machine.DriveTable;
import dev.jstech.computers.os.media.DockStationBlockEntity;
import dev.jstech.computers.os.media.MediaDriveType;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.text.TextLists;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.Collections;
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
                    final ResourceLocation osId = OsDisks.systemOn(stack);
                    final DiskShares shares = DiskShares.of(stack);
                    /*
                     * The three shares travel separately, so the disk can show where its space
                     * actually went instead of one anonymous "used" number.
                     */
                    disks.add(new ThisPcPayload.WireDisk(slot, GameText.of(stack.getHoverName()),
                            cap, shares.usedItems(), stack == sys, osId != null ? osId.getPath() : "",
                            shares.systemItems(), shares.storageItems(), shares.fileItems()));
                }
                slot++;
            }
            // In the order the machine letters them, so a row's letter is the drive's; a dock's disks after its stick.
            final List<Long> endpoints = new ArrayList<>(computer.enabledEndpoints());
            Collections.sort(endpoints);
            for (final long endpoint : endpoints) {
                if (Loaded.blockEntity(level, BlockPos.of(endpoint))
                        instanceof MediaReaderBlockEntity reader) {
                    media.add(mediaRow(computer, payload.hostPos(), endpoint, reader));
                    if (reader instanceof DockStationBlockEntity dock) {
                        for (int bay = 0; bay < DockStationBlockEntity.BAYS; bay++) {
                            if (!dock.disk(bay).isEmpty()) {
                                media.add(dockedRow(payload.hostPos(), endpoint, dock, bay));
                            }
                        }
                    }
                }
            }
            installed.addAll(computer.console().installed());
            PacketDistributor.sendToPlayer(player, new ThisPcPayload(payload.hostPos(),
                    machineCard(level, computer, payload.hostPos()), disks, media, installed));
            return;
        }
        PacketDistributor.sendToPlayer(player,
                new ThisPcPayload(payload.hostPos(), ThisPcPayload.WireMachine.EMPTY, disks, media, installed));
    }

    /** A disk in a Dock Station's tray, as This PC lists it: an external drive, its name, size and use. */
    private static ThisPcPayload.WireMedia dockedRow(final BlockPos host, final long endpoint,
                                                     final DockStationBlockEntity dock, final int bay) {
        final ItemStack disk = dock.disk(bay);
        final long capItems = disk.getItem() instanceof DiskItem item ? item.spec().capacityItems() : 0L;
        final long usedItems = Math.max(0L, capItems - DriveTable.freeWeightOf(disk) / StorageKey.MB_EQ_PER_ITEM);
        final int blocksAway = BlockPos.of(endpoint).distManhattan(host);
        return new ThisPcPayload.WireMedia(endpoint, MediaDriveType.DOCK_STATION.serializedName(),
                GameText.of(disk.getHoverName()), "", "", false, "", 0, "", List.of(), 0L, blocksAway, bay,
                capItems, usedItems);
    }

    /** One drive row for This PC: what is in the drive and, for an installer, what it would install. */
    private static ThisPcPayload.WireMedia mediaRow(
            final IOsHost computer, final BlockPos host,
            final long endpoint, final MediaReaderBlockEntity reader) {
        final ItemStack m = reader.readableMedium();
        final MediaKind kind = m.isEmpty() ? null : reader.insertedKind();
        final ResourceLocation pl = m.isEmpty() ? null : reader.insertedPayload();
        String payloadName = "";
        int payloadYear = 0;
        String packageId = "";
        List<Text> needs = List.of();
        boolean installable = false;
        if (pl != null && kind == MediaKind.PROGRAM_INSTALL) {
            final ProgramSpec spec =
                    OsRegistry.getProgram(pl);
            installable = !computer.console().isInstalled(pl.toString());
            if (spec != null) {
                payloadName = spec.displayName();
                payloadYear = Branding.year(spec.era());
                packageId = spec.commandName();
                needs = lines(MinSpecTooltip.programMinSpec(pl));
            }
        } else if (pl != null && kind == MediaKind.OS_INSTALL) {
            final OsDef os =
                    OsRegistry.getOs(pl);
            if (os != null) {
                payloadName = os.displayName();
                payloadYear = Branding.osYear(os.displayName(), os.minEra());
                packageId = os.id().getPath();
                needs = lines(MinSpecTooltip.osMinSpec(pl));
            }
        }
        final long stored = kind == MediaKind.DATA
                ? reader.insertedData().total() : 0L;
        final int blocksAway = BlockPos.of(endpoint).distManhattan(host);
        return new ThisPcPayload.WireMedia(endpoint, reader.driveType().serializedName(),
                m.isEmpty() ? Text.EMPTY : GameText.of(m.getHoverName()), kind != null ? kind.serializedName() : "",
                pl != null ? pl.getPath() : "", installable, payloadName, payloadYear, packageId, needs,
                stored, blocksAway);
    }

    /** The machine card for This PC: what this computer is, in one block of text the client draws. */
    private static ThisPcPayload.WireMachine machineCard(
            final ServerLevel level, final IOsHost computer,
            final BlockPos host) {
        final TextKey kind;
        if (computer instanceof MainframeBlockEntity) {
            kind = ThisPcPayload.MAINFRAME;
        } else if (computer instanceof CraftingComputerBlockEntity) {
            kind = ThisPcPayload.CRAFTING_COMPUTER;
        } else if (computer instanceof ClusterManagementComputerBlockEntity) {
            kind = ThisPcPayload.CLUSTER_MANAGEMENT_COMPUTER;
        } else if (computer instanceof ServerRackBlockEntity) {
            kind = ThisPcPayload.SERVER;
        } else {
            kind = ThisPcPayload.PERSONAL_COMPUTER;
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
        Text board = Text.EMPTY;
        Text cpu = Text.EMPTY;
        Text isa = Text.EMPTY;
        int cpus = 0;
        int gpus = 0;
        Text psu = Text.EMPTY;
        boolean valid = false;
        if (computer instanceof AbstractComputerBlockEntity be) {
            final ItemStackHandler hardware = be.getHardware();
            for (int i = 0; i < hardware.getSlots(); i++) {
                final ItemStack part = hardware.getStackInSlot(i);
                if (part.isEmpty()) {
                    continue;
                }
                if (part.getItem() instanceof MotherboardItem) {
                    board = GameText.of(part.getHoverName());
                } else if (part.getItem() instanceof CpuItem chip) {
                    cpus++;
                    if (cpu.isEmpty()) {
                        cpu = GameText.of(part.getHoverName());
                        // A machine has one instruction set, so the first chip answers for all of them.
                        isa = HardwareTooltip.isa(chip.spec());
                    }
                } else if (part.getItem() instanceof GpuItem) {
                    gpus++;
                } else if (part.getItem() instanceof PsuItem) {
                    psu = GameText.of(part.getHoverName());
                }
            }
            valid = be.buildValid();
        }
        final ThisPcPayload.AboutFacts about = aboutFactsOf(computer);
        final int mhz = computer.maxCpuMhz();
        if (!cpu.isEmpty() && mhz > 0) {
            cpu = ThisPcPayload.WITH_CLOCK.with(cpu,
                    mhz >= 1000 ? String.format(Locale.ROOT, "%.1f GHz", mhz / 1000.0) : mhz + " MHz");
        }
        return new ThisPcPayload.WireMachine(computer.customName(), kind.text(),
                MinSpecTooltip.eraLabel(computer.displayEra()),
                osLabel, osYear, network == null ? "" : networkLabel(network), board, cpu, cpus, isa,
                (int) Math.min(Integer.MAX_VALUE, computer.ramBuffer()), computer.totalVramMb(), gpus, psu,
                valid, peripherals(level, computer), about);
    }

    /**
     * What an About-style This PC page needs (KDE's Info Center, GNOME's About, Cinnamon's System Info), reusing
     * the same facts Workstation Info reads on CDE rather than asking the machine twice for the system's name,
     * its architecture and its desktop. {@link ThisPcPayload.AboutFacts#EMPTY} on every other platform, since
     * none of those three desktops ever bundles This PC there.
     *
     * <p>Public and self-contained (it asks nothing but the machine) so it is called the same way from a test as
     * from {@link #machineCard}.
     */
    public static ThisPcPayload.AboutFacts aboutFactsOf(final IOsHost computer) {
        final OsDef os = computer.installedOs();
        final Platform platform = os == null ? Platform.FRAMES : os.platform();
        if (platform != Platform.FREEBSD && platform != Platform.LINUX) {
            return ThisPcPayload.AboutFacts.EMPTY;
        }
        final ItemStack disk = computer.systemDisk();
        final long diskMb = DiskShares.capacityMbOf(disk);
        /*
         * A Linux names its distribution alone here: the kernel row already carries the version this system
         * put beside its name, and repeating it in both places is the same fact twice.
         */
        final String operatingSystem = platform == Platform.LINUX ? os.displayName()
                : WorkstationInfoPayloads.systemOf(os);
        return new ThisPcPayload.AboutFacts(Text.literal(operatingSystem),
                Text.literal(KernelNames.architecture(platform, computer.processorBits())),
                Text.literal(KernelNames.kernelLine(platform)),
                Text.literal(WorkstationInfoPayloads.desktopName(computer)),
                Text.literal(Installers.hostName(computer)),
                WorkstationFacts.processorClock(WelcomePayloads.cpuText(computer), computer.maxCpuMhz()),
                computer.ramTotalMb(), diskMb, SettingsSnapshots.usedMb(disk), platform == Platform.FREEBSD,
                computer.processorBits() >= 64);
    }

    /** The linked peripherals by name, each kind named once with how many there are. */
    private static Text peripherals(final ServerLevel level, final IOsHost computer) {
        final Map<String, Integer> counts = new LinkedHashMap<>();
        final Map<String, Text> names = new LinkedHashMap<>();
        for (final long endpoint : computer.enabledEndpoints()) {
            final BlockEntity be =
                    Loaded.blockEntity(level, BlockPos.of(endpoint));
            final Text label;
            if (be instanceof MediaReaderBlockEntity reader) {
                label = (switch (reader.driveType()) {
                    case FLOPPY_DRIVE -> ThisPcPayload.FLOPPY_DRIVE;
                    case CD_DRIVE -> ThisPcPayload.CD_DRIVE;
                    case DVD_DRIVE -> ThisPcPayload.DVD_DRIVE;
                    case BLU_RAY_DRIVE -> ThisPcPayload.BLU_RAY_DRIVE;
                    case DOCK_STATION -> ThisPcPayload.DOCK_STATION;
                }).text();
            } else if (be instanceof MonitorBlockEntity) {
                label = ThisPcPayload.MONITOR.text();
            } else if (be != null) {
                label = GameText.of(be.getBlockState().getBlock().getName());
            } else {
                continue;
            }
            // Counted by what the English reads, so two of one kind are one entry whatever language reads it.
            counts.merge(label.english(), 1, Integer::sum);
            names.putIfAbsent(label.english(), label);
        }
        final List<Text> parts = new ArrayList<>();
        for (final Map.Entry<String, Integer> e : counts.entrySet()) {
            final Text name = names.get(e.getKey());
            parts.add(e.getValue() > 1 ? ThisPcPayload.COUNTED.with(e.getValue(), name) : name);
        }
        return TextLists.join(", ", parts);
    }

    /** A requirement a line, the blank ones left out, as many as a row carries. */
    private static List<Text> lines(final List<Component> components) {
        final List<Text> out = new ArrayList<>();
        for (final Component line : components) {
            if (!line.getString().isBlank() && out.size() < ThisPcPayload.MAX_NEEDS) {
                out.add(GameText.of(line));
            }
        }
        return out;
    }

    private static void handleEjectMedia(final EjectMediaPayload payload, final ServerPlayer player,
                                         final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer
                && computer.enabledEndpoints().contains(payload.readerPos())
                && Loaded.blockEntity(level, BlockPos.of(payload.readerPos()))
                        instanceof MediaReaderBlockEntity reader) {
            // An optical drive's eject opens its tray, as its button does; the disc waits on it for the player's hand.
            if (payload.bay() < 0 && reader.driveType().ejectButton() != null) {
                if (!reader.reading() && !reader.tray().isOpen()) {
                    reader.tray().press(level, reader.getBlockPos());
                }
                return;
            }
            final ItemStack ejected = payload.bay() < 0 ? reader.ejectMedia()
                    : reader instanceof DockStationBlockEntity dock ? dock.ejectDisk(payload.bay()) : ItemStack.EMPTY;
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
        if (!computer.enabledEndpoints().contains(payload.readerPos())
                || !(Loaded.blockEntity(level, BlockPos.of(payload.readerPos()))
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
                reader.insertedFormat(), false, PackageManagerKind.NONE);
    }

    /** A machine telling a desktop how its setup is going: the desktop's Setup window is a view of it. */
    private static void handleSetupProgress(final SetupProgressPayload payload, final Player player) {
        ActiveDesktop.acceptSetup(payload);
    }

    /** A player at the Setup window's Cancel: the machine stops and nothing is installed. */
    private static void handleCancelSetup(final CancelSetupPayload payload, final ServerPlayer player,
                                          final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost host) {
            SetupRunner.cancel(host, level, payload.hostPos());
        }
    }
}
