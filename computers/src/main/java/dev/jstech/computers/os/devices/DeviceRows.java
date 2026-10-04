/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.devices;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.text.TextLists;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A {@link DeviceMap} as the rows of a Device Manager's tree, in one of its three views: by port (every port by kind
 * with what is plugged into it, the free ones too, and how many device ports are in use), by type (the parts by what
 * they are) and by connection (what hangs from what: the board, its cards and its port controller, and what is plugged
 * into each). A branch the player folded keeps its rows out until it is opened again.
 *
 * <p>Pure, with no Minecraft types, so every view is unit-tested.
 */
@TextHolder
public final class DeviceRows {

    private static final TextKey VIDEO_OUTPUTS = TextKey.of("jsc.devices.video_outputs", "Video outputs");
    private static final TextKey AUDIO_OUTPUTS = TextKey.of("jsc.devices.audio_outputs", "Audio outputs");
    private static final TextKey DEVICE_PORTS = TextKey.of("jsc.devices.device_ports", "Device ports");
    private static final TextKey FREE = TextKey.of("jsc.devices.free", "free");
    private static final TextKey PORT_ROW = TextKey.of("jsc.devices.port_row", "%s: %s");
    private static final TextKey SUMMARY = TextKey.of("jsc.devices.summary",
            "%s of %s device ports in use (%s on the board, %s on the hub)");
    private static final TextKey COMPUTER = TextKey.of("jsc.devices.type.computer", "Computer");
    private static final TextKey PROCESSORS = TextKey.of("jsc.devices.type.processors", "Processors");
    private static final TextKey MEMORY = TextKey.of("jsc.devices.type.memory", "Memory");
    private static final TextKey DISK_DRIVES = TextKey.of("jsc.devices.type.disk_drives", "Disk drives");
    private static final TextKey DISPLAY_ADAPTERS = TextKey.of("jsc.devices.type.display_adapters",
            "Display adapters");
    private static final TextKey SOUND_DEVICES = TextKey.of("jsc.devices.type.sound_devices", "Sound devices");
    private static final TextKey NETWORK_ADAPTERS = TextKey.of("jsc.devices.type.network_adapters",
            "Network adapters");
    private static final TextKey EXPANSION_CARDS = TextKey.of("jsc.devices.type.expansion_cards", "Expansion cards");
    private static final TextKey CONTROLLER_SERIAL = TextKey.of("jsc.devices.controller.serial",
            "Serial and parallel ports");
    private static final TextKey CONTROLLER_USB = TextKey.of("jsc.devices.controller.usb", "USB controller");
    private static final TextKey CONTROLLER_USB3 = TextKey.of("jsc.devices.controller.usb3", "USB 3 controller");

    /** The branch the by-type view opens folded, as the systems of the day did: the memory modules. */
    public static final String MEMORY_KEY = "type/memory";

    /** The icon of the machine itself at the root of every view, which is This PC's picture. */
    public static final String HOST_ICON = "this_pc";

    private DeviceRows() {
    }

    /** How a Device Manager orders its tree. */
    public enum View {
        BY_PORT, BY_TYPE, BY_CONNECTION
    }

    /** How a row reads: as a heading or a device, a free port, a disabled device, or the count at the foot. */
    public enum State {
        NORMAL, FREE, DISABLED, SUMMARY
    }

    /**
     * One row of the tree.
     *
     * @param depth      how far it is indented, from nought for the machine itself
     * @param icon       the kind its icon is drawn by, empty for none
     * @param label      what it reads
     * @param state      how it reads
     * @param key        what names its branch, so folding it is remembered across the map's answers
     * @param expandable whether it has rows under it that it can fold away
     * @param pos        the device it stands for, packed, or {@link #NO_DEVICE} for a heading or a port
     */
    public record Row(int depth, String icon, Text label, State state, String key, boolean expandable, long pos) {

        /** Whether it stands for a device on a port, which is what can be disabled. */
        public boolean isDevice() {
            return pos != NO_DEVICE;
        }
    }

    /** What a row that stands for no device carries in place of a position. */
    public static final long NO_DEVICE = Long.MIN_VALUE;

    /** The rows of {@code map} in {@code view}, every branch in {@code folded} kept closed. */
    public static List<Row> rows(final DeviceMap map, final View view, final Set<String> folded) {
        final Builder b = new Builder(folded);
        b.heading(0, HOST_ICON, Text.literal(map.host()), "host", true);
        if (b.open("host")) {
            switch (view) {
                case BY_PORT -> byPort(map, b);
                case BY_TYPE -> byType(map, b);
                case BY_CONNECTION -> byConnection(map, b);
            }
        }
        if (view == View.BY_PORT) {
            b.rows.add(new Row(0, "", SUMMARY.with(map.devicePortsInUse(), map.devicePortsTotal(),
                    map.devicePorts().size(), map.hubPorts()), State.SUMMARY, "summary", false, NO_DEVICE));
        }
        return b.rows;
    }

    /** The name of the board's port controller, by the kind of port it drives. */
    public static Text controllerName(final DeviceMap.PortFamily family) {
        return switch (family) {
            case SERIAL_PARALLEL -> CONTROLLER_SERIAL.text();
            case USB -> CONTROLLER_USB.text();
            case USB3 -> CONTROLLER_USB3.text();
        };
    }

    private static void byPort(final DeviceMap map, final Builder b) {
        b.heading(1, "video_port", VIDEO_OUTPUTS.text(), "port/video", true);
        if (b.open("port/video")) {
            for (int c = 0; c < map.video().size(); c++) {
                final DeviceMap.VideoCard card = map.video().get(c);
                final String key = "port/video/" + c;
                b.heading(2, "gpu", card.name(), key, !card.outputs().isEmpty());
                if (b.open(key)) {
                    for (final DeviceMap.Port output : card.outputs()) {
                        b.port(3, output, "video_port", key);
                    }
                }
            }
        }
        b.heading(1, "audio_jack", AUDIO_OUTPUTS.text(), "port/audio", true);
        if (b.open("port/audio")) {
            for (final DeviceMap.AudioSource source : map.audio()) {
                for (final DeviceMap.Port output : source.outputs()) {
                    b.port(2, output, "audio_jack", "port/audio");
                }
            }
        }
        b.heading(1, map.family().icon(0), DEVICE_PORTS.text(), "port/devices", true);
        if (b.open("port/devices")) {
            for (int i = 0; i < map.devicePorts().size(); i++) {
                b.portWithHub(2, map.devicePorts().get(i), map.family().icon(i), "port/devices/" + i, map.family());
            }
        }
    }

    private static void byType(final DeviceMap map, final Builder b) {
        b.group(1, "motherboard", COMPUTER.text(), "type/computer",
                map.board().isEmpty() ? List.of() : List.of(map.board()), "motherboard");
        b.group(1, "cpu", PROCESSORS.text(), "type/processors", map.processors(), "cpu");
        b.group(1, "ram", MEMORY.text(), MEMORY_KEY, map.memory(), "ram");
        b.group(1, "disk", DISK_DRIVES.text(), "type/disks", map.disks(), "disk");
        b.group(1, "gpu", DISPLAY_ADAPTERS.text(), "type/display",
                map.video().stream().map(DeviceMap.VideoCard::name).toList(), "gpu");
        b.group(1, "sound_card", SOUND_DEVICES.text(), "type/sound",
                map.audio().stream().map(DeviceMap.AudioSource::name).toList(), "sound_card");
        b.group(1, "network", NETWORK_ADAPTERS.text(), "type/network", map.network(), "network");
        if (!map.cards().isEmpty()) {
            b.group(1, "crafting_card", EXPANSION_CARDS.text(), "type/cards", map.cards(), "crafting_card");
        }
    }

    private static void byConnection(final DeviceMap map, final Builder b) {
        b.heading(1, "motherboard", map.board().isEmpty() ? COMPUTER.text() : map.board(), "connection/board", true);
        if (!b.open("connection/board")) {
            return;
        }
        for (final Text cpu : map.processors()) {
            b.leaf(2, "cpu", cpu);
        }
        for (final Text module : map.memory()) {
            b.leaf(2, "ram", module);
        }
        for (final Text disk : map.disks()) {
            b.leaf(2, "disk", disk);
        }
        for (int c = 0; c < map.video().size(); c++) {
            final DeviceMap.VideoCard card = map.video().get(c);
            final String key = "connection/video/" + c;
            final List<DeviceMap.Device> screens = plugged(card.outputs());
            b.heading(2, "gpu", card.name(), key, !screens.isEmpty());
            if (b.open(key)) {
                screens.forEach(device -> b.device(3, device, key));
            }
        }
        for (int s = 0; s < map.audio().size(); s++) {
            final DeviceMap.AudioSource source = map.audio().get(s);
            final String key = "connection/audio/" + s;
            final List<DeviceMap.Device> speakers = plugged(source.outputs());
            b.heading(2, "sound_card", source.name(), key, !speakers.isEmpty());
            if (b.open(key)) {
                speakers.forEach(device -> b.device(3, device, key));
            }
        }
        final List<DeviceMap.Device> onPorts = plugged(map.devicePorts());
        b.heading(2, map.family().icon(0), controllerName(map.family()), "connection/ports", !onPorts.isEmpty());
        if (b.open("connection/ports")) {
            onPorts.forEach(device -> b.deviceWithHub(3, device, "connection/ports"));
        }
        for (final Text adapter : map.network()) {
            b.leaf(2, "network", adapter);
        }
        for (final Text card : map.cards()) {
            b.leaf(2, "crafting_card", card);
        }
    }

    private static List<DeviceMap.Device> plugged(final List<DeviceMap.Port> ports) {
        final List<DeviceMap.Device> out = new ArrayList<>();
        for (final DeviceMap.Port port : ports) {
            out.addAll(port.plugged());
        }
        return out;
    }

    /* Collects rows and knows which branches are folded. */
    private static final class Builder {

        private final Set<String> folded;
        private final List<Row> rows = new ArrayList<>();

        Builder(final Set<String> folded) {
            this.folded = folded;
        }

        boolean open(final String key) {
            return !folded.contains(key);
        }

        void heading(final int depth, final String icon, final Text label, final String key,
                     final boolean expandable) {
            rows.add(new Row(depth, icon, label, State.NORMAL, key, expandable, NO_DEVICE));
        }

        void leaf(final int depth, final String icon, final Text label) {
            rows.add(new Row(depth, icon, label, State.NORMAL, "", false, NO_DEVICE));
        }

        /* A type and its parts, the type folding them away. */
        void group(final int depth, final String icon, final Text label, final String key, final List<Text> parts,
                   final String partIcon) {
            heading(depth, icon, label, key, !parts.isEmpty());
            if (open(key)) {
                for (final Text part : parts) {
                    leaf(depth + 1, partIcon, part);
                }
            }
        }

        /* A port with what is plugged into it, or free; the row stands for the first device on it. */
        void port(final int depth, final DeviceMap.Port port, final String portIcon, final String parent) {
            if (port.isFree()) {
                rows.add(new Row(depth, portIcon, PORT_ROW.with(port.name(), FREE.text()), State.FREE, "", false,
                        NO_DEVICE));
                return;
            }
            final DeviceMap.Device first = port.plugged().getFirst();
            final List<Text> names = port.plugged().stream().map(DeviceMap.Device::name).toList();
            rows.add(new Row(depth, first.icon().isEmpty() ? portIcon : first.icon(),
                    PORT_ROW.with(port.name(), TextLists.join(", ", names)),
                    first.disabled() ? State.DISABLED : State.NORMAL, parent + "/" + first.pos(), false,
                    first.pos()));
        }

        /* A device port, and when a hub is plugged into it the hub's own ports under it. */
        void portWithHub(final int depth, final DeviceMap.Port port, final String portIcon, final String key,
                         final DeviceMap.PortFamily family) {
            if (port.isFree() || !port.plugged().getFirst().isHub()) {
                port(depth, port, portIcon, key);
                return;
            }
            final DeviceMap.Device hub = port.plugged().getFirst();
            rows.add(new Row(depth, hub.icon(), PORT_ROW.with(port.name(), hub.name()),
                    hub.disabled() ? State.DISABLED : State.NORMAL, key, true, hub.pos()));
            if (open(key)) {
                for (int i = 0; i < hub.ports().size(); i++) {
                    portWithHub(depth + 1, hub.ports().get(i), family.icon(0), key + "/" + i, family);
                }
            }
        }

        void device(final int depth, final DeviceMap.Device device, final String parent) {
            rows.add(new Row(depth, device.icon(), device.name(), device.disabled() ? State.DISABLED : State.NORMAL,
                    parent + "/" + device.pos(), false, device.pos()));
        }

        /* A device, and a hub's devices under it. */
        void deviceWithHub(final int depth, final DeviceMap.Device device, final String parent) {
            final String key = parent + "/" + device.pos();
            final List<DeviceMap.Device> hanging = plugged(device.ports());
            rows.add(new Row(depth, device.icon(), device.name(), device.disabled() ? State.DISABLED : State.NORMAL,
                    key, !hanging.isEmpty(), device.pos()));
            if (open(key)) {
                hanging.forEach(child -> deviceWithHub(depth + 1, child, key));
            }
        }
    }
}
