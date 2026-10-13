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

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link DeviceMap} as KDE's Info Center lists it on its "Devices by port" page: a heading per graphics card with
 * its outputs under it, the audio with its sources, and the board's device ports, each port beside what is plugged
 * into it or "free". A hub is one row with a line under it saying how many of its own ports are in use.
 *
 * <p>Pure, with no Minecraft types, so the page's rows are unit-tested.
 */
@TextHolder
public final class InfoCenterRows {

    private static final TextKey AUDIO = TextKey.of("jsc.info_center.audio", "Audio");
    private static final TextKey USB = TextKey.of("jsc.info_center.usb", "USB");
    private static final TextKey SERIAL_PARALLEL = TextKey.of("jsc.info_center.serial_parallel",
            "Serial and parallel");
    private static final TextKey FREE = TextKey.of("jsc.info_center.free", "free");
    private static final TextKey ON_THE_HUB = TextKey.of("jsc.info_center.on_the_hub", "on the hub");
    private static final TextKey IN_USE = TextKey.of("jsc.info_center.in_use", "%s of %s in use");
    private static final TextKey SOURCE_OUTPUT = TextKey.of("jsc.info_center.source_output", "%s, %s");

    private InfoCenterRows() {
    }

    /** What a row of the page is: a heading, a port with what is on it, or the count under a hub. */
    public enum Kind {
        HEADING, PORT, HUB_COUNT
    }

    /**
     * One row of the page.
     *
     * @param kind       what the row is
     * @param icon       the kind the heading's or the port's icon is drawn by, empty for none
     * @param deviceIcon the kind the icon beside what is plugged in is drawn by, empty for none
     * @param port       the heading's words, the port's name, or "on the hub"
     * @param device     what is plugged into the port, "free", or the hub's count; empty on a heading
     * @param free       whether the port has nothing plugged into it
     * @param disabled   whether what is plugged into it is disabled
     * @param pos        the device it stands for, packed, or {@link DeviceRows#NO_DEVICE}
     */
    public record Row(Kind kind, String icon, String deviceIcon, Text port, Text device, boolean free,
                      boolean disabled, long pos) {

        /** Whether it stands for a device, which is what can be selected and disabled. */
        public boolean isDevice() {
            return pos != DeviceRows.NO_DEVICE;
        }
    }

    /** The rows of the page for {@code map}, in the order they are read. */
    public static List<Row> rows(final DeviceMap map) {
        final List<Row> out = new ArrayList<>();
        for (final DeviceMap.VideoCard card : map.video()) {
            out.add(heading("gpu", card.name()));
            for (final DeviceMap.Port output : card.outputs()) {
                out.add(port("video_port", output.name(), output));
            }
        }
        if (!map.audio().isEmpty()) {
            out.add(heading("sound_card", AUDIO.text()));
            for (final DeviceMap.AudioSource source : map.audio()) {
                for (final DeviceMap.Port output : source.outputs()) {
                    final Text name = source.outputs().size() == 1 ? source.name()
                            : SOURCE_OUTPUT.with(source.name(), output.name());
                    out.add(port("audio_jack", name, output));
                }
            }
        }
        if (!map.devicePorts().isEmpty()) {
            final DeviceMap.PortFamily family = map.family();
            out.add(heading(family.icon(0),
                    (family == DeviceMap.PortFamily.SERIAL_PARALLEL ? SERIAL_PARALLEL : USB).text()));
            for (int i = 0; i < map.devicePorts().size(); i++) {
                final DeviceMap.Port port = map.devicePorts().get(i);
                out.add(port(family.icon(i), port.name(), port));
                if (!port.isFree() && port.plugged().getFirst().isHub()) {
                    final List<DeviceMap.Port> hubPorts = port.plugged().getFirst().ports();
                    final long used = hubPorts.stream().filter(hubPort -> !hubPort.isFree()).count();
                    out.add(new Row(Kind.HUB_COUNT, "", "", ON_THE_HUB.text(), IN_USE.with(used, hubPorts.size()),
                            false, false, DeviceRows.NO_DEVICE));
                }
            }
        }
        return out;
    }

    private static Row heading(final String icon, final Text words) {
        return new Row(Kind.HEADING, icon, "", words, Text.EMPTY, false, false, DeviceRows.NO_DEVICE);
    }

    /* A port beside what is plugged into it; the row stands for the first device on it. */
    private static Row port(final String portIcon, final Text name, final DeviceMap.Port port) {
        if (port.isFree()) {
            return new Row(Kind.PORT, portIcon, "", name, FREE.text(), true, false, DeviceRows.NO_DEVICE);
        }
        final DeviceMap.Device first = port.plugged().getFirst();
        return new Row(Kind.PORT, portIcon, first.icon(), name, port.pluggedNames(), false,
                first.disabled(), first.pos());
    }
}
