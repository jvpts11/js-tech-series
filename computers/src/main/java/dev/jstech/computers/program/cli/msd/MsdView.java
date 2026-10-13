/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.msd;

import dev.jstech.computers.os.devices.DeviceMap;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * What the diagnostics screen shows, read off the machine: what each of its buttons found, and every port of the
 * board with what is attached to it, a hub's own ports under the hub.
 *
 * <p>The machine draws the screen and the terminal only shows it, so a port enabled or disabled from it is enabled or
 * disabled on the machine, and the screen that comes back is the machine's as it then stands.
 *
 * <p>The screen travels as the rows of a file, so its words are put in English, the machine's language, until it
 * travels as words still to be put in the reader's.
 */
@TextHolder
public final class MsdView {

    /** What asks for the picked port's device to be enabled, and disabled. */
    public static final String ENABLE = "enable";
    public static final String DISABLE = "disable";

    /** What asks for the ports dialog to open on the first parallel port, and on the first serial one. */
    public static final String LPT = "lpt";
    public static final String COM = "com";

    private static final TextKey ON = TextKey.of("jsc.cli.msd.on", "On");
    private static final TextKey OFF = TextKey.of("jsc.cli.msd.off", "Off (disabled)");

    /** The memory below the first megabyte that a program of that age could use, which it always reported. */
    private static final int CONVENTIONAL_KB = 640;

    /** How far in a hub's ports are written under the hub. */
    private static final String HUB_INDENT = "  ";

    private MsdView() {
    }

    /** One port of the board or of a hub on it, with the device attached to it, or none. */
    private record Slot(String port, @Nullable DeviceMap.Device device) {
    }

    /**
     * The screen for that state: carries out what was asked of the picked port, then reads the machine and draws it
     * with the picked button or port kept on the glass.
     */
    public static List<String> screen(final ICliComputer computer, final MsdState state) {
        act(computer, state);
        final DeviceMap map = computer.devices();
        final List<Slot> slots = slots(map);
        final List<MsdScreen.PortRow> ports = new ArrayList<>(slots.size());
        for (final Slot slot : slots) {
            ports.add(row(slot));
        }
        final int wanted = LPT.equals(state.action()) ? firstPortOf(slots, "LPT")
                : COM.equals(state.action()) ? firstPortOf(slots, "COM") : state.picked();
        final int last = state.ports() ? Math.max(0, ports.size() - 1) : MsdScreen.BUTTONS.length - 1;
        final MsdState shown = state.done().picking(Math.min(wanted, last));
        return MsdScreen.render(shown, new MsdScreen.Data(found(computer, map), ports));
    }

    /** Enables or disables the device on the picked port, when that is what was asked and a device is there. */
    private static void act(final ICliComputer computer, final MsdState state) {
        if (!state.ports() || !(ENABLE.equals(state.action()) || DISABLE.equals(state.action()))) {
            return;
        }
        final List<Slot> slots = slots(computer.devices());
        if (state.picked() < slots.size() && slots.get(state.picked()).device() != null) {
            computer.setDeviceDisabled(slots.get(state.picked()).device().pos(), DISABLE.equals(state.action()));
        }
    }

    /**
     * The row a button opens the ports dialog on: the first port of its kind, the first row when the machine has
     * none of that kind.
     */
    private static int firstPortOf(final List<Slot> slots, final String kind) {
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).port().startsWith(kind)) {
                return i;
            }
        }
        return 0;
    }

    /** Every port of the board in order, a hub's ports after the hub and written in under it. */
    private static List<Slot> slots(@Nullable final DeviceMap map) {
        final List<Slot> out = new ArrayList<>();
        if (map == null) {
            return out;
        }
        final boolean serial = map.family() == DeviceMap.PortFamily.SERIAL_PARALLEL;
        for (int i = 0; i < map.devicePorts().size(); i++) {
            final DeviceMap.Port port = map.devicePorts().get(i);
            // A Vintage board's ports by the names DOS gave the devices behind them: COM1, LPT1, COM2, LPT2.
            final String name = serial ? (i % 2 == 0 ? "COM" : "LPT") + (i / 2 + 1) : english(port.name());
            collect(port, name, "", out);
        }
        return out;
    }

    private static void collect(final DeviceMap.Port port, final String name, final String indent,
                                final List<Slot> out) {
        final DeviceMap.Device device = port.isFree() ? null : port.plugged().getFirst();
        out.add(new Slot(indent + name + ":", device));
        if (device != null) {
            for (final DeviceMap.Port hubPort : device.ports()) {
                collect(hubPort, english(hubPort.name()), indent + HUB_INDENT, out);
            }
        }
    }

    private static MsdScreen.PortRow row(final Slot slot) {
        if (slot.device() == null) {
            return new MsdScreen.PortRow(slot.port(), MsdScreen.NOTHING_ATTACHED, "");
        }
        return new MsdScreen.PortRow(slot.port(), english(slot.device().name()),
                english((slot.device().disabled() ? OFF : ON).text()));
    }

    /** What each button of the main screen found, in the buttons' order. */
    private static List<String> found(final ICliComputer computer, @Nullable final DeviceMap map) {
        final ICliComputer.SystemInfo system = computer.systemInfo();
        final List<String> out = new ArrayList<>();
        out.add(map == null ? "" : first(map.processors()));
        out.add(memory(computer.memory().totalMb()));
        out.add(map == null || map.video().isEmpty() ? MsdScreen.NOTHING_ATTACHED
                : english(map.video().getFirst().name()));
        // The network by its cable, as that program named it: a board's own port is no adapter worth a name.
        out.add(map == null || map.network().isEmpty() ? MsdScreen.NOTHING_ATTACHED
                : english(DeviceMap.cableOf(map.network().getFirst())));
        out.add(system == null ? "" : system.os());
        out.add(drives(computer.mounts()));
        final boolean serial = map != null && map.family() == DeviceMap.PortFamily.SERIAL_PARALLEL;
        final int boardPorts = map == null ? 0 : map.devicePorts().size();
        // A Vintage board's ports run COM1, LPT1, COM2, LPT2, so the parallel ones are every second port.
        out.add(String.valueOf(serial ? boardPorts / 2 : 0));
        out.add(String.valueOf(serial ? (boardPorts + 1) / 2 : 0));
        return out;
    }

    /**
     * The memory as that program reported it: the conventional 640K, and what lies above the first megabyte as
     * extended memory, in kilobytes.
     */
    static String memory(final int totalMb) {
        if (totalMb <= 1) {
            return CONVENTIONAL_KB + "K";
        }
        return CONVENTIONAL_KB + "K, " + (totalMb * 1024 - 1024) + "K Ext";
    }

    /** The drive letters the machine sees, as that program listed them. */
    private static String drives(final List<ICliComputer.MountInfo> mounts) {
        final StringBuilder out = new StringBuilder();
        for (final ICliComputer.MountInfo mount : mounts) {
            out.append(out.isEmpty() ? "" : " ").append(Character.toUpperCase(mount.drive())).append(':');
        }
        return out.toString();
    }

    private static String first(final List<Text> names) {
        return names.isEmpty() ? "" : english(names.getFirst());
    }

    private static String english(final Text text) {
        return text.english();
    }
}
