/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.devices;

import dev.jstech.computers.printer.PrinterModel;
import dev.jstech.core.id.IStableId;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.text.TextLists;
import dev.jstech.core.tier.HardwareEra;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A machine's hardware as its system's Device Manager shows it: what it is built of, by type, and every port it has,
 * by kind, with what is plugged into each. The video outputs of each graphics card, the audio outputs (each drives a
 * pair of speakers), and the device ports on the board, where a hub plugged into one adds ports of its own.
 *
 * <p>Read off the machine on the server and drawn by each system in its own way; pure, with no Minecraft types, so
 * how its ports are named and counted is unit-tested.
 *
 * @param host        the name the machine answers to
 * @param board       the motherboard by name, empty when none is seated
 * @param processors  the processors by name
 * @param memory      the memory modules by name
 * @param disks       the disks by name
 * @param video       the graphics cards, and a server board's own console output, with their video outputs
 * @param audio       what plays the machine's sound, each with its audio outputs
 * @param family      the kind of device port the board has
 * @param devicePorts the device ports on the board, in order
 * @param network     the network adapters by name: the board's own port first, then the cards
 * @param cards       the other expansion cards by name
 * @param cardIcons   the icon each of those cards is drawn with, in the same order; a card past the end of the list
 *                    is drawn with the crafting card's
 */
@TextHolder
public record DeviceMap(String host, Text board, List<Text> processors, List<Text> memory, List<Text> disks,
                        List<VideoCard> video, List<AudioSource> audio, PortFamily family, List<Port> devicePorts,
                        List<Text> network, List<Text> cards, List<String> cardIcons) {

    private static final TextKey OUTPUT = TextKey.of("jsc.devices.output", "Output %s");
    private static final TextKey HUB_PORT = TextKey.of("jsc.devices.hub_port", "Hub port %s");
    private static final TextKey USB_PORT = TextKey.of("jsc.devices.usb_port", "USB %s");
    private static final TextKey SERIAL_PORT = TextKey.of("jsc.devices.serial_port", "Serial (COM%s)");
    private static final TextKey PARALLEL_PORT = TextKey.of("jsc.devices.parallel_port", "Parallel (LPT%s)");
    private static final TextKey BOARD_AUDIO = TextKey.of("jsc.devices.board_audio", "Audio on the board");
    private static final TextKey BOARD_CONSOLE = TextKey.of("jsc.devices.board_console",
            "Console output (on the board)");
    private static final TextKey BOARD_NETWORK = TextKey.of("jsc.devices.board_network", "%s port (on the board)");
    private static final TextKey HUB = TextKey.of("jsc.devices.hub", "%s (%s ports)");
    private static final TextKey NAMED = TextKey.of("jsc.devices.named", "%s \"%s\"");
    private static final TextKey UNKNOWN = TextKey.of("jsc.devices.unknown", "Device out of reach");
    /** The icon an expansion card is drawn with when it names none of its own. */
    public static final String CARD_ICON = "crafting_card";

    public DeviceMap {
        host = host == null ? "" : host;
        board = board == null ? Text.EMPTY : board;
        processors = List.copyOf(processors);
        memory = List.copyOf(memory);
        disks = List.copyOf(disks);
        video = List.copyOf(video);
        audio = List.copyOf(audio);
        devicePorts = List.copyOf(devicePorts);
        network = List.copyOf(network);
        cards = List.copyOf(cards);
        cardIcons = List.copyOf(cardIcons);
    }

    /** A machine whose cards all wear the crafting card's icon. */
    public DeviceMap(final String host, final Text board, final List<Text> processors, final List<Text> memory,
                     final List<Text> disks, final List<VideoCard> video, final List<AudioSource> audio,
                     final PortFamily family, final List<Port> devicePorts, final List<Text> network,
                     final List<Text> cards) {
        this(host, board, processors, memory, disks, video, audio, family, devicePorts, network, cards, List.of());
    }

    /** The icon expansion card {@code index} is drawn with. */
    public String cardIcon(final int index) {
        return index >= 0 && index < cardIcons.size() && !cardIcons.get(index).isEmpty() ? cardIcons.get(index)
                : CARD_ICON;
    }

    /** A graphics card, or a server board's console output, with its video outputs in order. */
    public record VideoCard(Text name, List<Port> outputs) {

        public VideoCard {
            outputs = List.copyOf(outputs);
        }
    }

    /** What plays the machine's sound, its sound card or the board's own, with its audio outputs in order. */
    public record AudioSource(Text name, List<Port> outputs) {

        public AudioSource {
            outputs = List.copyOf(outputs);
        }
    }

    /**
     * One port: its name, and what is plugged into it, nothing while it is free. An audio output carries the pair of
     * speakers it drives; every other port one device at most.
     */
    public record Port(Text name, List<Device> plugged) {

        public Port {
            plugged = List.copyOf(plugged);
        }

        /** A port with nothing plugged into it. */
        public static Port free(final Text name) {
            return new Port(name, List.of());
        }

        public boolean isFree() {
            return plugged.isEmpty();
        }

        /** The names of everything plugged into the port, joined with commas. */
        public Text pluggedNames() {
            return TextLists.join(", ", plugged.stream().map(Device::name).toList());
        }
    }

    /**
     * A device on a port: the kind its icon is drawn by, its name as the machine knows it, where it stands (packed),
     * whether its computer disabled it, and for a hub the ports it adds.
     */
    public record Device(String icon, Text name, long pos, boolean disabled, List<Port> ports) {

        public Device {
            icon = icon == null ? "" : icon;
            ports = List.copyOf(ports);
        }

        /** Whether it is a hub, adding ports of its own. */
        public boolean isHub() {
            return !ports.isEmpty();
        }
    }

    /** The kind of device port a board has: serial and parallel on the Vintage, then USB, then USB 3. */
    public enum PortFamily implements IStableId {
        SERIAL_PARALLEL(0, "serial_port"),
        USB(1, "usb_port"),
        USB3(2, "usb3_port");

        private final int id;
        private final String icon;

        PortFamily(final int id, final String icon) {
            this.id = id;
            this.icon = icon;
        }

        @Override
        public int id() {
            return id;
        }

        /** The icon of a port of this family; a parallel port has its own beside the serial ones. */
        public String icon(final int index) {
            return this == SERIAL_PARALLEL && index % 2 == 1 ? "parallel_port" : icon;
        }

        /**
         * The name of the board's port at {@code index}, counted from nought: USB 1, USB 2 and on; on a Vintage board
         * a serial port and a parallel port in turn, COM1 and LPT1, then COM2 and LPT2.
         */
        public Text portName(final int index) {
            if (this == SERIAL_PARALLEL) {
                return (index % 2 == 0 ? SERIAL_PORT : PARALLEL_PORT).with(index / 2 + 1);
            }
            return USB_PORT.with(index + 1);
        }
    }

    /** The name of a video output, counted from one. */
    public static Text outputName(final int number) {
        return OUTPUT.with(number);
    }

    /** The name of a hub's port, counted from one. */
    public static Text hubPortName(final int number) {
        return HUB_PORT.with(number);
    }

    /** The audio a board carries, named as a sound source. */
    public static Text boardAudio() {
        return BOARD_AUDIO.text();
    }

    /** The video output a server board carries for its console, named as a graphics card. */
    public static Text boardConsole() {
        return BOARD_CONSOLE.text();
    }

    /** The network port a board carries, by its cable's name. */
    public static Text boardNetwork(final Text cable) {
        return BOARD_NETWORK.with(cable);
    }

    /** The cable's name alone when {@code adapter} is the board's network port, the adapter's name otherwise. */
    public static Text cableOf(final Text adapter) {
        return adapter instanceof Text.Translated named && named.key().equals(BOARD_NETWORK)
                && !named.args().isEmpty() ? named.args().getFirst() : adapter;
    }

    /** A hub by its name with how many ports it adds. */
    public static Text hubName(final Text name, final int ports) {
        return HUB.with(name, ports);
    }

    /** A device by its kind's name, with the name a player gave it when there is one. */
    public static Text named(final Text kind, final String given) {
        return given == null || given.isBlank() ? kind : NAMED.with(kind, given);
    }

    /** A linked device whose chunk is not loaded, which the machine cannot read now. */
    public static Text unknown() {
        return UNKNOWN.text();
    }

    /**
     * The icon kind of a device, from its block's id: one icon serves every era of a hub, a Redstone Interface and a
     * Pattern Encoder, and a Network Gateway wears the network's; the monitors, speakers and drives each have their
     * own.
     */
    public static String iconOf(final String blockId) {
        if (blockId.endsWith("_hub") || blockId.equals("hub")) {
            return "usb_hub";
        }
        if (blockId.equals("network_gateway")) {
            return "network";
        }
        if (blockId.endsWith("redstone_interface")) {
            return "redstone_interface";
        }
        if (blockId.endsWith("pattern_encoder")) {
            return "pattern_encoder";
        }
        // A printer wears the icon of its kind: the dot matrix, the inkjet, the all-in-one, the laser, the ink tank.
        if (blockId.equals("printer") || blockId.endsWith("_printer")) {
            return PrinterModel.of(switch (blockId) {
                case "vintage_printer" -> HardwareEra.VINTAGE;
                case "legacy_printer" -> HardwareEra.LEGACY;
                case "transition_printer" -> HardwareEra.TRANSITION;
                case "advanced_printer" -> HardwareEra.ADVANCED;
                default -> HardwareEra.STANDARD;
            }).icon();
        }
        return blockId;
    }

    /** The same machine under another name: the Frames editions write a computer's name in capitals. */
    public DeviceMap withHost(final String name) {
        return new DeviceMap(name, board, processors, memory, disks, video, audio, family, devicePorts, network, cards,
                cardIcons);
    }

    /** How many device ports the machine has: the board's and every hub's on them. */
    public int devicePortsTotal() {
        return devicePorts.size() + hubPortsOf(devicePorts);
    }

    /** How many device ports a device takes: on the board and on every hub. */
    public int devicePortsInUse() {
        return usedOf(devicePorts);
    }

    /** How many device ports the hubs add. */
    public int hubPorts() {
        return hubPortsOf(devicePorts);
    }

    /** Every device on every port, hubs and what hangs from them included, in the order the ports run. */
    public List<Device> devices() {
        final List<Device> out = new ArrayList<>();
        for (final VideoCard card : video) {
            collect(card.outputs(), out);
        }
        for (final AudioSource source : audio) {
            collect(source.outputs(), out);
        }
        collect(devicePorts, out);
        return out;
    }

    /** The device standing at the packed position {@code pos}, or null when none of the machine's does. */
    @Nullable
    public Device device(final long pos) {
        // Walks the ports without building the list of every device: this is asked of each frame a page draws.
        final Port port = portOf(pos);
        if (port == null) {
            return null;
        }
        for (final Device device : port.plugged()) {
            if (device.pos() == pos) {
                return device;
            }
        }
        return null;
    }

    /** The port the device at {@code pos} is plugged into, or null when none of the machine's carries it. */
    @Nullable
    public Port portOf(final long pos) {
        for (final VideoCard card : video) {
            final Port found = portIn(card.outputs(), pos);
            if (found != null) {
                return found;
            }
        }
        for (final AudioSource source : audio) {
            final Port found = portIn(source.outputs(), pos);
            if (found != null) {
                return found;
            }
        }
        return portIn(devicePorts, pos);
    }

    @Nullable
    private static Port portIn(final List<Port> ports, final long pos) {
        for (final Port port : ports) {
            for (final Device device : port.plugged()) {
                if (device.pos() == pos) {
                    return port;
                }
                final Port deeper = portIn(device.ports(), pos);
                if (deeper != null) {
                    return deeper;
                }
            }
        }
        return null;
    }

    private static int hubPortsOf(final List<Port> ports) {
        int count = 0;
        for (final Port port : ports) {
            for (final Device device : port.plugged()) {
                count += device.ports().size() + hubPortsOf(device.ports());
            }
        }
        return count;
    }

    private static int usedOf(final List<Port> ports) {
        int count = 0;
        for (final Port port : ports) {
            if (!port.isFree()) {
                count++;
            }
            for (final Device device : port.plugged()) {
                count += usedOf(device.ports());
            }
        }
        return count;
    }

    private static void collect(final List<Port> ports, final List<Device> out) {
        for (final Port port : ports) {
            for (final Device device : port.plugged()) {
                out.add(device);
                collect(device.ports(), out);
            }
        }
    }
}
