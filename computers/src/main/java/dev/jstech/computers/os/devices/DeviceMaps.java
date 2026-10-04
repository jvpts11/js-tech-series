/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.devices;

import dev.jstech.computers.block.DataLinkNames;
import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.blockentity.RedstoneInterfaceBlockEntity;
import dev.jstech.computers.blockentity.SpeakerBlockEntity;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.hardware.MachinePorts;
import dev.jstech.computers.item.ClusterInterfaceCardItem;
import dev.jstech.computers.item.CpuItem;
import dev.jstech.computers.item.CraftingCardItem;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.item.GpuItem;
import dev.jstech.computers.item.MotherboardItem;
import dev.jstech.computers.item.NetworkCardItem;
import dev.jstech.computers.item.PhiCoprocessorItem;
import dev.jstech.computers.item.RamItem;
import dev.jstech.computers.item.SoundCardItem;
import dev.jstech.computers.item.WorkshopCardItem;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.install.Installers;
import dev.jstech.core.peripheral.IPeripheralHub;
import dev.jstech.core.peripheral.PortKind;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.util.Loaded;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Reads a machine's {@link DeviceMap} off it as it stands: its parts by name from the items seated in it, and its
 * ports with what its peripheral cables link to each. A port takes its linked devices in the order they were linked,
 * which is the order that decides who keeps a port when there are fewer ports than devices.
 */
public final class DeviceMaps {

    private DeviceMaps() {
    }

    /** The map of {@code computer}, read in {@code level}; an empty one for a machine not built from parts. */
    public static DeviceMap of(final ServerLevel level, final IOsHost computer) {
        final ComputerBuild build = computer.currentBuild();
        final HardwareEra era = build == null ? HardwareEra.STANDARD : build.motherboard().era();
        Text board = Text.EMPTY;
        final List<Text> processors = new ArrayList<>();
        final List<Text> memory = new ArrayList<>();
        final List<Text> disks = new ArrayList<>();
        final List<ItemStack> gpus = new ArrayList<>();
        final List<Text> soundCards = new ArrayList<>();
        final List<Text> network = new ArrayList<>();
        final List<Text> cards = new ArrayList<>();
        final List<String> cardIcons = new ArrayList<>();
        network.add(DeviceMap.boardNetwork(DataLinkNames.access(era).text()));
        for (final ItemStack part : computer.hardwareStacks()) {
            final Text name = GameText.of(part.getHoverName());
            switch (part.getItem()) {
                case MotherboardItem ignored -> board = name;
                case CpuItem ignored -> processors.add(name);
                case RamItem ignored -> memory.add(name);
                case DiskItem ignored -> disks.add(name);
                case GpuItem ignored -> gpus.add(part);
                case SoundCardItem ignored -> soundCards.add(name);
                case NetworkCardItem ignored -> network.add(name);
                case CraftingCardItem ignored -> addCard(cards, cardIcons, name, DeviceMap.CARD_ICON);
                case ClusterInterfaceCardItem ignored -> addCard(cards, cardIcons, name, DeviceMap.CARD_ICON);
                case PhiCoprocessorItem ignored -> addCard(cards, cardIcons, name, DeviceMap.CARD_ICON);
                // A personal-use card wears its own picture, under the id of its item.
                case WorkshopCardItem card -> addCard(cards, cardIcons, name, card.spec().card().id());
                default -> {
                }
            }
        }
        if (build == null) {
            return new DeviceMap(Installers.hostName(computer), board, processors, memory, disks, List.of(),
                    List.of(), family(era), List.of(), network, cards, cardIcons);
        }
        final Linked linked = new Linked(level, computer);
        return new DeviceMap(Installers.hostName(computer), board, processors, memory, disks,
                video(build, gpus, linked), audio(build, soundCards, linked), family(era), devicePorts(era, linked),
                network, cards, cardIcons);
    }

    /** The kind of device port a board of {@code era} has. */
    public static DeviceMap.PortFamily family(final HardwareEra era) {
        if (era == HardwareEra.VINTAGE) {
            return DeviceMap.PortFamily.SERIAL_PARALLEL;
        }
        return era.isAtMost(HardwareEra.TRANSITION) ? DeviceMap.PortFamily.USB : DeviceMap.PortFamily.USB3;
    }

    /*
     * The graphics cards in their slots' order, after the board's own outputs: a server board's console output, and
     * the output of the graphics its processor carries on the die. The monitors in turn.
     */
    private static void addCard(final List<Text> cards, final List<String> icons, final Text name, final String icon) {
        cards.add(name);
        icons.add(icon);
    }

    private static List<DeviceMap.VideoCard> video(final ComputerBuild build, final List<ItemStack> gpus,
                                                   final Linked linked) {
        final List<DeviceMap.VideoCard> out = new ArrayList<>();
        final Deque<Long> monitors = linked.ofKind(PortKind.VIDEO);
        if (build.motherboard().formFactor() == FormFactor.EEB) {
            out.add(new DeviceMap.VideoCard(DeviceMap.boardConsole(), outputs(1, monitors, linked)));
        }
        for (final CpuSpec cpu : build.cpus()) {
            if (cpu.hasIntegratedGraphics()) {
                out.add(new DeviceMap.VideoCard(Text.literal(cpu.design().graphics().model()),
                        outputs(1, monitors, linked)));
                break;
            }
        }
        for (final ItemStack gpu : gpus) {
            final int count = gpu.getItem() instanceof GpuItem item ? MachinePorts.videoOutputs(item.spec().era()) : 1;
            out.add(new DeviceMap.VideoCard(GameText.of(gpu.getHoverName()), outputs(count, monitors, linked)));
        }
        return out;
    }

    private static List<DeviceMap.Port> outputs(final int count, final Deque<Long> monitors, final Linked linked) {
        final List<DeviceMap.Port> out = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            final Long monitor = monitors.pollFirst();
            out.add(monitor == null ? DeviceMap.Port.free(DeviceMap.outputName(i))
                    : new DeviceMap.Port(DeviceMap.outputName(i), List.of(linked.device(monitor))));
        }
        return out;
    }

    /* The board's own sound from the Transition on, then the sound card; each output drives a pair of speakers. */
    private static List<DeviceMap.AudioSource> audio(final ComputerBuild build, final List<Text> soundCards,
                                                     final Linked linked) {
        final List<Text> sources = new ArrayList<>();
        if (build.motherboard().hasOnBoardAudio()) {
            sources.add(DeviceMap.boardAudio());
        }
        sources.addAll(soundCards);
        final Deque<Long> speakers = linked.ofKind(PortKind.AUDIO);
        final List<DeviceMap.AudioSource> out = new ArrayList<>();
        for (final Text source : sources) {
            final List<DeviceMap.Device> pair = new ArrayList<>(MachinePorts.SPEAKERS_PER_AUDIO_OUTPUT);
            while (pair.size() < MachinePorts.SPEAKERS_PER_AUDIO_OUTPUT && !speakers.isEmpty()) {
                pair.add(linked.device(speakers.pollFirst()));
            }
            out.add(new DeviceMap.AudioSource(source, List.of(new DeviceMap.Port(source, pair))));
        }
        return out;
    }

    /* The board's device ports, each with the device linked to it, and on a hub the hub's own ports in turn. */
    private static List<DeviceMap.Port> devicePorts(final HardwareEra era, final Linked linked) {
        final DeviceMap.PortFamily family = family(era);
        final Deque<Long> onBoard = linked.onPortsOf(null);
        final int count = MachinePorts.devicePorts(era);
        final List<DeviceMap.Port> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final Long device = onBoard.pollFirst();
            out.add(device == null ? DeviceMap.Port.free(family.portName(i))
                    : new DeviceMap.Port(family.portName(i), List.of(linked.device(device))));
        }
        return out;
    }

    /* What the machine's cables link, read once: each link's kind, its hub and whether it is disabled. */
    private static final class Linked {

        private final ServerLevel level;
        private final IOsHost computer;
        private final List<Long> order;

        /** How deep a chain of hubs is drawn, as deep as the links themselves allow. */
        private static final int MAX_HUB_DEPTH = 16;

        Linked(final ServerLevel level, final IOsHost computer) {
            this.level = level;
            this.computer = computer;
            this.order = computer.linkedEndpoints();
        }

        /* The linked devices taking a port of {@code kind} on the machine itself, in the order they were linked. */
        Deque<Long> ofKind(final PortKind kind) {
            final Deque<Long> out = new ArrayDeque<>();
            for (final long pos : order) {
                if (computer.kindOf(pos) == kind && computer.hubOf(pos).isEmpty()) {
                    out.add(pos);
                }
            }
            return out;
        }

        /* The linked devices on the device ports of the hub at {@code hub}, or of the board itself for null. */
        Deque<Long> onPortsOf(final Long hub) {
            final Deque<Long> out = new ArrayDeque<>();
            for (final long pos : order) {
                if (computer.kindOf(pos) != PortKind.DEVICE) {
                    continue;
                }
                final boolean onIt = hub == null ? computer.hubOf(pos).isEmpty()
                        : computer.hubOf(pos).isPresent() && computer.hubOf(pos).getAsLong() == hub;
                if (onIt) {
                    out.add(pos);
                }
            }
            return out;
        }

        DeviceMap.Device device(final long pos) {
            return deviceWithPorts(pos, 0);
        }

        /*
         * The device at {@code pos}; a hub with its ports and what hangs from them, to a bounded depth. One whose chunk
         * is away is named as a device the machine cannot see now.
         */
        private DeviceMap.Device deviceWithPorts(final long pos, final int depth) {
            final BlockEntity be = Loaded.blockEntity(level, BlockPos.of(pos));
            final boolean disabled = computer.isDisabled(pos);
            if (be == null) {
                return new DeviceMap.Device("", DeviceMap.unknown(), pos, disabled, List.of());
            }
            final String icon = DeviceMap.iconOf(BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock())
                    .getPath());
            final Text kind = GameText.of(be.getBlockState().getBlock().getName());
            if (be instanceof IPeripheralHub hub && depth < MAX_HUB_DEPTH) {
                final Deque<Long> hanging = onPortsOf(pos);
                final List<DeviceMap.Port> ports = new ArrayList<>(hub.hubPorts());
                for (int i = 1; i <= hub.hubPorts(); i++) {
                    final Long device = hanging.pollFirst();
                    ports.add(device == null ? DeviceMap.Port.free(DeviceMap.hubPortName(i))
                            : new DeviceMap.Port(DeviceMap.hubPortName(i),
                                    List.of(deviceWithPorts(device, depth + 1))));
                }
                return new DeviceMap.Device(icon, DeviceMap.hubName(kind, hub.hubPorts()), pos, disabled, ports);
            }
            return new DeviceMap.Device(icon, DeviceMap.named(kind, givenName(be)), pos, disabled, List.of());
        }

        /* The name a player gave the device, for the kinds that take one. */
        private static String givenName(final BlockEntity be) {
            return switch (be) {
                case SpeakerBlockEntity speaker -> speaker.name();
                case RedstoneInterfaceBlockEntity sensor -> sensor.name();
                case NetworkGatewayBlockEntity gateway -> gateway.name();
                default -> "";
            };
        }
    }
}
