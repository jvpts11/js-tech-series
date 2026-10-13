/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.devices.DeviceMap;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.text.TextLists;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What the Unix systems say about the machine's hardware and its ports: on Linux {@code lspci} for what sits on the
 * board and {@code lsusb} for what is plugged into its ports, {@code lsusb -t} as a tree with the hubs and the free
 * ports; on FreeBSD {@code pciconf -lv} and {@code usbconfig}. A device the computer disabled is marked so.
 *
 * <p>The words these tools print (the PCI classes, the tree's marks) are theirs and read the same in every language;
 * the names of the parts and the devices are the machine's, read through its Device Manager map.
 */
final class DeviceCommands {

    /** The mark of a device the computer disabled, as these tools would say it was unbound from its driver. */
    private static final String DISABLED = " [disabled]";

    private DeviceCommands() {
    }

    /** The device tools, each on the system that has it. */
    static List<ICliCommand> posix() {
        return List.of(new Lspci(), new Lsusb(), new Pciconf(), new Usbconfig());
    }

    /* One line of a PCI listing: where it sits, its class, and its name. */
    private record PciEntry(String slot, String className, String freebsdName, String freebsdClass, int classCode,
                            Text name) {
    }

    /* What sits on the board's bus, in the order a listing prints it. */
    private static List<PciEntry> pci(final DeviceMap map) {
        final List<PciEntry> out = new ArrayList<>();
        if (!map.board().isEmpty()) {
            out.add(new PciEntry("00:00.0", "Host bridge", "hostb0", "bridge", 0x060000, map.board()));
        }
        out.add(new PciEntry("00:14.0", controllerClass(map), controllerDriver(map) + "0", "serial bus", 0x0c0330,
                Text.literal(controllerName(map))));
        int bus = 1;
        int audio = 0;
        int network = 0;
        int video = 0;
        for (final DeviceMap.AudioSource source : map.audio()) {
            out.add(new PciEntry(source.name().equals(DeviceMap.boardAudio()) ? "00:1b.0" : slot(bus++),
                    "Audio device", "hdac" + audio++, "multimedia", 0x040300, source.name()));
        }
        for (int i = 0; i < map.network().size(); i++) {
            out.add(new PciEntry(i == 0 ? "00:19.0" : slot(bus++), i == 0 ? "Ethernet controller"
                    : "Network controller", "em" + network++, "network", 0x020000, map.network().get(i)));
        }
        for (final DeviceMap.VideoCard card : map.video()) {
            out.add(new PciEntry(slot(bus++), "VGA compatible controller", "vgapci" + video++, "display", 0x030000,
                    card.name()));
        }
        for (final Text card : map.cards()) {
            out.add(new PciEntry(slot(bus++), "Processing accelerator", "none" + (bus - 1), "processor", 0x120000,
                    card));
        }
        return out;
    }

    private static String slot(final int bus) {
        return String.format(Locale.ROOT, "%02x:00.0", bus);
    }

    /* The board's own port controller, named by the kind of port it drives and how many. */
    private static String controllerName(final DeviceMap map) {
        final String kind = switch (map.family()) {
            case SERIAL_PARALLEL -> "Serial and parallel ports";
            case USB -> "USB 2 controller";
            case USB3 -> "USB 3 controller";
        };
        return kind + ", " + map.devicePorts().size() + " ports";
    }

    private static String controllerClass(final DeviceMap map) {
        return map.family() == DeviceMap.PortFamily.SERIAL_PARALLEL ? "Communication controller" : "USB controller";
    }

    private static String controllerDriver(final DeviceMap map) {
        return switch (map.family()) {
            case SERIAL_PARALLEL -> "uart";
            case USB -> "ehci";
            case USB3 -> "xhci";
        };
    }

    /* A device's name, with the mark of one its computer disabled. */
    private static Text named(final DeviceMap.Device device) {
        return device.disabled() ? join(device.name(), Text.literal(DISABLED)) : device.name();
    }

    /* Pieces of a line one after the other: the tool's own words and the machine's names. */
    private static Text join(final Text... parts) {
        return TextLists.join("", List.of(parts));
    }

    /** The machine's map, or a complaint when there is none to read. */
    private static DeviceMap mapOf(final CliContext ctx, final String tool) {
        final DeviceMap map = ctx.computer().devices();
        if (map == null) {
            ctx.out().error(CliTexts.SAID_BY.with(tool, Texts.NO_MACHINE.text()));
        }
        return map;
    }

    @TextHolder
    static final class Texts {

        static final TextKey LSPCI = TextKey.of("jsc.cli.devices.lspci.summary", "list what sits on the board");
        static final TextKey LSUSB = TextKey.of("jsc.cli.devices.lsusb.summary", "list what is plugged into the ports");
        static final TextKey PCICONF = TextKey.of("jsc.cli.devices.pciconf.summary", "list what sits on the board");
        static final TextKey USBCONFIG = TextKey.of("jsc.cli.devices.usbconfig.summary",
                "list what is plugged into the ports");
        static final TextKey NO_MACHINE = TextKey.of("jsc.cli.devices.no_machine", "no hardware to read");
        static final TextKey BAD_OPTION = TextKey.of("jsc.cli.devices.bad_option", "invalid option -- '%s'");

        private Texts() {
        }
    }

    static final class Lspci implements ICliCommand {

        @Override public CommandScope scope() {
            return CommandScope.on(Platform.LINUX);
        }

        @Override public String name() { return "lspci"; }

        @Override public CommandGroup group() { return CommandGroup.MACHINE; }

        @Override public Text summary() { return Texts.LSPCI.text(); }

        @Override public void run(final CliContext ctx) {
            if (ctx.argCount() > 0) {
                ctx.out().error(CliTexts.SAID_BY.with(name(), Texts.BAD_OPTION.with(ctx.arg(0))));
                return;
            }
            final DeviceMap map = mapOf(ctx, name());
            if (map == null) {
                return;
            }
            for (final PciEntry entry : pci(map)) {
                ctx.out().line(join(Text.literal(entry.slot() + " " + entry.className() + ": "), entry.name()));
            }
        }
    }

    static final class Lsusb implements ICliCommand {

        @Override public CommandScope scope() {
            return CommandScope.on(Platform.LINUX);
        }

        @Override public String name() { return "lsusb"; }

        @Override public CommandGroup group() { return CommandGroup.MACHINE; }

        @Override public Text summary() { return Texts.LSUSB.text(); }

        /** Nothing but a switch, written as the tool takes it. */
        @Override public Text usage() { return Text.literal("[-t]"); }

        @Override public void run(final CliContext ctx) {
            final boolean tree = ctx.argCount() == 1 && ctx.arg(0).equals("-t");
            if (ctx.argCount() > 0 && !tree) {
                ctx.out().error(CliTexts.SAID_BY.with(name(), Texts.BAD_OPTION.with(ctx.arg(0))));
                return;
            }
            final DeviceMap map = mapOf(ctx, name());
            // A board with no USB has no bus to list, and the tool says nothing, as it never did.
            if (map == null || map.family() == DeviceMap.PortFamily.SERIAL_PARALLEL) {
                return;
            }
            if (tree) {
                ctx.out().line("/:  Bus 01.Port 1: " + controllerName(map));
                treeOf(ctx, map.devicePorts(), "    ");
                return;
            }
            ctx.out().line("Bus 001 Device 001: " + controllerName(map));
            int number = 2;
            for (final DeviceMap.Device device : portDevices(map.devicePorts())) {
                ctx.out().line(join(Text.literal(String.format(Locale.ROOT, "Bus 001 Device %03d: ", number++)),
                        named(device)));
            }
        }

        private static void treeOf(final CliContext ctx, final List<DeviceMap.Port> ports, final String indent) {
            for (int i = 0; i < ports.size(); i++) {
                final DeviceMap.Port port = ports.get(i);
                final String head = indent + "|__ Port " + (i + 1) + ": ";
                if (port.isFree()) {
                    ctx.out().line(head + "(free)");
                    continue;
                }
                final DeviceMap.Device device = port.plugged().getFirst();
                ctx.out().line(join(Text.literal(head), named(device)));
                treeOf(ctx, device.ports(), indent + "    ");
            }
        }
    }

    static final class Pciconf implements ICliCommand {

        @Override public CommandScope scope() {
            return CommandScope.on(Platform.FREEBSD);
        }

        @Override public String name() { return "pciconf"; }

        @Override public CommandGroup group() { return CommandGroup.MACHINE; }

        @Override public Text summary() { return Texts.PCICONF.text(); }

        /** Nothing but switches, written as the tool takes them. */
        @Override public Text usage() { return Text.literal("-l [-v]"); }

        @Override public void run(final CliContext ctx) {
            final boolean listed = ctx.argCount() == 1 && (ctx.arg(0).equals("-l") || ctx.arg(0).equals("-lv"));
            if (!listed) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            final DeviceMap map = mapOf(ctx, name());
            if (map == null) {
                return;
            }
            final boolean verbose = ctx.arg(0).equals("-lv");
            for (final PciEntry entry : pci(map)) {
                final String[] parts = entry.slot().split("[:.]");
                ctx.out().line(String.format(Locale.ROOT, "%s@pci0:%d:%d:%d:  class=0x%06x", entry.freebsdName(),
                        Integer.parseInt(parts[0], 16), Integer.parseInt(parts[1], 16), Integer.parseInt(parts[2]),
                        entry.classCode()));
                if (verbose) {
                    ctx.out().line(join(Text.literal("    device   = '"), entry.name(), Text.literal("'")));
                    ctx.out().line("    class    = " + entry.freebsdClass());
                }
            }
        }
    }

    static final class Usbconfig implements ICliCommand {

        @Override public CommandScope scope() {
            return CommandScope.on(Platform.FREEBSD);
        }

        @Override public String name() { return "usbconfig"; }

        @Override public CommandGroup group() { return CommandGroup.MACHINE; }

        @Override public Text summary() { return Texts.USBCONFIG.text(); }

        @Override public void run(final CliContext ctx) {
            if (ctx.argCount() > 0) {
                ctx.out().error(CliTexts.SAID_BY.with(name(), Texts.BAD_OPTION.with(ctx.arg(0))));
                return;
            }
            final DeviceMap map = mapOf(ctx, name());
            if (map == null || map.family() == DeviceMap.PortFamily.SERIAL_PARALLEL) {
                return;
            }
            ctx.out().line("ugen0.1: <" + controllerName(map) + "> at usbus0, cfg=0 md=HOST");
            int number = 2;
            for (final DeviceMap.Device device : portDevices(map.devicePorts())) {
                // A device set to configuration 255 is the one the system let go of: disabled.
                ctx.out().line(join(Text.literal(String.format(Locale.ROOT, "ugen0.%d: <", number++)),
                        device.name(), Text.literal("> at usbus0, cfg=" + (device.disabled() ? 255 : 0) + " md=HOST")));
            }
        }
    }

    /* Every device on the ports, hubs and what hangs from them in turn, in the order the ports run. */
    private static List<DeviceMap.Device> portDevices(final List<DeviceMap.Port> ports) {
        final List<DeviceMap.Device> out = new ArrayList<>();
        for (final DeviceMap.Port port : ports) {
            for (final DeviceMap.Device device : port.plugged()) {
                out.add(device);
                out.addAll(portDevices(device.ports()));
            }
        }
        return out;
    }
}
