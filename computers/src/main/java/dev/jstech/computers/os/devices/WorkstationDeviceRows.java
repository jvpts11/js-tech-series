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

/**
 * A {@link DeviceMap} as CDE's Workstation Info lists it in its Devices group: a fact per port, labelled as the
 * workstations of the day labelled them (Video, Audio, then the serial and parallel ports by their names), with the
 * card or the sound source and what is plugged into it, or "free". A hub's own ports follow the hub.
 *
 * <p>Pure, with no Minecraft types, so the group's facts are unit-tested.
 */
@TextHolder
public final class WorkstationDeviceRows {

    private static final TextKey VIDEO = TextKey.of("jsc.workstation_info.devices.video", "Video");
    private static final TextKey AUDIO = TextKey.of("jsc.workstation_info.devices.audio", "Audio");
    private static final TextKey FREE = TextKey.of("jsc.workstation_info.devices.free", "free");
    private static final TextKey ON = TextKey.of("jsc.workstation_info.devices.on", "%s: %s");
    private static final TextKey OUTPUT_OF = TextKey.of("jsc.workstation_info.devices.output_of", "%s %s");

    private WorkstationDeviceRows() {
    }

    /**
     * One fact of the group.
     *
     * @param label    what the fact is: Video, Audio, or the port's name
     * @param value    the card or source and what is plugged into it, or what is plugged into the port, or "free"
     * @param free     whether nothing is plugged in
     * @param disabled whether what is plugged in is disabled
     * @param pos      the device it stands for, packed, or {@link DeviceRows#NO_DEVICE}
     */
    public record Row(Text label, Text value, boolean free, boolean disabled, long pos) {

        /** Whether it stands for a device, which is what Disable and Enable work on. */
        public boolean isDevice() {
            return pos != DeviceRows.NO_DEVICE;
        }
    }

    /** The facts of the group for {@code map}, in the order they are read. */
    public static List<Row> rows(final DeviceMap map) {
        final List<Row> out = new ArrayList<>();
        for (final DeviceMap.VideoCard card : map.video()) {
            for (final DeviceMap.Port output : card.outputs()) {
                out.add(row(VIDEO.text(), card.outputs().size() == 1 ? card.name()
                        : OUTPUT_OF.with(card.name(), output.name()), output));
            }
        }
        for (final DeviceMap.AudioSource source : map.audio()) {
            for (final DeviceMap.Port output : source.outputs()) {
                out.add(row(AUDIO.text(), source.outputs().size() == 1 ? source.name()
                        : OUTPUT_OF.with(source.name(), output.name()), output));
            }
        }
        final boolean serial = map.family() == DeviceMap.PortFamily.SERIAL_PARALLEL;
        for (int i = 0; i < map.devicePorts().size(); i++) {
            // A Vintage board's ports by the names its system gave them: COM1, LPT1, COM2, LPT2.
            final Text name = serial ? Text.literal((i % 2 == 0 ? "COM" : "LPT") + (i / 2 + 1))
                    : map.devicePorts().get(i).name();
            port(name, map.devicePorts().get(i), out);
        }
        return out;
    }

    /* A card's or a source's output: the card's name, and what is plugged into the output after it. */
    private static Row row(final Text label, final Text of, final DeviceMap.Port output) {
        if (output.isFree()) {
            return new Row(label, ON.with(of, FREE.text()), true, false, DeviceRows.NO_DEVICE);
        }
        final DeviceMap.Device first = output.plugged().getFirst();
        final List<Text> names = output.plugged().stream().map(DeviceMap.Device::name).toList();
        return new Row(label, ON.with(of, TextLists.join(", ", names)), false, first.disabled(), first.pos());
    }

    /* A device port with what is plugged into it, and a hub's own ports after it. */
    private static void port(final Text name, final DeviceMap.Port port, final List<Row> out) {
        if (port.isFree()) {
            out.add(new Row(name, FREE.text(), true, false, DeviceRows.NO_DEVICE));
            return;
        }
        final DeviceMap.Device device = port.plugged().getFirst();
        out.add(new Row(name, device.name(), false, device.disabled(), device.pos()));
        for (final DeviceMap.Port hubPort : device.ports()) {
            port(hubPort.name(), hubPort, out);
        }
    }
}
